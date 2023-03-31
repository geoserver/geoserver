/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.LinksBuilder;
import org.geotools.dggs.gstore.DGGSStore;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

/** List of DAPA variables for a collection */
public class DAPAVariables extends AbstractDocument {

    String collectionId;
    List<DAPAVariable> variables;

    public DAPAVariables(String collectionId, FeatureTypeInfo info) throws IOException {
        Set<String> excludedAttributes = getExcludedAttributes(info);
        SimpleFeatureType schema = (SimpleFeatureType) info.getFeatureType();
        this.collectionId = collectionId;
        this.variables =
                schema.getAttributeDescriptors().stream()
                        .filter(
                                ad ->
                                        !(ad instanceof GeometryDescriptor)
                                                && !excludedAttributes.contains(ad.getLocalName()))
                        .map(ad -> new DAPAVariable(ad))
                        .collect(Collectors.toList());
        addSelfLinks("ogc/dggs/v1/collections/" + collectionId + "/dapa/variables");
        new LinksBuilder(CollectionDocument.class, "ogc/dggs/collections/")
                .segment(collectionId, true)
                .title("foobar")
                .rel("collection")
                .add(this);
    }

    public Set<String> getExcludedAttributes(FeatureTypeInfo info) {
        Set<String> excludedAttributes = new HashSet<>();
        excludedAttributes.add(DGGSStore.ZONE_ID);
        excludedAttributes.add(DGGSStore.RESOLUTION);
        DimensionInfo time = info.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time != null) {
            excludedAttributes.add(time.getAttribute());
            if (time.getEndAttribute() != null) excludedAttributes.add(time.getEndAttribute());
        }
        return excludedAttributes;
    }

    public List<DAPAVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<DAPAVariable> variables) {
        this.variables = variables;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }
}
