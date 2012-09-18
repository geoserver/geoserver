/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import java.util.Map;

import org.geoserver.csw.GetRepositoryItemBean;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/**
 * GetRepositoryItemBean KVP request reader
 * 
 * @author Alessio Fabiani, GeoSolutions
 */
public class GetRepositoryItemKvpRequestReader extends KvpRequestReader {

    private Service csw;
    
    public GetRepositoryItemKvpRequestReader(Service csw) {
        super(GetRepositoryItemBean.class);
        this.csw = csw;
    }

    @Override
    public Object read(Object req, Map kvp, Map rawKvp) throws Exception {

        GetRepositoryItemBean request = (GetRepositoryItemBean) super.read(req, kvp, rawKvp);
        
        final String version = request.getVersion();
        if (null == version) {
            String code = "NoVersionInfo";
            String simpleName = getClass().getSimpleName();
            throw new ServiceException(
                    "Version parameter not provided for GetRepositoryItemBean operation", code, simpleName);
        }
        
        if (!csw.getVersion().toString().equals(version)) {
            throw new ServiceException("Wrong value for version parameter: " + version
                    + ". This server accetps version " + csw.getVersion(), "InvalidVersion",
                    getClass().getSimpleName());
        }
        
        if (null == request.getService())
        {
            request.setService(csw.getId());
        }
        
        if (request.getId() == null)
        {
            String code = "NoID";
            String simpleName = getClass().getSimpleName();
            throw new ServiceException(
                    "ID parameter not provided for GetRepositoryItemBean operation", code, simpleName);
        }
        
        return request;
    }

}
