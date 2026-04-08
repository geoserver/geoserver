/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.server.web.LayerAttributeModel)
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.GeoServerDataProvider;

@SuppressWarnings("serial")
public class LayerAttributeDataProvider extends GeoServerDataProvider<MutableLayerAttribute> {

    public static final Property<MutableLayerAttribute> NAME = new BeanProperty<>("name");

    public static final Property<MutableLayerAttribute> DATATYPE = new BeanProperty<>("dataType");

    public static final Property<MutableLayerAttribute> ACCESS = new BeanProperty<>("access");

    private IModel<List<MutableLayerAttribute>> attributes;

    public LayerAttributeDataProvider(IModel<List<MutableLayerAttribute>> attributes) {
        this.attributes = attributes;
    }

    @Override
    protected List<Property<MutableLayerAttribute>> getProperties() {
        return List.of(NAME, DATATYPE, ACCESS);
    }

    @Override
    protected List<MutableLayerAttribute> getItems() {
        List<MutableLayerAttribute> values = attributes.getObject();
        return values == null ? List.of() : new ArrayList<>(values);
    }
}
