/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.dto.AttributeCollection;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.FieldTypeEnum;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.util.logging.Logging;

public class AttributeDataProvider extends GeoServerDataProvider<AttributeConfiguration> {

    private static final long serialVersionUID = -4454769618643460913L;

    private static final Logger LOGGER = Logging.getLogger(AttributeDataProvider.class);

    public static Property<AttributeConfiguration> NAME =
            new BeanProperty<AttributeConfiguration>("name", "label");

    public static Property<AttributeConfiguration> VALUE =
            new AbstractProperty<AttributeConfiguration>("value") {
                private static final long serialVersionUID = -1889227419206718295L;

                @Override
                public Object getPropertyValue(AttributeConfiguration item) {
                    return null;
                }
            };

    private List<AttributeConfiguration> items = new ArrayList<>();

    private ResourceInfo rInfo;

    public AttributeDataProvider(ResourceInfo rInfo) {
        this.rInfo = rInfo;
        ConfigurationService metadataConfigurationService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ConfigurationService.class);
        load(metadataConfigurationService.getMetadataConfiguration());
    }

    /** Provide attributes for the given complex type configuration. */
    public AttributeDataProvider(String typename, ResourceInfo rInfo) {
        this.rInfo = rInfo;
        ConfigurationService metadataConfigurationService =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(ConfigurationService.class);
        AttributeCollection typeConfiguration =
                metadataConfigurationService.getMetadataConfiguration().findType(typename);
        if (typeConfiguration != null) {
            load(typeConfiguration);
        }
    }

    protected void load(AttributeCollection coll) {
        for (AttributeConfiguration config : coll.getAttributes()) {
            if (shouldDisplay(config)) {
                items.add(config);
            }
        }
    }

    private boolean shouldDisplay(AttributeConfiguration config) {
        if (config.getFieldType() == FieldTypeEnum.DERIVED) {
            return false; // don't display derived fields!
        }
        if (config.getCondition() != null && rInfo != null) {
            try {
                Object result = CQL.toExpression(config.getCondition()).evaluate(rInfo);
                if (!Boolean.TRUE.equals(result)) {
                    return false;
                }
            } catch (CQLException e) {
                LOGGER.log(Level.WARNING, "Failed to parse condition for " + config.getKey(), e);
            }
        }
        return true;
    }

    @Override
    protected List<Property<AttributeConfiguration>> getProperties() {
        return Arrays.asList(NAME, VALUE);
    }

    @Override
    protected List<AttributeConfiguration> getItems() {
        return items;
    }
}
