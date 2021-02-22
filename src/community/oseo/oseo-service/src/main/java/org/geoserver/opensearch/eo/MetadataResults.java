/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Represents the result of a search for metadata on a collection or product
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MetadataResults {

    MetadataRequest request;

    String metadata;

    public MetadataResults(MetadataRequest request, String metadata) {
        super();
        this.request = request;
        this.metadata = metadata;
    }

    public MetadataRequest getRequest() {
        return request;
    }

    public String getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
