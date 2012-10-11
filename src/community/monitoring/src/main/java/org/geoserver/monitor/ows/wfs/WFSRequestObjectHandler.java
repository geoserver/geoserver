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
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public abstract class WFSRequestObjectHandler extends RequestObjectHandler {

    Catalog catalog;
    protected WFSRequestObjectHandler(String reqObjClassName, MonitorConfig config, Catalog catalog) {
        super(reqObjClassName, config);
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
    protected CoordinateReferenceSystem crsFromTypeName(QName typeName) {
            FeatureTypeInfo featureType = catalog.getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());
            
            return featureType.getCRS();
    }
    
    // There are two slight differences between the different WFS requests in how they store
    // their filters.  These methods are overridden to regularize these differences.
    protected abstract List<Object> getElements(Object request);
    
    protected Object unwrapElement(Object element){
        return element;
    }
    protected CoordinateReferenceSystem getCrsFromElement(Object element) {
        return crsFromTypeName((QName) OwsUtils.get(element, "typeName"));
    }
    
    @Override
    protected BoundingBox getBBox(Object request) {
        List<Object> elements = getElements(request);
        if (elements==null) return null;

        BoundingBox result = new ReferencedEnvelope(monitorConfig.getBboxLogCrs());
        for(Object e : elements){
            e = unwrapElement(e);
            CoordinateReferenceSystem defaultCrs = getCrsFromElement(e);
            
            if (defaultCrs==null) return null;
            
            BBoxFilterVisitor visitor = new BBoxFilterVisitor(monitorConfig.getBboxLogCrs(), defaultCrs);
            Filter f = (Filter) OwsUtils.get(e, "filter");
            if(f!=null) f.accept(visitor, null);
            result.include(visitor.getBbox());
        }
        return result;
    }

}
