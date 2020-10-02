/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geootols.dggs.clickhouse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.DGGSSetFunction;
import org.geotools.dggs.Zone;
import org.geotools.dggs.gstore.DGGSResolutionCalculator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.spatial.BBOX;

/**
 * Duplicates the filter, turning simple spatial filters and {@link org.geotools.dggs.DGGSFunction}
 * instances into filters against {@link ClickHouseDGGSDataStore#ZONE_ID}, for a given resolution.
 * Spatial filters must already be expressed in {@link DefaultGeographicCRS#WGS84}
 */
public class DGGSFilterTransformer extends DuplicatingFilterVisitor {

    public static final int RESOLUTION_NOT_SPECIFIED = -1;

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    private final DGGSResolutionCalculator resolutionCalculator;

    public static Filter adapt(
            Filter filter,
            DGGSInstance dggs,
            DGGSResolutionCalculator resolutions,
            int resolution) {
        DGGSFilterTransformer adapter = new DGGSFilterTransformer(dggs, resolutions, resolution);
        return (Filter) filter.accept(adapter, null);
    }

    DGGSInstance dggs;
    int resolution;

    public DGGSFilterTransformer(
            DGGSInstance dggs, DGGSResolutionCalculator resolutions, int resolution) {
        this.dggs = dggs;
        this.resolutionCalculator = resolutions;
        this.resolution = resolution;
    }

    // TODO: turn DGGSFunction too

    @Override
    public Object visit(PropertyIsEqualTo filter, Object extraData) {
        if (filter.getExpression1() instanceof DGGSSetFunction
                && filter.getExpression2() instanceof Literal
                && Boolean.TRUE.equals(filter.getExpression2().evaluate(null, Boolean.class))) {
            DGGSSetFunction function = (DGGSSetFunction) filter.getExpression1();
            if (function.isStable()) {
                Iterator<Zone> zones = function.getMatchedZones();
                return getFilterFrom(zones);
            }
        }

        return super.visit(filter, extraData);
    }

    @Override
    public Object visit(BBOX filter, Object extraData) {
        ReferencedEnvelope envelope = ReferencedEnvelope.reference(filter.getBounds());
        if (resolution == RESOLUTION_NOT_SPECIFIED) {
            // TODO: ask the DGGS instance to return a compact list, with parent zones to be
            // used as a Like filter
            //            NumberRange<Integer> resolutions =
            // resolutionCalculator.getValidResolutions();
            //            List<Filter> filters =
            //                    IntStream.range(resolutions.getMinValue(),
            // resolutions.getMaxValue() + 1)
            //                            .mapToObj(r ->
            // getFilterFrom(dggs.zonesFromEnvelope(envelope, r)))
            //                            .collect(Collectors.toList());
            //            return FF.or(filters);
            return super.visit(filter, extraData);
        } else {
            return getFilterFrom(dggs.zonesFromEnvelope(envelope, resolution, true), resolution);
        }
    }

    private PropertyIsEqualTo getFilterFrom(Iterator<Zone> zones) {
        List<Expression> expressions = new ArrayList<>();
        expressions.add(FF.property(ClickHouseDGGSDataStore.ZONE_ID));
        while (zones.hasNext()) {
            expressions.add(FF.literal(zones.next().getId()));
        }
        Function inFunction =
                FF.function("in", expressions.toArray(new Expression[expressions.size()]));
        return FF.equal(inFunction, FF.literal(Boolean.TRUE), false);
    }

    private Filter getFilterFrom(Iterator<Zone> zones, int resolution) {
        List<Filter> filters = new ArrayList<>();
        List<Expression> inExpressions = new ArrayList<>();
        inExpressions.add(FF.property(ClickHouseDGGSDataStore.ZONE_ID));
        while (zones.hasNext()) {
            Zone zone = zones.next();
            // exact match
            if (zone.getResolution() == resolution) {
                inExpressions.add(FF.literal(zone.getId()));
            } else { // parent match
                Filter childFilter = dggs.getChildFilter(FF, zone.getId(), resolution, false);
                filters.add(childFilter);
            }
        }

        if (filters.size() > 0) {
            Or or = FF.or(new ArrayList<>(filters));
            filters.clear();
            filters.add(or);
        }
        if (inExpressions.size() > 1) {
            Function inFunction =
                    FF.function("in", inExpressions.toArray(new Expression[inExpressions.size()]));
            Filter directChildMatches = FF.equal(inFunction, FF.literal(Boolean.TRUE), false);
            filters.add(directChildMatches);
        }

        if (filters.size() > 1) {
            return FF.or(filters);
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return Filter.EXCLUDE;
        }
    }
}
