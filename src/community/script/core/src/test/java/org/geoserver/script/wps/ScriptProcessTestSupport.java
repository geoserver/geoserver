/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptIntTestSupport;

/**
 * Base class to perform tests against a single process
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class ScriptProcessTestSupport extends ScriptIntTestSupport {

    protected File script;

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        script = copyScriptIfExists(getProcessName());
    }

    protected File copyScriptIfExists(String baseName) throws IOException {
        File wps = scriptMgr.wps().dir();
        File directory;
        if (getNamespace().equals(getExtension())) {
            directory = wps;
        } else {
            directory = new File(wps, getNamespace());
        }
        File script = new File(directory, baseName + "." + getExtension());

        URL u = getClass().getResource(script.getName());
        if (u != null) {
            FileUtils.copyURLToFile(u, script);
            return script;
        }
        return null;
    }

    public abstract String getExtension();

    public String getNamespace() {
        return getExtension();
    }

    public abstract String getProcessName();
}
