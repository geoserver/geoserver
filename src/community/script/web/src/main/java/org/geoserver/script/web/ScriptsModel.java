/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.script.ScriptManager;
import org.geotools.util.logging.Logging;

public class ScriptsModel extends LoadableDetachableModel<List<Script>> {

    private static final long serialVersionUID = 2762280972166257950L;
    private static final Logger LOGGER = Logging.getLogger("org.geoserver.script.web");

    @Override
    protected List<Script> load() {
        List<Script> scripts = getScripts();
        Collections.sort(scripts, new ScriptComparator());
        return scripts;
    }

    protected static class ScriptComparator implements Comparator<Script> {

        public ScriptComparator() {
            //
        }

        public int compare(Script s1, Script s2) {
            return s1.getName().compareToIgnoreCase(s2.getName());
        }
    }

    protected List<Script> getScripts() {
        List<Script> scripts = new ArrayList<Script>();
        ScriptManager scriptManager = (ScriptManager) GeoServerExtensions.bean("scriptManager");
        try {
            Resource[] dirs = {
                scriptManager.wps(),
                scriptManager.wfsTx(),
                scriptManager.function(),
                scriptManager.app()
            };
            for (Resource dir : dirs) {
                List<Resource> files = dir.list();
                for (Resource file : files) {
                    if (dir.name().equals("apps")) {
                        if (file.getType() == Type.DIRECTORY) {
                            Resource mainFile = scriptManager.findAppMainScript(file);
                            if (mainFile != null) {
                                Script script = new Script(mainFile);
                                scripts.add(script);
                            } else {
                                LOGGER.info("Could not find main app file in " + file.path());
                            }
                        }
                    } else if (dir.name().equals("wps")) {
                        if (file.getType() == Type.DIRECTORY) {
                            List<Resource> fs = file.list();
                            for (Resource f : fs) {
                                scripts.add(new Script(f));
                            }
                        } else {
                            scripts.add(new Script(file));
                        }
                    } else {
                        scripts.add(new Script(file));
                    }
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return scripts;
    }
}
