/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.rb;

import org.geoserver.script.ScriptPlugin;

import com.sun.script.jruby.JRubyScriptEngineFactory;

public class RubyPlugin extends ScriptPlugin {

    public RubyPlugin() {
        super("rb", JRubyScriptEngineFactory.class);
    }

    @Override
    public String getId() {
        return "ruby";
    }
    
    @Override
    public String getDisplayName() {
        return "Ruby";
    }

}
