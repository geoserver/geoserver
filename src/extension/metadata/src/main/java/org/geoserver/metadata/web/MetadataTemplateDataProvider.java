/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class MetadataTemplateDataProvider extends GeoServerDataProvider<MetadataTemplate> {

    @Serial
    private static final long serialVersionUID = -8246320435114536132L;

    public static final Property<MetadataTemplate> PRIORITY = new AbstractProperty<>("priority") {
        @Serial
        private static final long serialVersionUID = -1889227419206718295L;

        @Override
        public Object getPropertyValue(MetadataTemplate item) {
            return null;
        }
    };

    public static final Property<MetadataTemplate> NAME = new BeanProperty<>("name", "name");

    public static final Property<MetadataTemplate> DESCRIPTION = new BeanProperty<>("description", "description");

    private IModel<List<MetadataTemplate>> templates;

    public MetadataTemplateDataProvider(IModel<List<MetadataTemplate>> templates) {
        this.templates = templates;
    }

    @Override
    protected List<Property<MetadataTemplate>> getProperties() {
        return Arrays.asList(PRIORITY, NAME, DESCRIPTION);
    }

    @Override
    protected List<MetadataTemplate> getItems() {
        return templates.getObject();
    }
}
