/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.visitors;

/**
 * This class is meant to hold information about the keys of the builder node being parsed by the
 * {@link PropertySelectionVisitor}. Namely, if at least one dynamic parent key was found, or if the
 * current builder has a dynamic key, or the static full key of the current node.
 */
public class PropertySelectionContext {

    private boolean dynamicKeyParent;
    private boolean dynamicKeyCurrent;
    private String staticFullKey;

    public PropertySelectionContext(PropertySelectionContext extradata) {
        this.staticFullKey = extradata.staticFullKey;
        if (extradata.isDynamicKeyCurrent()) this.dynamicKeyParent = true;
        this.dynamicKeyCurrent = false;
    }

    public PropertySelectionContext(
            String staticFullKey, boolean dynamicKeyCurrent, boolean dynamicKeyParent) {
        this.staticFullKey = staticFullKey;
        this.dynamicKeyCurrent = dynamicKeyCurrent;
        this.dynamicKeyParent = dynamicKeyParent;
    };

    public PropertySelectionContext() {};

    /**
     * @return true if the current node as at least one parent with a dynamic key. false otherwise.
     */
    public boolean isDynamicKeyParent() {
        return dynamicKeyParent;
    }

    /**
     * Set the dynamicKeyParent flag.
     *
     * @param dynamicKeyParent
     */
    public void setDynamicKeyParent(boolean dynamicKeyParent) {
        this.dynamicKeyParent = dynamicKeyParent;
    }

    /**
     * @return the static full key of the current node. Should return null if at least one parent
     *     has a dynamic key.
     */
    public String getStaticFullKey() {
        return staticFullKey;
    }

    /**
     * Set the static full key based on the current node.
     *
     * @param staticFullKey the static full key of the current node.
     */
    public void setStaticFullKey(String staticFullKey) {
        this.staticFullKey = staticFullKey;
    }

    /**
     * Does the current builder has a dynamic key?
     *
     * @return true if has, false otherwise.
     */
    public boolean isDynamicKeyCurrent() {
        return dynamicKeyCurrent;
    }

    /**
     * Set the flag for the dynamicKeyCurrent value.
     *
     * @param dynamicKeyCurrent
     */
    public void setDynamicKeyCurrent(boolean dynamicKeyCurrent) {
        this.dynamicKeyCurrent = dynamicKeyCurrent;
    }
}
