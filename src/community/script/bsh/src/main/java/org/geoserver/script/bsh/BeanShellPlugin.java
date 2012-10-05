/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.bsh;

import org.geoserver.script.ScriptPlugin;
import bsh.BshScriptEngineFactory;

/**
 * Script plugin for BeanShell.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class BeanShellPlugin extends ScriptPlugin {

    public BeanShellPlugin() {
        super("bsh", BshScriptEngineFactory.class);
    }

    @Override
    public String getId() {
        return "beanshell";
    }

    @Override
    public String getDisplayName() {
        return "BeanShell";
    }

    @Override
    public String getEditorMode() {
        return "javascript";
    }
}
