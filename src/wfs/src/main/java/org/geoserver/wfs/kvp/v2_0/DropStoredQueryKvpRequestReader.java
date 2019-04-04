/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp.v2_0;

import java.net.URI;
import java.util.List;
import java.util.Map;
import net.opengis.wfs20.DropStoredQueryType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.kvp.WFSKvpRequestReader;
import org.geotools.xsd.EMFUtils;

public class DropStoredQueryKvpRequestReader extends WFSKvpRequestReader {

    public DropStoredQueryKvpRequestReader() {
        super(DropStoredQueryType.class, Wfs20Factory.eINSTANCE);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        EObject obj = (EObject) super.read(request, kvp, rawKvp);

        // handle storedQuery_id parameter
        if (kvp.containsKey("storedQuery_id")) {
            // we get it back as a list of uri
            List<URI> list = (List<URI>) kvp.get("storedQuery_id");
            EMFUtils.set(obj, "id", list.get(0).toString());
        }
        return obj;
    }
}
