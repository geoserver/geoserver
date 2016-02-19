# GeoServer Mac Installer Build Instructions

## Pre-requisites

* OSX 10.8+
* JDK 1.8+
* Apache Ant
* Adobe [64-bit unit types](http://helpx.adobe.com/photoshop/kb/unit-type-conversion-error-applescript.html)

## Building

1. Copy the `-bin.zip` into this directory.
2. Run the `build.sh` script.

Upon success a `.dmg` file should be created in the this directory.

## Notes

A [fork](https://bitbucket.org/infinitekind/appbundler) of the Oracle 
appbundler is required to build out the installer. 
