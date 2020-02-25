/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.dto.GeonetworkConfiguration;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.metadata.data.service.GeonetworkImportService;
import org.geoserver.metadata.data.service.GeonetworkXmlParser;
import org.geoserver.metadata.data.service.RemoteDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
public class GeonetworkImportServiceImpl implements GeonetworkImportService {

    @Autowired private ConfigurationService service;

    @Autowired private RemoteDocumentReader geonetworkReader;

    @Autowired private GeonetworkXmlParser xmlParser;

    @Autowired private ComplexMetadataService mapService;

    @Override
    public void importLayer(
            ResourceInfo resource, ComplexMetadataMap map, String geonetwork, String uuid)
            throws IOException {
        Document doc =
                geonetworkReader.readDocument(new URL(generateMetadataUrl(geonetwork, uuid)));
        xmlParser.parseMetadata(doc, resource, map);
        mapService.init(map); // fix all multi-valued complex fields
    }

    private String generateMetadataUrl(String geonetworkName, String uuid) {
        String url = null;

        if (geonetworkName != null) {
            for (GeonetworkConfiguration geonetwork :
                    service.getMetadataConfiguration().getGeonetworks()) {
                if (geonetworkName.equals(geonetwork.getName())) {
                    url = geonetwork.getUrl();
                }
            }
        }
        if (url == null) {
            throw new IllegalArgumentException("Unknown geonetwork: " + geonetworkName);
        }

        return PlaceHolderUtil.replacePlaceHolders(url, Collections.singletonMap("UUID", uuid));
    }
}
