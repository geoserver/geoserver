#!/bin/bash

# error out if any statements fail
set -e
#set -x

function usage() {
  echo "$0 [options] <tag>"
  echo
  echo " tag : Release tag (eg: 2.1.4, 2.2-beta1, ...)"
  echo " tag : Release tag (eg: 2.1.4, 2.2-beta1, ...)"
  echo
  echo "Options:"
  echo " -h          : Print usage"
  echo " -b <branch> : Branch to release from (eg: trunk, 2.1.x, ...)"
  echo " -r <rev>    : Revision to release (eg: 12345)"
  echo " -g <ver>    : GeoTools version/revision (eg: 2.7.4, trunk:12345)"
  echo " -w <ver>    : GeoWebCache version/revision (eg: 1.3-RC1, stable:abcd)"
  echo " -u <user>   : Subversion username"
  echo " -p <passwd> : Subversion password"
  echo
  echo "Environment variables:"
  echo " BUILD_FROM_BRANCH : Builds release from branch rather than tag"
  echo " SKIP_SVN_TAG : Skips creation of svn tag"
  echo " SKIP_BUILD : Skips main release build"
  echo " SKIP_INSTALLERS : Skips building of mac and windows installers"
  echo " SKIP_GT : Skips the GeoTools build"
  echo " SKIP_GWC : Skips the GeoWebCache build"
}

# parse options
while getopts "hb:r:g:w:u:p:" opt; do
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
    u)
      svn_user=$OPTARG
      ;;
    p)
      svn_passwd=$OPTARG
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

# sanity check
if [ -z $tag ] || [ ! -z $2 ]; then
  usage
  exit 1
fi

# load properties + functions
. "$( cd "$( dirname "$0" )" && pwd )"/properties
. "$( cd "$( dirname "$0" )" && pwd )"/functions

echo "Building release with following parameters:"
echo "  branch = $branch"
echo "  revision = $rev"
echo "  tag = $tag"
echo "  geotools = $gt_ver"
echo "  geowebcache = $gwc_ver"

echo "maven/java settings:"
mvn -version

svn_opts="--username $svn_user --password $svn_passwd --non-interactive --trust-server-cert"

if [ "$branch" == "trunk" ]; then
   svn_url=$SVN_ROOT/trunk
else
   svn_url=$SVN_ROOT/branches/$branch
fi

if [ ! -z $BUILD_FROM_BRANCH ]; then
  if [ ! -e tags/$tag ]; then
    echo "checking out $svn_url"
    svn co $svn_opts $svn_url tags/$tag  
  fi
else
  # check if the svn tag already exists
  if [ -z $SKIP_SVN_TAG ]; then
    svn_tag_url=$SVN_ROOT/tags/$tag
    set +e && svn ls $svn_opts $svn_tag_url >& /dev/null && set -e
    if [ $? == 0 ]; then
      # tag already exists
      echo "svn tag $tag already exists, deleteing"
      svn $svn_opts rm -m "removing $tag tag" $svn_tag_url
    fi
  
    # create svn tag
    revopt="-r $rev"
    if [ "$rev" == "latest" ]; then
      revopt=""
    fi
  
    echo "Creating $tag tag from $branch ($rev) at $svn_tag_url"
    svn cp $svn_opts -m "tagging $tag" $revopt $svn_url $svn_tag_url

    # checkout newly created tag
    if [ -e tags/$tag ]; then
      # remove old checkout
      rm -rf tags/$tag
    fi

    echo "checking out tag $tag"
    svn $svn_opts co $svn_tag_url tags/$tag
  fi
fi

if [ ! -z $SKIP_SVN_TAG ] || [ ! -z $BUILD_FROM_BRANCH ]; then
  echo "updating tag $tag"
  svn revert --recursive tags/$tag
  svn up tags/$tag 
