/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wfs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.geoserver.catalog.Catalog;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.xsd.EMFUtils;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TransactionHandler extends WFSRequestObjectHandler {

    public TransactionHandler(MonitorConfig config, Catalog catalog) {
        super("net.opengis.wfs.TransactionType", config, catalog);
    }

    @Override
    public void handle(Object request, RequestData data) {
        super.handle(request, data);

        // also determine the sub operation
        FeatureMap elements = (FeatureMap) EMFUtils.get((EObject) request, "group");
        if (elements == null) {
            return;
        }

        ListIterator<Object> i = elements.valueListIterator();
        int flag = 0;
        while (i.hasNext()) {
            Object e = i.next();
            if (e.getClass().getSimpleName().startsWith("Insert")) {
                flag |= 1;
            } else if (e.getClass().getSimpleName().startsWith("Update")) {
                flag |= 2;
            } else if (e.getClass().getSimpleName().startsWith("Delete")) {
                flag |= 4;
            } else {
                flag |= 8;
            }
        }

        StringBuffer sb = new StringBuffer();
        if ((flag & 1) == 1) sb.append("I");
        if ((flag & 2) == 2) sb.append("U");
        if ((flag & 4) == 4) sb.append("D");
        if ((flag & 8) == 8) sb.append("O");
        data.setSubOperation(sb.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getLayers(Object request) {
        FeatureMap elements = (FeatureMap) EMFUtils.get((EObject) request, "group");
        if (elements == null) {
            return null;
        }

        List<String> layers = new ArrayList<String>();
        ListIterator<Object> i = elements.valueListIterator();
        while (i.hasNext()) {
            Object e = i.next();
            if (EMFUtils.has((EObject) e, "typeName")) {
                Object typeName = EMFUtils.get((EObject) e, "typeName");
                if (typeName != null) {
                    layers.add(toString(typeName));
                }
            } else {
                // this is most likely an insert, determine layers from feature collection
                if (isInsert(e)) {
                    List<Feature> features = (List<Feature>) EMFUtils.get((EObject) e, "feature");
                    Set<String> set = new LinkedHashSet<String>();
                    for (Feature f : features) {
                        if (f instanceof SimpleFeature) {
                            set.add(((SimpleFeature) f).getType().getTypeName());
                        } else {
                            set.add(f.getType().getName().toString());
                        }
                    }

                    layers.addAll(set);
                }
            }
        }

        return layers;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Object> getElements(Object request) {
        return (List<Object>) OwsUtils.get(request, "group");
    }

    @Override
    protected Object unwrapElement(Object element) {
        // For some reason it's wrapped inside an extra EMF object here but not in the other
        // request types
        return OwsUtils.get(element, "value");
    }

    boolean isInsert(Object element) {
        return element.getClass().getSimpleName().startsWith("InsertElementType");
    }

    @Override
    protected ReferencedEnvelope getBBoxFromElement(Object element) {
        if (isInsert(element)) {
            // check for srsName on insert element
            ReferencedEnvelope bbox = null;
            if (OwsUtils.has(element, "srsName")) {
                Object srs = OwsUtils.get(element, "srsName");
                CoordinateReferenceSystem crs = crs(srs);

                if (crs != null) {
                    bbox = new ReferencedEnvelope(crs);
                    bbox.setToNull();
                }
            }

            // go through all the features and aggregate the bounding boxes
            for (Feature f : (List<Feature>) OwsUtils.get(element, "feature")) {
                BoundingBox fbbox = f.getBounds();
                if (fbbox == null) {
                    continue;
                }

                if (bbox == null) {
                    bbox = new ReferencedEnvelope(fbbox);
                }
                bbox.include(fbbox);
            }

            return bbox;
        }
        return null;
    }

    @Override
    protected CoordinateReferenceSystem getCrsFromElement(Object element) {
        // special case for insert
        if (isInsert(element) && OwsUtils.has(element, "srsName")) {
            CoordinateReferenceSystem crs = crs(OwsUtils.get(element, "srsName"));
            if (crs != null) {
                return crs;
            }
        }

        return super.getCrsFromElement(element);
    }

    CoordinateReferenceSystem crs(Object srs) {
        try {
            return srs != null ? CRS.decode(srs.toString()) : null;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }
        return null;
    }
}
