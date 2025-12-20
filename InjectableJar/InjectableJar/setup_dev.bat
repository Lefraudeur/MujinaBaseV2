@echo off
echo Generating client_searge.jar
java -jar remapper\tiny-remapper-0.9.0-fat.jar remapper\1.8.9\client.jar remapper\1.8.9\client_searge.jar remapper\1.8.9\1.8.9.tiny obfuscated searge remapper\libs\1.8.9

echo Generating client_named.jar...
java -jar remapper\tiny-remapper-0.9.0-fat.jar remapper\1.8.9\client_searge.jar remapper\1.8.9\client_named.jar remapper\1.8.9\1.8.9.tiny searge named remapper\libs\1.8.9

echo.
echo Installing client_named.jar to local maven repo...
call mvn install:install-file -Dfile="remapper/1.8.9/client_named.jar" -DgroupId=minecraft.client -DartifactId=named -Dversion=1.8.9 -Dpackaging=jar -DlocalRepositoryPath="local_maven_repo"

echo.
echo Setup complete. You can now build the project.
pause
