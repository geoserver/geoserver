/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.geoserver.wms.decoration.MapDecorationLayout.FF;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WatermarkInfo;
import org.geoserver.wms.decoration.MapDecoration;
import org.geoserver.wms.decoration.MapDecorationLayout;
import org.geoserver.wms.decoration.MetatiledMapDecorationLayout;
import org.geoserver.wms.decoration.WatermarkDecoration;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.data.DataUtilities;
import org.geotools.feature.FeatureTypes;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.filter.visitor.SpatialFilterVisitor;
import org.geotools.geometry.jts.Decimator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.lite.MetaBufferEstimator;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.springframework.util.Assert;

/**
 * Base class for formats that do actually draw a map
 *
 * @author Simone Giannecchini, GeoSolutions
 * @author Gabriel Roldan
 */
public abstract class AbstractMapOutputFormat implements GetMapOutputFormat {
    /** A logger for this class. */
    public static final Logger LOGGER = Logging.getLogger(GetMapOutputFormat.class);

    private final String mime;

    private final Set<String> outputFormatNames;
    protected WMS wms;

    protected AbstractMapOutputFormat(final String mime) {
        this(mime, new String[] {mime});
    }

    protected AbstractMapOutputFormat(final String mime, final String... outputFormats) {
        this(
                mime,
                outputFormats == null
                        ? Collections.emptySet()
                        : new HashSet<>(Arrays.asList(outputFormats)));
    }

    protected AbstractMapOutputFormat(final String mime, Set<String> outputFormats) {
        Assert.notNull(mime, "mime");
        this.mime = mime;
        if (outputFormats == null) {
            outputFormats = Collections.emptySet();
        }

        Set<String> formats = caseInsensitiveOutputFormats(outputFormats);
        formats.add(mime);
        this.outputFormatNames = Collections.unmodifiableSet(formats);
    }

    private static Set<String> caseInsensitiveOutputFormats(Set<String> outputFormats) {
        Set<String> caseInsensitiveFormats = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveFormats.addAll(outputFormats);
        return caseInsensitiveFormats;
    }

    protected AbstractMapOutputFormat() {
        this(null, (String[]) null);
    }

    /** @see GetMapOutputFormat#getMimeType() */
    @Override
    public String getMimeType() {
        return mime;
    }

    /** @see GetMapOutputFormat#getOutputFormatNames() */
    @Override
    public Set<String> getOutputFormatNames() {
        return outputFormatNames;
    }

