/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.metadata.web.panel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class LinkedLayersDataProvider extends GeoServerDataProvider<ResourceInfo> {

    private static final long serialVersionUID = 9052136450762857446L;

    static final Property<ResourceInfo> NAME = new BeanProperty<>("name", "prefixedName");

    static final Property<ResourceInfo> TITLE = new BeanProperty<>("title", "title");

    IModel<MetadataTemplate> metadataTemplateModel;

    public LinkedLayersDataProvider(IModel<MetadataTemplate> metadataTemplateModel) {
        this.metadataTemplateModel = metadataTemplateModel;
    }

    @Override
    protected List<Property<ResourceInfo>> getProperties() {
        return Arrays.asList(TITLE, NAME);
    }

    @Override
    protected List<ResourceInfo> getItems() {
        Catalog rawCatalog =
                (Catalog) GeoServerApplication.get().getApplicationContext().getBean("rawCatalog");
        List<ResourceInfo> list = new ArrayList<>();
        for (String id : metadataTemplateModel.getObject().getLinkedLayers()) {
            ResourceInfo ri = rawCatalog.getResource(id, ResourceInfo.class);
            if (ri != null) {
                list.add(ri);
            }
        }

        return list;
    }
}
