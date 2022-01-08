@echo off
REM -----------------------------------------------------------------------------
REM Shutdown Script for GeoServer
REM -----------------------------------------------------------------------------


cls
echo Shutting down GeoServer...
echo.
set error=0

rem JAVA_HOME not defined
if "%JAVA_HOME%" == "" goto trySystemJava

rem JAVA_HOME defined incorrectly
if not exist "%JAVA_HOME%\bin\java.exe" goto badJava

rem Setup the java command and move on
set RUN_JAVA=%JAVA_HOME%\bin\java
echo JAVA_HOME: %JAVA_HOME%
echo.

:checkGeoServerHome
rem GEOSERVER_HOME not defined
if "%GEOSERVER_HOME%" == "" goto noHome

rem GEOSERVER_HOME defined incorrectly
if not exist "%GEOSERVER_HOME%\bin\startup.bat" goto badHome

rem No errors
goto shutdown

:trySystemJava
  echo The JAVA_HOME environment variable is not defined, trying to use System Java
for /f %%i in ('where java') do set RUN_JAVA=%%i
rem --- we might be on amd64 having only x86 jre installed ---
if "%RUN_JAVA%"=="" if DEFINED ProgramFiles(x86) if NOT "%PROCESSOR_ARCHITECTURE%"=="x86" (
    rem --- restart the batch in x86 mode---
    echo Warning: No java interpreter found in path.
    echo Retry using Wow64 filesystem [32bit environment] redirection.
    %SystemRoot%\SysWOW64\cmd.exe /c %0 %*
    exit /b %ERRORLEVEL%
  )
if "%RUN_JAVA%"=="" goto noJava
  echo Using System Java at:
  echo    %RUN_JAVA%
  echo.
goto checkGeoServerHome

:noJava
  echo The JAVA_HOME environment variable is not defined.
goto JavaFail

:badJava
  echo The JAVA_HOME environment variable is not defined correctly.
goto JavaFail

:JavaFail
  echo This environment variable is needed to run this program.
  echo.
  echo Set this environment variable via the following command:
  echo    set JAVA_HOME=[path to Java]
  echo Example:
  echo    set JAVA_HOME=C:\Program Files\Java\jdk6
  echo.
  set error=1
goto end


:noHome
  if exist ..\start.jar goto noHomeOK
  echo The GEOSERVER_HOME environment variable is not defined.
goto HomeFail

:badHome
  if exist ..\start.jar goto badHomeOK
  echo The GEOSERVER_HOME environment variable is not defined correctly.
goto HomeFail

:HomeFail
  echo This environment variable is needed to run this program.
  echo.
  echo Set this environment variable via the following command:
  echo    set GEOSERVER_HOME=[path to GeoServer]
  echo Example:
  echo    set GEOSERVER_HOME=C:\Program Files\GeoServer
  echo.
  set error=1
goto end


:noHomeOK
  echo The GEOSERVER_HOME environment variable is not defined.
goto setHome

:badHomeOK
  echo The GEOSERVER_HOME environment variable is not defined correctly.
goto setHome

:setHome
  echo Temporarily setting GEOSERVER_HOME to the following directory:
  cd ..
  set GEOSERVER_HOME=%CD%
  echo %GEOSERVER_HOME%
  echo.
goto shutdown

:shutdown

  cd "%GEOSERVER_HOME%"
  "%RUN_JAVA%" -DSTOP.PORT=8079 -DSTOP.KEY=geoserver -jar start.jar --stop
  cd bin
goto end

:end
  if %error% == 1 echo Shutting down GeoServer was unsuccessful. 
  echo.
  pause
























