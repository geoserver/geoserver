/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.Ows11Factory;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.wps.WPSException;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;

/**
 * Process parameter input / output for bounding boxes
 *
 * @author Andrea Aime, OpenGeo
 */
public class BoundingBoxPPIO extends ProcessParameterIO {

    public BoundingBoxPPIO(Class<?> type) {
        super(type, type);
    }

    /**
     * Decodes the parameter from an external source or input stream.
     *
     * <p>This method should parse the input stream into its "internal" representation.
     *
     * @return An object of type {@link #getType()}.
     */
    public Object decode(BoundingBoxType boundingBoxType) throws Exception {
        if (boundingBoxType == null) {
            return null;
        } else {
            return toTargetType(boundingBoxType);
        }
    }

    @SuppressWarnings("unchecked") // EMF model without generics...
    private Object toTargetType(BoundingBoxType bbox) throws Exception {
        CoordinateReferenceSystem crs = null;
        if (bbox.getCrs() != null) {
            crs = CRS.decode(bbox.getCrs());
        }

        double[] lower = ordinates(bbox.getLowerCorner());
        double[] upper = ordinates(bbox.getUpperCorner());

        if (ReferencedEnvelope.class.isAssignableFrom(getType()) || BoundingBox.class.isAssignableFrom(getType())) {
            return new ReferencedEnvelope(lower[0], upper[0], lower[1], upper[1], crs);
        } else if (Envelope.class.isAssignableFrom(getType())) {
            return new Envelope(lower[0], upper[0], lower[1], upper[1]);
        } else if (org.geotools.api.geometry.Bounds.class.isAssignableFrom(getType())) {
            GeneralBounds ge = new GeneralBounds(lower, upper);
            ge.setCoordinateReferenceSystem(crs);
            return ge;
        } else {
            throw new WPSException("Failed to convert from OWS 1.1 Bounding box type "
                    + "to the internal representation: "
                    + getType());
        }
    }

    double[] ordinates(List<Double> corner) {
        Double[] objects = corner.toArray(new Double[corner.size()]);
        double[] result = new double[objects.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = objects[i];
        }
        return result;
    }

    /**
     * Encodes the internal representation of the object to an XML stream.
     *
     * @param object An object of type {@link #getType()}.
     */
    public BoundingBoxType encode(Object object) throws WPSException {
        if (object == null) {
            throw new IllegalArgumentException("Cannot encode a null bounding box");
        }
        return fromTargetType(object);
    }

    BoundingBoxType fromTargetType(Object object) throws WPSException {
        Ows11Factory factory = Ows11Factory.eINSTANCE;
        BoundingBoxType bbox = factory.createBoundingBoxType();

        // basic conversion and collect the crs
        CoordinateReferenceSystem crs = null;
        if (object instanceof Envelope env) {
            if (object instanceof ReferencedEnvelope re) {
                crs = re.getCoordinateReferenceSystem();
            }
            bbox.setLowerCorner(Arrays.asList(env.getMinX(), env.getMinY()));
            bbox.setUpperCorner(Arrays.asList(env.getMaxX(), env.getMaxY()));
        } else if (org.geotools.api.geometry.Bounds.class.isAssignableFrom(getType())) {
            org.geotools.api.geometry.Bounds env = (org.geotools.api.geometry.Bounds) object;
            crs = env.getCoordinateReferenceSystem();
            bbox.setLowerCorner(doubleArrayToList(env.getLowerCorner().getCoordinate()));
            bbox.setUpperCorner(doubleArrayToList(env.getUpperCorner().getCoordinate()));
        } else {
            throw new WPSException("Failed to convert from " + object + " to an OWS 1.1 Bounding box type");
        }

        // handle the EPSG code
        if (crs != null) {
            try {
                bbox.setCrs(ResourcePool.lookupIdentifier(crs, false));
            } catch (Exception e) {
                throw new WPSException("Could not lookup epsg code for " + crs, e);
            }
        }

        return bbox;
    }

    private List<Double> doubleArrayToList(double[] coordinate) {
        return Arrays.stream(coordinate).boxed().collect(Collectors.toList());
    }
}
