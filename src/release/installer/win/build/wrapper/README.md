## About
The Windows executable `GeoServer.exe` in this directory serves as a 64-bit Java service wrapper, so that Windows users are able to install and run GeoServer as a Windows service.

The executable is identical to the original `jsl64.exe` *Java Service Launcher* (as described below). The only difference besides the fact that it has been renamed to avoid confusion, is that it has been digitally signed by the [Open Source Geospatial Foundation](https://www.osgeo.org).

## Java Service Launcher (JSL)
Java Service Launcher is a Java service wrapper, a small executable used to start 32bit and 64bit JAVA-programs as a Windows Service.

Note that JSL is **not** a JVM. It's simply a utility like java, javap, javac or javah which will launch a Java application through an already an installed JVM.

64bit and 32bit should work with Windows 7 and later (last tested version is Windows 10). No recent tests have been performed on Windows XP or older.

JSL should work with any JVM fom Java 1.2 onwards as the JNI interfaces used to launch the JVM has been standardized by Sun early on. JSL has been tested with OpenJDK based JDKs like Azul Zulu, and Oracle's JDK. JSL has also been used with IBMs J9.

## Release info

```
Current Release:    JSL 0.99w
Release Date:	    January 2020
Download:	        jsl.zip
Change log:	        http://www.roeschter.de/#changelog
SourceForge:        https://sourceforge.net/projects/jslwin/
Contact by Mail:	michael@roeschter.com
Contact by Phone:   +49 157 5616 8246 (Central European Time)
License:	        Public Domain
Home Page:	        http://www.roeschter.de/index.html
```
