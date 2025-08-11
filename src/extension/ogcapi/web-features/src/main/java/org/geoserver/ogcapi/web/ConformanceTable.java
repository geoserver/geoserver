/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

public class ConformanceTable extends GeoServerTablePanel<APIConformance> {

    public ConformanceTable(String id, ConformanceInfo<?> conformanceInfo, Component parent) {
        super(id, new ConformanceDataProvider(conformanceInfo, parent));

        // set up for editing
        setPageable(false);
        setOutputMarkupId(true);
        setSortable(false);
        setItemReuseStrategy(new DefaultItemReuseStrategy());
        setSelectable(false); // no selection, the editable checkboxes are a different case
        setFilterable(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Component getComponentForProperty(
            String id, IModel<APIConformance> itemModel, GeoServerDataProvider.Property<APIConformance> property) {
        if ("enabled".equals(property.getName())) {
            Fragment fragment = new Fragment(id, "checkboxFragment", this);
            fragment.add(new ThreeStateCheckBox("checkbox", (IModel<Boolean>) property.getModel(itemModel)));
            return fragment;
        }
        // default to label
        return null;
    }

    private static class ConformanceDataProvider extends GeoServerDataProvider<APIConformance> {

        static final Property<APIConformance> ID = new BeanProperty<>("id");
        static final Property<APIConformance> LEVEL = new BeanProperty<>("level");
        static final Property<APIConformance> TYPE = new BeanProperty<>("type");

        private final ConformanceInfo<?> conformanceInfo;
        private final Component parent;

        public ConformanceDataProvider(ConformanceInfo<?> conformanceInfo, Component parent) {
            this.conformanceInfo = conformanceInfo;
            this.parent = parent;
        }

        @Override
        protected List<Property<APIConformance>> getProperties() {
            Property<APIConformance> enabled = new AbstractProperty<>("enabled") {
                @Override
                public Object getPropertyValue(APIConformance item) {
                    return new IModel<Boolean>() {

                        @Override
                        public Boolean getObject() {
                            return conformanceInfo.isEnabled(item);
                        }

                        @Override
                        public void setObject(Boolean object) {
                            conformanceInfo.setEnabled(item, object);
                        }
                    };
                }
            };
            Property<APIConformance> name = new AbstractProperty<>("name") {
                @Override
                public Object getPropertyValue(APIConformance item) {
                    return new ParamResourceModel(conformanceInfo.getId() + "." + item.getProperty(), parent)
                            .getString();
                }

                @Override
                public IModel<?> getModel(IModel<APIConformance> itemModel) {
                    return super.getModel(itemModel);
                }
            };
            return List.of(enabled, name, ID, LEVEL, TYPE);
        }

        @Override
        protected List<APIConformance> getItems() {
            return conformanceInfo.configurableConformances();
        }
    }
}
