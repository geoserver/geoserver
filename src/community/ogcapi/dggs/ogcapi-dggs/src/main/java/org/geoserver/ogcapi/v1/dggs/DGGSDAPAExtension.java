/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import static java.util.Collections.singletonMap;
import static org.geoserver.ogcapi.APIException.INVALID_PARAMETER_VALUE;
import static org.geotools.dggs.gstore.DGGSStore.VP_RESOLUTION;
import static org.geotools.util.factory.Hints.VIRTUAL_TABLE_PARAMETERS;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.DateTimeConverter;
import org.geoserver.ogcapi.DateTimeList;
import org.geoserver.ogcapi.DefaultContentType;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.ogcapi.v1.features.FeaturesResponse;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.GroupedMatrixAggregate;
import org.geotools.dggs.MatrixAggregate;
import org.geotools.dggs.Zone;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.visitor.Aggregate;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.FactoryException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Implementation of OGC API - DGGS DAPA extensions */
@APIService(
        service = "DGGS",
        version = "1.0.1",
        landingPage = "ogc/dggs/v1",
        serviceClass = DGGSInfo.class,
        core = false)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/dggs/v1")
public class DGGSDAPAExtension {

    private static final String GEOMETRY = "geometry";
    private static final String PHENOMENON_TIME = "phenomenonTime";
    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    private final DGGSService service;

    public DGGSDAPAExtension(DGGSService service) {
        this.service = service;
    }

    @GetMapping(path = "collections/{collectionId}/processes", name = "dapaDescription")
    @ResponseBody
    @HTMLResponseBody(templateName = "dapa.ftl", fileName = "dapa.html")
    public CollectionDAPA dapa(@PathVariable(name = "collectionId") String collectionId)
            throws IOException {
        FeatureTypeInfo info = getFeatureType(collectionId);
        return new CollectionDAPA(collectionId, info);
    }

