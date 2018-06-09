/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.groovy;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.function.FunctionHook;
import org.geoserver.script.wps.WpsHook;

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

    @Override
    public WpsHook createWpsHook() {
        return new GroovyWpsHook(this);
    }

    @Override
    public FunctionHook createFunctionHook() {
        return new GroovyFunctionHook(this);
    }
}
