/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.Link;
import org.geoserver.api.NCNameResourceCodec;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.StyleVisitor;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.UserLayer;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.MultiValuedFilter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.coordinate.Polygon;
import org.opengis.style.Symbolizer;
import org.springframework.http.MediaType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StyleLayer implements Serializable {

    static final Logger LOGGER = Logging.getLogger(StyleLayer.class);

    enum LayerType {
        point,
        line,
        polygon,
        geometry,
        raster
    };

    String id;
    LayerType type;
    Link sampleData;
    List<StyleAttribute> attributes;

    public StyleLayer(StyleInfo si, StyledLayer source, Catalog catalog) {
        this.id = Optional.ofNullable(source.getName()).orElse("layer");

        // look for layer associations
        try (CloseableIterator<LayerInfo> layers =
                catalog.list(
                        LayerInfo.class,
                        Predicates.or(
                                Predicates.equal("defaultStyle", si),
                                Predicates.equal(
                                        "styles", si, MultiValuedFilter.MatchAction.ANY)))) {
            while (layers.hasNext()) {
                LayerInfo layer = layers.next();
                try {
                    ResourceInfo resource = layer.getResource();
                    String resourceId = NCNameResourceCodec.encode(resource);
                    if (resource instanceof FeatureTypeInfo) {
                        FeatureType featureType = ((FeatureTypeInfo) resource).getFeatureType();
                        this.type = getLayerType(featureType);
                        // link to Features API
                        String href =
                                ResponseUtils.buildURL(
                                        APIRequestInfo.get().getBaseURL(),
                                        "ogc/features/collections/" + resourceId,
                                        null,
                                        URLMangler.URLType.SERVICE);
                        this.sampleData =
                                new Link(href, "data", MediaType.APPLICATION_JSON_VALUE, null);

                        this.attributes = getAttributes(source, featureType);
                    } else if (resource instanceof CoverageInfo) {
                        type = LayerType.raster;
                        // link to an hyphotetical Coverages API
                        String href =
                                ResponseUtils.buildURL(
                                        APIRequestInfo.get().getBaseURL(),
                                        "ogc/coverages/collections/"
                                                + resourceId
                                                + "/coverages/"
                                                + resourceId,
                                        null,
                                        URLMangler.URLType.SERVICE);
                        this.sampleData =
                                new Link(
                                        href,
                                        "data",
                                        MediaType.APPLICATION_JSON_VALUE,
                                        "Coveages API not yet implemented, this is just a sample URL sorry!");
                    }

                    // found at least one?
                    if (sampleData != null) {
                        break;
                    }
                } catch (IOException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Could not grab type information from layer "
                                    + layer.prefixedName()
                                    + ", skipping and moving on");
                }
            }
        }

        // did we find a sample? if not, try to guess from the symbolizer types (it's a heuristic
        // that won't work for all styles, but hopefully, for most. One case where it'll break is
        // "generic")
        if (type == null) {
            this.type = guessLayerType(source);
            this.attributes = getAttributes(source, null);
        }
    }

    private LayerType guessLayerType(StyledLayer source) {
        SymbolizerTypeVisitor typeVisitor = new SymbolizerTypeVisitor();
        acceptvisitor(source, typeVisitor);
        Set<Class<? extends Symbolizer>> types = typeVisitor.getSymbolizerTypes();
        if (containsSymbolizer(types, RasterSymbolizer.class)) {
            return LayerType.raster;
        } else if (containsSymbolizer(types, PolygonSymbolizer.class)) {
            // polygons can also contain line and point for centroid and border decorations
            return LayerType.polygon;
        } else if (containsSymbolizer(types, LineSymbolizer.class)) {
            //  lines can also contain point for end-of-line decorations
            return LayerType.line;
        } else if (containsSymbolizer(types, PointSymbolizer.class)) {
            return LayerType.point;
        } else {
            return LayerType.geometry;
        }
    }

    private boolean containsSymbolizer(
            Set<Class<? extends Symbolizer>> types, Class<? extends Symbolizer> target) {
        return types.stream().anyMatch(symbolizerClass -> target.isAssignableFrom(symbolizerClass));
    }

    private LayerType getLayerType(FeatureType featureType) {
        GeometryDescriptor gd = featureType.getGeometryDescriptor();
        Class<?> binding = gd.getType().getBinding();
        if (Point.class.isAssignableFrom(binding) || MultiPoint.class.isAssignableFrom(binding)) {
            return LayerType.point;
        } else if (LineString.class.isAssignableFrom(binding)
                || MultiLineString.class.isAssignableFrom(binding)) {
            return LayerType.line;
        } else if (Polygon.class.isAssignableFrom(binding)
                || MultiPolygon.class.isAssignableFrom(binding)) {
            return LayerType.polygon;
        } else {
            return LayerType.geometry;
        }
    }

    public void acceptvisitor(StyledLayer source, StyleVisitor visitor) {
        if (source instanceof NamedLayer) {
            ((NamedLayer) source).accept(visitor);
        } else {
            ((UserLayer) source).accept(visitor);
        }
    }

    private List<StyleAttribute> getAttributes(StyledLayer source, FeatureType featureType) {
        StyleAttributeExtractor extractor =
                featureType instanceof SimpleFeatureType
                        ? new StyleAttributeExtractor((SimpleFeatureType) featureType)
                        : new StyleAttributeExtractor();
        acceptvisitor(source, extractor);
        Map<PropertyName, Class> propertyTypes = extractor.getPropertyTypes();

        return propertyTypes
                .entrySet()
                .stream()
                .map(
                        e -> {
                            StyleAttribute sa = new StyleAttribute();
                            sa.setId(e.getKey().getPropertyName());
                            sa.setType(getAttributeType(e.getValue()));
                            return sa;
                        })
                .collect(Collectors.toList());
    }

    private StyleAttribute.AttributeType getAttributeType(Class<?> binding) {
        if (Number.class.isAssignableFrom(binding)) {
            if (Float.class.isAssignableFrom(binding)
                    || Double.class.isAssignableFrom(binding)
                    || BigDecimal.class.isAssignableFrom(binding)) {
                return StyleAttribute.AttributeType.number;
            } else {
                return StyleAttribute.AttributeType.integer;
            }
        } else if (Date.class.isAssignableFrom(binding)) {
            return StyleAttribute.AttributeType.date;
        } else if (java.util.Date.class.isAssignableFrom(binding)) {
            return StyleAttribute.AttributeType.dateTime;
        } else if (Boolean.class.isAssignableFrom(binding)) {
            return StyleAttribute.AttributeType.bool;
        } else {
            // fallback
            return StyleAttribute.AttributeType.string;
        }
    }

    public String getId() {
        return id;
    }

    public LayerType getType() {
        return type;
    }

    public Link getSampleData() {
        return sampleData;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<StyleAttribute> getAttributes() {
        return attributes;
    }
}
