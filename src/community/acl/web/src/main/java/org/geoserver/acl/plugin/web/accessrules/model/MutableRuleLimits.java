/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import java.io.Serializable;
import lombok.Data;
import lombok.NonNull;
import org.geolatte.geom.MultiPolygon;
import org.geoserver.acl.domain.rules.CatalogMode;
import org.geoserver.acl.domain.rules.RuleLimits;
import org.geoserver.acl.domain.rules.SpatialFilterType;

@Data
@SuppressWarnings("serial")
public class MutableRuleLimits implements Serializable, Cloneable {

    private MultiPolygon<?> allowedArea;
    private SpatialFilterType spatialFilterType = RuleLimits.DEFAULT_SPATIAL_FILTERTYPE;
    private CatalogMode catalogMode = RuleLimits.DEFAULT_CATALOG_MODE;

    public MutableRuleLimits() {}

    public MutableRuleLimits(@NonNull RuleLimits limits) {
        allowedArea = limits.getAllowedArea();
        spatialFilterType = limits.getSpatialFilterType();
        catalogMode = limits.getCatalogMode();
    }

    public @Override MutableRuleLimits clone() {
        try {
            return (MutableRuleLimits) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
