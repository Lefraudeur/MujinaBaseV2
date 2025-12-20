@echo off
java -jar remapper\tiny-remapper-0.9.0-fat.jar target\InjectableJar-1.0-SNAPSHOT-shaded.jar remapped\InjectableJar_int.jar remapper\1.8.9\1.8.9.tiny named searge remapper\libs\1.8.9 remapper\1.8.9\client_named.jar

java -jar remapper\tiny-remapper-0.9.0-fat.jar remapped\InjectableJar_int.jar remapped\InjectableJar.jar remapper\1.8.9\1.8.9.tiny searge obfuscated remapper\libs\1.8.9 remapper\1.8.9\client_searge.jar

del remapped\InjectableJar_int.jar