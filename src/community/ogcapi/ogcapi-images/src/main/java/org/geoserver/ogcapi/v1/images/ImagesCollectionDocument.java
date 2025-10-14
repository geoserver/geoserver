/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.images;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ogcapi.Link;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.springframework.http.MediaType;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links"})
public class ImagesCollectionDocument extends AbstractCollectionDocument<CoverageInfo> {
    static final Logger LOGGER = Logging.getLogger(ImagesCollectionDocument.class);
    StructuredGridCoverage2DReader reader;

    /**
     * Builds a description of an image collection
     *
     * @param coverage The {@link CoverageInfo} that backs the images collection
     * @param summary If true, the info provided is minimal and assumed to be part of a
     *     {@link ImagesCollectionsDocument}, otherwise it's full and assumed to be the main response
     */
    public ImagesCollectionDocument(CoverageInfo coverage, boolean summary)
            throws FactoryException, TransformException, IOException {
        super(coverage);
        // basic info
        this.id = coverage.prefixedName();
        this.title = coverage.getTitle();
        this.description = coverage.getAbstract();

        String baseURL = APIRequestInfo.get().getBaseURL();
        this.extent = new CollectionExtents(coverage.getLatLonBoundingBox());

        // backlinks in same and other formats
        addSelfLinks("ogc/images/v1/collections/" + id);

        // add links to the images resource
        Collection<MediaType> imagesFormats = APIRequestInfo.get().getProducibleMediaTypes(ImagesResponse.class, true);
        for (MediaType format : imagesFormats) {
            String metadataURL = buildURL(
                    baseURL,
                    "ogc/images/v1/collections/" + ResponseUtils.urlEncode(id) + "/images",
                    Collections.singletonMap("f", format.toString()),
                    URLMangler.URLType.SERVICE);

            Link link = new Link(metadataURL, "images", format.toString(), "The images metadata as " + format);
            addLink(link);
        }
    }
}
