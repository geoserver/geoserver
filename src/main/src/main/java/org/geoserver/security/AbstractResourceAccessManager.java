/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.filter.expression.InternalVolatileFunction;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.springframework.security.core.Authentication;

/**
 * Abstract base class for {@link ResourceAccessManager} implementations.
 * <p>
 * This base class returns null from every method meaning no limits.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class AbstractResourceAccessManager implements ResourceAccessManager {

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user,
            WorkspaceInfo workspace) {
        return null;
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        return null;
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        return null;
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        return null;
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        return null;
    }
    
    protected Catalog getCatalog() {
        return (Catalog) GeoServerExtensions.bean("catalog");
    }
    protected SecureCatalogImpl getSecurityWrapper() {
        return GeoServerExtensions.bean(SecureCatalogImpl.class);
    }

    @Override
    public Filter getSecurityFilter(final Authentication user,
            final Class<? extends CatalogInfo> clazz) {
        
        org.opengis.filter.expression.Function visible = new InternalVolatileFunction() {
            @Override
            public Boolean evaluate(Object object) {
                CatalogInfo info = (CatalogInfo) object;
                if(info instanceof NamespaceInfo) {
                    info = getCatalog().getWorkspaceByName(((NamespaceInfo) info).getPrefix());
                }
                String name = (String) OwsUtils.property(info, "name", String.class);
                WrapperPolicy policy = getSecurityWrapper()
                        .buildWrapperPolicy(AbstractResourceAccessManager.this, user, info);
                AccessLevel accessLevel = policy.getAccessLevel();
                boolean visible = !AccessLevel.HIDDEN.equals(accessLevel);
                return Boolean.valueOf(visible);
            }
        };
        
        FilterFactory factory = Predicates.factory;
        
        // create a filter combined with the security credentials check
        Filter filter = factory.equals(factory.literal(Boolean.TRUE), visible);
        
        return filter;
    }

}
