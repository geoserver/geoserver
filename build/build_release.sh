#!/bin/bash

# error out if any statements fail
set -e
#set -x

function usage() {
  echo "$0 [options] <tag> <user> <email>"
  echo
  echo " tag :  Release tag (eg: 2.1.4, 2.2-beta1, ...)"
  echo " user:  Git username"
  echo " email: Git email"
  echo
  echo "Options:"
  echo " -h          : Print usage"
  echo " -b <branch> : Branch to release from (eg: trunk, 2.1.x, ...)"
  echo " -r <rev>    : Revision to release (eg: 12345)"
  echo " -g <ver>    : GeoTools version/revision (eg: 2.7.4, trunk:12345)"
  echo " -w <ver>    : GeoWebCache version/revision (eg: 1.3-RC1, stable:abcd)"
  echo
  echo "Environment variables:"
  echo " SKIP_BUILD : Skips main release build"
  echo " SKIP_INSTALLERS : Skips building of mac and windows installers"
  echo " SKIP_GT : Skips the GeoTools build"
  echo " SKIP_GWC : Skips the GeoWebCache build"
}

# parse options
while getopts "hb:r:g:w:" opt; do
  case $opt in
    h)
      usage
      exit
      ;;
    b)
      branch=$OPTARG
      ;;
    r)
      rev=$OPTARG
      ;;
    g)
      gt_ver=$OPTARG
      ;;
    w)
      gwc_ver=$OPTARG
      ;;
    \?)
      usage
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument"
      exit 1
      ;;
  esac 
done

# clear options to parse main arguments
shift $(( OPTIND -1 ))
tag=$1
git_user=$2
git_email=$3

# sanity check
if [ -z $tag ] || [ -z $git_user ] || [ -z $git_email ] || [ ! -z $4 ]; then
  usage
  exit 1
fi

# load properties + functions
. "$( cd "$( dirname "$0" )" && pwd )"/properties
. "$( cd "$( dirname "$0" )" && pwd )"/functions

# more sanity checks
if [ `is_version_num $tag` == "0" ]; then
  echo "$tag is a not a valid release tag"
  exit 1
fi
if [ `is_primary_branch_num $tag` == "1" ]; then
  echo "$tag is a not a valid release tag, can't be same as primary branch name"
  exit 1
fi

echo "Building release with following parameters:"
echo "  branch = $branch"
echo "  revision = $rev"
echo "  tag = $tag"
echo "  geotools = $gt_ver"
echo "  geowebcache = $gwc_ver"

echo "maven/java settings:"
mvn -version

# move to root of source tree
pushd .. > /dev/null

# clear out any changes
git reset --hard HEAD

# checkout and update primary branch
git checkout $branch
git pull origin $branch

# check to see if a release branch already exists
set +e && git checkout rel_$tag && set -e
if [ $? == 0 ]; then
  # release branch already exists, kill it
  git checkout $branch
  echo "branch rel_$tag exists, deleting it"
  git branch -D rel_$tag
fi

# checkout the branch to release from
git checkout $branch

# ensure the specified revision actually on this branch
if [ $rev != "HEAD" ]; then
  set +e
  git log | grep $rev
  if [ $? != 0 ]; then
     echo "Revision $rev not a revision on branch $branch"
     exit -1
  fi
  set -e
fi

# create a release branch
git checkout -b rel_$tag $rev

# generate release notes
jira_id=`get_jira_id $tag`
if [ -z $jira_id ]; then
  echo "Could not locate release $tag in JIRA"
  exit -1
fi
echo "jira id = $jira_id"

# update README
#search?jql=project+%3D+%22GEOS%22+and+fixVersion+%3D+%222.2-beta2%22"

