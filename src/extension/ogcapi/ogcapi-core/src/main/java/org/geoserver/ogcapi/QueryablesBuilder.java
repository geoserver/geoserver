/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.geotools.data.complex.util.ComplexFeatureConstants.FEATURE_CHAINING_LINK_NAME;

import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.feature.FeatureTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class QueryablesBuilder {

    public static final String POINT = "Point";
    public static final String MULTIPOINT = "MultiPoint";
    public static final String LINESTRING = "LineString";
    public static final String MULTILINESTRING = "MultiLineString";
    public static final String POLYGON = "Polygon";
    public static final String MULTIPOLYGON = "MultiPolygon";
    public static final String GENERIC_GEOMETRY = "Any geometry";
    public static final String DATE = "Date";
    public static final String DATE_TIME = "DateTime";
    public static final String TIME = "Time";

    Queryables queryables;

    public QueryablesBuilder(String id) {
        this.queryables = new Queryables(id);
        this.queryables.setType("object");
    }

    public QueryablesBuilder forType(FeatureTypeInfo ft) throws IOException {
        this.queryables.setCollectionId(ft.prefixedName());
        this.queryables.setTitle(Optional.ofNullable(ft.getTitle()).orElse(ft.prefixedName()));
        this.queryables.setDescription(ft.getDescription());
        return forType(ft.getFeatureType());
    }

    public QueryablesBuilder forType(FeatureType ft) {
        Map<String, Schema> properties = ft.getDescriptors().stream()
                .filter( // ignore feature chaining links, they might be duplicated
                        ad -> !ad.getName().equals(FEATURE_CHAINING_LINK_NAME))
                .collect(Collectors.toMap(
                        ad -> ad.getName().getLocalPart(),
                        ad -> getSchema(ad),
                        (u, v) -> {
                            throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        () -> new LinkedHashMap<>()));
        this.queryables.setProperties(properties);
        return this;
    }

    private Schema<?> getSchema(PropertyDescriptor descriptor) {
        Class<?> binding = descriptor.getType().getBinding();
        Schema schema = getSchema(binding);
        int fieldLength = FeatureTypes.getFieldLength(descriptor);
        if (fieldLength != FeatureTypes.ANY_LENGTH) {
            schema.setMaxLength(fieldLength);
        }
        return schema;
    }

    /** Returns the schema for a given data type */
    public static Schema<?> getSchema(Class<?> binding) {
        if (Geometry.class.isAssignableFrom(binding)) return getGeometrySchema(binding);
        else return getAlphanumericSchema(binding);
    }

    private static Schema<?> getGeometrySchema(Class<?> binding) {
        Schema schema = new Schema<>();
        String title;
        if (Point.class.isAssignableFrom(binding)) {
            title = POINT;
        } else if (MultiPoint.class.isAssignableFrom(binding)) {
            title = MULTIPOINT;
        } else if (LineString.class.isAssignableFrom(binding)) {
            title = LINESTRING;
        } else if (MultiLineString.class.isAssignableFrom(binding)) {
            title = MULTILINESTRING;
        } else if (Polygon.class.isAssignableFrom(binding)) {
            title = POLYGON;
        } else if (MultiPolygon.class.isAssignableFrom(binding)) {
            title = MULTIPOLYGON;
        } else {
            title = GENERIC_GEOMETRY;
        }

        schema.setTitle(title);
        if (title.equals(GENERIC_GEOMETRY)) {
            schema.setFormat("geometry-any");
        } else {
            schema.setFormat("geometry-" + title.toLowerCase());
        }

        return schema;
    }

    /**
     * Returns a schema for a given data type, assuming it is alphanumeric
     *
     * @param binding the data type
     * @return the schema
     */
    public static Schema<?> getAlphanumericSchema(Class<?> binding) {
        Schema<?> schema = new Schema<>();

        schema.setType(org.geoserver.ogcapi.AttributeType.fromClass(binding).getType());
        schema.setTitle(schema.getType());
        if (java.sql.Date.class.isAssignableFrom(binding)) {
            schema.setFormat("date");
            schema.setTitle(DATE);
        } else if (java.sql.Time.class.isAssignableFrom(binding)) {
            schema.setFormat("time");
            schema.setTitle(TIME);
        } else if (java.util.Date.class.isAssignableFrom(binding)) {
            schema.setFormat("date-time");
            schema.setTitle(DATE_TIME);
        } else if (UUID.class.isAssignableFrom(binding)) {
            schema.setFormat("uuid");
        } else if (URL.class.isAssignableFrom(binding)) {
            schema.setTitle("uri");
        }
        return schema;
    }

    public Queryables build() {
        return queryables;
    }
}
