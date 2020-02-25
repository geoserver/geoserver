/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.rb;

import com.sun.script.jruby.JRubyScriptEngineFactory;
import org.geoserver.script.ScriptPlugin;

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