    protected MapDecorationLayout findDecorationLayout(GetMapRequest request, final boolean tiled) {
        if (wms == null) {
            throw new IllegalAccessError();
        }
        String layoutName = null;
        if (request.getFormatOptions() != null) {
            layoutName = (String) request.getFormatOptions().get("layout");
        }

        MapDecorationLayout layout = null;
        if (layoutName != null && !layoutName.trim().isEmpty()) {
            try {
                GeoServerResourceLoader loader = wms.getCatalog().getResourceLoader();
                Resource layouts = loader.get("layouts");
                if (layouts.getType() == Type.DIRECTORY) {
                    Resource layoutConfig = layouts.get(layoutName + ".xml");

                    if (layoutConfig.getType() == Type.RESOURCE) {
                        layout = MapDecorationLayout.fromFile(layoutConfig, tiled);
                    } else {
                        LOGGER.log(Level.WARNING, "Unknown layout requested: " + layoutName);
                    }
                } else {
                    LOGGER.log(Level.WARNING, "No layouts directory defined");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to load layout: " + layoutName, e);
            }

            if (layout == null) {
                throw new ServiceException("Could not find decoration layout named: " + layoutName);
            }
        }

        if (layout == null) {
            layout = tiled ? new MetatiledMapDecorationLayout() : new MapDecorationLayout();
        }

        MapDecorationLayout.Block watermark = getWatermark(wms.getServiceInfo());
        if (watermark != null) {
            layout.addBlock(watermark);
        }

        return layout;
    }

    public static MapDecorationLayout.Block getWatermark(WMSInfo wms) {
        WatermarkInfo watermark = (wms == null ? null : wms.getWatermark());
        if (watermark != null && watermark.isEnabled()) {
            Map<String, Expression> options = new HashMap<>();
            options.put("url", FF.literal(watermark.getURL()));
            options.put("opacity", FF.literal((255f - watermark.getTransparency()) / 2.55f));

            MapDecoration d = new WatermarkDecoration();
            try {
                d.loadOptions(options);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Couldn't construct watermark from configuration", e);
                throw new ServiceException(e);
            }

            MapDecorationLayout.Block.Position p = null;

            switch (watermark.getPosition()) {
                case TOP_LEFT:
                    p = MapDecorationLayout.Block.Position.UL;
                    break;
                case TOP_CENTER:
                    p = MapDecorationLayout.Block.Position.UC;
                    break;
                case TOP_RIGHT:
                    p = MapDecorationLayout.Block.Position.UR;
                    break;
                case MID_LEFT:
                    p = MapDecorationLayout.Block.Position.CL;
                    break;
                case MID_CENTER:
                    p = MapDecorationLayout.Block.Position.CC;
                    break;
                case MID_RIGHT:
                    p = MapDecorationLayout.Block.Position.CR;
                    break;
                case BOT_LEFT:
                    p = MapDecorationLayout.Block.Position.LL;
                    break;
                case BOT_CENTER:
                    p = MapDecorationLayout.Block.Position.LC;
                    break;
                case BOT_RIGHT:
                    p = MapDecorationLayout.Block.Position.LR;
                    break;
                default:
                    throw new ServiceException(
                            "Unknown WatermarkInfo.Position value.  Something is seriously wrong.");
            }

            return new MapDecorationLayout.Block(d, p, null, new Point(0, 0));
        }

        return null;
    }

    public static List<Query> getStyleQuery(List<Layer> layers, WMSMapContent mapContent)
            throws IOException {
        return layers.stream()
                .map(
                        layer -> {
                            try {
                                return getStyleQuery(layer, mapContent);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .collect(Collectors.toList());
    }

    /**
     * Creates a query selecting those features relevant to the style and extent of the given map,
     * for the given layer.
     */
    public static Query getStyleQuery(Layer layer, WMSMapContent mapContent) throws IOException {

        final ReferencedEnvelope renderingArea = mapContent.getRenderingArea();
        final Rectangle screenSize =
                new Rectangle(mapContent.getMapWidth(), mapContent.getMapHeight());
        final double mapScale = getMapScale(mapContent, renderingArea);

        final int requestBufferScreen = mapContent.getBuffer();

        double[] pixelSize = getPixelSize(renderingArea, screenSize);

        FeatureSource<?, ?> featureSource = layer.getFeatureSource();
        FeatureType schema = featureSource.getSchema();
        List<LiteFeatureTypeStyle> styleList =
                getFeatureStyles(layer, screenSize, mapScale, schema);

        // if there aren't any styles to render, we don't need to get any data....
        if (styleList.isEmpty()) {
            Query query = new Query(schema.getName().getLocalPart());
            query.setProperties(Query.NO_PROPERTIES);
            query.setFilter(Filter.EXCLUDE);
            return query;
        }

        final int bufferScreen = getComputedBuffer(requestBufferScreen, styleList);

        final ReferencedEnvelope queryArea = new ReferencedEnvelope(renderingArea);
        queryArea.expandBy(bufferScreen * Math.max(pixelSize[0], pixelSize[1]));

        GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();

        Query styleQuery;
        try {
            styleQuery =
                    getStyleQuery(
                            featureSource, styleList, queryArea, screenSize, geometryDescriptor);
        } catch (IllegalFilterException | FactoryException e) {
            throw new RuntimeException(e);
        }
        // take into account the origin query (coming from cql_filter or featureid)
        Query query = DataUtilities.mixQueries(styleQuery, layer.getQuery(), null);
        query.setProperties(Query.ALL_PROPERTIES);

        Hints hints = query.getHints();
        hints.put(Hints.FEATURE_2D, Boolean.TRUE);

        return query;
    }

    /**
     * Computes the scale denominator for the given map content and rendering area
     *
     * @param mapContent the map content
     * @param renderingArea the rendering area
     * @return the scale denominator
     */
    protected static double getMapScale(
            WMSMapContent mapContent, final ReferencedEnvelope renderingArea) {
        return RendererUtilities.calculateOGCScale(renderingArea, mapContent.getMapWidth(), null);
    }

    /**
     * Computes the buffer to be used for rendering, taking into account the request buffer screen
     *
     * @param requestBufferScreen the buffer requested by the user
     * @param styleList the list of styles to be rendered
     * @return the buffer to be used for rendering
     */
    protected static int getComputedBuffer(
            final int requestBufferScreen, List<LiteFeatureTypeStyle> styleList) {
        final int bufferScreen;
        if (requestBufferScreen <= 0) {
            MetaBufferEstimator bufferEstimator = new MetaBufferEstimator();
            styleList.stream()
                    .flatMap(
                            fts ->
                                    Stream.concat(
                                            Arrays.stream(fts.elseRules),
                                            Arrays.stream(fts.ruleList)))
                    .forEach(bufferEstimator::visit);
            bufferScreen = bufferEstimator.getBuffer();
        } else {
            bufferScreen = requestBufferScreen;
        }
        return bufferScreen;
    }

    /**
     * Gets the feature styles for the given layer, screen size, map scale and feature type
     *
     * @param layer the layer
     * @param screenSize the screen size
     * @param mapScale the map scale
     * @param schema the feature type
     * @return the list of feature type styles
     * @throws IOException in case of error
     */
    protected static List<LiteFeatureTypeStyle> getFeatureStyles(
            Layer layer, final Rectangle screenSize, final double mapScale, FeatureType schema)
            throws IOException {
        Style style = layer.getStyle();
        List<FeatureTypeStyle> featureStyles = style.featureTypeStyles();
        List<LiteFeatureTypeStyle> styleList =
                createLiteFeatureTypeStyles(layer, featureStyles, schema, mapScale, screenSize);
        return styleList;
    }

    protected static double[] getPixelSize(
            final ReferencedEnvelope renderingArea, final Rectangle screenSize) {
        double[] pixelSize;
        try {
            pixelSize =
                    Decimator.computeGeneralizationDistances(
                            ProjectiveTransform.create(
                                            RendererUtilities.worldToScreenTransform(
                                                    renderingArea, screenSize))
                                    .inverse(),
                            screenSize,
                            1.0);
        } catch (TransformException ex) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Error while computing pixel size", ex);
            }
            pixelSize =
                    new double[] {
                        renderingArea.getWidth() / screenSize.getWidth(),
                        renderingArea.getHeight() / screenSize.getHeight()
                    };
        }
        return pixelSize;
    }

    /**
     * Creates a query selecting those features relevant to the style and extent of the given map,
     * for the given layer.
     *
     * @param source the feature source
     * @param styleList the list of styles
     * @param queryArea the query area
     * @param screenSize the screen size
     * @param geometryAttribute the geometry attribute
     * @return the query
     * @throws IllegalFilterException in case of error
     * @throws IOException in case of error
     * @throws FactoryException in case of error
     */
    private static Query getStyleQuery(
            FeatureSource<?, ?> source,
            List<LiteFeatureTypeStyle> styleList,
            ReferencedEnvelope queryArea,
            Rectangle screenSize,
            GeometryDescriptor geometryAttribute)
            throws IllegalFilterException, IOException, FactoryException {

        final FeatureType schema = source.getSchema();
        Query query = new Query(schema.getName().getLocalPart());
        query.setProperties(Query.ALL_PROPERTIES);

        String geomName = geometryAttribute.getLocalName();
        Filter filter =
                reprojectSpatialFilter(
                        queryArea.getCoordinateReferenceSystem(),
                        schema,
                        FF.bbox(FF.property(geomName), queryArea));

        query.setFilter(filter);

        LiteFeatureTypeStyle[] styles =
                styleList.toArray(new LiteFeatureTypeStyle[styleList.size()]);

        try {
            processRuleForQuery(styles, query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // simplify the filter
        SimplifyingFilterVisitor simplifier = new SimplifyingFilterVisitor();
        simplifier.setFeatureType(source.getSchema());
        Filter simplifiedFilter = (Filter) query.getFilter().accept(simplifier, null);
        query.setFilter(simplifiedFilter);
        return query;
    }

    /*
     * Reprojects spatial filters so that they match the feature source native CRS, and assuming all literal
     * geometries are specified in the specified declaredCRS
     *
     * Modified from StreamingRenderer
     */
    private static Filter reprojectSpatialFilter(
            CoordinateReferenceSystem declaredCRS, FeatureType schema, Filter filter) {
        // NPE avoidance
        if (filter == null) {
            return null;
        }

        // do we have any spatial filter?
        SpatialFilterVisitor sfv = new SpatialFilterVisitor();
        filter.accept(sfv, null);
        if (!sfv.hasSpatialFilter()) {
            return filter;
        }

        // all right, we need to default the literals to the declaredCRS and then reproject to
        // the native one
        DefaultCRSFilterVisitor defaulter = new DefaultCRSFilterVisitor(FF, declaredCRS);
        Filter defaulted = (Filter) filter.accept(defaulter, null);
        ReprojectingFilterVisitor reprojector = new ReprojectingFilterVisitor(FF, schema);
        Filter reprojected = (Filter) defaulted.accept(reprojector, null);
        return reprojected;
    }

    /**
     * Creates a list of LiteFeatureTypeStyles for the given layer, feature styles, feature type,
     * scale denominator and screen size
     *
     * @param layer the layer
     * @param featureStyles the feature styles
     * @param ftype the feature type
     * @param scaleDenominator the scale denominator
     * @param screenSize the screen size
     * @return a list of LiteFeatureTypeStyles
     * @throws IOException in case of error
     */
    protected static ArrayList<LiteFeatureTypeStyle> createLiteFeatureTypeStyles(
            Layer layer,
            List<FeatureTypeStyle> featureStyles,
            FeatureType ftype,
            double scaleDenominator,
            Rectangle screenSize)
            throws IOException {

        ArrayList<LiteFeatureTypeStyle> result = new ArrayList<>();

        LiteFeatureTypeStyle lfts;

        for (FeatureTypeStyle fts : featureStyles) {
            if (isFeatureTypeStyleActive(ftype, fts)) {
                // DJB: this FTS is compatible with this FT.

                // get applicable rules at the current scale
                List<Rule>[] splittedRules = splitRules(fts, scaleDenominator);
                List<Rule> ruleList = splittedRules[0];
                List<Rule> elseRuleList = splittedRules[1];

                // if none, skip it
                if ((ruleList.isEmpty()) && (elseRuleList.isEmpty())) continue;

                // we can optimize this one and draw directly on the graphics, assuming
                // there is no composition
                Graphics2D graphics = null;
                lfts =
                        new LiteFeatureTypeStyle(
                                layer, graphics, ruleList, elseRuleList, fts.getTransformation());

                result.add(lfts);
            }
        }
        return result;
    }

    /**
     * Splits the rules of a feature type style into two lists: one for the rules that are active at
     * the current scale and one for the else rules
     *
     * @param fts the feature type style
     * @param scaleDenominator the scale denominator
     * @return an array of two lists: the first one contains the rules that are active at the
     *     current
     */
    private static List<Rule>[] splitRules(
            final FeatureTypeStyle fts, final double scaleDenominator) {

        List<Rule> ruleList = new ArrayList<>();
        List<Rule> elseRuleList = new ArrayList<>();

        for (Rule r : fts.rules()) {
            if (isWithInScale(r, scaleDenominator)) {
                if (r.isElseFilter()) {
                    elseRuleList.add(r);
                } else {
                    ruleList.add(r);
                }
            }
        }

        @SuppressWarnings({"unchecked", "PMD.UseShortArrayInitializer"})
        List<Rule>[] ret = new List[] {ruleList, elseRuleList};
        return ret;
    }

    /**
     * Checks if a rule can be triggered at the current scale level
     *
     * @return true if the scale is compatible with the rule settings
     */
    private static boolean isWithInScale(Rule r, double scaleDenominator) {
        /** Tolerance used to compare doubles for equality */
        final double TOLERANCE = 1e-6;
        return ((r.getMinScaleDenominator() - TOLERANCE) <= scaleDenominator)
                && ((r.getMaxScaleDenominator() + TOLERANCE) > scaleDenominator);
    }

    /**
     * Checks if a feature type style is active for a given feature type
     *
     * @param ftype the feature type
     * @param fts the feature type style
     * @return true if the feature type style is active
     */
    private static boolean isFeatureTypeStyleActive(FeatureType ftype, FeatureTypeStyle fts) {
        // TODO: find a complex feature equivalent for this check
        return fts.featureTypeNames().isEmpty()
                || fts.featureTypeNames().stream().anyMatch(tn -> FeatureTypes.matches(ftype, tn));
    }

    /**
     * JE: If there is a single rule "and" its filter together with the query's filter and send it
     * off to datastore. This will allow as more processing to be done on the back end... Very
     * useful if DataStore is a database. Problem is that worst case each filter is ran twice. Next
     * we will modify it to find a "Common" filter between all rules and send that to the datastore.
     *
     * <p>DJB: trying to be smarter. If there are no "elseRules" and no rules w/o a filter, then it
     * makes sense to send them off to the Datastore We limit the number of Filters sent off to the
     * datastore, just because it could get a bit rediculous. In general, for a database, if you can
     * limit 10% of the rows being returned you're probably doing quite well. The main problem is
     * when your filters really mean you're secretly asking for all the data in which case sending
     * the filters to the Datastore actually costs you. But, databases are *much* faster at
     * processing the Filters than JAVA is and can use statistical analysis to do it.
     */
    private static void processRuleForQuery(LiteFeatureTypeStyle[] styles, Query query) {
        try {

            // first we check to see if there are >
            // "getMaxFiltersToSendToDatastore" rules
            // if so, then we dont do anything since no matter what there's too
            // many to send down.
            // next we check for any else rules. If we find any --> dont send
            // anything to Datastore
            // next we check for rules w/o filters. If we find any --> dont send
            // anything to Datastore
            //
            // otherwise, we're gold and can "or" together all the filters then
            // AND it with the original filter.
            // ie. SELECT * FROM ... WHERE (the_geom && BBOX) AND (filter1 OR
            // filter2 OR filter3);

            final int maxFilters = 5;
            final List<Filter> filtersToDS = new ArrayList<>();
            // look at each featuretypestyle
            for (LiteFeatureTypeStyle style : styles) {
                if (style.elseRules.length > 0) // uh-oh has elseRule
                return;
                // look at each rule in the featuretypestyle
                for (Rule r : style.ruleList) {
                    if (r.getFilter() == null) return; // uh-oh has no filter (want all rows)
                    filtersToDS.add(r.getFilter());
                }
            }

            // if too many bail out
            if (filtersToDS.size() > maxFilters) return;

            // or together all the filters
            org.geotools.api.filter.Filter ruleFiltersCombined;
            if (filtersToDS.size() == 1) {
                ruleFiltersCombined = filtersToDS.get(0);
            } else {
                ruleFiltersCombined = FF.or(filtersToDS);
            }

            // combine with the pre-existing filter
            ruleFiltersCombined = FF.and(query.getFilter(), ruleFiltersCombined);
            query.setFilter(ruleFiltersCombined);
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                        Level.SEVERE,
                        "Could not send rules to datastore due to: " + e.getMessage(),
                        e);
            }
        }
    }
}
