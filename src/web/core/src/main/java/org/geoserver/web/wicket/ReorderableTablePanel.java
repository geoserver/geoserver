/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDataProvider.PropertyPlaceholder;

/**
 * Base class for tables that have up/down modifiers
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 * @param <T>
 */
@SuppressWarnings({ "serial", "rawtypes" })
public abstract class ReorderableTablePanel<T> extends GeoServerTablePanel<T> {

    static class ReorderableDataProvider<T> extends GeoServerDataProvider<T> {

        private List<T> items;

        private List<org.geoserver.web.wicket.GeoServerDataProvider.Property<T>> properties;

        public ReorderableDataProvider(List<T> items, List<Property<T>> properties) {
            this.items = items;
            this.properties = new ArrayList<Property<T>>(properties);
            this.properties.add(0, POSITION);
            this.properties.add(0, RENDERING_ORDER);
        }

        @Override
        protected List<Property<T>> getProperties() {
            return properties;
        }

        @Override
        protected List<T> getItems() {
            return items;
        }

    }

    /**
     * Cannot declare these non static, because they would be initialized too late, and as static,
     * they cannot have the right type argument
     */
    static Property POSITION = new PropertyPlaceholder("position");

    static Property RENDERING_ORDER = new PropertyPlaceholder("order");

    private List<T> items;

    public ReorderableTablePanel(String id, List<T> items, List<Property<T>> properties) {
        super(id, new ReorderableDataProvider(items, properties));
        this.items = items;
    }

    protected void buildRowListView(final GeoServerDataProvider<T> dataProvider, Item item,
            final IModel itemModel) {
        // create one component per viewable property
        ListView items = new ListView("itemProperties", dataProvider.getVisibleProperties()) {

            @Override
            protected void populateItem(ListItem item) {
                Property<T> property = (Property<T>) item.getModelObject();

                Component component = null;
                if (property == POSITION) {
                    ParamResourceModel upTitle = new ParamResourceModel("moveToTop", this);
                    ParamResourceModel downTitle = new ParamResourceModel("moveToBottom", this);
                    component = new UpDownPanel<T>("component", (T) itemModel.getObject(),
                            dataProvider.getItems(), ReorderableTablePanel.this, upTitle, downTitle);

                } else if (property == RENDERING_ORDER) {
                    component = new Label("component", new Model());
                } else {
                    component = getComponentForProperty("component", itemModel, property);
                }

                if (component == null) {
                    // show a plain label if the the subclass did not create any component
                    component = new Label("component", property.getModel(itemModel));
                } else if (!"component".equals(component.getId())) {
                    // add some checks for the id, the error message
                    // that wicket returns in case of mismatch is not
                    // that helpful
                    throw new IllegalArgumentException("getComponentForProperty asked "
                            + "to build a component " + "with id = 'component' " + "for property '"
                            + property.getName() + "', but got '" + component.getId() + "' instead");
                }
                item.add(component);
                onPopulateItem(property, item);
            }
        };
        items.setReuseItems(true);
        item.add(items);

        this.setOutputMarkupId(true);
    }

    protected void onPopulateItem(GeoServerDataProvider.Property<T> property,
            org.apache.wicket.markup.html.list.ListItem item) {
        if (property == RENDERING_ORDER) {
            Label label = (Label) item.get(0);
            OddEvenItem rowContainer = (OddEvenItem) item.getParent().getParent();
            label.setDefaultModel(new Model(rowContainer.getIndex() + 1));
            item.add(new AbstractBehavior() {

                public void onComponentTag(Component component, ComponentTag tag) {
                    tag.put("style", "width:1%");
                }
            });
        }
    };

}
