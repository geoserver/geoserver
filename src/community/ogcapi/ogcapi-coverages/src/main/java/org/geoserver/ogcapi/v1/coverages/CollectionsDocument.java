/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;

/**
 * A class representing the Coverages server "collections" in a way that Jackson can easily
 * translate to JSON/YAML (and can be used as a Freemarker template model)
 */
@JsonPropertyOrder({"links", "collections"})
public class CollectionsDocument extends AbstractDocument {

    static final Logger LOGGER = Logging.getLogger(CollectionsDocument.class);

    private final GeoServer geoServer;
    private final List<String> crs;

    public CollectionsDocument(GeoServer geoServer, List<String> crsList) {
        this.geoServer = geoServer;
        this.crs = crsList;

        // build the self links
        String path = "ogc/coverages/v1/collections/";
        addSelfLinks(path);
    }

    @Override
    @JacksonXmlProperty(localName = "Links")
    public List<Link> getLinks() {
        return links;
    }

    @JacksonXmlProperty(localName = "Collection")
    @SuppressWarnings("PMD.CloseResource")
    public Iterator<CollectionDocument> getCollections() {
        CloseableIterator<CoverageInfo> coverages =
                geoServer.getCatalog().list(CoverageInfo.class, Filter.INCLUDE);
        return new Iterator<CollectionDocument>() {

            CollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }

                try {
                    while (coverages.hasNext()) {
                        CoverageInfo coverage = coverages.next();
                        try {
                            List<String> crs =
                                    CoveragesService.getCoverageCRS(
                                            coverage, Collections.singletonList("#/crs"));
                            CollectionDocument collection =
                                    new CollectionDocument(geoServer, coverage, crs);

                            next = collection;
                            return true;
                        } catch (Exception e) {
                            // e.g., maybe the plugin to read the format is missing
                            LOGGER.log(
                                    Level.WARNING,
                                    "Failed to build collection for "
                                            + coverage.prefixedName()
                                            + ", skipping",
                                    e);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed while iterating over collections", e);
                }
                coverages.close();
                return false;
            }

            @Override
            public CollectionDocument next() {
                CollectionDocument result = next;
                this.next = null;
                return result;
            }
        };
    }

    public List<String> getCrs() {
        return crs;
    }
}
