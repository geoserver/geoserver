/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import org.geoserver.metadata.data.dto.CustomNativeMappingsConfiguration;
import org.geoserver.metadata.data.dto.GeonetworkMappingConfiguration;
import org.geoserver.metadata.data.dto.MetadataConfiguration;

/**
 * Service responsible for interaction with yaml files. It will search for all *.yaml files in a
 * given directory and try to parse the files. Yaml files that cannot do parsed will be ignored.
 *
 * @author Timothy De Bock
 */
public interface ConfigurationService {

    MetadataConfiguration getMetadataConfiguration();

    GeonetworkMappingConfiguration getGeonetworkMappingConfiguration();

    CustomNativeMappingsConfiguration getCustomNativeMappingsConfiguration();
}