# setup geotools and geowebcache dependencies
if [ -z $SKIP_GT ]; then
  if [ ! -z $gt_ver ]; then
    # deterine if this is a revision or version number 
    if [ "`is_version_num $gt_ver`" == "1" ]; then
       gt_tag=$gt_ver
    fi

    # if version specified as branch/revision check it out
    if [ -z $gt_tag ]; then
      arr=(${gt_ver//@/ }) 
      if [ "${#arr[@]}" != "2" ]; then
         echo "Bad version $gt_ver, must be specified as <branch>@<revision>"
         exit 1
      fi

      gt_branch=${arr[0]}
      gt_rev=${arr[1]}
      gt_dir=build/geotools/$gt_branch/$gt_rev
      if [ ! -e $gt_dir ]; then
         echo "cloning geotools repo from $GT_GIT_URL"
         git clone $GT_GIT_URL $gt_dir
      fi

      # checkout branch/rev
      pushd $gt_dir > /dev/null
      echo "checking out geotools ${gt_branch}@${gt_rev}"
      git checkout $gt_rev

      # build geotools
      mvn clean install -DskipTests -Dall
      popd > /dev/null

      # derive the gt version
      gt_tag=`get_pom_version $git_dir/pom.xml`
    fi
  fi
fi

if [ ! -z $gt_tag ]; then
  echo "GeoTools version = $gt_tag"
fi

# setup geowebcache dependency
if [ -z $SKIP_GWC ]; then
  if [ ! -z $gwc_ver ]; then
    # deterine if this is a revision or version number 
    if [ "`is_version_num $gwc_ver`" == "1" ]; then
       gwc_tag=$gwc_ver
    fi

    # if version specified as branch/revision check it out
    if [ -z $gwc_tag ]; then
      arr=(${gwc_ver//@/ }) 
      if [ "${#arr[@]}" != "2" ]; then
         echo "Bad version $gwc_ver, must be specified as <branch>:<revision>"
         exit 1 
      fi

      gwc_branch=${arr[0]}
      gwc_rev=${arr[1]}
      gwc_dir=build/geowebcache/$gwc_branch/$gwc_rev
      if [ ! -e $gwc_dir -o ! -e $gwc_dir/geowebcache ]; then
         rm -rf $gwc_dir
         mkdir -p $gwc_dir 
         echo "checking out geowebache ${gwc_branch}@${gwc_rev}"
         git clone $GWC_GIT_URL $gwc_dir
         pushd $gwc_dir > /dev/null
         git checkout $gwc_rev
         popd > /dev/null
      fi

      # build geowebache
      pushd $gwc_dir/geowebcache > /dev/null
      mvn -o clean install -DskipTests

      # derive the gwc version
      gwc_tag=`get_pom_version pom.xml`

      popd > /dev/null
    fi
  fi
fi

if [ ! -z $gwc_tag ]; then
  echo "GeoWebCache version = $gwc_tag"
fi

# update geotools + geowebcache versions
if [ ! -z $gt_tag ]; then
  sed -i "s/\(<gt.version>\).*\(<\/gt.version>\)/\1$gt_tag\2/g" src/pom.xml
else
  # look up from pom instead
  gt_tag=`cat src/pom.xml | grep "<gt.version>" | sed 's/ *<gt.version>\(.*\)<\/gt.version>/\1/g'`
fi

if [ ! -z $gwc_tag ]; then
  sed -i "s/\(<gwc.version>\).*\(<\/gwc.version>\)/\1$gwc_tag\2/g" src/pom.xml
else
  gwc_tag=`cat src/pom.xml | grep "<gwc.version>" | sed 's/ *<gwc.version>\(.*\)<\/gwc.version>/\1/g'`
fi

# update version numbers
old_ver=`get_pom_version src/pom.xml`

echo "updating version numbers from $old_ver to $tag"
find src -name pom.xml -exec sed -i "s/$old_ver/$tag/g" {} \;
find doc -name conf.py -exec sed -i "s/$old_ver/$tag/g" {} \;

pushd src/release > /dev/null
shopt -s extglob
sed -i "s/$old_ver/$tag/g" !(pom).xml installer/win/*.nsi installer/win/*.conf 
shopt -u extglob
popd > /dev/null

pushd src > /dev/null

# build the release
if [ -z $SKIP_BUILD ]; then
  echo "building release"
  mvn $MAVEN_FLAGS clean install -DskipTests -P release
  
  # build the javadocs
  mvn javadoc:aggregate

  ##################
  # Build the docs
  ##################



  pushd ../doc/en > /dev/null

  # 2.11 and older uses make
  if [ -e user/Makefile ]
  then
    # build the user docs
    cd user
    make clean html
    make latex
    cd build/latex

    sed  "s/includegraphics/includegraphics[scale=0.5]/g" GeoServerUserManual.tex > manual.tex
    # run pdflatex twice in a row to get the TOC, and ignore errors 
    set +e
    pdflatex -interaction batchmode manual.tex
    pdflatex -interaction batchmode manual.tex
    set -e

    if [ ! -f manual.pdf ]; then
      echo "Failed to build pdf manual. Printing latex log:"
      cat manual.log
    fi

    # build the developer docs
    cd ../../../developer
    make clean html

  # 2.12 and newer uses ant to do everything
  else
    ant clean user -Dproject.version=$tag
    ant user-pdf -Dproject.version=$tag
    ant developer -Dproject.version=$tag
  fi

  popd > /dev/null


fi

mvn $MAVEN_FLAGS assembly:attached

# copy over the artifacts
if [ ! -e $DIST_PATH ]; then
  mkdir -p $DIST_PATH
fi
dist=$DIST_PATH/$tag
if [ -e $dist ]; then
  rm -rf $dist
fi
mkdir $dist
mkdir $dist/plugins

artifacts=`pwd`/target/release
# bundle up mac and windows installer stuff
pushd release/installer/mac > /dev/null
zip -r $artifacts/geoserver-$tag-mac.zip *
popd > /dev/null
pushd release/installer/win > /dev/null
zip -r $artifacts/geoserver-$tag-win.zip *
popd > /dev/null

pushd $artifacts > /dev/null

# setup doc artifacts
if [ -e user ]; then
  unlink user
fi
if [ -e developer ]; then
  unlink developer
fi

# paths for 2.12 and newer docbuild
usertarget=target/user
devtarget=target/developer
# paths for 2.11 and older docbuild
if [ -e ../../../doc/en/user/Makefile ]; then
  usertarget=user/build
  devtarget=developer/build
fi

ln -sf ../../../doc/en/$usertarget/html user
ln -sf ../../../doc/en/$devtarget/html developer
ln -sf ../../../doc/en/release/README.txt readme

htmldoc=geoserver-$tag-htmldoc.zip
if [ -e $htmldoc ]; then
  rm -f $htmldoc 
fi
zip -r $htmldoc user developer readme
unlink user
unlink developer
unlink readme

popd > /dev/null

echo "copying artifacts to $dist"
cp $artifacts/*-plugin.zip $dist/plugins
for a in `ls $artifacts/*.zip | grep -v plugin`; do
  cp $a $dist
done

cp $artifacts/../../../doc/en/$usertarget/latex/manual.pdf $dist/geoserver-$tag-user-manual.pdf

# git commit changes on the release branch
pushd .. > /dev/null

init_git $git_user $git_email

git add . 
git commit -m "updating version numbers and release notes for $tag" .
popd > /dev/null

# fire off mac and windows build machines
if [ -z $SKIP_INSTALLERS ]; then
  echo "starting installer jobs"
  start_installer_job $WIN_JENKINS $WIN_JENKINS_USER $WIN_JENKINS_KEY $tag
  start_installer_job $MAC_JENKINS $MAC_JENKINS_USER $MAC_JENKINS_KEY $tag
fi

popd > /dev/null

echo "build complete, artifacts available at $DIST_URL/$tag"
exit 0
