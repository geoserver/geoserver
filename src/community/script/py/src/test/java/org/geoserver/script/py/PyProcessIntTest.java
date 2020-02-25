/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import org.geoserver.script.wps.ScriptProcessIntTest;

public class PyProcessIntTest extends ScriptProcessIntTest {

    @Override
    protected String getExtension() {
        return "py";
    }
}
