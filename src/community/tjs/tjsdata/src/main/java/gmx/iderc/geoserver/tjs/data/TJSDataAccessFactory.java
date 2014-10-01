/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data;

import org.geotools.data.DataAccessFactory.Param;
import org.geotools.factory.Factory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * @author root
 */
public interface TJSDataAccessFactory extends Factory {

    /**
     * Name suitable for display to end user.
     * <p/>
     * <p>
     * A non localized display name for this data store type.
     * </p>
     *
     * @return A short name suitable for display in a user interface.
     */
    String getDisplayName();

    /**
     * Describe the nature of the datasource constructed by this factory.
     * <p/>
     * <p>
     * A non localized description of this data store type.
     * </p>
     *
     * @return A human readable description that is suitable for inclusion in a
     *         list of available datasources.
     */
    String getDescription();

    /**
     * MetaData about the required Parameters (for createDataStore).
     * <p/>
     * <p>
     * Interpretation of FeatureDescriptor values:
     * </p>
     * <p/>
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
     * <p/>
     * <p>
     * This should be the same as:
     * </p>
     * <pre><code>
     * Object params = factory.getParameters();
     * BeanInfo info = getBeanInfo( params );
     * <p/>
     * return info.getPropertyDescriptors();
     * <code></pre>
     *
     * @return Param array describing the Map for createDataStore
     */
    Param[] getParametersInfo();

    /**
     * Test to see if this factory is suitable for processing the data pointed
     * to by the params map.
     * <p/>
     * <p>
     * If this datasource requires a number of parameters then this mehtod
     * should check that they are all present and that they are all valid. If
     * the datasource is a file reading data source then the extentions or
     * mime types of any files specified should be checked. For example, a
     * Shapefile datasource should check that the url param ends with shp,
     * such tests should be case insensative.
     * </p>
     *
     * @param params The full set of information needed to construct a live
     *               data source.
     * @return booean true if and only if this factory can process the resource
     *         indicated by the param set and all the required params are
     *         pressent.
     */
    boolean canProcess(java.util.Map<String, Serializable> params);

    /**
     * Test to see if the implementation is available for use.
     * This method ensures all the appropriate libraries to construct
     * the DataAccess are available.
     * <p/>
     * Most factories will simply return <code>true</code> as GeoTools will
     * distribute the appropriate libraries. Though it's not a bad idea for
     * DataStoreFactories to check to make sure that the  libraries are there.
     * <p/>
     * OracleDataStoreFactory is an example of one that may generally return
     * <code>false</code>, since GeoTools can not distribute the oracle jars.
     * (they must be added by the client.)
     * <p/>
     * One may ask how this is different than canProcess, and basically available
     * is used by the DataStoreFinder getAvailableDataStore method, so that
     * DataStores that can not even be used do not show up as options in gui
     * applications.
     *
     * @return <tt>true</tt> if and only if this factory has all the
     *         appropriate jars on the classpath to create DataStores.
     */
    boolean isAvailable();

    public TJSDataStore createDataStore(Map params) throws IOException;

    Map<String, Serializable> filterParamsForSave(Map params);

}
