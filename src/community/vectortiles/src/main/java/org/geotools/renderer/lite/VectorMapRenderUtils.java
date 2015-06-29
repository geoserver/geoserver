/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.renderer.lite;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureTypes;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.Decimator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.geotools.renderer.ScreenMap;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * Utility methods to deal with transformations and style based queries.
 * <p>
 * Note this class is in this package to access some package visible fields from the same geotools
 * package.
 *
 */
public class VectorMapRenderUtils {

    private static final Logger LOGGER = Logging.getLogger(VectorMapRenderUtils.class);

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    private static final double generalizationDistance = 0.8;

    public static Query getStyleQuery(//
            FeatureSource<?, ?> source, //
            FeatureType schema, //
            List<LiteFeatureTypeStyle> styleList, //
            ReferencedEnvelope mapArea,//
            CoordinateReferenceSystem mapCRS, //
            CoordinateReferenceSystem featCrs,//
            Rectangle screenSize, //
            GeometryDescriptor geometryAttribute,//
            AffineTransform worldToScreenTransform//
    ) throws IllegalFilterException, IOException, FactoryException {

        Query query = new Query(Query.ALL);

        String geomName = geometryAttribute.getLocalName();
        Filter filter = FF.bbox(FF.property(geomName), mapArea);

        LiteFeatureTypeStyle[] styles = styleList
                .toArray(new LiteFeatureTypeStyle[styleList.size()]);

        // ReferencedEnvelope envelope = new ReferencedEnvelope(mapArea, mapCRS);
        // see what attributes we really need by exploring the styles
        // for testing purposes we have a null case -->
        try {
            // now build the query using only the attributes and the
            // bounding box needed
            query = new Query(schema.getName().getLocalPart());
            query.setFilter(filter);
            // query.setProperties(attributes);
            processRuleForQuery(styles, query);
        } catch (Exception e) {
            throw Throwables.propagate(e);
            // final Exception txException = new Exception("Error transforming bbox", e);
            // query = new Query(schema.getName().getLocalPart());
            // query.setProperties(attributes);
            // Envelope bounds = source.getBounds();
            // if (bounds != null && envelope.intersects(bounds)) {
            // filter = null;
            // filter = createBBoxFilters(schema, attributes, Collections.singletonList(envelope));
            // query.setFilter(filter);
            // } else {
            // // LOGGER.log(Level.WARNING,
            // // "Got a tranform exception while trying to de-project the current "
            // // + "envelope, falling back on full data loading (no bbox query)", e);
            // query.setFilter(Filter.INCLUDE);
            // }
            // processRuleForQuery(styles, query);

        }

        // prepare hints
        // ... basic one, we want fast and compact coordinate sequences and geometries optimized
        // for the collection of one item case (typical in shapefiles)

        // LiteCoordinateSequenceFactory csFactory = new LiteCoordinateSequenceFactory();
        // GeometryFactory gFactory = new SimpleGeometryFactory(csFactory);
        // Hints hints = new Hints(Hints.JTS_COORDINATE_SEQUENCE_FACTORY, csFactory);
        Hints hints = new Hints();
        // hints.put(Hints.JTS_GEOMETRY_FACTORY, gFactory);
        // hints.put(Hints.FEATURE_2D, Boolean.TRUE);

        // update the screenmaps

        CoordinateReferenceSystem crs = featCrs;// getNativeCRS(schema, attributes);
        if (crs != null) {
            Set<RenderingHints.Key> fsHints = source.getSupportedHints();

            SingleCRS crs2D = crs == null ? null : CRS.getHorizontalCRS(crs);
            MathTransform mt = buildFullTransform(crs2D, mapCRS, worldToScreenTransform);
            // MathTransform mt = ProjectiveTransform.create(worldToScreenTransform);
            double[] spans;
            try {
                spans = Decimator.computeGeneralizationDistances(mt.inverse(), screenSize,
                        generalizationDistance);
            } catch (TransformException e) {
                throw Throwables.propagate(e);
            }
            double distance = spans[0] < spans[1] ? spans[0] : spans[1];
            for (LiteFeatureTypeStyle fts : styles) {
                if (fts.screenMap != null) {
                    fts.screenMap.setTransform(mt);
                    fts.screenMap.setSpans(spans[0], spans[1]);
                    if (fsHints.contains(Hints.SCREENMAP)) {
                        // replace the renderer screenmap with the hint, and avoid doing
                        // the work twice
                        hints.put(Hints.SCREENMAP, fts.screenMap);
                        fts.screenMap = null;
                    }
                }
            }
        }

        // if (renderingTransformation) {
        // // the RT might need valid geometries, we can at most apply a topology
        // // preserving generalization
        // if (fsHints.contains(Hints.GEOMETRY_GENERALIZATION)) {
        // hints.put(Hints.GEOMETRY_GENERALIZATION, distance);
        // inMemoryGeneralization = false;
        // }
        // } else {
        // // ... if possible we let the datastore do the generalization
        // if (fsHints.contains(Hints.GEOMETRY_SIMPLIFICATION)) {
        // // good, we don't need to perform in memory generalization, the datastore
        // // does it all for us
        // hints.put(Hints.GEOMETRY_SIMPLIFICATION, distance);
        // inMemoryGeneralization = false;
        // } else if (fsHints.contains(Hints.GEOMETRY_DISTANCE)) {
        // // in this case the datastore can get us close, but we can still
        // // perform some in memory generalization
        // hints.put(Hints.GEOMETRY_DISTANCE, distance);
        // }
        // }
        // }
        // } catch (Exception e) {
        // // LOGGER.log(Level.INFO, "Error computing the generalization hints", e);
        // }

        if (query.getHints() == null) {
            query.setHints(hints);
        } else {
            query.getHints().putAll(hints);
        }

        // simplify the filter
        SimplifyingFilterVisitor simplifier = new SimplifyingFilterVisitor();
        simplifier.setFeatureType(source.getSchema());
        Filter simplifiedFilter = (Filter) query.getFilter().accept(simplifier, null);
        query.setFilter(simplifiedFilter);
        return query;
    }

