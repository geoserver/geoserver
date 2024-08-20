/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wfs20;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.ows.wfs.WFSRequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.xsd.EMFUtils;

public class GetFeature20Handler extends WFSRequestObjectHandler {

    public GetFeature20Handler(MonitorConfig config, Catalog catalog) {
        super("net.opengis.wfs20.GetFeatureType", config, catalog);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getLayers(Object request) {
        List<Object> queries =
                (List<Object>) EMFUtils.get((EObject) request, "abstractQueryExpression");
        if (queries == null) {
            return null;
        }

        List<String> layers = new ArrayList<>();
        for (Object q : queries) {
            List<Object> typeNames = (List<Object>) EMFUtils.get((EObject) q, "typeNames");

            for (Object o : typeNames) {
                layers.add(toString(o));
            }
        }
        return layers;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Object> getElements(Object request) {
        return (List<Object>) OwsUtils.get(request, "abstractQueryExpression");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected CoordinateReferenceSystem getCrsFromElement(Object element) {
        List<Object> types = (List<Object>) OwsUtils.get(element, "typeNames");
        if (types.size() == 1) {
            return crsFromTypeName((QName) types.get(0));
        } else {
            return null;
        }
    }
}
