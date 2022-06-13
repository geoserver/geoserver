/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import java.io.IOException;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.ComplexMetadataMap;

public interface GeonetworkImportService {

    void importLayer(ResourceInfo resource, ComplexMetadataMap map, String geonetwork, String uuid)
            throws IOException;
}
