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
    public Object read(Object request, Map<String, Object> kvp, Map<String, Object> rawKvp) throws Exception {
        // fix propertyName before we get into EMF reflection mode
        Object propertyName = kvp.remove(PROPERTYNAME);

        if (propertyName != null) {
            if (propertyName instanceof List list && !list.isEmpty()) {
                Object property = null;

                if (list.get(0) instanceof List) {
                    property = ((List) list.get(0)).get(0);
                }

                if (property instanceof QName name) {
                    kvp.put(PROPERTYNAME, name.getLocalPart());
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
