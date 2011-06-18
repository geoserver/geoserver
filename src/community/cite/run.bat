@ECHO OFF

IF "%1"=="" GOTO usage
IF "%2"=="" GOTO usage

SET service=%1
SET version=%2

IF EXIST "tests/%service%/%version%/ets/ctl/main.xml" GOTO ctl1
IF EXIST "tests/%service%/%version%/ets/ctl/%service%.xml" GOTO ctl2

ECHO "Error: could not find control file 'main.xml' or '%service%.xml' under 'tests/%service%/%version%/ets/ctl/'"
GOTO end

:ctl1
  SET ctl=main.xml
  GOTO prerun

:ctl2
  SET ctl=%service%.xml
  GOTO prerun

:prerun   
  SET ctl=tests/%service%/%version%/ets/ctl/%ctl%

  IF NOT "%3"=="" GOTO retest
  IF EXIST "target/logs/%service%-%version%" GOTO resume
  GOTO test

:retest
  SET mode=retest
  GOTO run

:resume
  SET mode=resume
  GOTO run
 
:test
  SET mode=test
  GOTO run

:run
  engine/bin/test.bat -mode=%mode% -source=%ctl% -logdir=target/logs/ -session=%service%-%version% %3
  GOTO end

:usage
  ECHO "Usage: %0 <service> <version> [testid]"
  GOTO end

:end