/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links"})
@JacksonXmlRootElement(localName = "Collection", namespace = "http://www.opengis.net/wfs/3.0")
public class CollectionDocument extends AbstractCollectionDocument<Feature> {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);

    FeatureTypeInfo featureType;
    String mapPreviewURL;
    List<String> crs;

    public CollectionDocument(Feature feature) {
        super(feature);
        // basic info
        String collectionId = (String) feature.getProperty("name").getValue();
        this.id = collectionId;
        this.title = collectionId;
        this.description = (String) feature.getProperty("htmlDescription").getValue();
        ReferencedEnvelope bbox = ReferencedEnvelope.reference(feature.getBounds());
        //
        //        // links
        //        Collection<MediaType> formats =
        //                APIRequestInfo.get().getProducibleMediaTypes(FeaturesResponse.class,
        // true);
        //        String baseUrl = APIRequestInfo.get().getBaseURL();
        //        for (MediaType format : formats) {
        //            String apiUrl =
        //                    ResponseUtils.buildURL(
        //                            baseUrl,
        //                            "ogc/features/collections/" + collectionId + "/items",
        //                            Collections.singletonMap("f", format.toString()),
        //                            URLMangler.URLType.SERVICE);
        //            addLink(
        //                    new Link(
        //                            apiUrl,
        //                            Link.REL_ITEMS,
        //                            format.toString(),
        //                            collectionId + " items as " + format.toString(),
        //                            "items"));
        //        }
        //        addSelfLinks("ogc/features/collections/" + id);

    }
}