fi

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
         echo "Bad version $gt_ver, must be specified as <branch>:<revision>"
         exit 1
      fi

      gt_branch=${arr[0]}
      gt_rev=${arr[1]}
      gt_dir=geotools/$gt_branch/$gt_rev
      if [ ! -e $gt_dir ]; then
         mkdir -p $gt_dir 
         echo "checking out geotools ${gt_branch}@${gt_rev}"
         svn co -r $gt_rev $GT_SVN_ROOT/$gt_branch $gt_dir
      fi

      # build geotools
      pushd $gt_dir > /dev/null
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
      gwc_dir=geowebcache/$gwc_branch/$gwc_rev
      if [ ! -e $gwc_dir ]; then
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

pushd tags/$tag > /dev/null

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

# update the release notes
notes=RELEASE_NOTES.txt
pushd src/release > /dev/null
sed -i "s/@VER@/$tag/g" $notes
sed -i "s/@DATE@/`date "+%b %d, %Y"`/g" $notes
sed -i "s/@JIRA_VER@/$jira_id/g" $notes

gt_ver_info=$gt_tag
if [ ! -z $gt_rev ]; then
  gt_ver_info="$gt_ver_info, rev $gt_rev"
fi

gwc_ver_info=$gwc_tag
if [ ! -z $gwc_rev ]; then
  gwc_ver_info="$gwc_ver_info, rev $gwc_rev"
fi

sed -i "s/@GT_VER@/$gt_ver_info/g" $notes
sed -i "s/@GWC_VER@/$gwc_ver_info/g" $notes

popd > /dev/null

# update version numbers
old_ver=`get_pom_version src/pom.xml`

echo "updating version numbers from $old_ver to $tag"
find src -name pom.xml -exec sed -i "s/$old_ver/$tag/g" {} \;
find doc -name conf.py -exec sed -i "s/$old_ver/$tag/g" {} \;

pushd src/release > /dev/null
sed -i "s/$old_ver/$tag/g" *.xml installer/win/*.nsi installer/win/*.conf installer/mac/GeoServer.app/Contents/Info.plist
popd > /dev/null
popd > /dev/null

pushd tags/$tag/src > /dev/null

# build the release
if [ -z $SKIP_BUILD ]; then
  echo "building release"
  mvn $MAVEN_FLAGS clean install -DskipTests -P release
  
  # build the javadocs
  mvn javadoc:aggregate

  # build the user docs
  pushd ../doc/en/user > /dev/null
  make clean html

  cd ../developer
  make clean html

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

ln -sf ../../../doc/en/user/build/html user
ln -sf ../../../doc/en/developer/build/html developer
htmldoc=geoserver-$tag-htmldoc.zip
if [ -e $htmldoc ]; then
  rm -f $htmldoc 
fi
zip -r $htmldoc user developer
unlink user
unlink developer

# clean up source artifact
if [ -e tmp ]; then
  rm -rf tmp
fi
mkdir tmp
src=geoserver-$tag-src.zip
unzip -d tmp $src
pushd tmp > /dev/null

set +e && find . -type d -name target -exec rm -rf {} \; && set -e
rm ../$src
zip -r ../$src *

popd
popd > /dev/null

echo "copying artifacts to $dist"
cp $artifacts/*-plugin.zip $dist/plugins
for a in `ls $artifacts/*.zip | grep -v plugin`; do
  cp $a $dist
done

popd > /dev/null

# fire off mac and windows build machines
if [ -z $SKIP_INSTALLERS ]; then
  echo "starting installer jobs"
  start_installer_job $WIN_HUDSON $tag
  start_installer_job $MAC_HUDSON $tag
fi

# svn commit changes on the tag
if [ -z $SKIP_SVN_TAG ]; then
  pushd tags/$tag > /dev/null
  svn commit $svn_opts -m "updating version numbers and release notes for $tag" .
  popd > /dev/null
fi

echo "build complete, artifacts available at $DIST_URL/$tag"
exit 0
