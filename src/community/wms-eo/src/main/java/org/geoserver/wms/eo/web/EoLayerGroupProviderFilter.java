/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.data.layergroup.LayerGroupProviderFilter;

/**
 * Filters EO layer groups.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public class EoLayerGroupProviderFilter implements LayerGroupProviderFilter {

    public static final EoLayerGroupProviderFilter INSTANCE = new EoLayerGroupProviderFilter();

    private EoLayerGroupProviderFilter() {}

    @Override
    public boolean accept(LayerGroupInfo group) {
        return LayerGroupInfo.Mode.EO.equals(group.getMode());
    }
}
