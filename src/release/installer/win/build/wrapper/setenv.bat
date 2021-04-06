@echo off
rem quotes are required for correct handling of path with spaces

rem default java home
set wrapper_home=%~dp0

rem default java exe for running the wrapper
rem note this is not the java exe for running the application. the exe for running the application is defined in the wrapper configuration file
if "%JAVA_HOME%" == "" (
 set java_exe="java"
 set javaw_exe="javaw"
 ) else (
 set java_exe="%JAVA_HOME%\bin\java"
 set javaw_exe="%JAVA_HOME%\bin\javaw"
)

rem location of the wrapper jar file. necessary lib files will be loaded by this jar. they must be at <wrapper_home>/lib/...
set wrapper_jar="%wrapper_home%/wrapper.jar"
set wrapper_app_jar="%wrapper_home%/wrapperApp.jar"

rem get java version
for /f "delims=" %%j in ('%java_exe% -fullversion 2^>^&1') do @set "jver=%%j"
if "%jver:~0,7%" == "openjdk" (
  set java_version=%jver:~22,2%
) else (
  set java_version=%jver:~19,2%
)

rem setting java options for wrapper process. depending on the scripts used, the wrapper may require more memory.
if "%java_version%" == "1." (
set wrapper_java_options=-Xmx30m -Dwrapper_home="%wrapper_home%" -Djna_tmpdir="%wrapper_home%/tmp" -Djava.net.preferIPv4Stack=true
) else (
set wrapper_java_options=-Xmx30m -Dwrapper_home="%wrapper_home%" -Djna_tmpdir="%wrapper_home%/tmp" -Djava.net.preferIPv4Stack=true --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED
)

rem wrapper bat file for running the wrapper
set wrapper_bat="%wrapper_home%/wrapper.bat"

rem configuration file used by all bat files
set conf_file=%wrapper_home%/wrapper.conf
