#!/bin/bash

set -e 

function usage() {
  echo "$0 [options] <tag>"
  echo
  echo " tag : Release tag (eg: 2.1.4, 2.2-beta1, ...)"
  echo 
  echo "Environment variables:"
  echo " SKIP_DOWNLOAD : Skips download of release artifacts"
  echo " SKIP_UPLOAD : Skips upload of mac installer"
}

tag=$1
if [ -z $tag ]; then
  usage
  exit 1
fi

# load properties
. "$( cd "$( dirname "$0" )" && pwd )"/properties

# load common functions
. "$( cd "$( dirname "$0" )" && pwd )"/functions

# download and ready the artifacts
setup_artifacts $tag mac

pushd tmp > /dev/null
app=GeoServer.app
res=$app/Contents/Resources/Java

# copy geoserver binary over
mv geoserver-$tag/* $res
rmdir geoserver-$tag

# build the console app
pushd console > /dev/null
mvn clean install
cp target/console*.jar ../$res/console.jar
popd > /dev/null

vol=geoserver-$tag

# unmount existing mounts
if [ -d "/Volumes/${vol}" ]; then
  umount "/Volumes/${vol}"
fi

# build the dmg volume
dmg_tmp="tmp-${vol}.dmg"
dmg_final="${vol}.dmg"
dmg_back="background.png"

set +e && rm *.dmg && set -e

hdiutil create \
    -srcfolder ${app} \
    -volname "${vol}" \
    -fs HFS+ \
    -fsargs "-c c=64,a=16,e=16" \
    -format UDRW \
    "${dmg_tmp}"

# mount the dmg
sleep 2
device=$(hdiutil attach -readwrite -noverify -noautoopen "${dmg_tmp}" | egrep '^/dev/' | sed 1q | awk '{print $1}')
sleep 5

echo "DEVICE: ${device}"

# copy the background image in
mkdir "/Volumes/${vol}/.background"
cp ${dmg_back} "/Volumes/${vol}/.background/"

# create an alias to Applications
ln -sf /Applications /Volumes/${vol}/Applications

# dmg window dimensions
dmg_width=522
dmg_height=361
dmg_topleft_x=200
dmg_topleft_y=200
dmg_bottomright_x=`expr $dmg_topleft_x + $dmg_width`
dmg_bottomright_y=`expr $dmg_topleft_y + $dmg_height`

# Set the background image and icon location
echo '
   tell application "Finder"
     tell disk "'${vol}'"
           open
           set current view of container window to icon view
           set toolbar visible of container window to false
           set statusbar visible of container window to false
           set the bounds of container window to {'${dmg_topleft_x}', '${dmg_topleft_y}', '${dmg_bottomright_x}', '${dmg_bottomright_y}'}
           set theViewOptions to the icon view options of container window
           set arrangement of theViewOptions to not arranged
           set icon size of theViewOptions to 104
           set background picture of theViewOptions to file ".background:'${dmg_back}'"
           set position of item "'${app}'" of container window to {120, 180}
           set position of item "'Applications'" of container window to {400, 180}
           close
           open
           update without registering applications
delay 5
           eject
           delay 5
     end tell
   end tell
' | osascript

# convert to compressed image, delete temp image
hdiutil convert "${dmg_tmp}" -format UDZO -imagekey zlib-level=9 -o "${dmg_final}"
if [ -f "${dmg_tmp}" ]; then
  rm -f "${dmg_tmp}"
fi

# upload dmg to final location
upload_installer $tag geoserver-$tag.dmg

popd > /dev/null
exit 0

