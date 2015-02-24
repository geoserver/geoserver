/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
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

    public static Property<AggregateTypeConfiguration> NAME = new BeanProperty<AggregateTypeConfiguration>(
            "name", "name");

    public static Property<AggregateTypeConfiguration> SOURCES = new AbstractProperty<AggregateTypeConfiguration>(
            "sources") {

        public IModel getModel(final IModel itemModel) {
            return new IModel() {

                @Override
                public void detach() {
                    // nothing to do                    
                }

                @Override
                public Object getObject() {
                    return getPropertyValue((AggregateTypeConfiguration) itemModel.getObject());
                }

                @Override
                public void setObject(Object object) {
                    // read only                    
                }
            };
        };
        
        @Override
        public Object getPropertyValue(AggregateTypeConfiguration item) {
            if (item.getSourceTypes() == null || item.getSourceTypes().size() == 0) {
                return "";
            } else {
                StringBuilder sb = new StringBuilder();
                for (SourceType st : item.getSourceTypes()) {
                    sb.append(st.getStoreName().getLocalPart() + "/" + st.getTypeName());
                    sb.append(", ");
                }
                sb.setLength(sb.length() - 2);
                return sb;
            }
        }
    };
    
    public static Property<AggregateTypeConfiguration> REMOVE = 
        new PropertyPlaceholder<AggregateTypeConfiguration>( "remove" );

    static List PROPERTIES = Arrays.asList(NAME, SOURCES, REMOVE);

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
