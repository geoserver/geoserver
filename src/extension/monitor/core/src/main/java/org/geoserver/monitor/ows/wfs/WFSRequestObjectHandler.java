/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wfs;

import java.util.List;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.filter.visitor.ExtractBoundsFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.spatial.BBOX;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

public abstract class WFSRequestObjectHandler extends RequestObjectHandler {

    static final Logger LOGGER = Logging.getLogger(WFSRequestObjectHandler.class);

    // TODO: this should probably be handled as an update or extension to ExtractBoundsFilterVisitor
    ExtractBoundsFilterVisitor visitor =
            new ExtractBoundsFilterVisitor() {
                public Object visit(BBOX filter, Object data) {
                    if (data == null) {
                        return null;
                    }

                    BoundingBox bbox = (BoundingBox) data;

                    BoundingBox bounds;
                    try {
                        bounds = filter.getBounds();
                        if (bounds.getCoordinateReferenceSystem() == null) {
                            bounds =
                                    ReferencedEnvelope.create(
                                            bounds, bbox.getCoordinateReferenceSystem());
                        }
                        bounds.toBounds(monitorConfig.getBboxCrs());
                    } catch (TransformException ex) {
                        // We're stuck so give up.
                        return null;
                    }

                    bbox.include(bounds);
                    return bbox;
                }
            };

    Catalog catalog;

    protected WFSRequestObjectHandler(
            String reqObjClassName, MonitorConfig config, Catalog catalog) {
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
        } else {
            return name.toString();
        }
    }

    /** Look up the CRS of the specified FeatureType */
    protected CoordinateReferenceSystem crsFromTypeName(QName typeName) {
        FeatureTypeInfo featureType =
                catalog.getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());

        return featureType.getCRS();
    }

    // There are two slight differences between the different WFS requests in how they store
    // their filters.  These methods are overridden to regularize these differences.
    protected abstract List<Object> getElements(Object request);

    protected Object unwrapElement(Object element) {
        return element;
    }

    protected CoordinateReferenceSystem getCrsFromElement(Object element) {
        if (OwsUtils.has(element, "typeName")) {
            return crsFromTypeName((QName) OwsUtils.get(element, "typeName"));
        }
        return null;
    }

    protected ReferencedEnvelope getBBoxFromElement(Object element) {
        return null;
    }

    @Override
    protected BoundingBox getBBox(Object request) {
        if (monitorConfig.getBboxMode() != MonitorConfig.BboxMode.FULL) {
            return null;
        }

        List<Object> elements = getElements(request);
        if (elements == null) return null;

        try {
            BoundingBox result = new ReferencedEnvelope(monitorConfig.getBboxCrs());
            for (Object e : elements) {
                e = unwrapElement(e);

                // first ask for a bounding box directly
                ReferencedEnvelope bbox = getBBoxFromElement(e);
                if (bbox == null) {
                    // try to infer it from a filter of the request
                    // This is the default CRS for the layer being queried
                    CoordinateReferenceSystem defaultCrs = getCrsFromElement(e);
                    if (defaultCrs == null) {
                        continue;
                    }

                    Filter f =
                            OwsUtils.has(e, "filter") ? (Filter) OwsUtils.get(e, "filter") : null;
                    if (f != null) {
                        ReferencedEnvelope startingBbox = new ReferencedEnvelope(defaultCrs);
                        bbox = (ReferencedEnvelope) f.accept(visitor, startingBbox);
                    }
                }

                if (bbox != null) {
                    result.include(bbox.toBounds(monitorConfig.getBboxCrs()));
                }
            }
            return result;
        } catch (TransformException ex) {
            LOGGER.warning("Could not Transform bounds to desired CRS");
            return null;
        }
    }
}
