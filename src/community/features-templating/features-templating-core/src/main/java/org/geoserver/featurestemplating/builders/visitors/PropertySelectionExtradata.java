/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.visitors;

/**
 * This class is meant to hold information about the keys of the builder node being parsed bu the
 * {@link PropertySelectionVisitor}. Namely if at least one dynamic parent key was found, or the
 * static full key of the current node.
 */
class PropertySelectionExtradata {

    private boolean dynamicKeyParent;
    private String staticFullKey;

    PropertySelectionExtradata(PropertySelectionExtradata extradata) {
        this.dynamicKeyParent = extradata.dynamicKeyParent;
        this.staticFullKey = extradata.staticFullKey;
    }

    PropertySelectionExtradata() {};

    /**
     * @return true if the current node as at least one parent with a dynamic key. false otherwise.
     */
    boolean isDynamicKeyParent() {
        return dynamicKeyParent;
    }

    /**
     * Set the dynamicKeyParent flag.
     *
     * @param dynamicKeyParent
     */
    void setDynamicKeyParent(boolean dynamicKeyParent) {
        this.dynamicKeyParent = dynamicKeyParent;
    }

    /**
     * @return the static full key of the current node. Should return null if at least one parent
     *     has a dynamic key.
     */
    String getStaticFullKey() {
        return staticFullKey;
    }

    /**
     * Set the static full key based on the current node.
     *
     * @param staticFullKey the static full key of the current node.
     */
    void setStaticFullKey(String staticFullKey) {
        this.staticFullKey = staticFullKey;
    }
}
