To build GeoServer with scripting support:

    mvn -DconfigDirectory=../../../data -Dscript clean install

This should build all included script modules.  To enable additional profiles, specify them separately (e.g. `-P wps`).
