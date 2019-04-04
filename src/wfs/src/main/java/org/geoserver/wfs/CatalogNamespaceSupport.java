/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Enumeration;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.util.CloseableIterator;
import org.xml.sax.helpers.NamespaceSupport;

/** NamespaceContext based on GeoServer catalog. */
public class CatalogNamespaceSupport extends NamespaceSupport {

    Catalog catalog;

    public CatalogNamespaceSupport(Catalog catalog) {
        super();
        this.catalog = catalog;
    }

    @Override
    public Enumeration getDeclaredPrefixes() {
        return getPrefixes();
    }

    @Override
    public Enumeration getPrefixes() {
        final CloseableIterator<NamespaceInfo> it =
                catalog.list(NamespaceInfo.class, Predicates.acceptAll());
        return new Enumeration() {
            @Override
            public boolean hasMoreElements() {
                if (it.hasNext()) {
                    return true;
                } else {
                    it.close();
                    return false;
                }
            }

            @Override
            public Object nextElement() {
                return it.next().getPrefix();
            }
        };
    }

    @Override
    public Enumeration getPrefixes(String uri) {
        final String pre = getPrefix(uri);
        if (pre == null) {
            return new Enumeration() {
                @Override
                public boolean hasMoreElements() {
                    return false;
                }

                @Override
                public Object nextElement() {
                    return null;
                }
            };
        }

        return new Enumeration() {
            boolean read = false;

            @Override
            public boolean hasMoreElements() {
                return !read;
            }

            @Override
            public Object nextElement() {
                try {
                    return pre;
                } finally {
                    read = false;
                }
            }
        };
    }

    @Override
    public String getPrefix(String uri) {
        NamespaceInfo ns =
                "".equals(uri) ? catalog.getDefaultNamespace() : catalog.getNamespaceByURI(uri);
        return ns != null ? ns.getPrefix() : null;
    }

    @Override
    public String getURI(String prefix) {
        NamespaceInfo ns =
                "".equals(prefix)
                        ? catalog.getDefaultNamespace()
                        : catalog.getNamespaceByPrefix(prefix);
        return ns != null ? ns.getURI() : null;
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public void pushContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void popContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean declarePrefix(String prefix, String uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] processName(String qName, String[] parts, boolean isAttribute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNamespaceDeclUris(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNamespaceDeclUris() {
        throw new UnsupportedOperationException();
    }
}
