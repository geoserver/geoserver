/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Provides access to OpenSearch for EO collections and products as an extension of {@link
 * DataAccess} with well known feature types
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface OpenSearchAccess extends DataAccess<FeatureType, Feature> {

    public static String EO_NAMESPACE = "http://a9.com/-/opensearch/extensions/eo/1.0/";

    public static String GEO_NAMESPACE = "http://a9.com/-/opensearch/extensions/geo/1.0/";

    /** Internal attribute pointing to the original package location */
    public static String ORIGINAL_PACKAGE_LOCATION = "originalPackageLocation";

    /** Internal attribute stating he original package mime type */
    public static String ORIGINAL_PACKAGE_TYPE = "originalPackageType";

    /** The optional property in collection and product containing the metadata (ISO or O&M) */
    public static Name METADATA_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, "metadata");

    /**
     * The optional property in collection and product containing the OGC links (it's a collection)
     */
    public static Name OGC_LINKS_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, "ogcLinks");

    /** The optional property in product containing the quicklook */
    public static Name QUICKLOOK_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, "quicklook");

    /**
     * Local part of the optional collection property containing the layers publishing information
     */
    public static String LAYERS = "layers";

    /** The optional property in collection containing the published layers */
    public static Name LAYERS_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, LAYERS);

    /**
     * Local part of the HTML description property. The namespace is the one assigned to the store,
     * this is not an EO property
     */
    public static String DESCRIPTION = "htmlDescription";

    /**
     * Local part of the product granules property. The namespace is the one assigned to the store,
     * this is not an EO property
     */
    public static String GRANULES = "granules";

    /**
     * Just like in WCS 2.0, setting up a separator that's unlikely to be found in the wild, since
     * there is no option that's absolutely unique
     */
    String BAND_LAYER_SEPARATOR = "__";

    /**
     * Returns the feature source backing collections (dynamic, as the store has to respect the
     * namespace URI given by GeoServer)
     */
    FeatureSource<FeatureType, Feature> getCollectionSource() throws IOException;

    /**
     * Returns the feature source backing products (dynamic, as the store has to respect the
     * namespace URI given by GeoServer)
     */
    FeatureSource<FeatureType, Feature> getProductSource() throws IOException;

    /** Returns a feature source to access the granules of a particular product */
    SimpleFeatureSource getGranules(String collectionId, String productId) throws IOException;

    SimpleFeatureType getCollectionLayerSchema() throws IOException;

    SimpleFeatureType getOGCLinksSchema() throws IOException;
}
