#!/bin/bash
###########################################################################
#    codesign-geoserver-app.sh                                            #
#    ---------------------                                                #
#    Date                 : September 2016                                #
#    Author               : Larry Shaffer                                 #
#    Copyright            : (C) 2016 by Boundless Spatial                 #
#    Email                : lshaffer at boundlessgeo dot com              #
###########################################################################
#                                                                         #
#   This program is free software; you can redistribute it and/or modify  #
#   it under the terms of the GNU General Public License as published by  #
#   the Free Software Foundation; either version 2 of the License, or     #
#   (at your option) any later version.                                   #
#                                                                         #
###########################################################################

set -e
# set -x

USAGE () {
  echo "usage: $0 'absolute path to GeoServer.app (no trailing slash)'"
  echo "       set CODESIGN_CERT_SHA1 env variable to SHA1 fingerprint of signing certificate"
  echo "       optionally set KEYCHAIN env variable to OS X keychain path (defaults to 'login')"
}

GS="${1}"

if ! [[ "${GS}" = /* ]] || ! [ -d "${GS}" ] || ! [[ "${GS}" = *.app ]]; then
  USAGE
  exit 1
fi

if [[ -z "${CODESIGN_CERT_SHA1}" ]]; then
  USAGE
  exit 1
fi

# optional env var for keychain path to use
# NOTE: needs to be unlocked first,
#       e.g.: /usr/bin/security unlock -p password $HOME/Library/Keychains/login.keychain
if [[ -z "${KEYCHAIN}" ]]; then
  KEYCHAIN="$HOME/Library/Keychains/login.keychain"
fi

GS_PARENT=$(dirname "${GS}")
GS_APP=$(basename "${GS}")

echo -e "\nClearing any quarantine settings..."
xattr -rd com.apple.quarantine "${GS}"

cd "${GS}"

echo -e "\nFixing up symlink in embedded JDK..."
JDK_DIR=$(find . -type d -and -path "*PlugIns/*.jdk/Contents/MacOS")
if [ -d "${JDK_DIR}" ]; then
  pushd "${JDK_DIR}" > /dev/null
    rsync $(readlink libjli.dylib) libjli.dylib
  popd > /dev/null
fi

SIGNCODE () {
  if [[ -z "${1}" ]]; then
    echo "No signing parameter passed"
    return
  fi
    codesign --force --keychain "${KEYCHAIN}" --timestamp --verbose -s $CODESIGN_CERT_SHA1 "${1}"
}

echo -e "\nSigning bundled frameworks, libs and binaries..."
MACHOS=$(find . \! -type l -and -type f -and \! -name "GeoServer" -and \! -path "*.app*" -print0 | \
         xargs -0 -n 100 -I jj file jj | grep 'Mach-O.*' | egrep '^[^:]+' -o | egrep ' ' -v)

for macho in $MACHOS
do
  #echo "  - ${macho}"
  SIGNCODE "${macho}"
done

echo -e "\nSigning .jar and .class files"
JAVAFILES=$(find . \! -type l -and -type f -and \( -name "*.jar" -or -name "*.class" \) | egrep ' ' -v)

for jf in $JAVAFILES
do
  #echo "  - ${jf}"
  SIGNCODE "${jf}"
done

echo -e "\nSigning bundled shell scripts..."
SHSCRPTS=$(find . \! -type l -and -type f -print0 | \
           xargs -0 -n 100 -I jj file jj | grep -E 'shell script' | egrep '^[^:]+' -o | egrep ' ' -v)

for shscrpt in $SHSCRPTS
do
  if [[ -x "${shscrpt}" ]]; then
    #ls -alGh "${shscrpt}"
    #echo "  - ${shscrpt}"
    SIGNCODE "${shscrpt}"
  fi
done

# sign helper app bundles (sign inside-out if they have embedded libs, frameworks, etc.)
# echo "Signing any bundled helper apps..."
# APPS=$(find . -type d -path "*.app" | egrep ' ' -v)
# 
# for app in $APPS
# do
#   echo "  - ${app}"
#   SIGNCODE "${app}"
# done

cd "${GS_PARENT}"

# sign main app bundle
echo -e "\nSigning main app bundle..."
codesign --keychain "${KEYCHAIN}" --timestamp --verbose -s $CODESIGN_CERT_SHA1 ./$GS_APP
  

echo -e "\nVerifying code signing for ${GS_APP}..."
codesign --verify --verbose ./$GS_APP

echo -e "\nGatekeeper assessment of code signing for ${GS_APP}..."
spctl --assess -vvv --type execute ./$GS_APP

exit 0
