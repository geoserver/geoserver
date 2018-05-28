/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs3.BaseRequest;
import org.geoserver.wfs3.DefaultWebFeatureService30;
import org.geoserver.wfs3.NCNameResourceCodec;
import org.geoserver.wfs3.WFSExtents;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"name", "title", "description", "extent", "links"})
public class CollectionDocument {
    String name;
    String title;
    String description;
    WFSExtents extent;
    List<Link> links = new ArrayList<>();

    public CollectionDocument(BaseRequest request, FeatureTypeInfo featureType) {
        // basic info
        String collectionId = NCNameResourceCodec.encode(featureType);
        setName(collectionId);
        setTitle(featureType.getTitle());
        setDescription(featureType.getDescription());
        ReferencedEnvelope bbox = featureType.getLatLonBoundingBox();
        setExtent(new WFSExtents(bbox));

        // links
        List<String> formats =
                DefaultWebFeatureService30.getAvailableFormats(FeatureCollectionResponse.class);
        String baseUrl = request.getBaseUrl();
        for (String format : formats) {
            String apiUrl =
                    ResponseUtils.buildURL(
                            baseUrl,
                            "wfs3/collections/" + collectionId + "/items",
                            Collections.singletonMap("f", format),
                            URLMangler.URLType.SERVICE);
            addLink(new Link(apiUrl, Link.REL_ABOUT, format, collectionId + " as " + format));
        }
    }

    @JacksonXmlProperty(localName = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JacksonXmlProperty(localName = "Title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JacksonXmlProperty(localName = "Description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WFSExtents getExtent() {
        return extent;
    }

    public void setExtent(WFSExtents extent) {
        this.extent = extent;
    }

    @JacksonXmlProperty(namespace = Link.ATOM_NS, localName = "link")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> getLinks() {
        return links;
    }

    public void addLink(Link link) {
        links.add(link);
    }
}
