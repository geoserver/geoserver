/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw.kvp;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.opengis.cat.csw20.GetDomainType;

/**
 * GetDomain KVP request reader
 * 
 * @author Andrea Aime, GeoSolutions
 */
public class GetDomainKvpRequestReader extends CSWKvpRequestReader {
    private static final String PROPERTYNAME = "PROPERTYNAME";

    public GetDomainKvpRequestReader() {
        super(GetDomainType.class);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // fix propertyName before we get into EMF reflection mode
        List propertyName = (List) kvp.remove(PROPERTYNAME);
        
        if (propertyName != null && propertyName.size() == 1)
        {
            Object property = null; 
            
            if (propertyName.get(0) instanceof List) {
                property = ((List)propertyName.get(0)).get(0);
            }
            
            if (property instanceof QName) {
                kvp.put(PROPERTYNAME, ((QName) property).getLocalPart());
            } else if (property instanceof String) {
                kvp.put(PROPERTYNAME, property);
            }
        }
        return super.read(request, kvp, rawKvp);
    }
}
