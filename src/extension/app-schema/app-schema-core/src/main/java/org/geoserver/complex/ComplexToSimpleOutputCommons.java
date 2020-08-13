/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.complex;

import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.ows.Request;

/** Common constants and functions for complex to simple features conversion. */
class ComplexToSimpleOutputCommons {

    static final String TYPENAME = "TYPENAME";
    static final String TYPENAMES = "TYPENAMES";

    public static final String RULES_METADATAMAP_KEY = "ComplexToSimpleRules";

    /** Returns the request layer name from the OWS request data. */
    public static QName getLayerName(Request request) {
        Object typeNameObject = request.getKvp().get(TYPENAME);
        if (typeNameObject == null) typeNameObject = request.getKvp().get(TYPENAMES);
        @SuppressWarnings("unchecked")
        List<List<QName>> layerNamesList = (List<List<QName>>) typeNameObject;
        QName qName = layerNamesList.get(0).get(0);
        return qName;
    }
}
