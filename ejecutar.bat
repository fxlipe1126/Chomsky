@echo off
REM ============================================================
REM  Abre el Simulador de la Jerarquia de Chomsky (no compila).
REM  Solo necesita Java 8 o superior (JRE o JDK).
REM ============================================================
setlocal
cd /d "%~dp0"
title Simulador de la Jerarquia de Chomsky

call :buscar_java
if not defined JAVA goto sin_java

echo Usando Java: "%JAVA%"
"%JAVA%" -jar "dist\SimuladorChomsky.jar"
if errorlevel 1 (
  echo.
  echo No se pudo abrir el simulador. Revise el mensaje de arriba.
  pause
)
exit /b

:sin_java
echo.
echo No se encontro Java en este computador.
echo Instale un JDK, por ejemplo Eclipse Temurin 21: https://adoptium.net
echo En el instalador active "Set JAVA_HOME variable" y "Add to PATH".
echo Luego vuelva a ejecutar este archivo.
echo.
pause
exit /b 1

REM ---- Busca java.exe: JAVA_HOME, PATH y carpetas de instalacion comunes ----
:buscar_java
set "JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"
if defined JAVA exit /b
for /f "delims=" %%J in ('where java 2^>nul') do if not defined JAVA set "JAVA=%%J"
if defined JAVA exit /b
for /d %%D in ("%USERPROFILE%\.jdks\*" "%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%ProgramFiles%\Microsoft\*" "%ProgramFiles%\Zulu\*" "%ProgramFiles%\Amazon Corretto\*") do if not defined JAVA if exist "%%~D\bin\java.exe" set "JAVA=%%~D\bin\java.exe"
if defined JAVA exit /b
REM Java que traen IntelliJ IDEA y Android Studio (carpeta jbr)
for /d %%D in ("%ProgramFiles%\JetBrains\*" "%LOCALAPPDATA%\Programs\*" "%ProgramFiles%\Android\*") do if not defined JAVA if exist "%%~D\jbr\bin\java.exe" set "JAVA=%%~D\jbr\bin\java.exe"
exit /b
