@echo off

echo Setting up environment
CALL "%~dp0\env.bat"

:start

echo.
echo.
echo Cleaning
del src\InjectableJar.jar.hpp
del InjectableJar\InjectableJar\remapped\InjectableJar.jar
del InjectableJar\InjectableJar\remapped\InjectableJar.jar.hpp
cd InjectableJar
CALL mvn clean


echo.
echo.
echo Building jar
CALL mvn package -Dremapper.destinationNamespace=obfuscated

echo.
echo.
echo Remapping Jar
cd InjectableJar
CALL remap_vanilla.bat
cd remapped

echo.
echo.
echo Writing jar bytes to header file
ignore_File2Hex.exe
copy InjectableJar.jar.hpp ..\..\..\src

echo.
echo.
echo Building dll
cd ..\..\..\
cmake -B build -G "NMake Makefiles" -DCMAKE_BUILD_TYPE=RelWithDebInfo -DMINECRAFT_CLASS="ave" --fresh
cmake --build build

echo.
echo.
echo Press enter to rebuild
pause > nul

goto start