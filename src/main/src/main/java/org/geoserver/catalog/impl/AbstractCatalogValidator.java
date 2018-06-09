/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.CatalogValidator;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * A convenience implementation of InfoValidator with all methods implemented as a no-op. You can
 * override individual methods without having to provide your own stubs.
 *
 * @author David Winslow, OpenGeo
 */
public abstract class AbstractCatalogValidator implements CatalogValidator {

    public void validate(ResourceInfo resource, boolean isNew) {}

    public void validate(StoreInfo store, boolean isNew) {}

    public void validate(WorkspaceInfo workspace, boolean isNew) {}

    public void validate(LayerInfo layer, boolean isNew) {}

    public void validate(StyleInfo style, boolean isNew) {}

    public void validate(LayerGroupInfo layerGroup, boolean isNew) {}

    public void validate(NamespaceInfo namespace, boolean isNew) {}
}
