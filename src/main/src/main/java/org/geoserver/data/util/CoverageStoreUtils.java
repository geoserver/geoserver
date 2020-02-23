/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * A collection of utilties for dealing with GeotTools Format.
 *
 * @author Richard Gould, Refractions Research, Inc.
 * @author cholmesny
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 * @version $Id: CoverageStoreUtils.java,v 1.12 2004/09/21 21:14:48 cholmesny Exp $
 */
public final class CoverageStoreUtils {
    public static final Format[] formats = GridFormatFinder.getFormatArray();

    private CoverageStoreUtils() {}

    public static Format acquireFormat(String type) throws IOException {
        Format[] formats = GridFormatFinder.getFormatArray();
        Format format = null;
        final int length = formats.length;

        for (int i = 0; i < length; i++) {
            if (formats[i].getName().equals(type)) {
                format = formats[i];

                break;
            }
        }

        if (format == null) {
            throw new IOException("Cannot handle format: " + type);
        } else {
            return format;
        }
    }

    /** Utility method for finding Params */
    public static ParameterValue find(Format format, String key) {
        return find(format.getReadParameters(), key);
    }

    /** Utility methods for find param by key */
    public static ParameterValue find(ParameterValueGroup params, String key) {
        List list = params.values();
        Iterator it = list.iterator();
        ParameterDescriptor descr;
        ParameterValue val;

        while (it.hasNext()) {
            val = (ParameterValue) it.next();
            descr = (ParameterDescriptor) val.getDescriptor();

            if (key.equalsIgnoreCase(descr.getName().toString())) {
                return val;
            }
        }

        return null;
    }

    /**
     * When loading from DTO use the params to locate factory.
     *
     * <p>bleck
     */
    public static Format aquireFactoryByType(String type) {
        final Format[] formats = GridFormatFinder.getFormatArray();
        Format format = null;
        final int length = formats.length;

        for (int i = 0; i < length; i++) {
            format = formats[i];

            if (format.getName().equals(type)) {
                return format;
            }
        }

        return null;
    }

    /** After user has selected Description can aquire Format based on description. */
    public static Format aquireFactory(String description) {
        Format[] formats = GridFormatFinder.getFormatArray();
        Format format = null;
        final int length = formats.length;

        for (int i = 0; i < length; i++) {
            format = formats[i];

            if (format.getDescription().equals(description)) {
                return format;
            }
        }

        return null;
    }

    /**
     * Returns the descriptions for the available DataFormats.
     *
     * <p>Arrrg! Put these in the select box.
     *
     * @return Descriptions for user to choose from
     */
    public static List listDataFormatsDescriptions() {
        List list = new ArrayList();
        Format[] formats = GridFormatFinder.getFormatArray();
        final int length = formats.length;

        for (int i = 0; i < length; i++) {
            if (!list.contains(formats[i].getDescription())) {
                list.add(formats[i].getDescription());
            }
        }

        return Collections.synchronizedList(list);
    }

    public static List listDataFormats() {
        List list = new ArrayList();
        Format[] formats = GridFormatFinder.getFormatArray();
        final int length = formats.length;

        for (int i = 0; i < length; i++) {
            if (!list.contains(formats[i])) {
                list.add(formats[i]);
            }
        }

        return Collections.synchronizedList(list);
    }

    public static Map defaultParams(String description) {
        return Collections.synchronizedMap(defaultParams(aquireFactory(description)));
    }

    public static Map defaultParams(Format factory) {
        Map defaults = new HashMap();
        ParameterValueGroup params = factory.getReadParameters();

        if (params != null) {
            List list = params.values();
            Iterator it = list.iterator();
            ParameterDescriptor descr = null;
            ParameterValue val = null;
            String key;
            Object value;

            while (it.hasNext()) {
                val = (ParameterValue) it.next();
                descr = (ParameterDescriptor) val.getDescriptor();

                key = descr.getName().toString();
                value = null;

                if (val.getValue() != null) {
                    // Required params may have nice sample values
                    //
                    if ("values_palette".equalsIgnoreCase(key)) {
                        value = val.getValue();
                    } else {
                        value = val.getValue().toString();
                    }
                }

                if (value == null) {
                    // or not
                    value = "";
                }

                if (value != null) {
                    defaults.put(key, value);
                }
            }
        }

        return Collections.synchronizedMap(defaults);
    }

    /**
     * Convert map to real values based on factory Params.
     *
     * @return Map with real values that may be acceptable to GDSFactory
     */
    public static Map toParams(GridFormatFactorySpi factory, Map params) throws IOException {
        final Map map = new HashMap(params.size());

        final ParameterValueGroup info = factory.createFormat().getReadParameters();
        // Convert Params into the kind of Map we actually need
        for (Iterator i = params.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            Object value = find(info, key).getValue();
            if (value != null) {
                map.put(key, value);
            }
        }

        return Collections.synchronizedMap(map);
    }

    /** Retrieve a WGS84 lon,lat envelope from the provided one. */
    public static GeneralEnvelope getWGS84LonLatEnvelope(final GeneralEnvelope envelope)
            throws IndexOutOfBoundsException, FactoryException, TransformException {
        final CoordinateReferenceSystem sourceCRS = envelope.getCoordinateReferenceSystem();

        ////
        //
        // Do we need to transform?
        //
        ////
        if (CRS.equalsIgnoreMetadata(sourceCRS, DefaultGeographicCRS.WGS84)) {
            return new GeneralEnvelope(envelope);
        }

        ////
        //
        // transform
        //
        ////
        final CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
        final GeneralEnvelope targetEnvelope;
        if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            targetEnvelope = CRS.transform(envelope, targetCRS);
        } else {
            targetEnvelope = new GeneralEnvelope(envelope);
        }

        targetEnvelope.setCoordinateReferenceSystem(targetCRS);

        return targetEnvelope;
    }
}
