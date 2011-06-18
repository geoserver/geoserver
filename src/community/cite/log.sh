if [ "$1" = "" ]; then
  echo "Usage: $0 <profile>"
  exit -1
fi

logdir=users/geoserver
if [ -e ${logdir}/$1 ]; then
  sh engine/bin/viewlog.sh -logdir=${logdir} -session=$1 $2
else
  echo "Error: profile '$1' does not exist."
  exit -1
fi
