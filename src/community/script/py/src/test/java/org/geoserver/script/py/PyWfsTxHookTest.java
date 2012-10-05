package org.geoserver.script.py;

import org.geoserver.script.wfs.WfsTxHookTest;

public class PyWfsTxHookTest extends WfsTxHookTest {

    @Override
    public String getExtension() {
        return "py";
    }

}
