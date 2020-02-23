/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.api.APIException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapDefaults;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RenderedImageMap;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.logging.Logging;
import org.opengis.filter.MultiValuedFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ThumbnailBuilder {

    static final Logger LOGGER = Logging.getLogger(ThumbnailBuilder.class);

    public static final int DEFAULT_PIXEL_SIZE = 512;
    DefaultWebMapService wms;
    private Catalog catalog;

    public ThumbnailBuilder(DefaultWebMapService wms, Catalog catalog) {
        this.wms = wms;
        this.catalog = catalog;
    }

    public RenderedImage buildThumbnailFor(StyleInfo styleInfo) throws IOException {

        GetMapRequest request = new GetMapRequest();
        setupLayersAndStyles(styleInfo, request);
        setupBoundsAndSizeFromConfiguration(styleInfo, request);

        // complete the request using
        GetMapDefaults defaulter = new GetMapDefaults();
        defaulter.setMaxSide(DEFAULT_PIXEL_SIZE);
        defaulter.autoSetMissingProperties(request);

        WebMap map = wms.getMap(request);
        if (map instanceof RenderedImageMap) {
            RenderedImageMap rim = (RenderedImageMap) map;
            return rim.getImage();
        }

        // unkonwn cause failure
        return null;
    }

    private void setupBoundsAndSizeFromConfiguration(StyleInfo styleInfo, GetMapRequest request) {
        Optional<StyleMetadataInfo> styleMetadata =
                Optional.ofNullable(
                        styleInfo
                                .getMetadata()
                                .get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class));
        if (styleMetadata.isPresent()) {
            StyleMetadataInfo metadata = styleMetadata.get();
            ReferencedEnvelope envelope = metadata.getThumbnailEnvelope();
            if (envelope != null) {
                request.setBbox(envelope);
                request.setCrs(envelope.getCoordinateReferenceSystem());
                int thumbnailWidth = metadata.getThumbnailWidth();

                if (thumbnailWidth > 0) {
                    request.setWidth(thumbnailWidth);
                }
            }
        }
    }

    public void setupLayersAndStyles(StyleInfo styleInfo, GetMapRequest request)
            throws IOException {
        StyledLayerDescriptor sld = styleInfo.getSLD();
        if (sld.getStyledLayers().length == 1) {
            // single layer case -> find the best candidate layer
            LayerInfo layer = getAssociatedLayer(styleInfo);
            if (layer == null) {
                throw new APIException(
                        "InternalError",
                        "Could not find a suitable sample layer to build the thumbnail image, please associate a layer to the style",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            request.setLayers(Arrays.asList(new MapLayerInfo(layer)));
            request.setStyles(Arrays.asList(styleInfo.getStyle()));
        } else {
            // multi-layer, build a style group
            try {
                LayerGroupInfo group = catalog.getFactory().createLayerGroup();
                group.getLayers().add(null);
                group.getStyles().add(styleInfo);
                request.setLayers(
                        group.layers()
                                .stream()
                                .map(l -> new MapLayerInfo(l))
                                .collect(Collectors.toList()));
                request.setStyles(
                        group.styles()
                                .stream()
                                .map(
                                        s -> {
                                            try {
                                                return s.getStyle();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        })
                                .collect(Collectors.toList()));
            } catch (Exception e) {
                throw new APIException(
                        "InternalError",
                        "Error happened while setting up thumbnail calculation for a style group",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        e);
            }
        }
    }

    /**
     * Picks the first layer that has the style associated as default, or if none found, the first
     * having it as associate. To get some stability in the output in case of multiple associations,
     * the styles are sorted by workspace and name
     */
    private LayerInfo getAssociatedLayer(StyleInfo styleInfo) {
        LayerInfo layer = null;
        try (CloseableIterator<LayerInfo> layers =
                catalog.list(
                        LayerInfo.class,
                        Predicates.or(
                                Predicates.equal("defaultStyle", styleInfo),
                                Predicates.equal(
                                        "styles", styleInfo, MultiValuedFilter.MatchAction.ANY)),
                        null,
                        null,
                        Predicates.sortBy("prefixedName", true))) {
            while (layers.hasNext()) {
                LayerInfo next = layers.next();
                if (styleInfo.equals(next.getDefaultStyle())) {
                    layer = next;
                    break;
                } else if (layer == null) {
                    layer = next;
                }
            }
        }

        return layer;
    }

    /** Returns true if sample data is available to build a thumbnail */
    public boolean canGenerateThumbnail(StyleInfo styleInfo) {
        GetMapRequest request = new GetMapRequest();
        try {
            setupLayersAndStyles(styleInfo, request);
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Could not setup thumbnail", e);
        }
        // if we have at least a layer, we can work it
        return request.getLayers().size() > 0;
    }
}
