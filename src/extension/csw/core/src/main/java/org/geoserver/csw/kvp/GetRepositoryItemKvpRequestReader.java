/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import java.util.Map;
import org.geoserver.csw.GetRepositoryItemType;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/**
 * GetRepositoryItemBean KVP request reader
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class GetRepositoryItemKvpRequestReader extends KvpRequestReader {

    // private Service csw;

    public GetRepositoryItemKvpRequestReader(Service csw) {
        super(GetRepositoryItemType.class);
        // this.csw = csw;
    }

    @Override
    public Object read(Object req, Map kvp, Map rawKvp) throws Exception {

        GetRepositoryItemType request = (GetRepositoryItemType) super.read(req, kvp, rawKvp);

        if (request.getId() == null) {
            throw new ServiceException(
                    "ID parameter not provided for GetRepositoryItemBean operation",
                    ServiceException.MISSING_PARAMETER_VALUE,
                    "id");
        }

        return request;
    }
}
