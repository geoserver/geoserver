if [ "$1" = "" ]; then
  echo "Usage: $0 <profile> [testid] [options]"
  echo "Options:"
  echo "\t-h : Run the engine headless"
  exit -1
fi

#parse out service and version
service=${1:0:3}
version=${1:4:$#1}
base=engine/scripts
logdir=users/geoserver
testid=""

#headless?
HEADLESS=0

if [ "$2" != "" ] && [ "$2" != "-h" ]; then
  testid=$2
fi

if [ "$2" == "-h" ] || [ "$3" == "-h" ]; then
  HEADLESS=1
fi

#find the control file
ctl=""
if [ -e $base/$service-$version/ctl/main.xml ]; then
  ctl=main.xml
else 
  if [ -e $base/$service-$version/ctl/main.ctl ]; then
    ctl=main.ctl
  else 
    if [ -e $base/$service-$version/ctl/$service.xml ]; then
      ctl=$service.xml
    else 
      if [ -e $base/$service-$version/ctl/all.xml ]; then
       ctl=all.xml
      fi
    fi
  fi
fi

if [ "$ctl" = "" ]; then
  echo "Error: could not find control file 'main.xml' or '$service.xml' under 'tests/$service-$version/ctl/'"
  exit -1
fi
ctl=$base/$service-$version/ctl/$ctl

mode=test
if [ "$testid" != "" ]; then
  if [ ! -e ${logdir}/$1 ]; then
    echo "Error: No logs found for profile '$1'."
    exit -1
  fi
  mode=retest
else
  if [ -e ${logdir}/$1 ]; then
    mode=resume
  fi
fi

JAVA_OPTS="-Xmx512m"
if [ $HEADLESS == 1 ]; then
  JAVA_OPTS="$JAVA_OPTS -Dcite.headless=true -Djava.awt.headless=true"
fi
export JAVA_OPTS

if [ "$mode" = "resume" ]; then
  sh engine/bin/test.sh -mode=$mode -source=$ctl -workdir=target/work -logdir=${logdir} -session=$1

else 
  sh engine/bin/test.sh -mode=$mode -source=$ctl -workdir=target/work -logdir=${logdir} -session=$1 $testid
fi

# copy session.xml file so it gets picked up by the web ui
cp target/sessions/session.xml.$1 $logdir/$1/session.xml

