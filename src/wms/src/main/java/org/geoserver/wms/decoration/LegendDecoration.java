/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.decoration;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.geoserver.catalog.*;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.legendgraphic.BufferedImageLegendGraphicBuilder;
import org.geoserver.wms.legendgraphic.GetLegendGraphicKvpReader;
import org.geoserver.wms.legendgraphic.LegendUtils;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.map.Layer;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.styling.FeatureTypeStyle;
import org.opengis.style.Rule;

public class LegendDecoration extends AbstractDispatcherCallback implements MapDecoration {
    private static int TITLE_INDENT = 5;
    private static double BETWEEN_LEGENDS_PERCENT_INDENT = 0.05;

    private final WMS wms;
    private Map<String, String> options;
    private ThreadLocal<List<LayerLegend>> legends = new ThreadLocal<List<LayerLegend>>();
    private List<String> layers;
    private boolean useSldTitle;

    public LegendDecoration(WMS wms) {
        this.wms = wms;
        this.layers = new ArrayList<String>();
    }

    @Override
    public void finished(Request request) {
        this.legends.remove();
    }

    public void loadOptions(Map<String, String> options) {
        this.options = new HashMap(options);
        String layers = this.options.remove("layers");
        if (layers != null) {
            String[] splittedLayers = layers.split(",");
            this.layers.addAll(Arrays.asList(splittedLayers));
        }

        String sldTitle = this.options.remove("sldTitle");
        if ("true".equalsIgnoreCase(sldTitle) || "on".equalsIgnoreCase(sldTitle)) {
            useSldTitle = true;
        }
    }

    public Dimension findOptimalSize(Graphics2D g2d, WMSMapContent mapContext) {
        double dpi = RendererUtilities.getDpi(mapContext.getRequest().getFormatOptions());
        double standardDpi = RendererUtilities.getDpi(Collections.emptyMap());
        double scaleFactor = dpi / standardDpi;

        List<LayerLegend> layerLegends = getLayerLegend(g2d, mapContext, null);

        legends.set(layerLegends);

        int width = 0;
        int height = 0;
        for (LayerLegend legend : layerLegends) {
            int legendHeight = legend.legend.getHeight();
            int legendWidth = legend.legend.getWidth();
            int titleWidth = 0;
            int titleHeight = 0;

            if (legend.title != null) {
                titleWidth = legend.title.getWidth();
                titleHeight = legend.title.getHeight();
            }

            int tmpHeight = legendHeight + titleHeight;
            int tmpWidth = legendWidth;
            if (titleWidth > legendWidth) {
                tmpWidth = titleWidth;
                tmpWidth += ((int) Math.ceil(TITLE_INDENT * 2 * scaleFactor)); // indent the title
            }

            Dimension size =
                    new BasicStroke((float) scaleFactor)
                            .createStrokedShape(new Rectangle(0, 0, tmpWidth, tmpHeight))
                            .getBounds()
                            .getSize();
            height +=
                    (int)
                            Math.ceil(
                                    size.getHeight()
                                            + (size.getHeight() * BETWEEN_LEGENDS_PERCENT_INDENT));
            if (size.getWidth() > width) {
                width = (int) Math.ceil(size.getWidth());
            }
        }

        return new Dimension(width, height);
    }

