/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service;

import java.io.IOException;
import java.io.Serializable;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.w3c.dom.Document;

public interface GeonetworkXmlParser extends Serializable {

    void parseMetadata(Document doc, ResourceInfo rInfo, ComplexMetadataMap metadataMap)
            throws IOException;
}
