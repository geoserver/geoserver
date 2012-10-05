/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import java.util.concurrent.atomic.AtomicLong;

import javax.script.ScriptEngine;

/**
 * Maintains the state of a {@link ScriptEngine} instance under an identifier.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class ScriptSession {

    static AtomicLong IDGEN = new AtomicLong();

    long id;
    String extension;
    ScriptEngine engine;

    ScriptSession(ScriptEngine engine, String extension) {
        this.engine = engine;
        this.extension = extension;
        this.id = IDGEN.getAndIncrement();
    }

    public long getId() {
        return id;
    }

    public String getExtension() {
        return extension;
    }

    public String getEngineName() {
        return engine.getFactory().getEngineName();
    }

    public ScriptEngine getEngine() {
        return engine;
    }

}
