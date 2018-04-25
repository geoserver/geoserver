/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.Parameter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.springframework.context.ApplicationContext;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Represents the input / output of a parameter in a process.
 * <p>
 * Instances of this interface are registered in a spring context to handle additional types of
 * </p>
 *
 * @author Lucas Reed, Refractions Research Inc
 * @author Justin Deoliveira, OpenGEO
 *
 */
public abstract class ProcessParameterIO {

    /**
     * PPIO possible direction:
     * <ul>
     * <li>encoding : PPIO suitable only for outputs</li>
     * <li>decoding : PPIO suitable only for inputs</li>
     * <li>both : PPIO suitable both for inputs and outputs</li>
     * </ul>
     */
    public enum PPIODirection {
        /** Only encoding supported, PPIO suitable only for outputs */
        ENCODING,
        /** Only decoding supported, PPIO suitable only for inputs */
        DECODING,
        /** Both encoding and decoding supported */
        BOTH
    };

    /**
     * list of default ppios supported out of the box
     */
    static List<ProcessParameterIO> defaults;
    static {
        defaults = new ArrayList<ProcessParameterIO>();

        // primitives
        defaults.add(new LiteralPPIO(BigInteger.class));
        defaults.add(new LiteralPPIO(BigDecimal.class));
        defaults.add(new LiteralPPIO(Double.class));
        defaults.add(new LiteralPPIO(double.class));
        defaults.add(new LiteralPPIO(Float.class));
        defaults.add(new LiteralPPIO(float.class));
        defaults.add(new LiteralPPIO(Integer.class));
        defaults.add(new LiteralPPIO(int.class));
        defaults.add(new LiteralPPIO(Long.class));
        defaults.add(new LiteralPPIO(long.class));
        defaults.add(new LiteralPPIO(Short.class));
        defaults.add(new LiteralPPIO(short.class));
        defaults.add(new LiteralPPIO(Byte.class));
        defaults.add(new LiteralPPIO(byte.class));
        defaults.add(new LiteralPPIO(Number.class));

        defaults.add(new LiteralPPIO(Boolean.class));
        defaults.add(new LiteralPPIO(boolean.class));

        defaults.add(new LiteralPPIO(String.class));
        defaults.add(new LiteralPPIO(CharSequence.class));

        defaults.add(new LiteralPPIO(Date.class));
        defaults.add(new LiteralPPIO(java.sql.Date.class));
        defaults.add(new LiteralPPIO(Time.class));
        defaults.add(new LiteralPPIO(Timestamp.class));

        // geometries
        defaults.add(new GMLPPIO.GML3.Geometry());
        defaults.add(new GMLPPIO.GML2.Geometry());
        defaults.add(new WKTPPIO());
        defaults.add(new GeoJSONPPIO.Geometries());
        defaults.add(new GMLPPIO.GML3.GeometryAlternate());
        defaults.add(new GMLPPIO.GML2.GeometryAlternate());

        // features
        defaults.add(new WFSPPIO.WFS10());
        defaults.add(new WFSPPIO.WFS11());
        defaults.add(new GeoJSONPPIO.FeatureCollections());
        defaults.add(new WFSPPIO.WFS10Alternate());
        defaults.add(new WFSPPIO.WFS11Alternate());

        // CRS
        defaults.add(new CoordinateReferenceSystemPPIO());

        // grids
        defaults.add(new GeoTiffPPIO());
        defaults.add(new ArcGridPPIO());
        defaults.add(new CoveragePPIO.PNGPPIO());
        defaults.add(new CoveragePPIO.JPEGPPIO());

        defaults.add(new ImagePPIO.PNGPPIO());
        defaults.add(new ImagePPIO.JPEGPPIO());

        // envelopes
        defaults.add(new BoundingBoxPPIO(ReferencedEnvelope.class));
        defaults.add(new BoundingBoxPPIO(org.opengis.geometry.Envelope.class));
        defaults.add(new BoundingBoxPPIO(Envelope.class));

        // filters
        defaults.add(new FilterPPIO.Filter10());
        defaults.add(new FilterPPIO.Filter11());
        defaults.add(new CQLFilterPPIO());
    }

    public static ProcessParameterIO find(Parameter<?> p, ApplicationContext context, String mime) {
        // enum special treatment
        if (p.type.isEnum()) {
            return new EnumPPIO(p.type);
        }

        // TODO: come up with some way to flag one as "default"
        List<ProcessParameterIO> all = findAll(p, context);
        if (all.isEmpty()) {
            return null;
        }

        if (mime != null) {
            for (ProcessParameterIO ppio : all) {
                if (ppio instanceof ComplexPPIO && ((ComplexPPIO) ppio).getMimeType().equals(mime)) {
                    return ppio;
                }
            }
        }

        // fall back on the first found
        return all.get(0);
    }

