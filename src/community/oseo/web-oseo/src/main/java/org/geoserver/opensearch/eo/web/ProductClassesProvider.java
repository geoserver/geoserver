/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.ProductClass;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class ProductClassesProvider extends GeoServerDataProvider<ProductClass> {

    public static final Property<ProductClass> REMOVE = new PropertyPlaceholder<>("remove");

    private final IModel<OSEOInfo> oseoModel;

    public ProductClassesProvider(IModel<OSEOInfo> model) {
        this.oseoModel = model;
    }

    @Override
    protected List<Property<ProductClass>> getProperties() {
        List<Property<ProductClass>> result = new ArrayList<>();
        result.add(new BeanProperty<>("name", "name"));
        result.add(new BeanProperty<>("prefix", "prefix"));
        result.add(new BeanProperty<>("namespace", "namespace"));
        result.add(REMOVE);

        return result;
    }

    @Override
    protected List<ProductClass> getItems() {
        return oseoModel.getObject().getProductClasses();
    }
}
