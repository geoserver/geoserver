package org.geoserver.script.groovy;

import org.geoserver.script.wfs.WfsTxHookTest;

public class GroovyTxHookTest extends WfsTxHookTest {

    @Override
    public String getExtension() {
        return "groovy";
    }

}
