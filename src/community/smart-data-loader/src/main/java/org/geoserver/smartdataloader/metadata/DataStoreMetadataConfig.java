/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata;

import java.util.Map;

/**
 * Configuration class that determines the type of DataStoreMetadata that the
 * DataStoreMetadataFactory will build.
 *
 * @author Jose Macchi - Geosolutions
 */
public abstract class DataStoreMetadataConfig {

    public abstract String getName();

    public abstract String getType();

    public abstract Map<String, String> getParameters();
}
