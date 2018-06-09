/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.kvp;

import java.util.Map;
import org.geoserver.opensearch.eo.MetadataRequest;
import org.geoserver.platform.OWS20Exception;

/**
 * Reads a "metadata" request
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MetadataRequestKvpReader extends AbstractProductRequestKvpReader {

    public MetadataRequestKvpReader() {
        super(MetadataRequest.class, false);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        MetadataRequest mr = (MetadataRequest) super.read(request, kvp, rawKvp);

        // check httpAccept
        if (mr.getParentId() == null) {
            assertHttpAccept(mr, MetadataRequest.ISO_METADATA);
        } else {
            assertHttpAccept(mr, MetadataRequest.OM_METADATA);
        }

        return mr;
    }

    private void assertHttpAccept(MetadataRequest request, String expectedMime) {
        // be nice and default the value
        if (request.getHttpAccept() == null) {
            request.setHttpAccept(expectedMime);
        } else if (!expectedMime.equals(request.getHttpAccept())) {
            throw new OWS20Exception(
                    "Unexpected value for httpAccept '"
                            + request.getHttpAccept()
                            + "', in this context it must be "
                            + expectedMime,
                    OWS20Exception.OWSExceptionCode.InvalidParameterValue,
                    "httpAccept");
        }
    }
}
