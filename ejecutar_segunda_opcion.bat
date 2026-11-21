@echo off
REM ============================================================
REM  Compila el codigo fuente (src) y ejecuta el simulador.
REM  Necesita el JDK (javac). Si no hay JDK, abre el JAR ya compilado.
REM ============================================================
setlocal
cd /d "%~dp0"
title Compilar Simulador de la Jerarquia de Chomsky

call :buscar_javac
if not defined JDKBIN (
  echo No se encontro javac ^(JDK^). Se abrira la version ya compilada.
  call "%~dp0ejecutar.bat"
  exit /b
)

echo Usando JDK: "%JDKBIN%"
if not exist out mkdir out
"%JDKBIN%\javac.exe" -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
  echo.
  echo Error de compilacion. Revise el mensaje de arriba.
  pause
  exit /b 1
)
"%JDKBIN%\java.exe" -cp out SimuladorChomsky
exit /b

REM ---- Busca la carpeta bin de un JDK (la que tiene javac.exe) ----
:buscar_javac
set "JDKBIN="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JDKBIN=%JAVA_HOME%\bin"
if defined JDKBIN exit /b
for /f "delims=" %%J in ('where javac 2^>nul') do if not defined JDKBIN set "JDKBIN=%%~dpJ"
if defined JDKBIN (
  set "JDKBIN=%JDKBIN:~0,-1%"
  exit /b
)
for /d %%D in ("%USERPROFILE%\.jdks\*" "%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%ProgramFiles%\Microsoft\*" "%ProgramFiles%\Zulu\*" "%ProgramFiles%\Amazon Corretto\*") do if not defined JDKBIN if exist "%%~D\bin\javac.exe" set "JDKBIN=%%~D\bin"
exit /b
