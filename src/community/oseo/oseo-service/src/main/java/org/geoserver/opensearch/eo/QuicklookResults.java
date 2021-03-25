/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Represents the result of a quicklook search
 *
 * @author Andrea Aime - GeoSolutions
 */
public class QuicklookResults {

    QuicklookRequest request;

    byte[] payload;

    String mimeType;

    public QuicklookResults(QuicklookRequest request, byte[] payload, String mimeType) {
        super();
        this.request = request;
        this.payload = payload;
        this.mimeType = mimeType;
    }

    public QuicklookRequest getRequest() {
        return request;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
