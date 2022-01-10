@echo off
rem -----------------------------------------------------------------------------
rem Startup Script for GeoServer
rem -----------------------------------------------------------------------------

cls
echo Welcome to GeoServer!
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

goto checkDataDir

:trySystemJava
  echo The JAVA_HOME environment variable is not defined, trying to use System Java
for /f "tokens=*" %%i in ('where java') do set RUN_JAVA=%%i
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
  echo The JAVA_HOME environment variable is not defined, and no Java executable could be found.
goto JavaFail

:badJava
  echo The JAVA_HOME environment variable is not defined correctly.
goto JavaFail

:JavaFail
  echo Please install Java or, if present but not in the path, set this environment variable via the following command:
  echo    set JAVA_HOME=[path to Java]
  echo Example:
  echo    set JAVA_HOME=C:\Program Files\Java\jdk8
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
goto checkDataDir


:checkDataDir
  rem GEOSERVER_DATA_DIR not defined
  if "%GEOSERVER_DATA_DIR%" == "" goto noDataDir
  goto setMarlinRenderer

:noDataDir
  rem if GEOSERVER_DATA_DIR is not defined then use GEOSERVER_HOME/data_dir/
  if exist "%GEOSERVER_HOME%\data_dir" goto setDataDir
  echo No valid GeoServer data directory could be located.
  echo Please set the GEOSERVER_DATA_DIR environment variable.
  echo.
  echo Set this environment variable via the following command:
  echo    set GEOSERVER_DATA_DIR=[path to data_dir]
  echo Example:
  echo    set GEOSERVER_DATA_DIR=C:\Program Files\GeoServer\data_dir
  echo.
  set error=1
goto end

:setDataDir
  set GEOSERVER_DATA_DIR=%GEOSERVER_HOME%\data_dir
  echo The GEOSERVER_DATA_DIR environment variable is not defined correctly.
  echo Temporarily setting GEOSERVER_DATA_DIR to the following directory:
  echo %GEOSERVER_DATA_DIR%
  echo.
goto setMarlinRenderer

:setMarlinRenderer
  cd "%GEOSERVER_HOME%"
  for /f "delims=" %%i in ('dir /b/s "%GEOSERVER_HOME%\webapps\geoserver\WEB-INF\lib\marlin*.jar"') do set MARLIN_JAR=%%i
  if "%MARLIN_JAR%" == "" (
    echo Marlin renderer jar not found
    goto run
  )
  set MARLIN_ENABLER=-Xbootclasspath/a:"%MARLIN_JAR%" -Dsun.java2d.renderer=org.marlin.pisces.MarlinRenderingEngine
  set JAVA_OPTS=%JAVA_OPTS% %MARLIN_ENABLER%
goto run

:run
  cd "%GEOSERVER_HOME%"
  echo Please wait while loading GeoServer...
  echo.
  "%RUN_JAVA%" %JAVA_OPTS% -DGEOSERVER_DATA_DIR="%GEOSERVER_DATA_DIR%" -Djava.awt.headless=true -DSTOP.PORT=8079 -DSTOP.KEY=geoserver -jar start.jar
  cd bin
goto end


:end
  if %error% == 1 echo Startup of GeoServer was unsuccessful. 
  echo.
  pause
