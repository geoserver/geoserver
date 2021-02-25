/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

/**
 * The context against which evaluate during the output encoding process It holds a reference to the
 * current Feature being evaluated as well as a reference to the parent context if source evaluation
 * happens during the encoding
 */
public class TemplateBuilderContext {

    private String currentSource;

    private Object currentObj;

    private TemplateBuilderContext parent;

    public TemplateBuilderContext(Object currentObj) {
        this.currentObj = currentObj;
    }

    public TemplateBuilderContext(Object currentObj, String currentSource) {
        this.currentObj = currentObj;
        this.currentSource = currentSource;
    }

    /**
     * Get the current object being evaluated
     *
     * @return the object being evaluated
     */
    public Object getCurrentObj() {
        return currentObj;
    }

    /**
     * Get the parent context
     *
     * @return the parent context
     */
    public TemplateBuilderContext getParent() {
        return parent;
    }

    /**
     * Set the parent context
     *
     * @param parent the parent context
     */
    public void setParent(TemplateBuilderContext parent) {
        this.parent = parent;
    }

    /**
     * Get the current source
     *
     * @return the current source
     */
    public String getCurrentSource() {
        return currentSource;
    }
}
