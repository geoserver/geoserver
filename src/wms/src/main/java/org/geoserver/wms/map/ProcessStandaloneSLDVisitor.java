package org.geoserver.wms.map;

import org.geoserver.catalog.*;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.styling.*;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Processes a standalone SLD document for use in a WMS GetMap request
 *
 * Replacement for {@link org.geoserver.wms.map.GetMapKvpRequestReader#processStandaloneSld}, using {@link SLDVisitor}
 */
public class ProcessStandaloneSLDVisitor extends SLDVisitor {

    final WMS wms;
    final GetMapRequest request;
    final List<MapLayerInfo> layers;
    final List<Style> styles;
    MapLayerInfo currLayer = null;

    public ProcessStandaloneSLDVisitor(final WMS wms, final GetMapRequest request) {
        super(wms.getCatalog(), request.getCrs());
        this.wms = wms;
        this.request = request;

        layers = new ArrayList<>();
        styles = new ArrayList<>();
    }

    @Override
    public ProcessStandaloneSLDVisitor apply(StyledLayerDescriptor sld) throws IOException {
        super.apply(sld);
        request.setLayers(layers);
        request.setStyles(styles);
        return this;
    }
    @Override
    public PublishedInfo visitNamedLayer(StyledLayer sl) {
        currLayer = null;
        String layerName = sl.getName();

        LayerGroupInfo groupInfo = wms.getLayerGroupByName(layerName);
        if (groupInfo == null) {
            LayerInfo layerInfo = wms.getLayerByName(layerName);

            if (layerInfo == null) {
                throw new ServiceException("Unknown layer: " + layerName);
            }

            currLayer = new MapLayerInfo(layerInfo);
            if (sl instanceof NamedLayer) {
                NamedLayer namedLayer = ((NamedLayer) sl);
                currLayer.setLayerFeatureConstraints(namedLayer
                        .getLayerFeatureConstraints());
            }
            return layerInfo;
        }
        return groupInfo;
    }

    @Override
    public void visitUserLayerRemoteOWS(UserLayer ul, List<LayerInfo> layers) {
        currLayer = null;
        final FeatureTypeConstraint[] featureConstraints = ul.getLayerFeatureConstraints();
        if (request.getFilter() == null) {
            request.setFilter(new ArrayList());
        }
        for (int i = 0; i < featureConstraints.length; i++) {
            // make sure the layer is there
            String name = featureConstraints[i].getFeatureTypeName();

            // grab the filter
            Filter filter = featureConstraints[i].getFilter();
            if (filter == null) {
                filter = Filter.INCLUDE;
            }
            request.getFilter().add(filter);
        }
    }

    @Override
    public void visitUserLayerInlineFeature(UserLayer ul, LayerInfo info) {
        currLayer = new MapLayerInfo(info);
    }

    @Override
    public Style visitNamedStyle(StyledLayer layer, NamedStyle namedStyle, LayerInfo obj) throws IOException {
        Style s = wms.getStyleByName(namedStyle.getName());

        if (s == null) {
            String failMessage = "couldn't find style named '" + namedStyle.getName() + "'";
            if (currLayer.getType() == MapLayerInfo.TYPE_RASTER) {
                // hmm, well, the style they specified in the wms request wasn't found.
                // Let's try the default raster style named 'raster'
                s = wms.getStyleByName("raster");
                if (s == null) {
                    // nope, no default raster style either. Give up.
                    throw new ServiceException(failMessage + "  Also tried to use "
                            + "the generic raster style 'raster', but it wasn't available.");
                }
            } else {
                throw new ServiceException(failMessage);
            }
        }

        if (currLayer != null) {
            layers.add(currLayer);
            styles.add(s);
        }

        return s;
    }

    @Override
    public void visitUserStyle(StyledLayer layer, Style userStyle, LayerInfo obj) {
        if (currLayer != null) {
            layers.add(currLayer);
            styles.add(userStyle);
        } else if (obj != null) {
            layers.add(new MapLayerInfo(obj));
            styles.add(userStyle);
        }
    }
}
