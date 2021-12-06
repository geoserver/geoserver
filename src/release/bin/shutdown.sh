#!/bin/sh
set -o errexit
set -o nounset

IFS=$(printf '\n\t')
# -----------------------------------------------------------------------------
# Start Script for GEOSERVER
#
# $Id$
# -----------------------------------------------------------------------------

# Guard against misconfigured JAVA_HOME
if [ -n "${JAVA_HOME:-}" ] && [ ! -x "${JAVA_HOME}/bin/java" ]; then
  echo "The JAVA_HOME environment variable is set but JAVA_HOME/bin/java"
  echo "is missing or not executable:"
  echo "    JAVA_HOME=${JAVA_HOME}"
  echo "Please either set JAVA_HOME so that the Java runtime is JAVA_HOME/bin/java"
  echo "or unset JAVA_HOME to use the Java runtime on the PATH."
  exit 1
fi

# Find java from JAVA_HOME or PATH
if [ -n "${JAVA_HOME:-}" ]; then
  _RUNJAVA="${JAVA_HOME}/bin/java"
elif [ -n "$(command -v java)" ]; then
  _RUNJAVA=java
else
  echo "A Java runtime (java) was not found in JAVA_HOME/bin or on the PATH."
  echo "Please either set the JAVA_HOME environment variable so that the Java runtime"
  echo "is JAVA_HOME/bin/java or add the Java runtime to the PATH."
  exit 1
fi

if [ -z "${GEOSERVER_HOME:-}" ]; then
  #If GEOSERVER_HOME not set then guess a few locations before giving
  # up and demanding user set it.
  if [ -r start.jar ]; then
     echo "GEOSERVER_HOME environment variable not found, using current "
     echo "directory.  If not set then running this script from other "
     echo "directories will not work in the future."
     GEOSERVER_HOME="$(pwd)"
  else 
    if [ -r ../start.jar ]; then
      echo "GEOSERVER_HOME environment variable not found, using current "
      echo "location.  If not set then running this script from other "
      echo "directories will not work in the future."
      GEOSERVER_HOME="$(pwd)/.."
    fi
  fi 

  if [ -z "${GEOSERVER_HOME:-}" ]; then
    echo "The GEOSERVER_HOME environment variable is not defined"
    echo "This environment variable is needed to run this program"
    echo "Please set it to the directory where geoserver was installed"
    exit 1
  fi

  export GEOSERVER_HOME
fi

if [ ! -r "${GEOSERVER_HOME}/start.jar" ]; then
  echo "The GEOSERVER_HOME environment variable is not defined correctly"
  echo "This environment variable is needed to run this program"
  exit 1
fi

cd "${GEOSERVER_HOME}" || exit 1
IFS=$(printf '\n\t ')
exec "${_RUNJAVA}" ${JAVA_OPTS:--DnoJavaOpts} -DSTOP.PORT=8079 -DSTOP.KEY=geoserver -jar start.jar --stop
