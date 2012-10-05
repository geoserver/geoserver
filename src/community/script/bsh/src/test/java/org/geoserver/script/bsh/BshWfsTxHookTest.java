package org.geoserver.script.bsh;

import org.geoserver.script.wfs.WfsTxHookTest;

public class BshWfsTxHookTest extends WfsTxHookTest {

    @Override
    public String getExtension() {
        return "bsh";
    }

}
