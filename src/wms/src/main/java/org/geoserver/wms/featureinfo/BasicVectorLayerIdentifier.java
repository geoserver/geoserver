/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.Filters;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.MetaBufferEstimator;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Or;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

/**
 * An identifier for vector layers that will take into account the filters, viewparams, styles and
 * build a bbox (plus extra filters) query against the database
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class BasicVectorLayerIdentifier implements LayerIdentifier {

    private static final double TOLERANCE = 1e-6;

    static final int MIN_BUFFER_SIZE = Integer.getInteger(
            "org.geoserver.wms.featureinfo.minBuffer", 5);

    private WMS wms;

    public BasicVectorLayerIdentifier(final WMS wms) {
        this.wms = wms;
    }

    public boolean canHandle(MapLayerInfo layer) {
        int type = layer.getType();
        return type == MapLayerInfo.TYPE_VECTOR || type == MapLayerInfo.TYPE_REMOTE_VECTOR;
    }

    public List<FeatureCollection> identify(FeatureInfoRequestParameters params, int maxFeatures) throws Exception {
        final MapLayerInfo layer = params.getLayer();
        final Filter filter = params.getFilter();
        final Style style = params.getStyle();
        // ok, internally rendered layer then, we check the style to see what's active
        final List<Rule> rules = getActiveRules(style, params.getScaleDenominator());
        if (rules.size() == 0) {
            return null;
        }
        final Map<String, String> viewParams = params.getViewParams();
        CoordinateReferenceSystem dataCRS = layer.getCoordinateReferenceSystem();

        // compute the request radius
        double radius;
        int buffer = params.getBuffer();
        if (buffer <= 0) {
            Integer layerBuffer = null;
            final LayerInfo layerInfo = layer.getLayerInfo();
            if (layerInfo != null) { // it is a local layer
                layerBuffer = layerInfo.getMetadata().get(LayerInfo.BUFFER, Integer.class);
            }
            if (layerBuffer != null && layerBuffer > 0) {
                radius = layerBuffer / 2.0;
            } else {
                // estimate the radius given the currently active rules
                MetaBufferEstimator estimator = new MetaBufferEstimator();
                for (Rule rule : rules) {
                    rule.accept(estimator);
                }

                int estimatedRadius = estimator.getBuffer() / 2;
                if (estimatedRadius < MIN_BUFFER_SIZE || !estimator.isEstimateAccurate()) {
                    radius = MIN_BUFFER_SIZE;
                } else {
                    radius = estimatedRadius;
                }
            }
        } else {
            radius = buffer;
        }

        // make sure we don't go overboard, the admin might have set a maximum
        int maxRadius = wms.getMaxBuffer();
        if (maxRadius > 0 && radius > maxRadius)
            radius = maxRadius;

        ReferencedEnvelope queryEnvelope = getEnvelopeFilter(params, radius);
        CoordinateReferenceSystem requestedCRS = params.getRequestedCRS();
        if ((requestedCRS != null) && !CRS.equalsIgnoreMetadata(dataCRS, requestedCRS)) {
            if (dataCRS.getCoordinateSystem().getDimension() == 3
                    && requestedCRS.getCoordinateSystem().getDimension() == 2) {
                queryEnvelope = JTS.transformTo3D(queryEnvelope, dataCRS, true, 10);
            } else {
                queryEnvelope = queryEnvelope.transform(dataCRS, true);
            }
        }

        final FeatureSource<? extends FeatureType, ? extends Feature> featureSource;
        featureSource = layer.getFeatureSource(false);
        FeatureType schema = featureSource.getSchema();

        Filter getFInfoFilter = null;
        FilterFactory2 ff = params.getFilterFactory();
        try {
            GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
            String localName = geometryDescriptor.getLocalName();
            Polygon queryPolygon = JTS.toGeometry(queryEnvelope);
            getFInfoFilter = ff.intersects(ff.property(localName), ff.literal(queryPolygon));
        } catch (IllegalFilterException e) {
            e.printStackTrace();
            throw new ServiceException("Internal error : " + e.getMessage(), e);
        }

        // include the eventual layer definition filter
        if (filter != null) {
            getFInfoFilter = ff.and(getFInfoFilter, filter);
        }

        // see if we can include the rule filters as well, if too many we'll do them in
        // memory
        Filter postFilter = Filter.INCLUDE;
        Filter rulesFilters = buildRulesFilter(ff, rules);
        if (!(featureSource.getSchema() instanceof SimpleFeatureType)
                || !(rulesFilters instanceof Or)
                || (rulesFilters instanceof Or && ((Or) rulesFilters).getChildren().size() <= 20)) {
            getFInfoFilter = ff.and(getFInfoFilter, rulesFilters);
        } else {
            postFilter = rulesFilters;
        }

        // handle time/elevation
        Filter timeElevationFilter = wms.getTimeElevationToFilter(params.getTimes(),
                params.getElevations(), layer.getFeature());
        getFInfoFilter = Filters.and(ff, getFInfoFilter, timeElevationFilter);

        // simplify the filter
        SimplifyingFilterVisitor simplifier = new SimplifyingFilterVisitor();
        getFInfoFilter = (Filter) getFInfoFilter.accept(simplifier, null);

        // build the query
        String typeName = schema.getName().getLocalPart();
        Query q = new Query(typeName, null, getFInfoFilter, maxFeatures, params.getPropertyNames(),
                null);

        // handle sql view params

        if (viewParams != null && viewParams.size() > 0) {
            q.setHints(new Hints(Hints.VIRTUAL_TABLE_PARAMETERS, viewParams));
        }

        FeatureCollection match;
        match = featureSource.getFeatures(q);

        // if we could not include the rules filter into the query, post process in
        // memory
        if (!Filter.INCLUDE.equals(postFilter)) {
            match = new FilteringFeatureCollection(match, postFilter);
        }

        return Collections.singletonList(match);

    }

    private Filter buildRulesFilter(org.opengis.filter.FilterFactory ff, List<Rule> rules) {
        // build up a or of all the rule filters
        List<Filter> filters = new ArrayList<Filter>();
        for (Rule rule : rules) {
            if (rule.getFilter() == null || rule.isElseFilter())
                return Filter.INCLUDE;
            filters.add(rule.getFilter());
        }
        // not or and and simplify (if there is any include/exclude we'll get
        // a very simple result ;-)
        Filter or = ff.or(filters);
        SimplifyingFilterVisitor simplifier = new SimplifyingFilterVisitor();
        return (Filter) or.accept(simplifier, null);
    }

    /**
     * Selects the rules active at this zoom level
     * 
     * @param style
     * @param scaleDenominator
     * @return
     */
    private List<Rule> getActiveRules(Style style, double scaleDenominator) {
        List<Rule> result = new ArrayList<Rule>();

        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            for (Rule r : fts.rules()) {
                if ((r.getMinScaleDenominator() - TOLERANCE <= scaleDenominator)
                        && (r.getMaxScaleDenominator() + TOLERANCE > scaleDenominator)) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    private ReferencedEnvelope getEnvelopeFilter(FeatureInfoRequestParameters params, double radius) {
        final int x = params.getX();
        final int y = params.getY();
        final ReferencedEnvelope bbox = params.getRequestedBounds();
        final int width = params.getWidth();
        final int height = params.getHeight();
        Coordinate upperLeft = WMS.pixelToWorld(x - radius, y - radius, bbox, width, height);
        Coordinate lowerRight = WMS.pixelToWorld(x + radius, y + radius, bbox, width, height);

        return new ReferencedEnvelope(upperLeft.x, lowerRight.x, lowerRight.y, upperLeft.y,
                bbox.getCoordinateReferenceSystem());
    }

}