    public static List<ProcessParameterIO> findAll(Parameter<?> p, ApplicationContext context) {
        // enum special treatment
        if (p.type.isEnum()) {
            List<ProcessParameterIO> result = new ArrayList<ProcessParameterIO>();
            result.add(new EnumPPIO(p.type));
            return result;
        }

        // load all extensions
        List<ProcessParameterIO> l = new ArrayList<ProcessParameterIO>(defaults);
        if (context != null) {
            l.addAll(GeoServerExtensions.extensions(ProcessParameterIO.class, context));
        } else {
            l.addAll(GeoServerExtensions.extensions(ProcessParameterIO.class));
        }

        // load by factory
        List<PPIOFactory> ppioFactories;
        if (context != null) {
            ppioFactories = GeoServerExtensions.extensions(PPIOFactory.class, context);
        } else {
            ppioFactories = GeoServerExtensions.extensions(PPIOFactory.class);
        }
        for (PPIOFactory factory : ppioFactories) {
            l.addAll(factory.getProcessParameterIO());
        }

        // find parameters that match
        List<ProcessParameterIO> matches = new ArrayList<ProcessParameterIO>();

        // do a two phase search, first try to match the identifier
        for (ProcessParameterIO ppio : l) {
            if (ppio.getIdentifer() != null && ppio.getIdentifer().equals(p.key)
                    && ppio.getType().isAssignableFrom(p.type)) {
                matches.add(ppio);
            }
        }

        // if no matches, look for just those which match by type
        if (matches.isEmpty()) {
            for (ProcessParameterIO ppio : l) {
                if (ppio.getType().isAssignableFrom(p.type)) {
                    matches.add(ppio);
                }
            }
        }

        return matches;
    }

    /*
     * Look for PPIO matching the parameter type and suitable for direction handling
     */
    private static List<ProcessParameterIO> findByDirection(Parameter<?> p,
            ApplicationContext context, PPIODirection direction) {
        List<ProcessParameterIO> ppios = new ArrayList<ProcessParameterIO>();
        List<ProcessParameterIO> matches = findAll(p, context);
        for (ProcessParameterIO ppio : matches) {
            if (ppio.getDirection() == PPIODirection.BOTH || ppio.getDirection() == direction) {
                ppios.add(ppio);
            }
        }
        return ppios;
    }

    /*
     * Look for PPIO matching the parameter type and suitable for output handling
     */
    public static List<ProcessParameterIO> findEncoder(Parameter<?> p, ApplicationContext context) {
        return findByDirection(p, context, PPIODirection.ENCODING);
    }

    /*
     * Look for PPIO matching the parameter type and suitable for input handling
     */
    public static List<ProcessParameterIO> findDecoder(Parameter<?> p, ApplicationContext context) {
        return findByDirection(p, context, PPIODirection.DECODING);
    }

    /**
     * Returns true if the specified parameter is a complex one
     *
     * @param param
     * @param applicationContext
     *
     */
    public static boolean isComplex(Parameter<?> param, ApplicationContext applicationContext) {
        List<ProcessParameterIO> ppios = findAll(param, applicationContext);
        if (ppios.isEmpty()) {
            return false;
        } else {
            return ppios.get(0) instanceof ComplexPPIO;
        }
    }

    /**
     * java class of parameter when reading and writing i/o.
     */
    final protected Class externalType;

    /**
     * java class of parameter when running internal process.
     */
    final protected Class internalType;

    /**
     * identifier for the parameter
     */
    protected String identifer;

    protected ProcessParameterIO(Class externalType, Class internalType) {
        this(externalType, internalType, null);
    }

    protected ProcessParameterIO(Class externalType, Class internalType, String identifier) {
        this.externalType = externalType;
        this.internalType = internalType;
        this.identifer = identifier;
    }

    /**
     * The type of the parameter with regard to doing I/O.
     * <p>
     * The external type is used when reading and writing the parameter from an external source.
     * </p>
     */
    public final Class getExternalType() {
        return externalType;
    }

    /**
     * The type of the parameter corresponding to {@link Parameter#type}.
     * <p>
     * The internal type is used when going to and from the internal process engine.
     * </p>
     */
    public final Class getType() {
        return internalType;
    }

    /**
     * The identifier for the parameter, this value may be null.
     */
    public final String getIdentifer() {
        return identifer;
    }

    /**
     * Used to advertise if the PPIO can support encoding, decoding, or both. By default BOTH is returned, subclass can override with their specific
     * abilities
     */
    public PPIODirection getDirection() {
        return PPIODirection.BOTH;
    }

}