    public FeatureTypeInfo getFeatureType(@PathVariable(name = "collectionId") String collectionId)
            throws IOException {
        FeatureTypeInfo ft = service.getFeatureType(collectionId);
        DimensionInfo time = ft.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null)
            throw new APIException(
                    APIException.NO_APPLICABLE_CODE,
                    "This colleection does not support DAPA",
                    HttpStatus.NOT_FOUND);
        return ft;
    }

    @GetMapping(path = "collections/{collectionId}/variables", name = "dapaVariables")
    @ResponseBody
    @HTMLResponseBody(templateName = "dapaVariables.ftl", fileName = "dapa.html")
    public DAPAVariables variableNames(@PathVariable(name = "collectionId") String collectionId)
            throws IOException {
        FeatureTypeInfo info = getFeatureType(collectionId);
        // TODO: eventually make it work for complex features
        DAPAVariables result = new DAPAVariables(collectionId, info);
        return result;
    }

    // this is exactly the same as "zones"
    @GetMapping(
            path = "collections/{collectionId}/processes/area:retrieve",
            name = "dapaAreaRetrieve")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse area(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "resolution", required = false, defaultValue = "0")
                    Integer resolution,
            @RequestParam(name = "datetime", required = false) DateTimeList datetime,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "geom", required = false) String wkt,
            @RequestParam(name = "zones", required = false) String zones,
            @RequestParam(name = "variables", required = false) String variableNames,
            @RequestParam(
                            name = "f",
                            required = false,
                            defaultValue = OGCAPIMediaTypes.GEOJSON_VALUE)
                    String format)
            throws Exception {
        return service.zones(
                collectionId,
                startIndex,
                limit,
                resolution,
                datetime,
                bbox,
                wkt,
                zones,
                variableNames,
                format);
    }

    @GetMapping(
            path = "collections/{collectionId}/processes/area:aggregate-space",
            name = "dapaAreaSpaceAggregate")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public SimpleFeatureCollection areaSpaceAggregation(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "geom", required = false) String wkt,
            @RequestParam(name = "zones", required = false) String zones,
            @RequestParam(name = "datetime", required = false) String dateTimeSpec,
            @RequestParam(name = "functions", required = false, defaultValue = "max,min,count")
                    Aggregate[] functions,
            @RequestParam(name = "variables", required = false) String variableNames,
            @RequestParam(name = "resolution", required = false, defaultValue = "0") int resolution)
            throws IOException, FactoryException, ParseException {
        FeatureTypeInfo ft = getFeatureType(collectionId);

        // parse inputs
        DGGSGeometryFilterParser geometryParser =
                new DGGSGeometryFilterParser(FF, service.getDGGSInstance(collectionId));
        geometryParser.setBBOX(bbox);
        geometryParser.setGeometry(wkt);
        geometryParser.setZoneIds(zones, resolution);

        List<Filter> filters = new ArrayList<>();
        if (geometryParser.getFilter() != null) {
            filters.add(geometryParser.getFilter());
        }
        if (dateTimeSpec != null) {
            DateTimeList datetime = new DateTimeConverter().convert(dateTimeSpec);
            filters.add(service.buildDateTimeFilter(ft, datetime));
        }
        Filter filter = service.FF.and(filters);
        String[] variables = parseVariables(collectionId, variableNames);

        // setup and run the query with aggregation
        Query q = new Query(ft.getName(), filter);
        q.getHints().put(VIRTUAL_TABLE_PARAMETERS, singletonMap(VP_RESOLUTION, resolution));
        SimpleFeatureSource fs = (SimpleFeatureSource) ft.getFeatureSource(null, null);
        List<Expression> expressions =
                Arrays.stream(variables)
                        .map(v -> (Expression) FF.property(v))
                        .collect(Collectors.toList());
        // run a full aggregate and build the feature
        List<Expression> timeGroupExpressions = getTimeGroup(ft);
        GroupedMatrixAggregate aggregate =
                new GroupedMatrixAggregate(
                        expressions, Arrays.asList(functions), timeGroupExpressions);
        fs.getFeatures(q).accepts(aggregate, null);

        // build the target feature type and feature
        SimpleFeatureType targetType =
                getAreaSpaceAggregateTargetType(
                        (SimpleFeatureType) ft.getFeatureType(),
                        variables,
                        functions,
                        timeGroupExpressions);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetType);
        GroupedMatrixAggregate.IterableResult result =
                (GroupedMatrixAggregate.IterableResult) aggregate.getResult();
        return new GroupMatrixFeatureCollection(
                targetType,
                result,
                gr -> {
                    fb.add(geometryParser.getGeometry());
                    gr.getKey().forEach(v -> fb.add(v));
                    gr.getValues().forEach(v -> fb.add(v));
                    return fb.buildFeature(
                            "area_space_time_"
                                    + gr.getKey().stream()
                                            .map(k -> k.toString())
                                            .collect(Collectors.joining("_"))
                                            .replace(" ", "_"));
                });
    }

    private List<Expression> getTimeGroup(FeatureTypeInfo ft) {
        DimensionInfo time = ft.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        List<Expression> result = new ArrayList<>();
        result.add(FF.property(time.getAttribute()));
        if (time.getEndAttribute() != null) {
            result.add(FF.property(time.getEndAttribute()));
        }
        return result;
    }

    @GetMapping(
            path = "collections/{collectionId}/processes/area:aggregate-time",
            name = "dapaAreaTimeAggregate")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public SimpleFeatureCollection areaTimeAggregation(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "geom", required = false) String wkt,
            @RequestParam(name = "zones", required = false) String zones,
            @RequestParam(name = "datetime", required = false) String dateTimeSpec,
            @RequestParam(name = "functions", required = false, defaultValue = "max,min,count")
                    Aggregate[] functions,
            @RequestParam(name = "variables", required = false) String variableNames,
            @RequestParam(name = "resolution", required = false, defaultValue = "0") int resolution)
            throws IOException, FactoryException, ParseException {
        FeatureTypeInfo ft = getFeatureType(collectionId);

        // parse inputs
        DGGSGeometryFilterParser geometryParser =
                new DGGSGeometryFilterParser(FF, service.getDGGSInstance(collectionId));
        geometryParser.setBBOX(bbox);
        geometryParser.setGeometry(wkt);
        geometryParser.setZoneIds(zones, resolution);

        List<Filter> filters = new ArrayList<>();
        if (geometryParser.getFilter() != null) {
            filters.add(geometryParser.getFilter());
        }
        if (dateTimeSpec != null) {
            DateTimeList datetime = new DateTimeConverter().convert(dateTimeSpec);
            filters.add(service.buildDateTimeFilter(ft, datetime));
        }
        Filter filter = service.FF.and(filters);
        String[] variables = parseVariables(collectionId, variableNames);

        // setup and run the query with aggregation
        Query q = new Query(ft.getName(), filter);
        q.getHints().put(VIRTUAL_TABLE_PARAMETERS, singletonMap(VP_RESOLUTION, resolution));
        SimpleFeatureSource fs = (SimpleFeatureSource) ft.getFeatureSource(null, null);
        List<Expression> expressions =
                Arrays.stream(variables)
                        .map(v -> (Expression) FF.property(v))
                        .collect(Collectors.toList());
        // run a full aggregate and build the feature
        GroupedMatrixAggregate aggregate =
                new GroupedMatrixAggregate(
                        expressions,
                        Arrays.asList(functions),
                        Arrays.asList(FF.property(DGGSStore.ZONE_ID)));
        fs.getFeatures(q).accepts(aggregate, null);

        // build the target feature type and feature
        SimpleFeatureType targetType =
                getTimeTargetType(
                        (SimpleFeatureType) ft.getFeatureType(), variables, functions, "area");
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetType);
        GroupedMatrixAggregate.IterableResult result =
                (GroupedMatrixAggregate.IterableResult) aggregate.getResult();
        return new GroupMatrixFeatureCollection(
                targetType,
                result,
                gr -> {
                    fb.add(getFeatureGeometry(ft, geometryParser));
                    gr.getKey().forEach(v -> fb.add(v));
                    gr.getValues().forEach(v -> fb.add(v));
                    return fb.buildFeature("area_time_" + gr.getKey().get(0));
                });
    }

    public Geometry getFeatureGeometry(
            FeatureTypeInfo ft, DGGSGeometryFilterParser geometryParser) {
        Geometry g = geometryParser.getGeometry();
        if (g == null) {
            g = JTS.toGeometry(ft.getLatLonBoundingBox());
        }
        return g;
    }

    @GetMapping(
            path = "collections/{collectionId}/processes/area:aggregate-space-time",
            name = "dapaAreaSpaceTimeAggregate")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public SimpleFeatureCollection areaSpaceTimeAggregation(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "bbox", required = false) String bbox,
            @RequestParam(name = "geom", required = false) String wkt,
            @RequestParam(name = "zones", required = false) String zones,
            @RequestParam(name = "datetime", required = false) String dateTimeSpec,
            @RequestParam(name = "functions", required = false, defaultValue = "max,min,count")
                    Aggregate[] functions,
            @RequestParam(name = "variables", required = false) String variableNames,
            @RequestParam(name = "resolution", required = false, defaultValue = "0") int resolution)
            throws IOException, FactoryException, ParseException {
        // parse inputs
        DGGSGeometryFilterParser geometryParser =
                new DGGSGeometryFilterParser(FF, service.getDGGSInstance(collectionId));
        geometryParser.setBBOX(bbox);
        geometryParser.setGeometry(wkt);
        geometryParser.setZoneIds(zones, resolution);
        FeatureTypeInfo ft = getFeatureType(collectionId);
        List<Filter> filters = new ArrayList<>();
        if (geometryParser.getFilter() != null) {
            filters.add(geometryParser.getFilter());
        }
        if (dateTimeSpec != null) {
            DateTimeList datetime = new DateTimeConverter().convert(dateTimeSpec);
            filters.add(service.buildDateTimeFilter(ft, datetime));
        }
        Filter filter = service.FF.and(filters);
        String[] variables = parseVariables(collectionId, variableNames);

        // setup and run the query with aggregation
        Query q = new Query(ft.getName(), filter);
        q.getHints().put(VIRTUAL_TABLE_PARAMETERS, singletonMap(VP_RESOLUTION, resolution));
        SimpleFeatureSource fs = (SimpleFeatureSource) ft.getFeatureSource(null, null);
        List<Expression> expressions =
                Arrays.stream(variables)
                        .map(v -> (Expression) FF.property(v))
                        .collect(Collectors.toList());
        // run a full aggregate and build the feature
        MatrixAggregate aggregate = new MatrixAggregate(expressions, Arrays.asList(functions));
        fs.getFeatures(q).accepts(aggregate, null);

        // build the target feature type and feature
        SimpleFeatureType targetType =
                getAreaSpaceTimeTargetType(
                        (SimpleFeatureType) ft.getFeatureType(), variables, functions);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetType);
        fb.set(GEOMETRY, getFeatureGeometry(ft, geometryParser));
        fb.set(PHENOMENON_TIME, dateTimeSpec != null ? dateTimeSpec : getFullTimeRangeSpec(ft));
        Iterator iterator = aggregate.getResult().toList().iterator();
        for (String variable : variables) {
            for (Aggregate function : functions) {
                fb.set(getAttributeName(variable, function), iterator.next());
            }
        }
        SimpleFeature feature = fb.buildFeature("space-time-aggregate");

        // wrap as a collection and return
        return DataUtilities.collection(feature);
    }

    private String getFullTimeRangeSpec(FeatureTypeInfo ft) throws IOException {
        DimensionInfo time = ft.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null) {
            return null;
        }
        MinVisitor minVisitor = new MinVisitor(time.getAttribute());
        SimpleFeatureCollection features =
                (SimpleFeatureCollection) ft.getFeatureSource(null, null).getFeatures();
        features.accepts(minVisitor, null);
        MaxVisitor maxVisitor =
                new MaxVisitor(
                        Optional.ofNullable(time.getEndAttribute()).orElse(time.getAttribute()));
        features.accepts(maxVisitor, null);
        Date min = (Date) minVisitor.getResult().getValue();
        Date max = (Date) maxVisitor.getResult().getValue();
        ISO8601Formatter formatter = new ISO8601Formatter();
        return formatter.format(min) + "/" + formatter.format(max);
    }

    // this is exactly the same as "zone"
    @GetMapping(
            path = "collections/{collectionId}/processes/position:retrieve",
            name = "dapaPositionRetrieve")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public FeaturesResponse position(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "resolution", required = false, defaultValue = "0")
                    Integer resolution,
            @RequestParam(name = "datetime", required = false, defaultValue = "0/3000")
                    DateTimeList datetime,
            @RequestParam(name = "geom", required = false) String wkt,
            @RequestParam(name = "zone_id", required = false) String zoneId,
            @RequestParam(name = "variables", required = false) String variableNames,
            @RequestParam(
                            name = "f",
                            required = false,
                            defaultValue = OGCAPIMediaTypes.GEOJSON_VALUE)
                    String format)
            throws Exception {
        @SuppressWarnings("PMD.CloseResource") // managed by the store
        DGGSInstance dggs = service.getDGGSInstance(collectionId);
        zoneId = getPositionZoneId(zoneId, wkt, resolution, dggs);

        return service.zone(collectionId, zoneId, datetime, variableNames, format);
    }

    // this is exactly the same as "zone"
    @GetMapping(
            path = "collections/{collectionId}/processes/position:aggregate-time",
            name = "dapaPositionTimeAggregate")
    @ResponseBody
    @DefaultContentType(OGCAPIMediaTypes.GEOJSON_VALUE)
    public SimpleFeatureCollection positionTimeAggregate(
            @PathVariable(name = "collectionId") String collectionId,
            @RequestParam(name = "startIndex", required = false, defaultValue = "0")
                    BigInteger startIndex,
            @RequestParam(name = "limit", required = false) BigInteger limit,
            @RequestParam(name = "resolution", required = false, defaultValue = "0")
                    Integer resolution,
            @RequestParam(name = "datetime", required = false) String dateTimeSpec,
            @RequestParam(name = "geom", required = false) String wkt,
            @RequestParam(name = "zone_id", required = false) String zoneId,
            @RequestParam(name = "functions", required = false, defaultValue = "max,min,count")
                    Aggregate[] functions,
            @RequestParam(name = "variables", required = false) String variableNames)
            throws Exception {
        FeatureTypeInfo ft = getFeatureType(collectionId);
        @SuppressWarnings("PMD.CloseResource") // managed by the store
        DGGSInstance dggs = service.getDGGSInstance(collectionId);
        zoneId = getPositionZoneId(zoneId, wkt, resolution, dggs);

        // parse inputs
        List<Filter> filters = new ArrayList<>();
        filters.add(FF.equals(FF.property(DGGSStore.ZONE_ID), FF.literal(zoneId)));
        if (dateTimeSpec != null) {
            filters.add(
                    service.buildDateTimeFilter(ft, new DateTimeConverter().convert(dateTimeSpec)));
        }
        Filter filter = service.FF.and(filters);
        String[] variables = parseVariables(collectionId, variableNames);

        // setup and run the query with aggregation
        Query q = new Query(ft.getName(), filter);
        q.getHints().put(VIRTUAL_TABLE_PARAMETERS, singletonMap(VP_RESOLUTION, resolution));
        SimpleFeatureSource fs = (SimpleFeatureSource) ft.getFeatureSource(null, null);
        List<Expression> expressions =
                Arrays.stream(variables)
                        .map(v -> (Expression) FF.property(v))
                        .collect(Collectors.toList());
        // run a full aggregate and build the feature
        GroupedMatrixAggregate aggregate =
                new GroupedMatrixAggregate(
                        expressions,
                        Arrays.asList(functions),
                        Arrays.asList(FF.property(DGGSStore.ZONE_ID)));
        fs.getFeatures(q).accepts(aggregate, null);

        // build the target feature type and feature
        SimpleFeatureType targetType =
                getTimeTargetType(
                        (SimpleFeatureType) ft.getFeatureType(), variables, functions, "position");
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetType);
        GroupedMatrixAggregate.IterableResult result =
                (GroupedMatrixAggregate.IterableResult) aggregate.getResult();
        Point center = dggs.getZone(zoneId).getCenter();
        return new GroupMatrixFeatureCollection(
                targetType,
                result,
                gr -> {
                    fb.add(center);
                    gr.getKey().forEach(v -> fb.add(v));
                    gr.getValues().forEach(v -> fb.add(v));
                    return fb.buildFeature("position_time_" + gr.getKey().get(0));
                });
    }

    public String getPositionZoneId(
            String zoneId, String wkt, Integer resolution, DGGSInstance dggs)
            throws ParseException {
        if (zoneId == null) {
            if (wkt != null) {
                DGGSGeometryFilterParser parser =
                        new DGGSGeometryFilterParser(FF, dggs, Point.class);
                parser.setGeometry(wkt);
                Geometry geom = parser.getGeometry();
                if (!(geom instanceof Point))
                    throw new APIException(
                            INVALID_PARAMETER_VALUE,
                            "If no zoneId is provided, geom parameter must be a point in CRS84",
                            BAD_REQUEST);
                Point p = (Point) geom;
                Zone zone = dggs.getZone(p.getX(), p.getY(), resolution);
                zoneId = zone.getId();
            } else {
                zoneId = dggs.getZone(0, 0, 0).getId();
            }
        }
        return zoneId;
    }

    private SimpleFeatureType getTimeTargetType(
            SimpleFeatureType featureType,
            String[] variables,
            Aggregate[] functions,
            String aggregatorSuffix) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(featureType.getTypeName() + "_" + aggregatorSuffix + "_time_aggregate");
        builder.add(GEOMETRY, Geometry.class, 4326);
        builder.add(DGGSStore.ZONE_ID, String.class);
        for (String variable : variables) {
            for (Aggregate function : functions) {
                Class<?> binding = featureType.getDescriptor(variable).getType().getBinding();
                Class<?> targetType = function.getTargetType(binding);
                String name = getAttributeName(variable, function);
                builder.add(name, targetType);
            }
        }

        return builder.buildFeatureType();
    }

    private SimpleFeatureType getAreaSpaceAggregateTargetType(
            SimpleFeatureType featureType,
            String[] variables,
            Aggregate[] functions,
            List<Expression> timeGroupExpressions) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(featureType.getTypeName() + "_area_space_aggregate");
        builder.add(GEOMETRY, Geometry.class, 4326);
        LinkedHashSet<String> timeAttributes =
                timeGroupExpressions.stream()
                        .map(e -> e.evaluate(featureType, AttributeDescriptor.class).getLocalName())
                        .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
        timeAttributes.forEach(att -> builder.add(att, Date.class));
        for (String variable : variables) {
            for (Aggregate function : functions) {
                Class<?> binding = featureType.getDescriptor(variable).getType().getBinding();
                Class<?> targetType = function.getTargetType(binding);
                String name = getAttributeName(variable, function);
                builder.add(name, targetType);
            }
        }

        return builder.buildFeatureType();
    }

    private SimpleFeatureType getAreaSpaceTimeTargetType(
            SimpleFeatureType featureType, String[] variables, Aggregate[] functions) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(featureType.getTypeName() + "_area_space_time_aggregate");
        builder.add(GEOMETRY, Geometry.class, 4326);
        builder.add(PHENOMENON_TIME, String.class);
        for (String variable : variables) {
            for (Aggregate function : functions) {
                Class<?> binding = featureType.getDescriptor(variable).getType().getBinding();
                Class<?> targetType = function.getTargetType(binding);
                String name = getAttributeName(variable, function);
                builder.add(name, targetType);
            }
        }

        return builder.buildFeatureType();
    }

    private String getAttributeName(String variable, Aggregate function) {
        return variable + "_" + function.name().toLowerCase().replace("_", "-");
    }

    private String[] parseVariables(String collectionId, String variableNames) throws IOException {
        Set<String> allVariables =
                variableNames(collectionId).getVariables().stream()
                        .map(dv -> dv.getId())
                        .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
        if (variableNames == null) return allVariables.toArray(new String[allVariables.size()]);

        String[] variables = variableNames.split("\\s*,\\s*");
        for (String variable : variables) {
            if (!allVariables.contains(variable)) {
                throw new APIException(
                        INVALID_PARAMETER_VALUE, "Unknown variable: " + variable, BAD_REQUEST);
            }
        }

        return variables;
    }
}