    /**
     * Builds a full transform going from the source CRS to the destination CRS and from there to
     * the screen.
     * <p>
     * Although we ask for 2D content (via {@link Hints#FEATURE_2D} ) not all DataStore
     * implementations are capable. In this event we will manually stage the information into
     * {@link DefaultGeographicCRS#WGS84}) and before using this transform.
     */
    private static MathTransform buildFullTransform(CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem destCRS, AffineTransform worldToScreenTransform)
            throws FactoryException {
        MathTransform mt = buildTransform(sourceCRS, destCRS);

        // concatenate from world to screen
        if (mt != null && !mt.isIdentity()) {
            mt = ConcatenatedTransform.create(mt,
                    ProjectiveTransform.create(worldToScreenTransform));
        } else {
            mt = ProjectiveTransform.create(worldToScreenTransform);
        }

        return mt;
    }

    /**
     * Builds the transform from sourceCRS to destCRS/
     * <p>
     * Although we ask for 2D content (via {@link Hints#FEATURE_2D} ) not all DataStore
     * implementations are capable. With that in mind if the provided soruceCRS is not 2D we are
     * going to manually post-process the Geomtries into {@link DefaultGeographicCRS#WGS84} - and
     * the {@link MathTransform2D} returned here will transition from WGS84 to the requested
     * destCRS.
     * 
     * @param sourceCRS
     * @param destCRS
     * @return the transform from {@code sourceCRS} to {@code destCRS}, will be an identity
     *         transform if the the two crs are equal
     * @throws FactoryException If no transform is available to the destCRS
     */
    public static MathTransform buildTransform(CoordinateReferenceSystem sourceCRS,
            CoordinateReferenceSystem destCRS) throws FactoryException {
        Preconditions.checkNotNull(sourceCRS, "sourceCRS");
        Preconditions.checkNotNull(destCRS, "destCRS");

        MathTransform transform = null;
        if (sourceCRS.getCoordinateSystem().getDimension() >= 3) {
            // We are going to transform over to DefaultGeographic.WGS84 on the fly
            // so we will set up our math transform to take it from there
            MathTransform toWgs84_3d = CRS.findMathTransform(sourceCRS,
                    DefaultGeographicCRS.WGS84_3D);
            MathTransform toWgs84_2d = CRS.findMathTransform(DefaultGeographicCRS.WGS84_3D,
                    DefaultGeographicCRS.WGS84);
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
     * DJB: trying to be smarter. If there are no "elseRules" and no rules w/o a filter, then it
     * makes sense to send them off to the Datastore We limit the number of Filters sent off to the
     * datastore, just because it could get a bit rediculous. In general, for a database, if you can
     * limit 10% of the rows being returned you're probably doing quite well. The main problem is
     * when your filters really mean you're secretly asking for all the data in which case sending
     * the filters to the Datastore actually costs you. But, databases are *much* faster at
     * processing the Filters than JAVA is and can use statistical analysis to do it.
     * 
     * @param styles
     * @param query
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
                    if (r.getFilter() == null)
                        return; // uh-oh has no filter (want all rows)
                    filtersToDS.add(r.getFilter());
                }
            }

            // if too many bail out
            if (filtersToDS.size() > maxFilters)
                return;

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
                LOGGER.log(Level.SEVERE,
                        "Could not send rules to datastore due to: " + e.getMessage(), e);
            }
        }
    }

    public static ArrayList<LiteFeatureTypeStyle> createLiteFeatureTypeStyles(
            List<FeatureTypeStyle> featureStyles, FeatureType ftype, double scaleDenominator,
            Rectangle screenSize) throws IOException {

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
                if ((ruleList.isEmpty()) && (elseRuleList.isEmpty()))
                    continue;

                // we can optimize this one and draw directly on the graphics, assuming
                // there is no composition
                Graphics2D graphics = null;
                lfts = new LiteFeatureTypeStyle(graphics, ruleList, elseRuleList,
                        fts.getTransformation());

                if (FeatureTypeStyle.VALUE_EVALUATION_MODE_FIRST.equals(fts.getOptions().get(
                        FeatureTypeStyle.KEY_EVALUATION_MODE))) {
                    lfts.matchFirst = true;
                }
                final boolean screenMapEnabled = true;// we don't care about opacity
                if (screenMapEnabled) {
                    int renderingBuffer = 0;// getRenderingBuffer();
                    lfts.screenMap = new ScreenMap(screenSize.x - renderingBuffer, screenSize.y
                            - renderingBuffer, screenSize.width + renderingBuffer * 2,
                            screenSize.height + renderingBuffer * 2);
                }

                result.add(lfts);
            }
        }

        return result;
    }

    private static List<Rule>[] splitRules(final FeatureTypeStyle fts, final double scaleDenominator) {

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
        List<Rule>[] ret = new List[] { ruleList, elseRuleList };
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
                || ((ftype.getName().getLocalPart() != null) && (ftype.getName().getLocalPart()
                        .equalsIgnoreCase(fts.getFeatureTypeName()) || FeatureTypes.isDecendedFrom(
                        ftype, null, fts.getFeatureTypeName())));
    }

}
