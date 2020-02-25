/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.http.MediaType;

/** Lists the {@link Queryable} for a given collection */
@JsonPropertyOrder({"queryables", "links"})
public class QueryablesDocument extends AbstractDocument {

    private final String collectionId;
    List<Queryable> queryables = new ArrayList();

    public QueryablesDocument(FeatureTypeInfo fti) throws IOException {
        this.collectionId = fti.prefixedName();
        SimpleFeatureType ft = (SimpleFeatureType) fti.getFeatureType();
        this.queryables =
                ft.getAttributeDescriptors()
                        .stream()
                        .map(ad -> new Queryable(ad.getLocalName(), ad.getType().getBinding()))
                        .collect(Collectors.toList());

        addSelfLinks(
                "ogc/features/collections/" + ResponseUtils.urlEncode(fti.prefixedName()),
                MediaType.APPLICATION_JSON);
    }

    public List<Queryable> getQueryables() {
        return queryables;
    }

    @JsonIgnore // only for HTML representation
    public String getCollectionId() {
        return collectionId;
    }
}
