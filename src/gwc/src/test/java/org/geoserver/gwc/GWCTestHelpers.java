/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

public class GWCTestHelpers {

    public static LayerInfoImpl mockLayer(
            String resourceName, String[] extraStyles, PublishedType type) {
        return mockLayer(resourceName, null, extraStyles, type);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static LayerInfoImpl mockLayer(
            String resourceName, String workSpaceName, String[] extraStyles, PublishedType type) {
        StoreInfo store = new DataStoreInfoImpl(null);
        store.setName(resourceName + "-store");
        store.setEnabled(true);

        if (workSpaceName != null) {
            WorkspaceInfo workspace = new WorkspaceInfoImpl();
            workspace.setName(workSpaceName);
            store.setWorkspace(workspace);
        }

        NamespaceInfoImpl ns = new NamespaceInfoImpl();
        ns.setPrefix("test");
        ns.setURI("http://example.com");
        FeatureTypeInfoImpl resource = new FeatureTypeInfoImpl(null);
        resource.setName(resourceName);
        resource.setNamespace(ns);

        ReferencedEnvelope box = new ReferencedEnvelope(-180, 0, 0, 90, DefaultGeographicCRS.WGS84);
        resource.setLatLonBoundingBox(box);
        resource.setNativeBoundingBox(box);
        resource.setEnabled(true);
        resource.setStore(store);

        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setId("id-" + resource.prefixedName());
        layer.setResource(resource);
        layer.setEnabled(true);

        StyleInfoImpl defaultStyle = new StyleInfoImpl(null);
        defaultStyle.setName("default");
        layer.setDefaultStyle(defaultStyle);

        if (extraStyles != null) {
            Set styles = new HashSet();
            for (String name : extraStyles) {
                StyleInfoImpl extra = new StyleInfoImpl(null);
                extra.setName(name);
                styles.add(extra);
            }
            layer.setStyles(styles);
        }

        layer.setType(type);

        return layer;
    }

    public static LayerGroupInfoImpl mockGroup(String name, PublishedInfo... layers) {
        LayerGroupInfoImpl lg = new LayerGroupInfoImpl();
        lg.setId("id-" + name);
        lg.setName(name);
        lg.setLayers(Lists.newArrayList(layers));
        lg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, DefaultGeographicCRS.WGS84));
        return lg;
    }
}
