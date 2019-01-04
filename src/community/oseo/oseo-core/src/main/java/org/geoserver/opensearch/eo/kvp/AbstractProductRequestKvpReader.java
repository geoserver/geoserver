/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.kvp;

import java.util.Map;
import org.geoserver.opensearch.eo.AbstractProductRequest;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.platform.OWS20Exception;

/**
 * Reads a generic product request
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractProductRequestKvpReader extends KvpRequestReader {

    private boolean parentIdRequired;

    public AbstractProductRequestKvpReader(Class requestBean, boolean parentIdRequired) {
        super(requestBean);
        this.parentIdRequired = parentIdRequired;
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        AbstractProductRequest apr = (AbstractProductRequest) super.read(request, kvp, rawKvp);

        // map uid
        String uid = (String) rawKvp.get("uid");
        if (uid == null) {
            throw new OWS20Exception(
                    "Missing mandatory uid parameter",
                    OWS20Exception.OWSExceptionCode.MissingParameterValue,
                    "uid");
        }
        apr.setId(uid);

        // check parentId if required
        if (parentIdRequired && apr.getParentId() == null) {
            throw new OWS20Exception(
                    "Missing mandatory parentId parameter",
                    OWS20Exception.OWSExceptionCode.MissingParameterValue,
                    "parentId");
        }

        return apr;
    }
}
