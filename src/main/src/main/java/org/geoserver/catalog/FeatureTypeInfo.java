/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.util.List;

import org.geoserver.config.GeoServerInfo;
import org.geotools.data.FeatureSource;
import org.geotools.factory.Hints;
import org.geotools.measure.Measure;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * A vector-based or feature based resource.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * @uml.dependency supplier="org.geoserver.catalog.FeatureResource"
 */
public interface FeatureTypeInfo extends ResourceInfo {
    
    /**
     * The sql view definition
     */
    static final String JDBC_VIRTUAL_TABLE = "JDBC_VIRTUAL_TABLE";
    
    /**
     * The cascaded stored query configuration
     */
    static final String STORED_QUERY_CONFIGURATION = "WFS_NG_STORED_QUERY_CONFIGURATION";

    /**
     * The data store the feature type is a part of.
     * <p>
     * </p>
     */
    DataStoreInfo getStore();
    
    /**
     * The attributes that the feature type exposes.
     * <p>
     * Services and client code will want to call the {@link #attributes()}
     * method over this one.
     * </p>
     */
    List<AttributeTypeInfo> getAttributes();
    
    /**
     * A filter which should be applied to all queries of the dataset
     * represented by the feature type.
     * 
     * @return A filter, or <code>null</code> if one not set.
     * @uml.property name="filter"
     */
    Filter getFilter();

    /**
     * Sets a filter which should be applied to all queries of the dataset
     * represented by the feature type.
     * 
     * @param filter
     *                A filter, can be <code>null</code>
     * @uml.property name="filter"
     */
    void setFilter(Filter filter);

    /**
     * A cap on the number of features that a query against this type can return.
     * <p>
     * Note that this value should override the global default: 
     *  {@link GeoServerInfo#getMaxFeatures()}.
     *  </p>
     */
    int getMaxFeatures();
    
    /**
     * Sets a cap on the number of features that a query against this type can return.
     * 
     */
    void setMaxFeatures( int maxFeatures );
    
    /**
     * The number of decimal places to use when encoding floating point 
     * numbers from data of this feature type.
     * <p>
     * Note that this value should override the global default: 
     *  {@link GeoServerInfo#getNumDecimals()}.
     *  </p>
     */
    int getNumDecimals();
    
    /**
     * Sets the number of decimal places to use when encoding floating point 
     * numbers from data of this feature type.
     */
    void setNumDecimals( int numDecimals );
    
    /**
     * Tolerance used to linearize this feature type, as an absolute value expressed in the
     * geometries own CRS
     * 
     * @return
     */
    Measure getLinearizationTolerance();

    /**
     * Tolerance used to linearize this feature type, as an absolute value expressed in the
     * geometries own CRS
     * 
     * @return
     */
    void setLinearizationTolerance(Measure tolerance);

    /**
     * True if this feature type info is overriding the WFS global SRS list
     * 
     * @return
     */
    boolean isOverridingServiceSRS();
    
    /**
     * Set to true if this feature type info is overriding the WFS global SRS list
     * @return
     */
    void setOverridingServiceSRS(boolean overridingServiceSRS);

    /**
     * True if this feature type info is overriding the counting of numberMatched.
     *
     * @return
     */
    boolean getSkipNumberMatched();

    /**
     * Set to true if this feature type info is overriding the default counting of numberMatched.
     * @param skipNumberMatched
     */
    void setSkipNumberMatched(boolean skipNumberMatched);

    /**
     * The srs's that the WFS service will advertise in the capabilities document for this feature type
     * (overriding the global WFS settings)
     */
    List<String> getResponseSRS();
    
    /**
     * Returns the derived set of attributes for the feature type.
     * <p>
     * This value is derived from the underlying feature, and any 
     * overrides configured via {@link #getAttributes()}.
     * </p>
     */
    List<AttributeTypeInfo> attributes() throws IOException;
    
    /**
     * Returns the underlying geotools feature type.
     * <p>
     * The returned feature type is "wrapped" to take into account "metadata", 
     * such as reprojection and name aliasing. 
     * </p>
     */
    FeatureType getFeatureType() throws IOException;
    
    /**
     * Returns the underlying feature source instance.
     * <p>
     * This method does I/O and is potentially blocking. The <tt>listener</tt>
     * may be used to report the progress of loading the feature source and also
     * to report any errors or warnings that occur.
     * </p>
     * 
     * @param listener
     *                A progress listener, may be <code>null</code>.
     * @param hints Hints to use while loading the featuer source, may be <code>null</code>.
     * 
     * @return The feature source.
     * 
     * @throws IOException
     *                 Any I/O problems.
     */
    FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource( ProgressListener listener, Hints hints )
            throws IOException;

	boolean isCircularArcPresent();
	
	void setCircularArcPresent(boolean arcsPresent);

    /**
     * The live feature resource, an instance of of {@link FeatureResource}.
     */
    //FeatureResource getResource(ProgressListener listener)
    //        throws IOException;
}
