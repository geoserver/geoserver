/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.server.web.ShortLayerAttribute)
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import java.io.Serializable;
import lombok.Data;
import org.geoserver.acl.domain.rules.LayerAttribute;
import org.geoserver.acl.domain.rules.LayerAttribute.AccessType;
import org.geoserver.catalog.AttributeTypeInfo;

@Data
@SuppressWarnings("serial")
public class MutableLayerAttribute implements Serializable {

    private String name;
    private String dataType;
    private AccessType access;

    public MutableLayerAttribute() {}

    public MutableLayerAttribute(String name, String binding, AccessType type) {
        this.name = name;
        this.dataType = binding;
        this.access = type;
    }

    MutableLayerAttribute(MutableLayerAttribute source) {
        this(source.getName(), source.getDataType(), source.getAccess());
    }

    MutableLayerAttribute(LayerAttribute latt) {
        this(latt.getName(), latt.getDataType(), latt.getAccess());
    }

    MutableLayerAttribute(AttributeTypeInfo ftatt) {
        this(ftatt.getName(), ftatt.getBinding().getName(), AccessType.READWRITE);
    }

    LayerAttribute toLayerAttribute() {
        return LayerAttribute.builder()
                .name(name)
                .dataType(dataType)
                .access(access)
                .build();
    }
}