    public void paint(Graphics2D g2d, Rectangle paintArea, WMSMapContent mapContext)
            throws Exception {
        // check if LayerLegends have been computed in the above method; if not
        // (cause a custom legend size and findOptimalSize has not been called)
        // they are produced here
        List<LayerLegend> legends =
                this.legends.get() != null
                        ? this.legends.get()
                        : getLayerLegend(g2d, mapContext, paintArea);
        double dpi = RendererUtilities.getDpi(mapContext.getRequest().getFormatOptions());
        double standardDpi = RendererUtilities.getDpi(Collections.emptyMap());
        double scaleFactor = dpi / standardDpi;

        Rectangle mainClip = g2d.getClipBounds();
        int heightOffset = 0;

        Iterator<LayerLegend> imageIterator = legends.iterator();
        while (imageIterator.hasNext()) {
            LayerLegend legend = imageIterator.next();

            int height = (int) paintArea.getHeight();
            int width = (int) paintArea.getWidth();
            if (legend.title != null) {
                height += legend.title.getHeight();
                if (width < legend.title.getWidth()) {
                    width = legend.title.getWidth();
                    width += ((int) Math.ceil(TITLE_INDENT * 2 * scaleFactor)); // indent the title
                }
            }

            Dimension size =
                    new BasicStroke((float) scaleFactor)
                            .createStrokedShape(new Rectangle(0, 0, width, height))
                            .getBounds()
                            .getSize();
            int strokeHeight = (int) Math.ceil(size.getHeight());
            int strokeWidth = (int) Math.ceil(size.getWidth());

            // output image
            BufferedImage finalLegend =
                    ImageUtils.createImage(
                            strokeWidth, strokeHeight, (IndexColorModel) null, false);
            Graphics2D finalGraphics =
                    ImageUtils.prepareTransparency(
                            false,
                            LegendUtils.getBackgroundColor(legend.request),
                            finalLegend,
                            new HashMap<RenderingHints.Key, Object>());

            // title
            int titleHeightOffset = 0;
            if (legend.title != null) {
                finalGraphics.drawImage(
                        legend.title,
                        ((strokeWidth - legend.title.getWidth()) / 2), // center
                        (strokeHeight - height),
                        null);
                titleHeightOffset += legend.title.getHeight();
            }

            // legend
            finalGraphics.drawImage(
                    legend.legend,
                    (strokeWidth - width) / 2, // place on the left
                    // (strokeWidth - legend.legend.getWidth()) / 2, // center
                    (strokeHeight - height) + titleHeightOffset,
                    null);

            // border
            finalGraphics.setColor(LegendUtils.DEFAULT_BORDER_COLOR);
            finalGraphics.fill(
                    new BasicStroke((float) scaleFactor)
                            .createStrokedShape(
                                    new Rectangle(0, 0, strokeWidth - 1, strokeHeight - 1)));

            // draw output image
            g2d.drawImage(
                    finalLegend,
                    mainClip.x + (int) Math.ceil((paintArea.getWidth() - strokeWidth) / 2),
                    mainClip.y + heightOffset,
                    null);

            heightOffset += strokeHeight + (strokeHeight * BETWEEN_LEGENDS_PERCENT_INDENT);
        }
    }

    private String findTitle(Layer layer, Catalog catalog) {
        if (layer.getTitle() == null) {
            return null;
        }
        String[] nameparts = layer.getTitle().split(":");

        ResourceInfo resource =
                nameparts.length > 1
                        ? catalog.getResourceByName(nameparts[0], nameparts[1], ResourceInfo.class)
                        : catalog.getResourceByName(nameparts[0], ResourceInfo.class);

        if (useSldTitle
                && layer.getStyle() != null
                && layer.getStyle().getDescription() != null
                && layer.getStyle().getDescription().getTitle() != null) {
            return layer.getStyle().getDescription().getTitle().toString();
        }
        if (resource != null) {
            return resource.getTitle();
        } else {
            return layer.getTitle();
        }
    }

