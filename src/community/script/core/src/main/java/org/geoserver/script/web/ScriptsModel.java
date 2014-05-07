/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.script.ScriptManager;

public class ScriptsModel extends LoadableDetachableModel {

    @Override
    protected Object load() {
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
        ScriptManager scriptManager = (ScriptManager) GeoServerExtensions.bean("scriptMgr");

        try {
            File[] dirs = { scriptManager.getWpsRoot(), scriptManager.getWfsTxRoot(),
                    scriptManager.getFunctionRoot(), scriptManager.getAppRoot() };
            for (File dir : dirs) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    if (dir.getName().equals("apps")) {
                        scripts.add(new Script(scriptManager.findAppMainScript(file)));
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
