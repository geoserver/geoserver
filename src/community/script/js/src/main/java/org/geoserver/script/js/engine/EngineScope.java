/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js.engine;

import java.util.ArrayList;
import javax.script.Bindings;
import javax.script.ScriptContext;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class EngineScope implements Scriptable {

    private ScriptContext context;
    private Scriptable prototype;
    private Scriptable parent;

    public EngineScope(ScriptContext context) {
        this.context = context;
    }

    @Override
    public String getClassName() {
        return "Engine";
    }

    @Override
    public synchronized Object get(String name, Scriptable start) {
        Object value;
        synchronized (context) {
            int scope = context.getAttributesScope(name);
            if (scope != -1) {
                value = context.getAttribute(name, scope);
            } else {
                value = NOT_FOUND;
            }
        }
        return value;
    }

    @Override
    public synchronized Object get(int index, Scriptable start) {
        return get(String.valueOf(index), start);
    }

    @Override
    public synchronized boolean has(String name, Scriptable start) {
        boolean has;
        synchronized (context) {
            has = context.getAttributesScope(name) != -1;
        }
        return has;
    }

    @Override
    public synchronized boolean has(int index, Scriptable start) {
        return has(String.valueOf(index), start);
    }

    @Override
    public synchronized void put(String name, Scriptable start, Object value) {
        if (start == this) {
            synchronized (context) {
                int scope = context.getAttributesScope(name);
                if (scope == -1) {
                    scope = ScriptContext.ENGINE_SCOPE;
                }
                context.setAttribute(name, value, scope);
            }
        } else {
            start.put(name, this, value);
        }
    }

    @Override
    public synchronized void put(int index, Scriptable start, Object value) {
        put(String.valueOf(index), start, value);
    }

    @Override
    public synchronized void delete(String name) {
        synchronized (context) {
            int scope = context.getAttributesScope(name);
            if (scope != -1) {
                context.removeAttribute(name, scope);
            }
        }
    }

    @Override
    public synchronized void delete(int index) {
        delete(String.valueOf(index));
    }

    @Override
    public Scriptable getPrototype() {
        return prototype;
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        this.prototype = prototype;
    }

    @Override
    public Scriptable getParentScope() {
        return parent;
    }

    @Override
    public void setParentScope(Scriptable parent) {
        this.parent = parent;
    }

    @Override
    public Object[] getIds() {
        ArrayList<String> list = new ArrayList<String>();
        synchronized (context) {
            for (int scope : context.getScopes()) {
                Bindings bindings = context.getBindings(scope);
                if (bindings != null) {
                    list.ensureCapacity(bindings.size());
                    for (String key : bindings.keySet()) {
                        list.add(key);
                    }
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return ScriptableObject.getDefaultValue(this, hint);
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        Scriptable proto = instance.getPrototype();
        boolean has = false;
        while (proto != null) {
            if (proto.equals(this)) {
                has = true;
                break;
            }
            proto = proto.getPrototype();
        }
        return has;
    }
}
