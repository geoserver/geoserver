/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.kvp;

import org.geoserver.wfs3.CollectionsRequest;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Map;

/**
 * Parses a "content" request
 */
public class CollectionsRequestKVPReader extends BaseKvpRequestReader {

    public CollectionsRequestKVPReader() {
        super(CollectionsRequest.class);
    }

    @Override
    
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        CollectionsRequest cr = (CollectionsRequest) super.read(request, kvp, rawKvp);
        Object objTypeName = kvp.get("TYPENAME");
        if (objTypeName instanceof ArrayList) {
            QName name = (QName) ((ArrayList) objTypeName).get(0);
            cr.setTypeName(name);
        }
        
        return cr;
    }
}
