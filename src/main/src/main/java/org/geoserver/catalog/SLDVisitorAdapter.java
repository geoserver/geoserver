package org.geoserver.catalog;

import org.geotools.styling.NamedStyle;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.UserLayer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.List;

/**
 * Adapter implementation of {@link SLDVisitor}
 */
public class SLDVisitorAdapter extends SLDVisitor {
    public SLDVisitorAdapter(Catalog catalog, CoordinateReferenceSystem fallbackCrs) {
        super(catalog, fallbackCrs);
    }

    @Override
    public PublishedInfo visitNamedLayer(StyledLayer namedLayer) {
        return null;
    }

    @Override
    public void visitUserLayerRemoteOWS(UserLayer userLayer, List<LayerInfo> layerInfos) {

    }

    @Override
    public void visitUserLayerInlineFeature(UserLayer userLayer, LayerInfo info) {

    }

    @Override
    public Style visitNamedStyle(StyledLayer layer, NamedStyle namedStyle, LayerInfo info) throws IOException {
        return null;
    }

    @Override
    public void visitUserStyle(StyledLayer layer, Style userStyle, LayerInfo info) {

    }
}
