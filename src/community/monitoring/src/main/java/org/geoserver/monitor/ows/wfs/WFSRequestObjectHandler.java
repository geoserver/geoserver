/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wfs;

import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public abstract class WFSRequestObjectHandler extends RequestObjectHandler {

    Catalog catalog;
    
    protected WFSRequestObjectHandler(String reqObjClassName, Catalog catalog) {
        super(reqObjClassName);
        this.catalog = catalog;
    }

    protected String toString(Object name) {
        if (name instanceof QName) {
            QName qName = (QName) name;
            String prefix = qName.getPrefix();
            if (prefix == null || "".equals(prefix)) {
                prefix = qName.getNamespaceURI();
            }
            if (prefix == null || "".equals(prefix)) {
                prefix = null;
            }

            return prefix != null ? prefix + ":" + qName.getLocalPart() : qName.getLocalPart();
        }
        else {
            return name.toString();
        }
         
    }
    
    /**
     * Look up the CRS of the specified FeatureType
     */
    protected CoordinateReferenceSystem crsFromTypeNames(QName typeName) {
            FeatureTypeInfo featureType = catalog.getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());
            
            return featureType.getCRS();
    }

}
