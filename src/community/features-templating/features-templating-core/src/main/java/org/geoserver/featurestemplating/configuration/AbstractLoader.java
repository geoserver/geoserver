/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.data.complex.feature.type.ComplexFeatureTypeImpl;
import org.geotools.data.complex.feature.type.Types;
import org.xml.sax.helpers.NamespaceSupport;

/** Manage the cache and the retrieving for all templates files */
public abstract class AbstractLoader {

    protected GeoServerDataDirectory dataDirectory;

    public AbstractLoader(GeoServerDataDirectory dd) {
        this.dataDirectory = dd;
    }

    /**
     * Extract Namespaces from given FeatureType
     *
     * @return Namespaces if found for the given FeatureType
     */
    private NamespaceSupport declareNamespaces(FeatureType type) {
        NamespaceSupport namespaceSupport = null;
        if (type instanceof ComplexFeatureTypeImpl) {
            Map namespaces = (Map) type.getUserData().get(Types.DECLARED_NAMESPACES_MAP);
            if (namespaces != null) {
                namespaceSupport = new NamespaceSupport();
                for (Iterator it = namespaces.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    String prefix = (String) entry.getKey();
                    String namespace = (String) entry.getValue();
                    namespaceSupport.declarePrefix(prefix, namespace);
                }
            }
        }
        return namespaceSupport;
    }

    protected GeoServerDataDirectory getDataDirectory() {
        return dataDirectory;
    }

    protected class CacheKey {
        private FeatureTypeInfo resource;
        private String identifier;

        public CacheKey(FeatureTypeInfo resource, String identifier) {
            this.resource = resource;
            this.identifier = identifier;
        }

        public FeatureTypeInfo getResource() {
            return resource;
        }

        public String getIdentifier() {
            return identifier;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CacheKey)) return false;
            CacheKey other = (CacheKey) o;
            if (!other.getIdentifier().equals(identifier)) return false;
            else if (!(other.getResource().getName().equals(resource.getName()))) return false;
            else if (!(other.getResource().getNamespace().equals(resource.getNamespace()))) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(resource, identifier);
        }
    }
}
