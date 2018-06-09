/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.aggregate;

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.data.aggregate.AggregateTypeConfiguration;
import org.geotools.data.aggregate.SourceType;

class ConfigurationListProvider extends GeoServerDataProvider<AggregateTypeConfiguration> {
    private static final long serialVersionUID = -600491576379986897L;

    public static Property<AggregateTypeConfiguration> NAME =
            new BeanProperty<AggregateTypeConfiguration>("name", "name");

    public static Property<AggregateTypeConfiguration> SOURCES =
            new AbstractProperty<AggregateTypeConfiguration>("sources") {

                private static final long serialVersionUID = 8445881898430736063L;

                public IModel<?> getModel(final IModel<AggregateTypeConfiguration> itemModel) {
                    return new IModel<String>() {

                        private static final long serialVersionUID = -1612531825990914783L;

                        @Override
                        public void detach() {
                            // nothing to do
                        }

                        @Override
                        public String getObject() {
                            return getPropertyValue(itemModel.getObject());
                        }

                        @Override
                        public void setObject(String object) {
                            // read only
                        }
                    };
                };

                @Override
                public String getPropertyValue(AggregateTypeConfiguration item) {
                    if (item.getSourceTypes() == null || item.getSourceTypes().size() == 0) {
                        return "";
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (SourceType st : item.getSourceTypes()) {
                            sb.append(st.getStoreName().getLocalPart() + "/" + st.getTypeName());
                            sb.append(", ");
                        }
                        sb.setLength(sb.length() - 2);
                        return sb.toString();
                    }
                }
            };

    public static Property<AggregateTypeConfiguration> REMOVE =
            new PropertyPlaceholder<AggregateTypeConfiguration>("remove");

    static List<org.geoserver.web.wicket.GeoServerDataProvider.Property<AggregateTypeConfiguration>>
            PROPERTIES = Arrays.asList(NAME, SOURCES, REMOVE);

    List<AggregateTypeConfiguration> items;

    public ConfigurationListProvider(List<AggregateTypeConfiguration> items) {
        this.items = items;
    }

    @Override
    protected List<AggregateTypeConfiguration> getItems() {
        return items;
    }

    @Override
    protected List<Property<AggregateTypeConfiguration>> getProperties() {
        return PROPERTIES;
    }
}
