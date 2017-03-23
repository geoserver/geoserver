/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;

import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Provides access to OpenSearch for EO collections and products as an extension of {@link DataAccess} with well known feature types
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface OpenSearchAccess extends DataAccess<FeatureType, Feature> {

    public static String EO_NAMESPACE = "http://a9.com/-/opensearch/extensions/eo/1.0/";

    public static String GEO_NAMESPACE = "http://a9.com/-/opensearch/extensions/geo/1.0/";

    /**
     * The optional property in collection and product containing the metadata (ISO or O&M)
     */
    public static Name METADATA_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, "metadata");
    
    /**
     * The optional property in product containing the quicklook
     */
    public static Name QUICKLOOK_PROPERTY_NAME = new NameImpl(EO_NAMESPACE, "quicklook");

    /**
     * Classes of products
     *
     * @author Andrea Aime - GeoSolutions
     */
    enum ProductClass {
        EOP_GENERIC("eop", "http://www.opengis.net/eop/2.1"), //
        OPTICAL("opt", "http://www.opengis.net/opt/2.1"), //
        RADAR("sar", "http://www.opengis.net/sar/2.1"), //
        ALTIMETRIC("atm", "http://www.opengis.net/atm/2.1"), //
        LIMB("lmb", "http://www.opengis.net/lmb/2.1"), //
        SSP("ssp", "http://www.opengis.net/ssp/2.1");

        private String namespace;

        private String prefix;

        private ProductClass(String prefix, String namespace) {
            this.prefix = prefix;
            this.namespace = namespace;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getPrefix() {
            return prefix;
        }

    }

    /**
     * Returns the feature source backing collections (dynamic, as the store has to respect the namespace URI given by GeoServer)
     * 
     * @throws IOException
     * 
     */
    FeatureSource<FeatureType, Feature> getCollectionSource() throws IOException;

    /**
     * Returns the feature source backing products (dynamic, as the store has to respect the namespace URI given by GeoServer)
     * 
     */
    FeatureSource<FeatureType, Feature> getProductSource() throws IOException;
    
    

}
