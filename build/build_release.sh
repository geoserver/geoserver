#!/bin/bash

# error out if any statements fail
set -e
#set -x

function usage() {
  echo "$0 [options] <tag> <user> <email>"
  echo
  echo " tag :  Release tag (eg: 2.14.4, 2.15-beta1, ...)"
  echo " user:  Git username"
  echo " email: Git email"
  echo
  echo "Options:"
  echo " -h          : Print usage"
  echo " -b <branch> : Branch to release from (eg: trunk, 2.14.x, ...)"
  echo " -r <rev>    : Revision to release (eg: 12345)"
  echo " -g <ver>    : GeoTools version/revision (eg: 20.4, master:12345)"
  echo " -w <ver>    : GeoWebCache version/revision (eg: 1.14.4, 1.14.x:abcd)"
  echo
  echo "Environment variables:"
  echo " SKIP_BUILD : Skips main release build"
  echo " SKIP_COMMUNITY : Skips community release build"
  echo " SKIP_TAG : Skips tag on release branch"
  echo " SKIP_INSTALLERS : Skips building of mac and windows installers"
  echo " SKIP_GT : Skips the GeoTools build, as used to build revision"
  echo " SKIP_GWC : Skips the GeoWebCache build, as used to build revision"
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
#checkout branch locally if it doesn't exist
if ! git show-ref refs/heads/$branch; then 
  echo "checkout branch #branch locally"
  git fetch origin $branch:$branch
fi

# ensure there is a jira release
jira_id=`get_jira_id $tag`
if [ -z $jira_id ]; then
  echo "Could not locate release $tag in JIRA"
  exit -1
fi

# move to root of source tree
pushd .. > /dev/null

dist=`pwd`/distribution/$tag
mkdir -p $dist/plugins

echo "Building release with following parameters:"
echo "  branch = $branch"
echo "  revision = $rev"
echo "  tag = $tag"
echo "  geotools = $gt_ver"
echo "  geowebcache = $gwc_ver"
echo "  jira id = $jira_id"
echo "  distribution = $dist"
echo
echo "maven/java settings:"
mvn -version

# clear out any changes
git reset --hard HEAD

# checkout and update primary branch
git checkout $branch
git pull origin $branch

# check to see if a release branch already exists
if [ `git branch --list rel_$tag | wc -l` == 1 ]; then
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

# setup geotools dependency
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
      mvn clean install $MAVEN_FLAGS -Dall -DskipTests
      popd > /dev/null

      # derive the gt version
      gt_tag=`get_pom_version $git_dir/pom.xml`
    fi
  fi
fi

if [ ! -z $gt_tag ]; then
  echo "GeoTools version = $gt_tag"
fi

# update geotools version
if [ ! -z $gt_tag ]; then
  sed -i "s/\(<gt.version>\).*\(<\/gt.version>\)/\1$gt_tag\2/g" src/pom.xml
else
  # look up from pom instead
  gt_tag=`cat src/pom.xml | grep "<gt.version>" | sed 's/ *<gt.version>\(.*\)<\/gt.version>/\1/g'`
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
      mvn clean install $MAVEN_FLAGS -DskipTests

      # derive the gwc version
      gwc_tag=`get_pom_version pom.xml`

      popd > /dev/null
    fi
  fi
fi

if [ ! -z $gwc_tag ]; then
  echo "GeoWebCache version = $gwc_tag"
fi

# update geowebcache version
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
  mvn clean install $MAVEN_FLAGS -DskipTests -P release
  
  # build the javadocs
  mvn javadoc:aggregate $MAVEN_FLAGS

  ##################
  # Build the docs
  ##################

  pushd ../doc/en > /dev/null

  ant clean user -Dproject.version=$tag
  ant user-pdf -Dproject.version=$tag
  ant developer -Dproject.version=$tag

  popd > /dev/null
else
   echo "Skipping mvn clean install $MAVEN_FLAGS -DskipTests -P release"
fi

if [ -z $SKIP_COMMUNITY ]; then
   pushd community > /dev/null
   set +e
   mvn clean install -P communityRelease -DskipTests $MAVEN_FLAGS || true
   set -e
   popd > /dev/null
else
   echo "Skipping mvn clean install -P communityRelease -DskipTests"
fi


mvn assembly:attached $MAVEN_FLAGS

artifacts=`pwd`/target/release
echo "artifacts = $artifacts"

# bundle up mac and windows installer stuff
pushd release/installer/mac > /dev/null
zip -q -r $artifacts/geoserver-$tag-mac.zip *
popd > /dev/null
pushd release/installer/win > /dev/null
zip -q -r $artifacts/geoserver-$tag-win.zip *
popd > /dev/null

pushd $artifacts > /dev/null

# setup doc artifacts
if [ -e user ]; then
  unlink user
fi
if [ -e developer ]; then
  unlink developer
fi

ln -sf ../../../doc/en/target/user/html user
ln -sf ../../../doc/en/target/developer/html developer
ln -sf ../../../doc/en/release/README.txt readme

htmldoc=geoserver-$tag-htmldoc.zip
if [ -e $htmldoc ]; then
  rm -f $htmldoc 
fi
zip -q -r $htmldoc user developer readme
unlink user
unlink developer
unlink readme

popd > /dev/null

# stage distribution artifacts
echo "copying artifacts to $dist"
cp $artifacts/*-plugin.zip $dist/plugins
for a in `ls $artifacts/*.zip | grep -v plugin`; do
  cp $a $dist
done

cp $artifacts/../../../doc/en/target/user/latex/manual.pdf $dist/geoserver-$tag-user-manual.pdf || true

echo "generated artifacts:"
ls -la $dist

# git commit changes on the release branch
pushd .. > /dev/null

init_git $git_user $git_email

git add doc
git add src
git commit -m "updating version numbers and release notes for $tag" .

# tag release branch
if [ -z $SKIP_TAG ]; then
    # fetch single tag, don't fail if its not there
    git fetch origin refs/tags/$tag:refs/tags/$tag --no-tags || true

    # check to see if tag already exists
    if [ `git tag --list $tag | wc -l` == 1 ]; then
      echo "tag $tag exists, deleting it"
      git tag -d $tag
    fi

    if  [ `git ls-remote --refs --tags origin tags/$tag | wc -l` == 1 ]; then
      echo "tag $tag exists on $GIT_ROOT, deleting it"
      git push --delete origin $tag
    fi

    # tag the release branch
    git tag $tag

    # push up tag
    git push origin $tag
fi

popd > /dev/null

# fire off mac and windows build machines
if [ -z $SKIP_INSTALLERS ]; then
  echo "starting installer jobs"
  start_installer_job $WIN_JENKINS $WIN_JENKINS_USER $WIN_JENKINS_KEY $tag
  start_installer_job $MAC_JENKINS $MAC_JENKINS_USER $MAC_JENKINS_KEY $tag
fi

popd > /dev/null

echo "build complete, artifacts available at $DIST_URL/distribution/$tag"
exit 0
