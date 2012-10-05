package org.geoserver.script.rb;

import org.geoserver.script.wfs.WfsTxHookTest;

public class RbTxHookTest extends WfsTxHookTest {

    @Override
    public String getExtension() {
        return "rb";
    }

}
