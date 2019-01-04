/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import java.util.Arrays;
import java.util.List;
import org.geoserver.geofence.core.model.LayerAttribute;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class LayerAttributeModel extends GeoServerDataProvider<LayerAttribute> {

    private static final long serialVersionUID = -7904736484716616708L;

    public static final Property<LayerAttribute> NAME = new BeanProperty<>("name", "name");

    public static final Property<LayerAttribute> DATATYPE =
            new BeanProperty<>("datatype", "datatype");

    public static final Property<LayerAttribute> ACCESS = new BeanProperty<>("access", "access");

    private List<LayerAttribute> attributes;

    public LayerAttributeModel(List<LayerAttribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    protected List<Property<LayerAttribute>> getProperties() {
        return Arrays.asList(NAME, DATATYPE, ACCESS);
    }

    @Override
    protected List<LayerAttribute> getItems() {
        return attributes;
    }
}
