/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import java.io.IOException;
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
import org.geotools.geometry.jts.ReferencedEnvelope;

public class GraticuleDataStoreFactory implements DataStoreFactorySpi {
    static final Logger log = Logger.getLogger("GraticuleDataStoreFactory");

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
    public static final Param TYPE =
            new Param("type", String.class, "the data store type (graticule)", true, "graticule");

    static final List<Param> params = new ArrayList<>();

    static {
        params.add(TYPE);
        params.add(STEPS);
        params.add(BOUNDS);
    }

    /**
     * Name suitable for display to end user.
     *
     * <p>A non localized display name for this data store type.
     *
     * @return A short name suitable for display in a user interface.
     */
    @Override
    public String getDisplayName() {
        return "Graticule";
    }

    /**
     * Describe the nature of the datasource constructed by this factory.
     *
     * <p>A non localized description of this data store type.
     *
     * @return A human readable description that is suitable for inclusion in a list of available
     *     datasources.
     */
    @Override
    public String getDescription() {
        return "Generate a set of graticules, with labels at the bounds of the display";
    }

    /**
     * MetaData about the required Parameters (for createDataStore).
     *
     * <p>
     * Interpretation of FeatureDescriptor values:
     * </p>
     *
     * <ul>
     * <li>
     * getDisplayName(): Gets the localized display name of this feature.
     * </li>
     * <li>
     * getName(): Gets the programmatic name of this feature (used as the key
     * in params)
     * </li>
     * <li>
     * getShortDescription(): Gets the short description of this feature.
     * </li>
     * </ul>
     *
     * <p>
     * This should be the same as:
     * </p>
     * <pre><code>
     * Object params = factory.getParameters();
     * BeanInfo info = getBeanInfo( params );
     *
     * return info.getPropertyDescriptors();
     * <code></pre>
     *
     * @return Param array describing the Map for createDataStore
     */
    @Override
    public Param[] getParametersInfo() {
        return params.toArray(new Param[0]);
    }

    /**
     * Test to see if the implementation is available for use. This method ensures all the
     * appropriate libraries to construct the DataAccess are available.
     *
     * <p>Most factories will simply return <code>true</code> as GeoTools will distribute the
     * appropriate libraries. Though it's not a bad idea for DataStoreFactories to check to make
     * sure that the libraries are there.
     *
     * <p>OracleDataStoreFactory is an example of one that may generally return <code>false</code>,
     * since GeoTools can not distribute the oracle jars. (they must be added by the client.)
     *
     * <p>One may ask how this is different than canProcess, and basically available is used by the
     * DataStoreFinder getAvailableDataStore method, so that DataStores that can not even be used do
     * not show up as options in gui applications.
     *
     * @return <tt>true</tt> if and only if this factory has all the appropriate jars on the
     *     classpath to create DataStores.
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Construct a live data source using the params specifed.
     *
     * <p>You can think of this as setting up a connection to the back end data source.
     *
     * <p>Magic Params: the following params are magic and are honoured by convention by the
     * GeoServer and uDig application.
     *
     * <ul>
     *   <li>"user": is taken to be the user name
     *   <li>"passwd": is taken to be the password
     *   <li>"namespace": is taken to be the namespace prefix (and will be kept in sync with
     *       GeoServer namespace management.
     * </ul>
     *
     * <p>When we eventually move over to the use of OpperationalParam we will have to find someway
     * to codify this convention.
     *
     * @param params The full set of information needed to construct a live data store. Typical key
     *     values for the map include: url - location of a resource, used by file reading
     *     datasources. dbtype - the type of the database to connect to, e.g. postgis, mysql
     * @return The created DataStore, this may be null if the required resource was not found or if
     *     insufficent parameters were given. Note that canProcess() should have returned false if
     *     the problem is to do with insuficent parameters.
     * @throws IOException if there were any problems setting up (creating or connecting) the
     *     datasource.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public DataStore createDataStore(Map<String, ?> params) throws IOException {
        List<Double> steps = lookup(STEPS, params, List.class);
        ReferencedEnvelope bounds = lookup(BOUNDS, params, ReferencedEnvelope.class);
        return new GraticuleDataStore(bounds, steps);
    }

    @Override
    public DataStore createNewDataStore(Map<String, ?> params) throws IOException {
        return createDataStore(params);
    }

    /**
     * Looks up a parameter, if not found it returns the default value, assuming there is one, or
     * null otherwise
     *
     * @param <T>
     */
    <T> T lookup(Param param, Map<String, ?> params, Class<T> target) throws IOException {
        log.finest("Looking up " + param.key + " in " + params);
        T result = target.cast(param.lookUp(params));
        if (result == null) {
            result = target.cast(param.getDefaultValue());
        }
        return result;
    }
}
