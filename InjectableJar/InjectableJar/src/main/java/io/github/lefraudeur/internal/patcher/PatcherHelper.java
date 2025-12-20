package io.github.lefraudeur.internal.patcher;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.lefraudeur.internal.Canceler;
import io.github.lefraudeur.internal.Thrower;
import net.minecraft.client.Minecraft;

// bunch of static methods called by c++ transformer
public class PatcherHelper
{
    private static Class<?>[] classesToTransform = null;
    private static Map<Class<?>, ClassModifier> classModifierMap = new HashMap<>();

    public static boolean init()
    {
        List<Class<?>> classes = new ArrayList<>();
        for (ClassModifier classModifier : io.github.lefraudeur.internal.patcher.Patcher.classModifiers)
        {
            try
            {
                Class<?> classModifierClass = PatcherHelper.class.getClassLoader().loadClass(classModifier.name.replace('/', '.'));
                classes.add(classModifierClass);
                classModifierMap.put(classModifierClass, classModifier);
            } catch (ClassNotFoundException e)
            {
                return false;
            }
        }
        classesToTransform = classes.toArray(new Class<?>[0]);

        Set<String> excludeSet = getLunarClassLoaderExcludeSet();
        if (excludeSet != null)
            excludeSet.addAll(getEventHandlerClassNames());

        return true;
    }

    public static Class<?>[] getClassesToTransform()
    {
        return classesToTransform;
    }

    public static ClassModifier getClassModifier(Class<?> classToModify)
    {
        return classModifierMap.get(classToModify);
    }

    // This is used for lunar client trick, which consists of adding these classes to the ones excluded by the genesis classLoader
    private static Set<String> getEventHandlerClassNames()
    {
        Set<String> excluded = new HashSet<>(Arrays.asList(Canceler.class.getName(), Thrower.class.getName()));

        for (ClassModifier classModifier : io.github.lefraudeur.internal.patcher.Patcher.classModifiers)
            excluded.addAll(classModifier.getEventHandlerClassesNames());

        return excluded;
    }

    // returns null if not on lunar
    private static Set<String> getLunarClassLoaderExcludeSet()
    {
        ClassLoader lunarClassLoader = Minecraft.class.getClassLoader();
        Class<?> lunarClassLoaderClass = lunarClassLoader.getClass();

        for (Field field : lunarClassLoaderClass.getDeclaredFields())
        {
            if (field.toGenericString().startsWith("private java.util.Set<java.lang.String> com.moonsworth.lunar.genesis."))
            {
                field.setAccessible(true);
                try {
                    return (Set<String>)field.get(lunarClassLoader);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }
}
