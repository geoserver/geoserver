/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.groovy;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.geoserver.script.ScriptPlugin;

/**
 * Script plugin for groovy.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class GroovyPlugin extends ScriptPlugin {

    public GroovyPlugin() {
        super("groovy", GroovyScriptEngineFactory.class);
    }

    @Override
    public String getId() {
        return "groovy";
    }
    
    @Override
    public String getDisplayName() {
        return "Groovy";
    }
}
