###########################################################################
#    code-sign-exe.sh                                                     #
#    ---------------------                                                #
#    Date                 : March 2017                                    #
#    Author               : Larry Shaffer                                 #
#    Copyright            : (C) 2017 by Boundless Spatial                 #
#    Email                : lshaffer at boundlessgeo dot com              #
###########################################################################
#                                                                         #
#   This program is free software; you can redistribute it and/or modify  #
#   it under the terms of the GNU General Public License as published by  #
#   the Free Software Foundation; either version 2 of the License, or     #
#   (at your option) any later version.                                   #
#                                                                         #
###########################################################################

# Code-sign .exe file using signtool and installed DigiCert code-signing 
# cert/key and CA
#
# Requirements:
#   MinGW / msys shell 
#   Need Win SDK 7.0 or higher
#   Need internet connection
#   Install signing cert/key bundle into Machine (all users) cert store
#   Install any intermediate CA into Machine cert store, 
#     as signtool will include it
#   Use /sm if signing cert was imported to Machine (not My) cert store
#   SHA1 signature of cert MUST be uppercase
#
# You can also use the DigiCert GUI-based utility:
#   https://www.digicert.com/util/
#   NOTE: utility only does SHA1 signing of exe, so not useful for Win 10+
#
# signtool docs, circa 2016:
#   https://msdn.microsoft.com/en-us/library/aa387764(v=vs.85).aspx
# example signing using Machine cert store and sha256
# signtool sign /sm /tr http://timestamp.digicert.com /td sha256 ^
#               /fd sha256 /sha1 UPPERCASESHA1SIGNTURE some.exe

set -e
# set -x

USAGE () {
  echo "usage: $0 certhash some.exe"
  echo "       certhash: signing cert SHA1 hash; must be UPPERCASE"
  echo "       some.exe: path of .exe to sign"
}

if [ "$#" -ne 2 ]; then
  usage
  exit 1
fi

if [ -z $1 ] || [ -z $2 ]; then
  USAGE
  exit 1
fi

if ! [ -f $2 ]; then
  echo "exe file not found"
  exit 1
fi

if ! [[ $2 == *.exe ]]; then
  echo "extension not .exe"
  exit 1
fi

export PATH="/c/Program Files/Microsoft SDKs/Windows/v7.1/Bin":/usr/bin:$PATH

# echo $1
# echo $2

signtool sign //sm //fd sha256 //sha1 $1 $2

signtool timestamp //tr http://timestamp.digicert.com //td sha256 $2

signtool verify //V //pa $2

