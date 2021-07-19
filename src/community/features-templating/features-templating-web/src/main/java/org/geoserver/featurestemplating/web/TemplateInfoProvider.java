/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import java.util.Arrays;
import java.util.List;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.web.wicket.GeoServerDataProvider;

class TemplateInfoProvider extends GeoServerDataProvider<TemplateInfo> {

    static final Property<TemplateInfo> NAME = new BeanProperty<>("templateName", "templateName");
    static final Property<TemplateInfo> EXTENSION = new BeanProperty<>("extension", "extension");
    static final Property<TemplateInfo> WORKSPACE = new BeanProperty<>("workspace", "workspace");
    static final Property<TemplateInfo> FEATURE_TYPE_INFO =
            new BeanProperty<>("featureTypeInfo", "featureType");

    @Override
    protected List<Property<TemplateInfo>> getProperties() {
        return Arrays.asList(NAME, EXTENSION, WORKSPACE, FEATURE_TYPE_INFO);
    }

    @Override
    protected List<TemplateInfo> getItems() {
        return TemplateInfoDAO.get().findAll();
    }
}
