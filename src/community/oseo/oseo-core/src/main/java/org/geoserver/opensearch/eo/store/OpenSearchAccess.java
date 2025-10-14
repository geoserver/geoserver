/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.List;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.feature.NameImpl;

/**
 * Provides access to OpenSearch for EO collections and products as an extension of {@link DataAccess} with well known
 * feature types
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

    /** The optional property in collection and product containing the OGC links (it's a collection) */
    public static Name OGC_LINKS_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, "ogcLinks");

    /** The optional property in product containing the quicklook */
    public static Name QUICKLOOK_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, "quicklook");

    /** The optional property in product containing the collection and most of its attributes */
    public static Name COLLECTION_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, "collection");

    /** The collection identifier property */
    public static Name EO_IDENTIFIER = new NameImpl(EO_NAMESPACE, "identifier");

    /** Local part of the optional collection property containing the layers publishing information */
    public static String LAYERS = "layers";

    /** The collection layer property containing the style list */
    String STYLES = "styles";

    /** The collection layer property containing the service list */
    String SERVICES = "services";

    /** The property in collection layers containing the styles */
    public static Name SERVICES_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, SERVICES);

    /**
     * Local part of the HTML description property. The namespace is the one assigned to the store, this is not an EO
     * property
     */
    public static String DESCRIPTION = "htmlDescription";

    /**
     * Local part of the product granules property. The namespace is the one assigned to the store, this is not an EO
     * property
     */
    public static String GRANULES = "granules";

    /** Property used to enable/disable products and collections */
    public static final String ENABLED = "enabled";

    /** The collection layer title, picked from the GeoServer configuration */
    public static final String LAYER_TITLE = "title";

    /** The collection layer description, picked from the GeoServer configuration */
    public static final String LAYER_DESCRIPTION = "description";

    /**
     * Just like in WCS 2.0, setting up a separator that's unlikely to be found in the wild, since there is no option
     * that's absolutely unique
     */
    String BAND_LAYER_SEPARATOR = "__";

    /** Attribute prefix, to be used when encoding XML outputs or referencing attributes in feature templates. */
    String PREFIX = "prefix";

    /**
     * Returns the feature source backing collections (dynamic, as the store has to respect the namespace URI given by
     * GeoServer)
     */
    FeatureSource<FeatureType, Feature> getCollectionSource() throws IOException;

    /**
     * Updates indexes on the given collection, based on a set of indexables. Compares with existing indexes and
     * creates/removes them as needed.
     */
    void updateIndexes(String collection, List<Indexable> indexables) throws IOException;

    /** Get List of Index Names for a Table */
    List<String> getIndexNames(String tableName) throws IOException;

    /**
     * Returns the feature source backing products (dynamic, as the store has to respect the namespace URI given by
     * GeoServer)
     */
    FeatureSource<FeatureType, Feature> getProductSource() throws IOException;

    /** Returns a feature source to access the granules of a particular product */
    SimpleFeatureSource getGranules(String collectionId, String productId) throws IOException;

    /**
     * Returns the feature source backing the collection layers (used for writes, when fetching the collection
     * containign the layers, the layers property contains complex features)
     */
    SimpleFeatureType getCollectionLayerSchema() throws IOException;

    SimpleFeatureType getOGCLinksSchema() throws IOException;

    /**
     * Returns the default namespace for this {@link OpenSearchAccess}
     *
     * @return
     */
    String getNamespaceURI();

    /** Returns the qualified name for the given local part */
    public default Name getName(String localPart) {
        return new NameImpl(getNamespaceURI(), localPart);
    }
}
