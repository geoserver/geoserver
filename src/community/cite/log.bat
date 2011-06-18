@ECHO OFF

IF "%1"=="" GOTO usage
IF "%2"=="" GOTO usage

IF NOT EXIST "target/logs/%1-%2" GOTO noprofile
engine/bin/viewlog.bat -logdir=target/logs -session=%1-%2 %3

GOTO end

:usage
  ECHO "%0 <service> <version> [testid]
  GOTO end

:noprofile:
  ECHO "Error: profile %1-%2 does not exist."
  GOTO end

:end