/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.filter.expression.InternalVolatileFunction;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * A convenient base class for catalog filters. By default does not filter anything, it is advised
 * to use this class as the base to protect yourself from CatalogFilter API changes, implement
 * CatalogFilter directly only if you need a different base class
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractCatalogFilter implements CatalogFilter {

    private static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(AbstractCatalogFilter.class);

    @Override
    public boolean hideLayer(LayerInfo layer) {
        return false;
    }

    @Override
    public boolean hideStyle(StyleInfo style) {
        return false;
    }

    @Override
    public boolean hideLayerGroup(LayerGroupInfo layerGroup) {
        return false;
    }

    @Override
    public boolean hideWorkspace(WorkspaceInfo workspace) {
        return false;
    }

    @Override
    public boolean hideResource(ResourceInfo resource) {
        return false;
    }

    protected Catalog getCatalog() {
        return (Catalog) GeoServerExtensions.bean("catalog");
    }

    /**
     * Returns a Filter equivalent to this CatalogFilter when applied to an object of the specified
     * type. Implementers should override and return an appropriate well known filter
     *
     * @see Predicates
     */
    @Override
    public Filter getSecurityFilter(final Class<? extends CatalogInfo> clazz) {
        org.opengis.filter.expression.Function visible;
        if (ResourceInfo.class.isAssignableFrom(clazz)) {
            visible =
                    new InternalVolatileFunction() {
                        @Override
                        public Boolean evaluate(Object object) {
                            return !hideResource((ResourceInfo) object);
                        }
                    };
        } else if (WorkspaceInfo.class.isAssignableFrom(clazz)) {
            visible =
                    new InternalVolatileFunction() {
                        @Override
                        public Boolean evaluate(Object object) {
                            return !hideWorkspace((WorkspaceInfo) object);
                        }
                    };
        } else if (LayerGroupInfo.class.isAssignableFrom(clazz)) {
            visible =
                    new InternalVolatileFunction() {
                        @Override
                        public Boolean evaluate(Object object) {
                            return !hideLayerGroup((LayerGroupInfo) object);
                        }
                    };
        } else if (StyleInfo.class.isAssignableFrom(clazz)) {
            visible =
                    new InternalVolatileFunction() {
                        @Override
                        public Boolean evaluate(Object object) {
                            return !hideStyle((StyleInfo) object);
                        }
                    };
        } else if (LayerInfo.class.isAssignableFrom(clazz)) {
            visible =
                    new InternalVolatileFunction() {
                        @Override
                        public Boolean evaluate(Object object) {
                            return !hideLayer((LayerInfo) object);
                        }
                    };
        } else if (NamespaceInfo.class.isAssignableFrom(clazz)) {
            visible =
                    new InternalVolatileFunction() {
                        @Override
                        public Boolean evaluate(Object object) {
                            WorkspaceInfo wsInfo =
                                    getCatalog()
                                            .getWorkspaceByName(
                                                    ((NamespaceInfo) object).getPrefix());
                            return !hideWorkspace(wsInfo);
                        }
                    };
        } else {
            LOGGER.log(
                    Level.FINE,
                    "CatalogFilter does not recognize interface {0} accepting all.",
                    clazz);
            return Predicates.acceptAll();
        }

        FilterFactory factory = Predicates.factory;

        // create a filter combined with the security credentials check
        Filter filter = factory.equals(factory.literal(Boolean.TRUE), visible);

        return filter;
    }
}
