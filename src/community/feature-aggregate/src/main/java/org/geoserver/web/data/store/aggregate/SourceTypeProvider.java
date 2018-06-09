/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.aggregate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.data.aggregate.AggregateTypeConfiguration;
import org.geotools.data.aggregate.SourceType;

class SourceTypeProvider extends GeoServerDataProvider<SourceType> {
    private static final long serialVersionUID = -5562124245239213938L;

    public Property<SourceType> STORE =
            new BeanProperty<SourceType>("storeName", "storeName.localPart");

    public Property<SourceType> TYPE = new BeanProperty<SourceType>("typeName", "typeName");

    public Property<SourceType> MAKE_DEFAULT = new PropertyPlaceholder<SourceType>("makeDefault");

    public Property<SourceType> REMOVE = new PropertyPlaceholder<SourceType>("remove");

    IModel<AggregateTypeConfiguration> config;

    DefaultSourceTypeProperty defaultSourceType;

    public SourceTypeProvider(IModel<AggregateTypeConfiguration> config) {
        this.config = config;
        this.defaultSourceType = new DefaultSourceTypeProperty("default", config);
    }

    @Override
    protected List<SourceType> getItems() {
        return config.getObject().getSourceTypes();
    }

    @Override
    protected List<Property<SourceType>> getProperties() {
        return Arrays.asList(STORE, TYPE, defaultSourceType, MAKE_DEFAULT, REMOVE);
    }

    public DefaultSourceTypeProperty getDefaultSourceTypeProperty() {
        return defaultSourceType;
    }

    static final class DefaultSourceTypeProperty extends AbstractProperty<SourceType> {
        private static final long serialVersionUID = 7215317236941087113L;
        IModel<AggregateTypeConfiguration> config;

        public DefaultSourceTypeProperty(String name, IModel<AggregateTypeConfiguration> config) {
            super(name);
            this.config = config;
        }

        public Comparator<SourceType> getComparator() {
            return null;
        }

        @Override
        public Object getPropertyValue(final SourceType item) {
            return new IModel<Boolean>() {

                private static final long serialVersionUID = 335379699073989386L;

                @Override
                public void detach() {}

                @Override
                public Boolean getObject() {
                    return config.getObject().getPrimarySourceType().equals(item);
                }

                @Override
                public void setObject(Boolean object) {
                    // read only
                }
            };
        }
    };
}
