/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.style.StyleDetachableModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/**
 * Base class for style listing table with clickable style names
 */
public abstract class StyleListPanel extends GeoServerTablePanel<StyleInfo> {

    protected static class StyleListProvider extends GeoServerDataProvider<StyleInfo> {
        @Override
        protected List<StyleInfo> getItems() {
            return getCatalog().getStyles();
        }

        @Override
        protected List<Property<StyleInfo>> getProperties() {
            return Arrays.asList( NAME );
        }

        public IModel newModel(Object object) {
            return new StyleDetachableModel( (StyleInfo) object );
        }
    }

    private static final long serialVersionUID = -811883647153309626L;

    static Property<StyleInfo> NAME = 
        new BeanProperty<StyleInfo>("name", "name");
    
    public StyleListPanel(String id, StyleListProvider styleProvider) {
        super(id, styleProvider);
        getTopPager().setVisible(false);
    }
    
    public StyleListPanel(String id) {
        this(id, new StyleListProvider());
    }
    
    @Override
    protected Component getComponentForProperty(String id, IModel itemModel,
            Property<StyleInfo> property) {
        final StyleInfo style = (StyleInfo) itemModel.getObject();
        if ( property == NAME ) {
            return new SimpleAjaxLink( id, NAME.getModel( itemModel ) ) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    handleStyle(style, target);
                }
            };
        }
        
        return null;
    }
    
    protected abstract void handleStyle( StyleInfo style, AjaxRequestTarget target );

}