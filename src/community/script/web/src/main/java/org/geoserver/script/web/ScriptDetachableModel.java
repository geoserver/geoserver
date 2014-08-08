/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.io.File;

import org.apache.wicket.model.IModel;

public class ScriptDetachableModel implements IModel {

    transient Script script;

    File file;

    public ScriptDetachableModel(Script script) {
        setObject(script);
    }

    public Object getObject() {
        if (script == null) {
            script = file != null ? new Script(file) : null;
        }
        return script;
    }

    public void setObject(Object object) {
        this.script = (Script) object;
        this.file = script != null ? script.getFile() : null;
    }

    public void detach() {
        this.script = null;
    }
}
