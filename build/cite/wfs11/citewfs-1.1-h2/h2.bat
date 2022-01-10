@echo off

set HOME=c:\Documents and Settings\YourUser
set H2=%HOME%\.m2\repository\org\h2database\h2\1.0-SNAPSHOT\h2-1.0-SNAPSHOT.jar
set M2_REPO=%HOME%\.m2\repository
set GT_VERSION=2.5-SNAPSHOT
set CP="%H2%";"%M2_REPO%/org/openplans/spatialdbbox/1.0-SNAPSHOT/spatialdbbox-1.0-SNAPSHOT.jar";"%M2_REPO%/org/geotools/gt-main/%GT_VERSION%/gt-main-%GT_VERSION%.jar";"%M2_REPO%/com/vividsolutions/jts/1.8/jts-1.8.jar"
echo Classpath: %CP%
set SF="2"

if "load" == "%1" goto load
if "clean" == "%1" goto clean
if "run" == "%1" goto run

:usage
echo "Usage: %0 <load|clean|run>"
goto:eof

:load
java -cp %CP% org.h2.tools.DeleteDbFiles -dir . -db cite
java -cp %CP% org.h2.tools.RunScript -url jdbc:h2:cite -script h2.sql -user "" -password ""
java -cp %CP% org.h2.tools.RunScript -url jdbc:h2:cite -script dataset-sf%SF%.sql -user "" -password ""
goto:eof

:run
java -cp %CP% org.h2.tools.Server

