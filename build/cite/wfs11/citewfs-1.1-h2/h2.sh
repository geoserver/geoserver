H2="$HOME/.m2/repository/org/h2database/h2/1.1.104/h2-1.1.104.jar"
M2_REPO="$HOME/.m2/repository"
GT_VERSION=2.6-SNAPSHOT
CP="$H2:$M2_REPO/org/geotools/gt-main/$GT_VERSION/gt-main-$GT_VERSION.jar:$M2_REPO/org/geotools/jdbc/gt-jdbc-h2/$GT_VERSION/gt-jdbc-h2-$GT_VERSION.jar:$M2_REPO/com/vividsolutions/jts/1.9/jts-1.9.jar"
SF="0"

if [ "load" = "$1" ]; then
   java -cp $CP org.h2.tools.DeleteDbFiles -dir . -db cite
   java -cp $CP org.h2.tools.RunScript -url jdbc:h2:cite -script h2.sql -user "" -password ""
   java -cp $CP org.h2.tools.RunScript -url jdbc:h2:cite -script dataset-sf${SF}.sql -user "" -password ""
else
  if [ "clean" = "$1" ]; then
     java -cp $CP org.h2.tools.DeleteDbFiles -dir . -db cite
  else
      if [ "run" = "$1" ]; then
        java -cp $CP org.h2.tools.Server
      else
         echo "Usage: $0 <load|clean|run>"
      fi
  fi
fi