    private void setLegendInfo(Layer layer, GetLegendGraphicRequest request, Rectangle size) {
        // online resource handling
        LayerInfo info = wms.getLayerByName(layer.getTitle());
        StyleInfo defaultStyle = info.getDefaultStyle();

        Predicate<StyleInfo> predicate =
                new Predicate<StyleInfo>() {
                    @Override
                    public boolean test(StyleInfo s) {
                        try {
                            return s.getName().equals(layer.getStyle().getName())
                                    && s.getStyle() != null;
                        } catch (IOException e) {
                            return false;
                        }
                    }
                };
        StyleInfo sInfo =
                info.getStyles()
                        .stream()
                        .filter(predicate)
                        .findFirst()
                        .orElseGet(() -> defaultStyle);

        LegendInfo legend = sInfo.getLegend();
        // if there is no online resource symbol size is computed in
        // the buffered legend graphic builder
        if (legend != null && legend.getOnlineResource() != null) {
            // if present takes the size passed into the decorator
            if (size != null) {
                request.setWidth((int) size.getWidth());
                request.setHeight((int) size.getHeight());
            } else {
                request.setWidth(legend.getWidth());
                request.setHeight(legend.getHeight());
            }
        }

        // using featureSource schema name because LegendRequest Name attribute
        // has been set from it
        GetLegendGraphicRequest.LegendRequest legendReq =
                request.getLegend(layer.getFeatureSource().getSchema().getName());
        legendReq.setLayerInfo(info);
        GetLegendGraphicKvpReader reader = new GetLegendGraphicKvpReader(wms);
        LegendInfo legendInfo = reader.resolveLegendInfo(sInfo.getLegend(), request, sInfo);
        if (legendInfo != null) {
            legendReq.setLegendInfo(legendInfo);
        }
    }

    private class LayerLegend {
        public BufferedImage title;
        public BufferedImage legend;
        public GetLegendGraphicRequest request;
    }

    private List<LayerLegend> getLayerLegend(
            Graphics2D g2d, WMSMapContent mapContext, Rectangle size) {
        List<LayerLegend> legendLayers = new ArrayList<>();
        double scaleDenominator = mapContext.getScaleDenominator(true);
        for (Layer layer : mapContext.layers()) {
            Rule[] applicableRules =
                    LegendUtils.getApplicableRules(
                            layer.getStyle().featureTypeStyles().toArray(new FeatureTypeStyle[0]),
                            scaleDenominator);
            if ((!layers.isEmpty() && !layers.contains(layer.getTitle()))
                    || applicableRules.length == 0) {
                continue;
            }

            BufferedImageLegendGraphicBuilder legendGraphicBuilder =
                    new BufferedImageLegendGraphicBuilder();
            GetLegendGraphicRequest request = new GetLegendGraphicRequest();
            request.setLayer(layer.getFeatureSource().getSchema());
            request.setTransparent(true);
            request.setScale(scaleDenominator);
            request.setStyle(layer.getStyle());
            request.setWms(wms);
            final Request dispatcherRequest = Dispatcher.REQUEST.get();
            if (dispatcherRequest != null) {
                request.setKvp(dispatcherRequest.getKvp());
                request.setRawKvp(dispatcherRequest.getRawKvp());
            }
            setLegendInfo(layer, request, size);

            Map legendOptions = new CaseInsensitiveMap(options);
            legendOptions.putAll(mapContext.getRequest().getFormatOptions());
            if (dispatcherRequest != null
                    && dispatcherRequest.getKvp().get("legend_options") != null) {
                legendOptions.putAll((Map) dispatcherRequest.getKvp().get("legend_options"));
            }
            request.setLegendOptions(legendOptions);

            LayerLegend legend = new LayerLegend();
            legend.request = request;
            String title = findTitle(layer, wms.getGeoServer().getCatalog());
            if (title != null) {
                Font newFont = LegendUtils.getLabelFont(request);
                newFont = newFont.deriveFont(Font.BOLD);
                newFont = newFont.deriveFont((float) newFont.getSize() + 2);

                Font oldFont = g2d.getFont();
                g2d.setFont(newFont);
                if (LegendUtils.isFontAntiAliasing(legend.request)) {
                    g2d.setRenderingHint(
                            RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                } else {
                    g2d.setRenderingHint(
                            RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                }
                BufferedImage titleImage = LegendUtils.renderLabel(title, g2d, request);
                g2d.setFont(oldFont);
                legend.title = titleImage;
            }

            BufferedImage legendImage = legendGraphicBuilder.buildLegendGraphic(request);
            legend.legend = legendImage;

            legendLayers.add(legend);
        }
        return legendLayers;
    }
}
