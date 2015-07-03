/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import static org.geoserver.catalog.Predicates.acceptAll;
import static org.geoserver.catalog.Predicates.or;
import static org.geoserver.catalog.Predicates.sortBy;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.web.data.layer.LayerDetachableModel;
import org.geoserver.web.data.layer.LayerProvider;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

import com.google.common.collect.Lists;

/**
 * Base class for a layer listing table with clickable layer names
 */
public abstract class LayerListPanel extends GeoServerTablePanel<LayerInfo> {
    
    protected static abstract class LayerListProvider extends LayerProvider {

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
        this( id, new LayerListProvider(){

            @Override
            public Iterator<LayerInfo> iterator(final int first, final int count) {
                Iterator<LayerInfo> iterator = filteredItems(first, count);
                if (iterator instanceof CloseableIterator) {
                    // don't know how to force wicket to close the iterator, lets return
                    // a copy. Shouldn't be much overhead as we're paging
                    try {
                        return Lists.newArrayList(iterator).iterator();
                    } finally {
                        CloseableIteratorAdapter.close(iterator);
                    }
                } else {
                    return iterator;
                }
            }

            /**
             * Returns the requested page of layer objects after applying any keyword
             * filtering set on the page
             */
            private Iterator<LayerInfo> filteredItems(Integer first, Integer count) {
                final Catalog catalog = getCatalog();

                // global sorting
                final SortParam sort = getSort();
                final Property<LayerInfo> property = getProperty(sort);

                SortBy sortOrder = null;
                if (sort != null) {
                    if(property instanceof BeanProperty){
                        final String sortProperty = ((BeanProperty<LayerInfo>)property).getPropertyPath();
                        sortOrder = sortBy(sortProperty, sort.isAscending());
                    }
                }

                final Filter filter = getFilter();
                //our already filtered and closeable iterator
                Iterator<LayerInfo> items = catalog.list(LayerInfo.class, filter, first, count, sortOrder);

                return items;
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