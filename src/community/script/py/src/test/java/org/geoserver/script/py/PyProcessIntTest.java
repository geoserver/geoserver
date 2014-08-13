package org.geoserver.script.py;

import org.geoserver.script.wps.ScriptProcessIntTest;

public class PyProcessIntTest extends ScriptProcessIntTest {

    @Override
    protected String getExtension() {
        return "py";
    }
}
