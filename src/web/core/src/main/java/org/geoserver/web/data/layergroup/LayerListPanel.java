/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import static org.geoserver.catalog.Predicates.sortBy;

import com.google.common.collect.Lists;
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
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.web.data.layer.LayerProvider;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;

/** Base class for a layer listing table with clickable layer names */
public abstract class LayerListPanel extends GeoServerTablePanel<LayerInfo> {

    protected abstract static class LayerListProvider extends LayerProvider {

        private static final long serialVersionUID = -4793382279386643262L;

        @Override
        protected List<Property<LayerInfo>> getProperties() {
            return Arrays.asList(NAME, STORE, WORKSPACE);
        }
    }

    private static final long serialVersionUID = 3638205114048153057L;

    static Property<LayerInfo> NAME = new BeanProperty<LayerInfo>("name", "name");

    static Property<LayerInfo> STORE = new BeanProperty<LayerInfo>("store", "resource.store.name");

    static Property<LayerInfo> WORKSPACE =
            new BeanProperty<LayerInfo>("workspace", "resource.store.workspace.name");

    public LayerListPanel(String id, final WorkspaceInfo workspace) {
        this(
                id,
                new LayerListProvider() {

                    private static final long serialVersionUID = 426375054014475107L;

                    @Override
                    public Iterator<LayerInfo> iterator(final long first, final long count) {
                        Iterator<LayerInfo> iterator = filteredItems((int) first, (int) count);
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

                    @Override
                    protected Filter getFilter() {
                        FilterFactory ff = CommonFactoryFinder.getFilterFactory2();
                        final Filter filter;
                        if (workspace == null) {
                            filter = super.getFilter();
                        } else {
                            filter =
                                    ff.and(
                                            super.getFilter(),
                                            ff.equal(
                                                    ff.property("resource.store.workspace.id"),
                                                    ff.literal(workspace.getId()),
                                                    true));
                        }
                        return filter;
                    }

                    /**
                     * Returns the requested page of layer objects after applying any keyword
                     * filtering set on the page
                     */
                    private Iterator<LayerInfo> filteredItems(Integer first, Integer count) {
                        final Catalog catalog = getCatalog();

                        // global sorting
                        final SortParam<?> sort = getSort();
                        final Property<LayerInfo> property = getProperty(sort);

                        SortBy sortOrder = null;
                        if (sort != null) {
                            if (property instanceof BeanProperty) {
                                final String sortProperty =
                                        ((BeanProperty<LayerInfo>) property).getPropertyPath();
                                sortOrder = sortBy(sortProperty, sort.isAscending());
                            }
                        }

                        final Filter filter = getFilter();
                        // our already filtered and closeable iterator
                        Iterator<LayerInfo> items =
                                catalog.list(LayerInfo.class, filter, first, count, sortOrder);

                        return items;
                    }
                });
    }

    protected LayerListPanel(String id, GeoServerDataProvider<LayerInfo> provider) {
        super(id, provider);
        getTopPager().setVisible(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Component getComponentForProperty(
            String id, final IModel<LayerInfo> itemModel, Property<LayerInfo> property) {
        IModel<?> model = property.getModel(itemModel);
        if (NAME == property) {
            return new SimpleAjaxLink<String>(id, (IModel<String>) model) {
                private static final long serialVersionUID = -2968338284881141281L;

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    LayerInfo layer = (LayerInfo) itemModel.getObject();
                    handleLayer(layer, target);
                }
            };
        } else {
            return new Label(id, model);
        }
    }

    protected void handleLayer(LayerInfo layer, AjaxRequestTarget target) {}
}
