/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.NamedStyle;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyledLayer;
import org.geotools.api.style.UserLayer;

/** Adapter implementation of {@link GeoServerSLDVisitor} */
public abstract class GeoServerSLDVisitorAdapter extends GeoServerSLDVisitor {
    public GeoServerSLDVisitorAdapter(Catalog catalog, CoordinateReferenceSystem fallbackCrs) {
        super(catalog, fallbackCrs);
    }

    @Override
    public PublishedInfo visitNamedLayerInternal(StyledLayer namedLayer) {
        return null;
    }

    @Override
    public void visitUserLayerRemoteOWS(UserLayer userLayer) {}

    @Override
    public void visitUserLayerInlineFeature(UserLayer userLayer) {}

    @Override
    public StyleInfo visitNamedStyleInternal(NamedStyle namedStyle) {
        return null;
    }

    @Override
    public void visitUserStyleInternal(Style userStyle) {}
}
