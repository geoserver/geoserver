/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp.v2_0;

import java.util.Map;

import net.opengis.wfs20.DescribeStoredQueriesType;
import net.opengis.wfs20.Wfs20Factory;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.kvp.WFSKvpRequestReader;
import org.geotools.xml.EMFUtils;

public class DescribeStoredQueriesKvpRequestReader extends WFSKvpRequestReader {

    public DescribeStoredQueriesKvpRequestReader() {
        super(DescribeStoredQueriesType.class, Wfs20Factory.eINSTANCE);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        EObject obj = (EObject) super.read(request, kvp, rawKvp);
        
        //handle storedQuery_id parameter
        if (kvp.containsKey("storedQuery_id")) {
            EMFUtils.add(obj, "storedQueryId", kvp.get("storedQuery_id"));
        }
        return obj;
    }
}
