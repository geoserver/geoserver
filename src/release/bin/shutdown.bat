@echo off
REM -----------------------------------------------------------------------------
REM Shutdown Script for GeoServer
REM -----------------------------------------------------------------------------


cls
echo Shutting down GeoServer...
echo.
set error=0

rem JAVA_HOME not defined
if "%JAVA_HOME%" == "" goto noJava

rem JAVA_HOME defined incorrectly
if not exist "%JAVA_HOME%\bin\java.exe" goto badJava

rem GEOSERVER_HOME not defined
if "%GEOSERVER_HOME%" == "" goto noHome

rem GEOSERVER_HOME defined incorrectly
if not exist "%GEOSERVER_HOME%\bin\startup.bat" goto badHome

rem No errors
goto shutdown



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
  set RUN_JAVA=%JAVA_HOME%\bin\java
  cd %GEOSERVER_HOME%
  "%RUN_JAVA%" -DSTOP.PORT=8079 -DSTOP.KEY=geoserver -jar start.jar --stop
  cd bin
goto end

:end
  if %error% == 1 echo Shutting down GeoServer was unsuccessful. 
  echo.
  pause
























