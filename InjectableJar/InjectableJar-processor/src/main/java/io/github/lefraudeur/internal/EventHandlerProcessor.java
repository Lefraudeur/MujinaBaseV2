package io.github.lefraudeur.internal;

import io.github.lefraudeur.internal.patcher.*;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.extras.MappingTreeRemapper;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes({"io.github.lefraudeur.internal.EventHandler"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({"remapper.mappingFilePath", "remapper.sourceNamespace", "remapper.destinationNamespace"})
public class EventHandlerProcessor extends AbstractProcessor
{
    private Messager messager = null;
    private Elements elements = null;
    private Map<String, ClassModifier> classModifierMap = new HashMap<>();
    private MappingTreeRemapper treeRemapper = null;

    @Override
    public void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();

        Map<String, String> options = processingEnv.getOptions();

        String mappingFilePath = options.get("remapper.mappingFilePath");
        if (mappingFilePath == null)
            throw new RuntimeException("invalid remapper.mappingFilePath");
        String from = options.get("remapper.sourceNamespace");
        if (from == null)
            throw new RuntimeException("invalid remapper.sourceNamespace");
        String to = options.get("remapper.destinationNamespace");
        if (to == null)
            throw new RuntimeException("invalid remapper.destinationNamespace");

        VisitableMappingTree tree = new MemoryMappingTree();
        try
        {
            MappingReader.read(Paths.get(mappingFilePath), MappingFormat.TINY_2_FILE, tree);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        treeRemapper = new MappingTreeRemapper(tree, from, to);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        processRound(annotations, roundEnv);

        if (roundEnv.processingOver())
            writePatcherClass(roundEnv);

        return false;
    }

    private void processRound(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if (annotations.isEmpty()) return;

        if (!annotations.contains(elements.getTypeElement(EventHandler.class.getCanonicalName()))) return;

        messager.printMessage(Diagnostic.Kind.WARNING, "hello from annotation processor");
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(EventHandler.class);
        for (Element element : annotatedElements)
        {
            if (!(element instanceof ExecutableElement) || element.getKind() != ElementKind.METHOD
                    || !element.getModifiers().contains(Modifier.STATIC))
            {
                messager.printMessage(Diagnostic.Kind.WARNING, "Skipping @EventHandler on: " + element.getSimpleName() + "as it is not a static method");
                continue;
            }

            ExecutableElement eventHandler = (ExecutableElement)element;
            EventHandler annotation = eventHandler.getAnnotation(EventHandler.class);

            String executableElementDescriptor = getExecutableElementDescriptor(eventHandler);
            checkAnnotationValidity(annotation, executableElementDescriptor);
            String remappedExecutableElementDescriptor = treeRemapper.mapMethodDesc(executableElementDescriptor);

            String eventHandlerClassName = ((TypeElement)eventHandler.getEnclosingElement()).getQualifiedName().toString();
            eventHandlerClassName = eventHandlerClassName.replace('.', '/');
            String eventHandlerMethodName = eventHandler.getSimpleName().toString();
            String remappedTargetClass = treeRemapper.map(annotation.targetClass());
            String remappedTargetMethod = treeRemapper.mapMethodName(annotation.targetClass(), annotation.targetMethodName(), annotation.targetMethodDescriptor());
            String remappedTargetMethodDescriptor = treeRemapper.mapMethodDesc(annotation.targetMethodDescriptor());

            ClassModifier classModifier = classModifierMap.computeIfAbsent(annotation.targetClass(), k -> new ClassModifier(remappedTargetClass));

            MethodModifier.MethodModifierInfo info = new MethodModifier.MethodModifierInfo(
                    remappedTargetMethod, remappedTargetMethodDescriptor,
                    eventHandlerClassName, eventHandlerMethodName, remappedExecutableElementDescriptor,
                    annotation.targetMethodIsStatic());

            switch (annotation.type())
            {
                // Triggered at the very first instruction of the target method.
                // Allows for argument inspection and method cancellation via a 'Canceler' object.
                case ON_ENTRY:
                    classModifier.addModifier(new EntryMethodModifier(info));
                    break;
                // Triggered before any RETURN or ATHROW instruction.
                // Captures the return value (if any) or the exception being thrown for modification.
                case ON_RETURN_THROW:
                    classModifier.addModifier(new ReturnThrowMethodModifier(info));
                    break;
                // Triggered when an LDC (Load Constant) instruction is encountered in the bytecode.
    // Replaces hardcoded values (Strings, Doubles, Integers) by passing them through the handler.
                case ON_LDC_CONSTANT:
                    classModifier.addModifier(new LDCConstantModifier(info));
                    break;
            }
        }
    }

    private void writePatcherClass(RoundEnvironment roundEnv)
    {
        StringBuilder builder = new StringBuilder();
        ClassModifier[] classModifiers = classModifierMap.values().toArray(new ClassModifier[0]);
        for (int i = 0; i < classModifiers.length; ++i)
        {
            builder.append("\t\t");
            builder.append(classModifiers[i].getNewInstanceCode());
            if (i == classModifiers.length - 1) break;
            builder.append(",\n");
        }
        try {
            JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile("io.github.lefraudeur.internal.patcher.Patcher");
            try (OutputStreamWriter writer = new OutputStreamWriter(javaFileObject.openOutputStream(), StandardCharsets.UTF_8))
            {
                writer.write
                (
                "// generated by InjectableJar-processor\n"
                + "// once injected, classModifiers is read by the c++ Transformer\n"
                + "package io.github.lefraudeur.internal.patcher;\n"
                + "import io.github.lefraudeur.internal.patcher.MethodModifier.MethodModifierInfo;\n\n"
                + "public class Patcher\n"
                + "{\n"
                + "\tpublic final static ClassModifier[] classModifiers = \n"
                + "\t{\n"
                + builder + "\n"
                + "\t};\n"
                + "}"
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAnnotationValidity(EventHandler annotation, String executableElementDescriptor)
    {
        try
        {
            String expectedDescriptor = getExpectedDescriptor(annotation);
            if (!executableElementDescriptor.equals(expectedDescriptor))
                throw new RuntimeException("EventHandler annotation and expected handler descriptor mismatch\n"+
                "got: " + executableElementDescriptor +
                "\nexpected: " + expectedDescriptor);
        }
        catch (Exception t)
        {
            throw new RuntimeException(t + "\nInvalid event handler: " + executableElementDescriptor);
        }
    }

    private String getExpectedDescriptor(EventHandler annotation) throws Exception
    {
        if (annotation.type() == MethodModifier.Type.ON_LDC_CONSTANT)
            return "(Ljava/lang/Object;)Ljava/lang/Object;";

        if (annotation.type() == MethodModifier.Type.ON_ENTRY || annotation.type() == MethodModifier.Type.ON_RETURN_THROW)
        {
            MethodType targetMethodType = MethodType.fromMethodDescriptorString(annotation.targetMethodDescriptor(), EventHandlerProcessor.class.getClassLoader());
            Class<?> returnType = targetMethodType.returnType();

            if (!annotation.targetMethodIsStatic())
                targetMethodType = targetMethodType.insertParameterTypes(0, Class.forName(annotation.targetClass().replace('/', '.')));

            if (annotation.type() == MethodModifier.Type.ON_ENTRY)
                targetMethodType = targetMethodType.insertParameterTypes(0, Canceler.class);

            if (annotation.type() == MethodModifier.Type.ON_RETURN_THROW)
            {
                targetMethodType = targetMethodType.insertParameterTypes(0, Thrower.class);
                if (returnType != void.class)
                    targetMethodType = targetMethodType.insertParameterTypes(0, returnType);
            }

            return targetMethodType.toMethodDescriptorString();
        }
        throw new UnsupportedOperationException("Expected Descriptor Not Implemented for type: " + annotation.type());
    }

    private String getExecutableElementDescriptor(ExecutableElement element)
    {
        StringBuilder descriptor = new StringBuilder("(");
        for (VariableElement e : element.getParameters())
        {
            TypeMirror type = e.asType();
            descriptor.append(getTypeMirrorDescriptor(type));
        }
        descriptor.append(")");
        descriptor.append(getTypeMirrorDescriptor(element.getReturnType()));
        return descriptor.toString();
    }

    private String getTypeMirrorDescriptor(TypeMirror type)
    {
        switch (type.getKind())
        {
            case DECLARED:
                DeclaredType declaredType = (DeclaredType)type;
                Name binaryName = elements.getBinaryName((TypeElement)declaredType.asElement());
                return "L" + removeTypeParameter(binaryName.toString()).replace('.', '/') + ";";
            case VOID:
                return "V";
            case BOOLEAN:
                return "Z";
            case BYTE:
                return "B";
            case CHAR:
                return "C";
            case SHORT:
                return "S";
            case INT:
                return "I";
            case LONG:
                return "J";
            case FLOAT:
                return "F";
            case DOUBLE:
                return "D";
            case TYPEVAR:
                return "Ljava/lang/Object;";
            case ARRAY:
                return "[" + getTypeMirrorDescriptor(((ArrayType)type).getComponentType());
            default:
                throw new IllegalArgumentException("unsupported TypeMirror: " + type.getKind());
        }
    }

    private String removeTypeParameter(String generic)
    {
        int firstIndex = generic.indexOf('<');
        if (firstIndex == -1) return generic;
        int lastIndex = generic.lastIndexOf('>');
        StringBuilder buff = new StringBuilder(generic);
        buff.delete(firstIndex, lastIndex + 1);
        return buff.toString();
    }
}
