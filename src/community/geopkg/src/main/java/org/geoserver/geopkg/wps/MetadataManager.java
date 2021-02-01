/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.fileupload.util.LimitedInputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.GeoPkgMetadata;
import org.geotools.geopkg.GeoPkgMetadataExtension;
import org.geotools.geopkg.GeoPkgMetadataReference;
import org.geotools.http.HTTPClient;
import org.geotools.http.HTTPClientFinder;
import org.geotools.http.HTTPResponse;
import org.geotools.util.logging.Logging;

/** Helper class that manages fetching and sharing metadata and references */
class MetadataManager {
    // TODO: make these limits configurable
    private static final long MAX_METADATA_SIZE_BYTES = 1024 * 1024 * 10;
    private static final int METADATA_CONNECT_TIMEOUT = 10;
    private static final int METADATA_READ_TIMEOUT = 30;
    private static final String DEFAULT_LINK = "http://www.geoserver.org/unkonwn/metadata/standard";

    static final Logger LOGGER = Logging.getLogger(MetadataManager.class);

    private final GeoPackage gpkg;
    private final SemanticAnnotationsExtension annotations;
    private final GeoPkgMetadataExtension metadatas;
    Map<String, Long> metadataLinks = new HashMap<>();

    public MetadataManager(GeoPackage gpkg) {
        this.gpkg = gpkg;
        this.metadatas = gpkg.getExtension(GeoPkgMetadataExtension.class);
        this.annotations = gpkg.getExtension(SemanticAnnotationsExtension.class);
    }

    public void addMetadata(FeatureTypeInfo ft) {
        List<MetadataLinkInfo> links = ft.getMetadataLinks();
        if (links == null) return;

        for (MetadataLinkInfo link : links) {
            try {
                Long metadataId = metadataLinks.get(link.getContent());
                GeoPkgMetadata metadata;
                if (metadataId == null) {
                    metadata = addMetadata(ft, metadatas, annotations, link);
                    metadataLinks.put(link.getContent(), metadata.getId());
                } else {
                    metadata = metadatas.getMetadata(metadataId);
                }

                GeoPkgMetadataReference reference =
                        new GeoPkgMetadataReference(
                                GeoPkgMetadataReference.Scope.Table,
                                ft.getName(),
                                null,
                                null,
                                new Date(),
                                metadata,
                                null);
                metadatas.addReference(reference);
            } catch (Exception e) {
                LOGGER.log(
                        Level.FINE,
                        "Skipping metadata " + link + " as an error occurred during its processing",
                        e);
            }
        }
    }

    private GeoPkgMetadata addMetadata(
            FeatureTypeInfo ft,
            GeoPkgMetadataExtension metadatas,
            SemanticAnnotationsExtension annotations,
            MetadataLinkInfo link)
            throws SQLException, IOException {
        String metadataBody = getMetadataBody(link);
        String metadataURI = Optional.ofNullable(link.getAbout()).orElse(DEFAULT_LINK);
        GeoPkgMetadata metadata =
                new GeoPkgMetadata(
                        GeoPkgMetadata.Scope.Dataset, metadataURI, link.getType(), metadataBody);
        metadatas.addMetadata(metadata);

        return metadata;
    }

    private String getMetadataBody(MetadataLinkInfo link) throws IOException {
        HTTPClient client = HTTPClientFinder.createClient();
        client.setConnectTimeout(METADATA_CONNECT_TIMEOUT);
        client.setReadTimeout(METADATA_READ_TIMEOUT);
        HTTPResponse response = client.get(new URL(link.getContent()));
        try (InputStream is =
                new LimitedInputStream(response.getResponseStream(), MAX_METADATA_SIZE_BYTES) {
                    @Override
                    protected void raiseError(long pSizeMax, long pCount) throws IOException {
                        throw new IOException(
                                "Metadata document size exceeds maximum size of "
                                        + pSizeMax / 1024
                                        + " KBs");
                    }
                }) {
            return IOUtils.toString(
                    is, Optional.ofNullable(response.getResponseCharset()).orElse("UTF-8"));
        }
    }
}
