/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.gml3;

import org.geoserver.wfs.WFSException;
import org.geotools.geometry.jts.JTS;
import org.geotools.gml2.bindings.GML2ParsingUtils;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.PointOutsideEnvelopeException;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;

/**
 * Subclass of {@link org.geotools.gml3.bindings.AbstractGeometryTypeBinding} which performs some
 * addtional validation checks.
 *
 * <p>Checks include:
 *
 * <ul>
 *   <li>All geometries have a crs, when not specified, the server default is used.
 *   <li>If a crs is specified it has a valid authority
 *   <li>Points defined on geometries fall into the valid coordinate space defined by crs.
 * </ul>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class AbstractGeometryTypeBinding
        extends org.geotools.gml3.bindings.AbstractGeometryTypeBinding {

    CoordinateReferenceSystem crs;

    public AbstractGeometryTypeBinding() {
        super(null, null);
    }

    public void setCRS(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    public void initializeChildContext(
            ElementInstance childInstance, Node node, MutablePicoContainer context) {
        // if an srsName is set for this geometry, put it in the context for
        // children, so they can use it as well
        if (node.hasAttribute("srsName")) {
            try {
                CoordinateReferenceSystem crs = GML2ParsingUtils.crs(node);
                if (crs != null) {
                    context.registerComponentInstance(CoordinateReferenceSystem.class, crs);
                }
            } catch (Exception e) {
                throw new WFSException(e, "InvalidParameterValue");
            }
        }
    }

    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        try {
            if (node.hasAttribute("srsName")) {
                CRS.decode(node.getAttributeValue("srsName").toString());
            }
        } catch (NoSuchAuthorityCodeException e) {
            throw new WFSException(
                    "Invalid Authority Code: " + e.getAuthorityCode(), "InvalidParameterValue");
        }

        Geometry geometry = (Geometry) super.parse(instance, node, value);

        if (geometry != null) {
            // 1. ensure a crs is set
            if (geometry.getUserData() == null) {
                // no crs set for the geometry, did we inherit one from a parent?
                if (crs != null) {
                    geometry.setUserData(crs);
                }
                // else {
                // for the moment we don't do anything since we miss the information
                // to infer the CRS from the feature type
                // }
            }

            // 2. ensure the coordinates of the geometry fall into valid space defined by crs
            CoordinateReferenceSystem crs = (CoordinateReferenceSystem) geometry.getUserData();
            if (crs != null)
                try {
                    JTS.checkCoordinatesRange(geometry, crs);
                } catch (PointOutsideEnvelopeException e) {
                    throw new WFSException(e, "InvalidParameterValue");
                }
        }

        return geometry;
    }
}
