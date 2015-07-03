#!/bin/bash

function check_rc() {
  if [ $1 -gt 0 ]; then
    echo "$2 failed with return value $1"
    exit 1
  else
    echo "$2 succeeded return value $1"
  fi
}

# build the app
ant
check_rc $? "build app"

APP_NAME=GeoServer.app
APP=target/$APP_NAME
VER=$( cat ${APP}/Contents/Info.plist | grep -A 1 CFBundleVersion | tail -n 1 | sed 's/.*<string>//g' | sed 's/<\/string>//g' )

VOL=geoserver-$VER

# unmount existing mounts
if [ -d "/Volumes/${VOL}" ]; then
  umount "/Volumes/${VOL}"
fi

# build the dmg volume
DMG_TMP="tmp-${VOL}.dmg"
DMG_FINAL="${VOL}.dmg"
DMG_BACK="background.png"

rm *.dmg

hdiutil create \
    -srcfolder ${APP} \
    -volname "${VOL}" \
    -fs HFS+ \
    -fsargs "-c c=64,a=16,e=16" \
    -format UDRW \
    "${DMG_TMP}"
check_rc $? "dmg build"

# mount the dmg
sleep 2
device=$(hdiutil attach -readwrite -noverify -noautoopen "${DMG_TMP}" | egrep '^/dev/' | sed 1q | awk '{print $1}')
sleep 5

echo "DEVICE: ${device}"

# copy the background image in
mkdir "/Volumes/${VOL}/.background"
cp ${DMG_BACK} "/Volumes/${VOL}/.background/"
check_rc $? "copy background img"

# create an alias to Applications
ln -sf /Applications /Volumes/${VOL}/Applications

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
     tell disk "'${VOL}'"
           open
           set current view of container window to icon view
           set toolbar visible of container window to false
           set statusbar visible of container window to false
           set the bounds of container window to {'${dmg_topleft_x}', '${dmg_topleft_y}', '${dmg_bottomright_x}', '${dmg_bottomright_y}'}
           set theViewOptions to the icon view options of container window
           set arrangement of theViewOptions to not arranged
           set icon size of theViewOptions to 104
           set background picture of theViewOptions to file ".background:'${DMG_BACK}'"
           set position of item "'${APP_NAME}'" of container window to {120, 180}
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
hdiutil convert "${DMG_TMP}" -format UDZO -imagekey zlib-level=9 -o "${DMG_FINAL}"
check_rc $? "dmg compressing"
if [ -f "${DMG_TMP}" ]; then
  rm -f "${DMG_TMP}"
fi

exit 0

