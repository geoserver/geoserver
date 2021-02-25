/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.flat;

/** Commong interface for all the flat builders */
public interface FlatBuilder {

    /**
     * Set the parent key attribute to the builder to concatenate it to children keys
     *
     * @param parentKey the parent key attribute to be concatenated
     */
    void setParentKey(String parentKey);
}
