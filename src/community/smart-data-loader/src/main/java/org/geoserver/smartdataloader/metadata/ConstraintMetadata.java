/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata;

/**
 * Class that represents metadata for constraints definitions on the underlying DataStore model.
 *
 * @author Jose Macchi - Geosolutions
 */
public abstract class ConstraintMetadata implements Comparable<ConstraintMetadata> {

    protected String name;

    public ConstraintMetadata(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
