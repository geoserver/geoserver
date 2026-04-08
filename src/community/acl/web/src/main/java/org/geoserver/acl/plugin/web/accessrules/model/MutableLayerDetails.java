/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.server.web.LayerDetailsFormData)
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NonNull;
import org.geolatte.geom.MultiPolygon;
import org.geoserver.acl.domain.rules.CatalogMode;
import org.geoserver.acl.domain.rules.LayerAttribute;
import org.geoserver.acl.domain.rules.LayerDetails;
import org.geoserver.acl.domain.rules.LayerDetails.Builder;
import org.geoserver.acl.domain.rules.LayerDetails.LayerType;
import org.geoserver.acl.domain.rules.SpatialFilterType;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;

@Data
@SuppressWarnings("serial")
public class MutableLayerDetails implements Serializable {

    private LayerType layerType;
    private String defaultStyle;
    private String cqlFilterRead;
    private String cqlFilterWrite;
    private MultiPolygon<?> area;
    private SpatialFilterType spatialFilterType;
    private CatalogMode catalogMode;
    private final Set<String> allowedStyles = new TreeSet<>();
    private final List<MutableLayerAttribute> attributes = new ArrayList<>();

    public MutableLayerDetails() {}

    public MutableLayerDetails(@NonNull LayerDetails ld) {
        setLayerType(ld.getType());
        setDefaultStyle(ld.getDefaultStyle());
        setCqlFilterRead(ld.getCqlFilterRead());
        setCqlFilterWrite(ld.getCqlFilterWrite());
        setArea(ld.getArea());
        setCatalogMode(ld.getCatalogMode());
        setSpatialFilterType(ld.getSpatialFilterType());
        setAllowedStyles(ld.getAllowedStyles());
        setAttributes(
                ld.getAttributes().stream().map(MutableLayerAttribute::new).collect(Collectors.toList()));
    }

    public void setAllowedStyles(Set<String> styles) {
        this.allowedStyles.clear();
        if (null != styles) this.allowedStyles.addAll(styles);
    }

    public void setAttributes(List<MutableLayerAttribute> list) {
        this.attributes.clear();
        if (null != list) this.attributes.addAll(list);
    }

    public LayerDetails toLayerDetails() {
        Set<LayerAttribute> atts =
                attributes.stream().map(MutableLayerAttribute::toLayerAttribute).collect(Collectors.toSet());
        Builder builder = LayerDetails.builder()
                .type(layerType)
                .defaultStyle(defaultStyle)
                .cqlFilterRead(cqlFilterRead)
                .cqlFilterWrite(cqlFilterWrite)
                .area(area)
                .allowedStyles(allowedStyles)
                .attributes(atts);
        // non-nullable attribtues
        if (null != spatialFilterType) builder.spatialFilterType(spatialFilterType);
        if (null != catalogMode) builder.catalogMode(catalogMode);

        return builder.build();
    }

    public boolean isNew() {
        return catalogMode == null;
    }

    public void setLayerTypeFrom(PublishedInfo info) {
        PublishedType type = info == null ? null : info.getType();
        if (null == type) {
            this.layerType = null;
            return;
        }
        switch (type) {
            case GROUP:
                this.layerType = LayerType.LAYERGROUP;
                break;
            case VECTOR:
                this.layerType = LayerType.VECTOR;
                break;
            case RASTER:
            case REMOTE:
            case WMS:
            case WMTS:
                this.layerType = LayerType.RASTER;
                break;
            default:
                this.layerType = null;
                break;
        }
    }
}
