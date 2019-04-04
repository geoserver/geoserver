/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
        Object propertyName = kvp.remove(PROPERTYNAME);

        if (propertyName != null) {
            if (propertyName instanceof List && ((List) propertyName).size() > 0) {
                Object property = null;

                if (((List) propertyName).get(0) instanceof List) {
                    property = ((List) ((List) propertyName).get(0)).get(0);
                }

                if (property instanceof QName) {
                    kvp.put(PROPERTYNAME, ((QName) property).getLocalPart());
                } else if (property instanceof String) {
                    kvp.put(PROPERTYNAME, property);
                }
            } else if (propertyName instanceof String) {
                kvp.put(PROPERTYNAME, propertyName);
            }
        }
        return super.read(request, kvp, rawKvp);
    }
}
