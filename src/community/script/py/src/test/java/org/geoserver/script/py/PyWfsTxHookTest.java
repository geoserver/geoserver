/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import org.geoserver.script.wfs.WfsTxHookTest;

public class PyWfsTxHookTest extends WfsTxHookTest {

    @Override
    public String getExtension() {
        return "py";
    }

}
