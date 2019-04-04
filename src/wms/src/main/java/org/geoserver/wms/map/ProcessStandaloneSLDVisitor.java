/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.*;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.styling.*;
import org.opengis.filter.Filter;

/**
 * Processes a standalone SLD document for use in a WMS GetMap request
 *
 * <p>Replacement for org.geoserver.wms.map.GetMapKvpRequestReader#processStandaloneSld, using
 * {@link GeoServerSLDVisitor}
 */
public class ProcessStandaloneSLDVisitor extends GeoServerSLDVisitor {

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
    public void visit(StyledLayerDescriptor sld) {
        try {
            super.visit(sld);
            request.setLayers(layers);
            request.setStyles(styles);
            // Convert various more specific exceptions into service exceptions
        } catch (IllegalStateException | UncheckedIOException | UnsupportedOperationException e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public PublishedInfo visitNamedLayerInternal(StyledLayer sl) {
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
                currLayer.setLayerFeatureConstraints(namedLayer.getLayerFeatureConstraints());
            }
            return layerInfo;
        }
        return groupInfo;
    }

    @Override
    public void visitUserLayerRemoteOWS(UserLayer ul) {
        currLayer = null;
        final FeatureTypeConstraint[] featureConstraints = ul.getLayerFeatureConstraints();
        if (request.getFilter() == null) {
            request.setFilter(new ArrayList());
        }
        for (int i = 0; i < featureConstraints.length; i++) {
            // grab the filter
            Filter filter = featureConstraints[i].getFilter();
            if (filter == null) {
                filter = Filter.INCLUDE;
            }
            request.getFilter().add(filter);
        }
    }

    @Override
    public void visitUserLayerInlineFeature(UserLayer ul) {
        currLayer = new MapLayerInfo((LayerInfo) info);
    }

    @Override
    public StyleInfo visitNamedStyleInternal(NamedStyle namedStyle) {
        StyleInfo s;
        s = catalog.getStyleByName(namedStyle.getName());
        if (s == null) {
            String failMessage = "couldn't find style named '" + namedStyle.getName() + "'";
            if (currLayer.getType() == MapLayerInfo.TYPE_RASTER) {
                // hmm, well, the style they specified in the wms request wasn't found.
                // Let's try the default raster style named 'raster'
                s = catalog.getStyleByName("raster");
                if (s == null) {
                    // nope, no default raster style either. Give up.
                    throw new ServiceException(
                            failMessage
                                    + "  Also tried to use "
                                    + "the generic raster style 'raster', but it wasn't available.");
                }
            } else {
                throw new ServiceException(failMessage);
            }
        }

        if (currLayer != null) {
            try {
                layers.add(currLayer);
                styles.add(s.getStyle());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return s;
    }

    @Override
    public void visitUserStyleInternal(Style userStyle) {
        if (currLayer != null) {
            layers.add(currLayer);
            styles.add(userStyle);
        } else if (info != null && info instanceof LayerInfo) {
            layers.add(new MapLayerInfo((LayerInfo) info));
            styles.add(userStyle);
        }
    }
}
