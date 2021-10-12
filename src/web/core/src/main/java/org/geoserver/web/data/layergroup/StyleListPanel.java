/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.web.data.style.StyleDetachableModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/** Base class for style listing table with clickable style names */
public abstract class StyleListPanel extends GeoServerTablePanel<StyleInfo> {

    protected static class StyleListProvider extends GeoServerDataProvider<StyleInfo> {
        private static final long serialVersionUID = -5061497681708482229L;
        private PublishedInfo publishedInfo;

        protected StyleListProvider(PublishedInfo publishedInfo) {
            this.publishedInfo = publishedInfo;
        }

        protected StyleListProvider() {}

        @Override
        protected List<StyleInfo> getItems() {
            List<StyleInfo> items;
            if (publishedInfo instanceof LayerGroupInfo)
                items = groupStyles((LayerGroupInfo) publishedInfo);
            else items = new ArrayList<>(getCatalog().getStyles());
            return items;
        }

        private List<StyleInfo> groupStyles(LayerGroupInfo groupInfo) {
            List<LayerGroupStyle> groupStyles = groupInfo.getLayerGroupStyles();
            List<StyleInfo> styles;
            if (groupStyles == null || groupStyles.isEmpty()) styles = Collections.emptyList();
            else styles = groupStyles.stream().map(gs -> gs.getName()).collect(Collectors.toList());
            return styles;
        }

        @Override
        protected List<Property<StyleInfo>> getProperties() {
            return Arrays.asList(NAME);
        }

        @Override
        public IModel<StyleInfo> newModel(StyleInfo object) {
            if (publishedInfo instanceof LayerGroupInfo) return Model.of(object);
            else return new StyleDetachableModel(object);
        }
    }

    private static final long serialVersionUID = -811883647153309626L;

    static Property<StyleInfo> NAME = new BeanProperty<>("name", "name");

    public StyleListPanel(String id, StyleListProvider styleProvider) {
        super(id, styleProvider);
        getTopPager().setVisible(false);
    }

    public StyleListPanel(String id, PublishedInfo publishedInfo) {
        this(id, new StyleListProvider(publishedInfo));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Component getComponentForProperty(
            String id, IModel<StyleInfo> itemModel, Property<StyleInfo> property) {
        final StyleInfo style = itemModel.getObject();
        if (property == NAME) {
            return new SimpleAjaxLink<String>(id, (IModel<String>) NAME.getModel(itemModel)) {
                private static final long serialVersionUID = -2537227506881638001L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    handleStyle(style, target);
                }
            };
        }

        return null;
    }

    protected abstract void handleStyle(StyleInfo style, AjaxRequestTarget target);
}
