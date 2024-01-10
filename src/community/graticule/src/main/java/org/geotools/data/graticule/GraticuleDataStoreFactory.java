/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DataUtilities;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.KVP;

public class GraticuleDataStoreFactory implements DataStoreFactorySpi {
    static final Logger log = Logger.getLogger("GraticuleDataStoreFactory");

    /** Optional - uri of the FeatureType's namespace */
    public static final Param NAMESPACEP =
            new Param(
                    "namespace",
                    URI.class,
                    "uri to a the namespace",
                    false,
                    null, // not required
                    new KVP(Param.LEVEL, "advanced"));

    public static Param STEPS =
            new Param(
                    "steps",
                    List.class,
                    "A list of steps for the grids to be produced for",
                    true,
                    null) {
                @Override
                public Object parse(String text) throws Throwable {
                    return Arrays.stream(text.split("\\s*,\\s*"))
                            .map(Double::parseDouble)
                            .collect(Collectors.toList());
                }
            };

    public static final Param BOUNDS =
            new Param(
                    "bounds",
                    ReferencedEnvelope.class,
                    "The maximum bounding box for the grids in the projection that will be used for the grid",
                    true,
                    null) {
                @Override
                public Object parse(String text) {
                    // ReferencedEnvelope[-180.0 : 180.0, -90.0 : 90.0]
                    // DefaultGeographicCRS[EPSG:WGS 84] AXIS["Geodetic longitude", EAST]
                    // AXIS["Geodetic latitude", NORTH]
                    Pattern pat =
                            Pattern.compile(
                                    "\\[([-+]?[0-9]*\\.?[0-9]+) : ([-+]?[0-9]*\\.?[0-9]+), ([-+]?[0-9]*\\.?[0-9]+) : ([-+]?[0-9]*\\.?[0-9]+)\\] \\{(.*)\\}",
                                    Pattern.MULTILINE | Pattern.DOTALL);

                    Matcher m = pat.matcher(text);

                    m.find();
                    double minX = Double.parseDouble(m.group(1));
                    double maxX = Double.parseDouble(m.group(2));
                    double minY = Double.parseDouble(m.group(3));
                    double maxY = Double.parseDouble(m.group(4));
                    try {
                        CoordinateReferenceSystem crs =
                                org.geotools.referencing.CRS.parseWKT(m.group(5));
                        return new ReferencedEnvelope(minX, maxX, minY, maxY, crs);
                    } catch (FactoryException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public String text(Object value) {
                    ReferencedEnvelope env = (ReferencedEnvelope) value;

                    return "["
                            + env.getMinX()
                            + " : "
                            + env.getMaxX()
                            + ", "
                            + env.getMinY()
                            + " : "
                            + env.getMaxY()
                            + "] {"
                            + env.getCoordinateReferenceSystem().toWKT()
                            + "}";
                }
            };

    /** The type of the data store, used for identification purposes. */
    public static final String STORE_TYPE = "graticule";

    public static final Param TYPE =
            new Param("type", String.class, "the data store type (graticule)", true, STORE_TYPE);

    static final List<Param> params = new ArrayList<>();

    static {
        params.add(TYPE);
        params.add(NAMESPACEP);
        params.add(STEPS);
        params.add(BOUNDS);
    }

    @Override
    public String getDisplayName() {
        return "Graticule";
    }

    @Override
    public String getDescription() {
        return "Generate a set of graticules, with labels at the bounds of the display";
    }

    @Override
    public Param[] getParametersInfo() {
        return params.toArray(new Param[0]);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public DataStore createDataStore(Map<String, ?> params) throws IOException {
        List<Double> steps = (List<Double>) STEPS.lookUp(params);
        ReferencedEnvelope bounds = (ReferencedEnvelope) BOUNDS.lookUp(params);
        URI namespace = (URI) NAMESPACEP.lookUp(params);
        GraticuleDataStore ds = new GraticuleDataStore(bounds, steps);
        if (namespace != null) ds.setNamespaceURI(namespace.toString());
        return ds;
    }

    @Override
    public DataStore createNewDataStore(Map<String, ?> params) throws IOException {
        return createDataStore(params);
    }

    @Override
    public boolean canProcess(Map<String, ?> params) {
        if (!DataUtilities.canProcess(params, getParametersInfo())) return false;
        try {
            return STORE_TYPE.equals(TYPE.lookUp(params));
        } catch (IOException e) {
            return false;
        }
    }
}
