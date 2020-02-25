/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.kvp;

import java.util.ArrayList;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.wfs3.CollectionRequest;

/** Parses a "collection" request */
public class CollectionRequestKVPReader extends BaseKvpRequestReader {

    public CollectionRequestKVPReader() {
        super(CollectionRequest.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        CollectionRequest cr = (CollectionRequest) super.read(request, kvp, rawKvp);
        Object objTypeName = kvp.get("TYPENAME");
        if (objTypeName instanceof ArrayList) {
            QName name = (QName) ((ArrayList) objTypeName).get(0);
            cr.setTypeName(name);
        }

        return cr;
    }
}
