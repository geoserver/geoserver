/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * Provides a filtered, sorted view over the catalog layers.
 * 
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class PreviewLayerProvider extends GeoServerDataProvider<PreviewLayer> {
    public static final Property<PreviewLayer> TYPE = new BeanProperty<PreviewLayer>(
            "type", "type");

    public static final AbstractProperty<PreviewLayer> NAME = new AbstractProperty<PreviewLayer>("name") {
        @Override
        public Object getPropertyValue(PreviewLayer item) {
            if (item.layerInfo != null) {
                return item.layerInfo.prefixedName();
            }
            if (item.groupInfo != null) {
                return item.groupInfo.prefixedName();
            }
            return null;
        }
    };

    public static final Property<PreviewLayer> TITLE = new BeanProperty<PreviewLayer>(
            "title", "title");
    
    public static final Property<PreviewLayer> ABSTRACT = new BeanProperty<PreviewLayer>(
            "abstract", "abstract", false);
    
    public static final Property<PreviewLayer> KEYWORDS = new BeanProperty<PreviewLayer>(
            "keywords", "keywords", false);

    public static final Property<PreviewLayer> COMMON = new PropertyPlaceholder<PreviewLayer>(
            "commonFormats");

    public static final Property<PreviewLayer> ALL = new PropertyPlaceholder<PreviewLayer>(
            "allFormats");

    public static final List<Property<PreviewLayer>> PROPERTIES = Arrays.asList(TYPE,
            NAME, TITLE, ABSTRACT, KEYWORDS, COMMON, ALL);

    @Override
    protected List<PreviewLayer> getItems() {
        List<PreviewLayer> result = new ArrayList<PreviewLayer>();

        List<LayerInfo> layers = getCatalog().getLayers();
        for (LayerInfo layer :layers ) {
            // ask for enabled() instead of isEnabled() to account for disabled resource/store
            if (layer.enabled() && layer.isAdvertised()) {
                result.add(new PreviewLayer(layer));
            }
        }

        final List<LayerGroupInfo> layerGroups = getCatalog().getLayerGroups();
        for (LayerGroupInfo group :layerGroups ) {
            boolean enabled = true;
            layers = group.getLayers();
            for (LayerInfo layer :layers ) {
                // ask for enabled() instead of isEnabled() to account for disabled resource/store
                enabled &= layer.enabled();
            }
            if (enabled)
                result.add(new PreviewLayer(group));
        }

        return result;
    }

    @Override
    protected List<Property<PreviewLayer>> getProperties() {
        return PROPERTIES;
    }

    public IModel newModel(Object object) {
        return new PreviewLayerModel((PreviewLayer) object);
    }
    
}
