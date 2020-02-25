/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.renderer.lite;

import com.google.common.base.Preconditions;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureTypes;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.filter.visitor.SpatialFilterVisitor;
import org.geotools.geometry.jts.Decimator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

/**
 * Utility methods to deal with transformations and style based queries.
 *
 * <p>Note, most code in this class has been taken and adapted from GeoTools' StreamingRenderer.
 */
public class VectorMapRenderUtils {

    private static final Logger LOGGER = Logging.getLogger(VectorMapRenderUtils.class);

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

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
                    VectorMapRenderUtils.getStyleQuery(
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

    public static double getMapScale(
            WMSMapContent mapContent, final ReferencedEnvelope renderingArea) {
        return RendererUtilities.calculateOGCScale(renderingArea, mapContent.getMapWidth(), null);
    }

    public static int getComputedBuffer(
            final int requestBufferScreen, List<LiteFeatureTypeStyle> styleList) {
        final int bufferScreen;
        if (requestBufferScreen <= 0) {
            MetaBufferEstimator bufferEstimator = new MetaBufferEstimator();
            styleList
                    .stream()
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

    public static List<LiteFeatureTypeStyle> getFeatureStyles(
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
     * Builds the transform from sourceCRS to destCRS/
     *
     * <p>Although we ask for 2D content (via {@link Hints#FEATURE_2D} ) not all DataStore
     * implementations are capable. With that in mind if the provided soruceCRS is not 2D we are
     * going to manually post-process the Geomtries into {@link DefaultGeographicCRS#WGS84} - and
     * the {@link MathTransform2D} returned here will transition from WGS84 to the requested
     * destCRS.
     *
     * @return the transform from {@code sourceCRS} to {@code destCRS}, will be an identity
     *     transform if the the two crs are equal
     * @throws FactoryException If no transform is available to the destCRS
     */
    public static MathTransform buildTransform(
            CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem destCRS)
            throws FactoryException {
        Preconditions.checkNotNull(sourceCRS, "sourceCRS");
        Preconditions.checkNotNull(destCRS, "destCRS");

        MathTransform transform = null;
        if (sourceCRS.getCoordinateSystem().getDimension() >= 3) {
            // We are going to transform over to DefaultGeographic.WGS84 on the fly
            // so we will set up our math transform to take it from there
            MathTransform toWgs84_3d =
                    CRS.findMathTransform(sourceCRS, DefaultGeographicCRS.WGS84_3D);
            MathTransform toWgs84_2d =
                    CRS.findMathTransform(
                            DefaultGeographicCRS.WGS84_3D, DefaultGeographicCRS.WGS84);
            transform = ConcatenatedTransform.create(toWgs84_3d, toWgs84_2d);
            sourceCRS = DefaultGeographicCRS.WGS84;
        }

        // the basic crs transformation, if any
        MathTransform2D sourceToTarget;
        sourceToTarget = (MathTransform2D) CRS.findMathTransform(sourceCRS, destCRS, true);

        if (transform == null) {
            return sourceToTarget;
        }
        if (sourceToTarget.isIdentity()) {
            return transform;
        }
        return ConcatenatedTransform.create(transform, sourceToTarget);
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
            final List<Filter> filtersToDS = new ArrayList<Filter>();
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
            org.opengis.filter.Filter ruleFiltersCombined;
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

    static ArrayList<LiteFeatureTypeStyle> createLiteFeatureTypeStyles(
            Layer layer,
            List<FeatureTypeStyle> featureStyles,
            FeatureType ftype,
            double scaleDenominator,
            Rectangle screenSize)
            throws IOException {

        ArrayList<LiteFeatureTypeStyle> result = new ArrayList<LiteFeatureTypeStyle>();

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

    private static List<Rule>[] splitRules(
            final FeatureTypeStyle fts, final double scaleDenominator) {

        List<Rule> ruleList = new ArrayList<Rule>();
        List<Rule> elseRuleList = new ArrayList<Rule>();

        ruleList = new ArrayList<>();
        elseRuleList = new ArrayList<>();

        for (Rule r : fts.rules()) {
            if (isWithInScale(r, scaleDenominator)) {
                if (r.isElseFilter()) {
                    elseRuleList.add(r);
                } else {
                    ruleList.add(r);
                }
            }
        }

        @SuppressWarnings("unchecked")
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

    private static boolean isFeatureTypeStyleActive(FeatureType ftype, FeatureTypeStyle fts) {
        // TODO: find a complex feature equivalent for this check
        return fts.featureTypeNames().isEmpty()
                || fts.featureTypeNames().stream().anyMatch(tn -> FeatureTypes.matches(ftype, tn));
    }
}
