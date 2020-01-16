/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageTypeSpecifier;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.ServiceException;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.RenderingVariables;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.renderer.RenderListener;
import org.geotools.renderer.lite.GraphicsAwareDpiRescaleStyleVisitor;
import org.geotools.renderer.lite.MetaBufferEstimator;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.renderer.style.StyleAttributeExtractor;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.visitor.DpiRescaleStyleVisitor;
import org.geotools.styling.visitor.UomRescaleStyleVisitor;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Painting based layer identifier: this method actually paints a reduced version of the map to find
 * out which features really intercept the clicked point
 *
 * @author Andrea Aime - GeoSolutions
 */
public class VectorRenderingLayerIdentifier extends AbstractVectorLayerIdentifier
        implements ExtensionPriority {

    static final Logger LOGGER = Logging.getLogger(VectorRenderingLayerIdentifier.class);
    private static final String FEATURE_INFO_RENDERING_ENABLED_KEY =
            "org.geoserver.wms.featureinfo.render.enabled";
    // smaller by default than VectorBasicLayerIdentifier because this mode accounts for symbol
    // sizes,
    // not just for info point to geometry distance
    protected static final int MIN_BUFFER_SIZE =
            Integer.getInteger(VectorBasicLayerIdentifier.FEATUREINFO_DEFAULT_BUFFER, 3);
    public static boolean RENDERING_FEATUREINFO_ENABLED;

    private WMS wms;
    private VectorBasicLayerIdentifier fallback;
    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    static {
        String value = System.getProperty(FEATURE_INFO_RENDERING_ENABLED_KEY, "true");
        RENDERING_FEATUREINFO_ENABLED = Boolean.valueOf(value);
        if (!RENDERING_FEATUREINFO_ENABLED) {
            LOGGER.info(
                    "Rendering based GetFeatureInfo disabled since "
                            + FEATURE_INFO_RENDERING_ENABLED_KEY
                            + " is set to "
                            + value);
        }
    }

    public VectorRenderingLayerIdentifier(WMS wms, VectorBasicLayerIdentifier fallback) {
        this.wms = wms;
        this.fallback = fallback;
    }

    @Override
    public boolean canHandle(MapLayerInfo layer) {
        // selectively disable based on system settings
        if (!RENDERING_FEATUREINFO_ENABLED) {
            return false;
        }

        return super.canHandle(layer);
    }

    @Override
    public List<FeatureCollection> identify(
            FeatureInfoRequestParameters params, final int maxFeatures) throws Exception {
        LOGGER.log(Level.FINER, "Applying rendering based feature info identifier");

        // at the moment the new identifier works only with simple features due to a limitation
        // in the StreamingRenderer
        if (!(params.getLayer().getFeatureSource(true, params.getRequestedCRS()).getSchema()
                instanceof SimpleFeatureType)) {
            return fallback.identify(params, maxFeatures);
        }

        final Style style =
                preprocessStyle(params.getStyle(), params.getLayer().getFeature().getFeatureType());
        final int userBuffer = params.getBuffer() > 0 ? params.getBuffer() : MIN_BUFFER_SIZE;
        final int buffer = getBuffer(userBuffer);

        // check the style to see what's active
        final List<Rule> rules = getActiveRules(style, params.getScaleDenominator());
        if (rules.size() == 0) {
            return null;
        }
        GetMapRequest getMap = params.getGetMapRequest();
        getMap.getFormatOptions().put("antialias", "NONE");
        WMSMapContent mc = new WMSMapContent(getMap);
        try {
            // prepare the fake web map content
            mc.setTransparent(true);
            mc.setBuffer(params.getBuffer());
            mc.getViewport().setBounds(new ReferencedEnvelope(getMap.getBbox(), getMap.getCrs()));
            mc.setMapWidth(getMap.getWidth());
            mc.setMapHeight(getMap.getHeight());
            FeatureLayer layer = getLayer(params, style);
            mc.addLayer(layer);
            // setup the env variables just like in the original GetMap
            RenderingVariables.setupEnvironmentVariables(mc);

            // setup the transformation from screen to world space
            AffineTransform worldToScreen =
                    RendererUtilities.worldToScreenTransform(
                            params.getRequestedBounds(),
                            new Rectangle(params.getWidth(), params.getHeight()));
            AffineTransform screenToWorld = worldToScreen.createInverse();

            // apply uom rescale on the rules
            rescaleRules(rules, params);

            // setup the area we are actually going to paint
            int radius = getSearchRadius(params, rules, layer, getMap, screenToWorld);
            if (radius < buffer) {
                radius = buffer;
            }
            Envelope targetRasterSpace =
                    new Envelope(
                            params.getX() - radius,
                            params.getX() + radius,
                            params.getY() - radius,
                            params.getY() + radius);
            Envelope targetModelSpace =
                    JTS.transform(targetRasterSpace, new AffineTransform2D(screenToWorld));

            // prepare the image we are going to check rendering against
            int paintAreaSize = radius * 2;
            final BufferedImage image =
                    ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB)
                            .createBufferedImage(paintAreaSize, paintAreaSize);
            image.setAccelerationPriority(0);

            // and now the listener that will check for painted pixels
            int mid = radius;
            int hitAreaSize = buffer * 2 + 1;
            if (hitAreaSize > paintAreaSize) {
                hitAreaSize = paintAreaSize;
            }
            Rectangle hitArea = new Rectangle(mid - buffer, mid - buffer, hitAreaSize, hitAreaSize);
            final FeatureInfoRenderListener featureInfoListener =
                    new FeatureInfoRenderListener(
                            image, hitArea, maxFeatures, params.getPropertyNames());

            // update the map context
            mc.getViewport().setBounds(new ReferencedEnvelope(targetModelSpace, getMap.getCrs()));
            mc.setMapWidth(paintAreaSize);
            mc.setMapHeight(paintAreaSize);

            // and now run the rendering _almost_ like a GetMap
            GetMapOutputFormat rim = createMapOutputFormat(image, featureInfoListener);
            rim.produceMap(mc);

            List<SimpleFeature> features = featureInfoListener.getFeatures();

            return aggregateByFeatureType(features, params.getRequestedCRS());
        } finally {
            mc.dispose();
        }
    }

    protected int getBuffer(final int userBuffer) {
        if (wms.getMaxBuffer() <= 0) {
            return userBuffer;
        } else {
            return Math.min(userBuffer, wms.getMaxBuffer());
        }
    }

    protected GetMapOutputFormat createMapOutputFormat(
            final BufferedImage image, final FeatureInfoRenderListener featureInfoListener) {
        return new RenderedImageMapOutputFormat(wms) {

            private Graphics2D graphics;

            @Override
            protected RenderedImage prepareImage(
                    int width, int height, IndexColorModel palette, boolean transparent) {
                return image;
            }

            @Override
            protected Graphics2D getGraphics(
                    boolean transparent,
                    Color bgColor,
                    RenderedImage preparedImage,
                    Map<Key, Object> hintsMap) {
                graphics = super.getGraphics(transparent, bgColor, preparedImage, hintsMap);
                return graphics;
            }

            @Override
            protected void onBeforeRender(StreamingRenderer renderer) {
                // force the renderer into serial painting mode, as we need to check what
                // was painted to decide which features to include in the results
                Map hints = renderer.getRendererHints();
                hints.put(StreamingRenderer.OPTIMIZE_FTS_RENDERING_KEY, Boolean.FALSE);
                // disable antialiasing to speed up rendering
                hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

                // TODO: should we disable the screenmap as well?
                featureInfoListener.setGraphics(graphics);
                featureInfoListener.setRenderer(renderer);
                renderer.addRenderListener(featureInfoListener);
            }
        };
    }

    private void rescaleRules(List<Rule> rules, FeatureInfoRequestParameters params) {
        Map<Object, Object> rendererParams = new HashMap<Object, Object>();
        Integer requestedDpi = ((Integer) params.getGetMapRequest().getFormatOptions().get("dpi"));
        if (requestedDpi != null) {
            rendererParams.put(StreamingRenderer.DPI_KEY, requestedDpi);
        }

        // apply dpi rescale if necessary
        double standardDpi = RendererUtilities.getDpi(rendererParams);
        if (requestedDpi != null && standardDpi != requestedDpi) {
            double scaleFactor = requestedDpi / standardDpi;
            DpiRescaleStyleVisitor dpiVisitor =
                    new GraphicsAwareDpiRescaleStyleVisitor(scaleFactor);
            for (int i = 0; i < rules.size(); i++) {
                rules.get(i).accept(dpiVisitor);
                Rule rescaled = (Rule) dpiVisitor.getCopy();
                rules.set(i, rescaled);
            }
        }

        // apply UOM rescaling
        double pixelsPerMeters =
                RendererUtilities.calculatePixelsPerMeterRatio(
                        params.getScaleDenominator(), rendererParams);
        UomRescaleStyleVisitor uomVisitor = new UomRescaleStyleVisitor(pixelsPerMeters);
        for (int i = 0; i < rules.size(); i++) {
            rules.get(i).accept(uomVisitor);
            Rule rescaled = (Rule) uomVisitor.getCopy();
            rules.set(i, rescaled);
        }
    }

    private Style preprocessStyle(Style style, FeatureType schema) {
        FeatureInfoStylePreprocessor preprocessor = new FeatureInfoStylePreprocessor(schema);
        style.accept(preprocessor);
        Style result = (Style) preprocessor.getCopy();

        return result;
    }

    private List<FeatureCollection> aggregateByFeatureType(
            List<? extends Feature> features, CoordinateReferenceSystem targetcrs) {
        // group by feature type (rendering transformations might cause us to get more
        // than one type from the original layer)
        Map<FeatureType, List<Feature>> map = new HashMap<FeatureType, List<Feature>>();
        for (Feature f : features) {
            FeatureType type = f.getType();
            List<Feature> list = map.get(type);
            if (list == null) {
                list = new ArrayList<Feature>();
                map.put(type, list);
            }
            list.add(f);
        }

        // build a feature collection for each group
        List<FeatureCollection> result = new ArrayList<FeatureCollection>();
        for (Map.Entry<FeatureType, List<Feature>> entry : map.entrySet()) {
            FeatureType type = entry.getKey();
            List<Feature> list = entry.getValue();
            if (type instanceof SimpleFeatureType) {
                result.add(
                        new ListFeatureCollection(
                                (SimpleFeatureType) type,
                                new ArrayList<SimpleFeature>((List) list)));
            } else {
                result.add(new ListComplexFeatureCollection(type, list));
            }
        }

        // let's see if we need to reproject
        if (!wms.isFeaturesReprojectionDisabled()) {
            // try to reproject to target CRS
            return LayerIdentifierUtils.reproject(result, targetcrs);
        }

        // reprojection no allowed
        return result;
    }

    private FeatureLayer getLayer(FeatureInfoRequestParameters params, Style style)
            throws IOException {
        // build the full filter
        List<Object> times = params.getTimes();
        List<Object> elevations = params.getElevations();
        Filter layerFilter = params.getFilter();
        MapLayerInfo layer = params.getLayer();
        Filter staticDimensionFilter =
                wms.getTimeElevationToFilter(times, elevations, layer.getFeature());
        Filter customDimensionsFilter =
                wms.getDimensionsToFilter(
                        params.getGetMapRequest().getRawKvp(), layer.getFeature());
        final Filter dimensionFilter =
                FF.and(Arrays.asList(staticDimensionFilter, customDimensionsFilter));
        Filter filter;
        if (layerFilter == null) {
            filter = dimensionFilter;
        } else if (dimensionFilter == null) {
            filter = layerFilter;
        } else {
            filter = FF.and(Arrays.asList(layerFilter, dimensionFilter));
        }

        GetMapRequest getMap = params.getGetMapRequest();
        FeatureSource<? extends FeatureType, ? extends Feature> featureSource =
                super.handleClipParam(params, layer.getFeatureSource(true, getMap.getCrs()));
        final Query definitionQuery = new Query(featureSource.getSchema().getName().getLocalPart());
        definitionQuery.setVersion(getMap.getFeatureVersion());
        definitionQuery.setFilter(filter);
        definitionQuery.setSortBy(params.getSort());
        Map<String, String> viewParams = params.getViewParams();
        if (viewParams != null) {
            definitionQuery.setHints(new Hints(Hints.VIRTUAL_TABLE_PARAMETERS, viewParams));
        }

        // check for startIndex + offset
        final Integer startIndex = getMap.getStartIndex();
        if (startIndex != null) {
            QueryCapabilities queryCapabilities = featureSource.getQueryCapabilities();
            if (queryCapabilities.isOffsetSupported()) {
                // fsource is required to support
                // SortBy.NATURAL_ORDER so we don't bother checking
                definitionQuery.setStartIndex(startIndex);
            } else {
                // source = new PagingFeatureSource(source,
                // request.getStartIndex(), limit);
                throw new ServiceException(
                        "startIndex is not supported for the " + layer.getName() + " layer");
            }
        }

        int maxFeatures =
                getMap.getMaxFeatures() != null ? getMap.getMaxFeatures() : Integer.MAX_VALUE;
        definitionQuery.setMaxFeatures(maxFeatures);

        FeatureLayer result =
                new FeatureLayer(
                        new FeatureInfoFeatureSource(featureSource, params.getPropertyNames()),
                        style);
        result.setQuery(definitionQuery);

        return result;
    }

    private int getSearchRadius(
            FeatureInfoRequestParameters params,
            List<Rule> rules,
            FeatureLayer layer,
            GetMapRequest getMap,
            AffineTransform screenToWorld)
            throws TransformException, FactoryException, IOException {
        // is it part of the request params?
        int requestBuffer = params.getBuffer();
        if (requestBuffer > 0) {
            return requestBuffer;
        }

        // was it manually configured?
        Integer layerBuffer = null;
        final LayerInfo layerInfo = params.getLayer().getLayerInfo();
        if (layerInfo != null) {
            // it is a local layer
            layerBuffer = layerInfo.getMetadata().get(LayerInfo.BUFFER, Integer.class);
        }
        if (layerBuffer != null && layerBuffer > 0) {
            return layerBuffer;
        }

        // estimate the radius given the currently active rules
        MetaBufferEstimator estimator = new MetaBufferEstimator();
        for (Rule rule : rules) {
            rule.accept(estimator);
        }

        // easy case, the style is static, we can just use size computed from the style
        int estimatedRadius = estimator.getBuffer() / 2;
        if (estimator.isEstimateAccurate()) {
            if (estimatedRadius < MIN_BUFFER_SIZE) {
                return MIN_BUFFER_SIZE;
            } else {
                return estimatedRadius;
            }
        } else {
            // ok, so we have an estimate based on the static portion of the style,
            // let's extract the dynamic one
            DynamicSizeStyleExtractor extractor = new DynamicSizeStyleExtractor();
            final List<Rule> dynamicRules = new ArrayList<Rule>();
            for (Rule rule : rules) {
                rule.accept(extractor);
                Rule copy = (Rule) extractor.getCopy();
                if (copy != null) {
                    dynamicRules.add(copy);
                }
            }

            // this can happen, the meta buffer estimator can get tripped by
            // graphic fills using dynamic sizes for their strokes
            if (dynamicRules.size() == 0) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "No dynamic rules found, even if the estimator initially though so, "
                                    + "using the static analysis result: "
                                    + estimatedRadius);
                }
                return estimatedRadius;
            }

            // TODO: verify what expressions are used, if they are simple links to attributes
            // or direct proportionalities we could just compute the max value of the fields
            // involved
            FeatureSource<?, ?> fs = layer.getFeatureSource();
            Envelope targetRasterSpace =
                    new Envelope(
                            -estimatedRadius,
                            params.getWidth() + estimatedRadius,
                            -estimatedRadius,
                            params.getWidth() + estimatedRadius);
            Envelope expanded =
                    JTS.transform(targetRasterSpace, new AffineTransform2D(screenToWorld));
            ReferencedEnvelope renderingBBOX = new ReferencedEnvelope(expanded, getMap.getCrs());
            ReferencedEnvelope queryBBOX =
                    renderingBBOX.transform(fs.getSchema().getCoordinateReferenceSystem(), true);

            // setup the query
            Query query = layer.getQuery();
            BBOX bbox = FF.bbox(FF.property(""), queryBBOX);
            if (query.getFilter() == null || query.getFilter() == Filter.INCLUDE) {
                query.setFilter(bbox);
            } else {
                Filter and = FF.and(query.getFilter(), bbox);
                query.setFilter(and);
            }
            String[] dynamicProperties = getDynamicProperties(dynamicRules);
            query.setPropertyNames(dynamicProperties);

            // visit all features and evaluate buffer size
            final DynamicBufferEstimator dbe = new DynamicBufferEstimator();
            fs.getFeatures(query)
                    .accepts(
                            new FeatureVisitor() {

                                @Override
                                public void visit(Feature feature) {
                                    dbe.setFeature(feature);
                                    for (Rule rule : dynamicRules) {
                                        rule.accept(dbe);
                                    }
                                }
                            },
                            null);

            int dynamicBuffer = dbe.getBuffer();
            return Math.max(dynamicBuffer / 2, estimatedRadius);
        }
    }

    private String[] getDynamicProperties(List<Rule> dynamicRules) {
        StyleAttributeExtractor extractor = new StyleAttributeExtractor();
        for (Rule rule : dynamicRules) {
            rule.accept(extractor);
        }

        return extractor.getAttributeNames();
    }

    /** Returns a priority higher than the default, but still allows for overrides */
    @Override
    public int getPriority() {
        return (ExtensionPriority.LOWEST + ExtensionPriority.HIGHEST) / 2;
    }

    /**
     * Checks if the features just rendered hit the target area, and collects them. Stops the
     * rendering once enough features are collected
     *
     * @author Andrea Aime - GeoSolutions
     */
    static final class FeatureInfoRenderListener implements RenderListener {
        private final int scanlineStride;

        private Rectangle hitArea;

        List<SimpleFeature> features = new ArrayList<SimpleFeature>();

        String[] propertyNames;

        SimpleFeatureBuilder retypeBuilder;

        private int maxFeatures;

        ColorModel cm;

        BufferedImage bi;

        StreamingRenderer renderer;

        Feature previous;

        Graphics2D graphics;

        public FeatureInfoRenderListener(
                BufferedImage bi, Rectangle hitArea, int maxFeatures, String[] propertyNames) {
            verifyColorModel(bi);
            Raster raster = getRaster(bi);
            this.scanlineStride = raster.getDataBuffer().getSize() / raster.getHeight();
            this.hitArea = hitArea;
            this.maxFeatures = maxFeatures;
            this.cm = bi.getColorModel();
            this.bi = bi;
        }

        public void setGraphics(Graphics2D graphics) {
            this.graphics = graphics;
        }

        public void setRenderer(StreamingRenderer renderer) {
            this.renderer = renderer;
        }

        public List<SimpleFeature> getFeatures() {
            return features;
        }

        private void verifyColorModel(BufferedImage bi) {
            ColorModel cm = bi.getColorModel();
            if (!(cm instanceof DirectColorModel)) {
                throw new IllegalArgumentException(
                        "Invalid color model, it should be a DirectColorModel");
            }
            DirectColorModel dcm = (DirectColorModel) cm;
            if (dcm.getNumColorComponents() != 3 || !dcm.hasAlpha()) {
                throw new IllegalArgumentException(
                        "Invalid color model, it should be a 3 bands DirectColorModel with alpha");
            }
        }

        private Raster getRaster(BufferedImage image) {
            // in case the raster has a parent, this is likely a subimage, we have to force
            // a copy of the raster to get a data buffer we can scroll over without issues
            Raster raster = image.getRaster();
            if (raster.getParent() != null) {
                throw new IllegalArgumentException(
                        "The provided raster is a child of another image");
            } else {
                return raster;
            }
        }

        @Override
        public void featureRenderer(SimpleFeature feature) {
            // TODO: handle the case the feature became a grid due to rendering transformations?

            // feature caught by more than one rule?
            if (feature == previous) {
                // clean the hit area anyways before returning, as the feature might
                // have been rendered twice in a row coloring the hit area twice
                cleanHitArea();
                return;
            }

            // note: we need to extract the raster here, caching it will make us
            // get the old version of it if hw acceleration kicks in
            Raster raster = getRaster(bi);
            int[] pixels = ((java.awt.image.DataBufferInt) raster.getDataBuffer()).getData();

            // scan and clean the hit area, bail out early if we find a hit
            boolean hit = false;
            for (int row = hitArea.y; row < (hitArea.y + hitArea.height) && !hit; row++) {
                int idx = row * scanlineStride + hitArea.x;
                for (int col = hitArea.x; col < (hitArea.x + hitArea.width) && !hit; col++) {
                    final int color = pixels[idx];
                    final int alpha = cm.getAlpha(color);
                    if (!hit && alpha > 0) {
                        hit = true;
                    }
                    idx++;
                }
            }

            if (hit) {
                previous = feature;
                if (features.size() < maxFeatures) {
                    SimpleFeature retyped = retype(feature);
                    features.add(retyped);
                } else {
                    // we're done, stop rendering
                    renderer.stopRendering();
                }
            }

            // clean the hit area to prepare for next feature
            cleanHitArea();
        }

        private SimpleFeature retype(SimpleFeature feature) {
            if (propertyNames == null) {
                return feature;
            } else {
                if (retypeBuilder == null) {
                    SimpleFeatureType targetType =
                            SimpleFeatureTypeBuilder.retype(
                                    feature.getFeatureType(), propertyNames);
                    retypeBuilder = new SimpleFeatureBuilder(targetType);
                }
                return SimpleFeatureBuilder.retype(feature, retypeBuilder);
            }
        }

        private void cleanHitArea() {
            Composite oldComposite = graphics.getComposite();
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
            graphics.setColor(new Color(0, true));
            graphics.fillRect(hitArea.x, hitArea.y, hitArea.width, hitArea.height);
            graphics.setComposite(oldComposite);
        }

        @Override
        public void errorOccurred(Exception e) {
            // nothing to do here, there are other listeners handling this
        }
    }

    /**
     * A tiny wrapper that forces the attributes needed by getfeatureinfo to be returned: the
     * renderer normally tries to get only the attributes it needs for performance reasons
     *
     * @author Andrea Aime - GeoSolutions
     * @param <T> FeatureType
     * @param <F> Feature
     */
    static class FeatureInfoFeatureSource extends DecoratingFeatureSource<FeatureType, Feature> {

        String[] propertyNames;

        public FeatureInfoFeatureSource(FeatureSource delegate, String[] propertyNames) {
            super(delegate);
            this.propertyNames = propertyNames;
        }

        @Override
        public FeatureCollection getFeatures(Query query) throws IOException {
            Query q = new Query(query);
            // we made the renderer believe we support the screenmap, but we don't want
            // it really be applied, so remove it
            if (query.getHints() != null) {
                Hints newHints = new Hints(query.getHints());
                newHints.remove(Hints.SCREENMAP);
                q.setHints(newHints);
            }
            if (propertyNames == null || propertyNames.length == 0) {
                // no property selection, we return them all
                q.setProperties(Query.ALL_PROPERTIES);
            } else {
                // properties got selected, mix them with the ones needed by the renderer
                if (query.getPropertyNames() == null || query.getPropertyNames().length == 0) {
                    q.setPropertyNames(propertyNames);
                } else {
                    Set<String> names = new LinkedHashSet<>(Arrays.asList(propertyNames));
                    names.addAll(Arrays.asList(q.getPropertyNames()));
                    String[] newNames = names.toArray(new String[names.size()]);
                    q.setPropertyNames(newNames);
                }
            }
            return super.getFeatures(q);
        }

        @Override
        public Set<Key> getSupportedHints() {
            // force cloning, and make streaming renderer believe we do support
            // the screenmap
            Set<Key> hints = delegate.getSupportedHints();
            Set<Key> result;
            if (hints == null) {
                result = new HashSet<RenderingHints.Key>();
            } else {
                result = new HashSet<RenderingHints.Key>(hints);
            }
            result.remove(Hints.FEATURE_DETACHED);
            result.add(Hints.SCREENMAP);
            return result;
        }
    }
}
