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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.data.layer.LayerDetachableModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/**
 * Base class for a layer listing table with clickable layer names
 */
public abstract class LayerListPanel extends GeoServerTablePanel<LayerInfo> {
    
    protected static abstract class LayerListProvider extends GeoServerDataProvider<LayerInfo> {
        @Override
        protected abstract List<LayerInfo> getItems();

        @Override
        protected List<Property<LayerInfo>> getProperties() {
            return Arrays.asList( NAME, STORE, WORKSPACE );
        }

        public IModel newModel(Object object) {
            return new LayerDetachableModel((LayerInfo)object);
        }
    }

    private static final long serialVersionUID = 3638205114048153057L;

    static Property<LayerInfo> NAME = 
        new BeanProperty<LayerInfo>("name", "name");
    
    static Property<LayerInfo> STORE = 
        new BeanProperty<LayerInfo>("store", "resource.store.name");
    
    static Property<LayerInfo> WORKSPACE = 
        new BeanProperty<LayerInfo>("workspace", "resource.store.workspace.name");
    
    public LayerListPanel( String id ) {
        this( id, new LayerListProvider() {
            
            @Override
            protected List<LayerInfo> getItems() {
                return getCatalog().getLayers();
            }
        });
    }
    
    protected LayerListPanel(String id, GeoServerDataProvider<LayerInfo> provider) {
        super(id, provider);
        getTopPager().setVisible(false);
    }
    
    
    @Override
    protected Component getComponentForProperty(String id, final IModel itemModel,
            Property<LayerInfo> property) {
        IModel model = property.getModel( itemModel );
        if ( NAME == property ) {
            return new SimpleAjaxLink( id, model ) {
                @Override
                protected void onClick(AjaxRequestTarget target) {
                    LayerInfo layer = (LayerInfo) itemModel.getObject();
                    handleLayer( layer, target );
                }
            };
        }
        else {
            return new Label( id, model );
        }
    }
    
    protected void handleLayer( LayerInfo layer, AjaxRequestTarget target ) {
    }
}