/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.builders.impl;

public class JsonBuilderContext {

    private String currentSource;

    private Object currentObj;

    private JsonBuilderContext parent;

    public JsonBuilderContext(Object currentObj) {
        this.currentObj = currentObj;
    }

    public JsonBuilderContext(Object currentObj, String currentSource) {
        this.currentObj = currentObj;
        this.currentSource = currentSource;
    }

    public Object getCurrentObj() {
        return currentObj;
    }

    public JsonBuilderContext getParent() {
        return parent;
    }

    public void setParent(JsonBuilderContext parent) {
        this.parent = parent;
    }

    public String getCurrentSource() {
        return currentSource;
    }
}
