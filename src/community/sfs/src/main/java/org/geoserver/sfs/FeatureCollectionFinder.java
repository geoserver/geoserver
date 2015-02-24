/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.json.JSONArray;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.restlet.Finder;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Looks up the describe object
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class FeatureCollectionFinder extends Finder {
    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2(null);

    Catalog catalog;

    public FeatureCollectionFinder(Catalog catalog) {
        this.catalog = catalog;
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String layerName = RESTUtils.getAttribute(request, "layer");
        LayerInfo layer = catalog.getLayerByName(layerName);

        // any of these conditions mean the layer is not currently
        // advertised in the capabilities document
        if (layer == null || !layer.isEnabled()
                || !(layer.getResource() instanceof FeatureTypeInfo)) {
            throw new RestletException("No such layer: " + layerName, Status.CLIENT_ERROR_NOT_FOUND);
        }

        // build the feature collection and wrap it in a resource
        final FeatureTypeInfo resource = (FeatureTypeInfo) layer.getResource();
        try {
            SimpleFeatureSource featureSource = (SimpleFeatureSource) resource.getFeatureSource(
                    null, null);
            Query query = buildQuery(request, featureSource.getSchema());
            
            // couple sanity checks
            final QueryCapabilities queryCapabilities = featureSource.getQueryCapabilities();
            if(!queryCapabilities.isOffsetSupported() && (query.getStartIndex() != null && query.getStartIndex() > 1)) {
                throw new RestletException("Offset is not supported on this data source", Status.SERVER_ERROR_INTERNAL);
            }
            
            SimpleFeatureCollection features = featureSource.getFeatures(query);

            Form form = request.getResourceRef().getQueryAsForm();
            String mode = form.getFirstValue("mode");

            if (mode == null || "features".equals(mode)) {
                return new FeatureCollectionResource(getContext(), request, response, features);
            } else if ("bounds".equals(mode)) {
                return new BoundsResource(getContext(), request, response, features.getBounds());
            } else if ("count".equals(mode)) {
                return new CountResource(getContext(), request, response, features.size());
            } else {
                throw new RestletException("Uknown mode '" + mode + "'",
                        Status.SERVER_ERROR_INTERNAL);
            }
        } catch (IOException e) {
            throw new RestletException("Internal error occurred while "
                    + "retrieving the features to be returned", Status.SERVER_ERROR_INTERNAL, e);
        }
    }

    /**
     * Build a query based on the
     * 
     * @param request
     * @return
     */
    private Query buildQuery(Request request, SimpleFeatureType schema) {
        // get the query string params as a form
        Form form = request.getResourceRef().getQueryAsForm();
        Query query = new Query();
        applyFilter(request, schema, form, query);
        applyAttributeSelection(schema, form, query);

        // the following apply only in feature collection mode
        String fid = RESTUtils.getAttribute(request, "fid");
        if (fid == null) {
            applyMaxFeatures(form, query);
            applyOffset(form, query);
            applyOrderBy(schema, form, query);
        }

        return query;
    }

    private void applyAttributeSelection(SimpleFeatureType schema, Form form, Query query) {
        Set<String> attributes = Collections.emptySet();
        String attrs = form.getFirstValue("attrs");
        if (attrs != null) {
            String[] parsedAttributes = attrs.split("\\s*,\\s*");
            attributes = new HashSet<String>(Arrays.asList(parsedAttributes));
        }

        // build the output property list, if any
        List<String> properties = new ArrayList<String>();
        boolean skipGeom = "true".equals(form.getFirstValue("no_geom"));
        boolean filterAttributes = attributes.size() > 0;
        if (skipGeom || attributes.size() > 0) {
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.setName(schema.getName());
            for (AttributeDescriptor attribute : schema.getAttributeDescriptors()) {
                // skip geometric attributes if so requested
                if (attribute instanceof GeometryDescriptor && skipGeom) {
                    continue;
                }
                // skip unselected attributes (but keep the geometry, that has to be excluded explicitly using nogeom)
                final String name = attribute.getLocalName();
                if (filterAttributes && !attributes.contains(name) 
                        && !attribute.equals(schema.getGeometryDescriptor())) {
                    continue;
                }
                properties.add(name);
                attributes.remove(name);
            }
            if (properties.size() > 0) {
                query.setPropertyNames(properties);
            }

            // check if we have residual, unknown attributes
            if (attributes.size() > 0) {
                throw new RestletException(
                        "The following attributes are not known to this service: " + attributes,
                        Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }
    }

    private void applyOrderBy(SimpleFeatureType schema, Form form, Query query) {
        String orderBy = form.getFirstValue("order_by");
        if (orderBy != null) {
            String[] orderByAtts = orderBy.split("\\s*,\\s*");
            String dir = form.getFirstValue("dir");
            String[] directions = null;
            if (dir != null) {
                directions = dir.split("\\s*,\\s*");
            }

            // check directions and attributes are matched
            if (directions != null && directions.length != orderByAtts.length) {
                if (directions.length < orderByAtts.length) {
                    throw new RestletException("dir list has less entries than order_by",
                            Status.CLIENT_ERROR_BAD_REQUEST);
                } else {
                    throw new RestletException("dir list has more entries than order_by",
                            Status.CLIENT_ERROR_BAD_REQUEST);
                }
            }

            SortBy[] sortBy = new SortBy[orderByAtts.length];
            for (int i = 0; i < orderByAtts.length; i++) {
                String name = orderByAtts[i];
                SortOrder order = getSortOrder(directions, i);

                AttributeDescriptor descriptor = schema.getDescriptor(name);
                if (descriptor == null) {
                    throw new RestletException("Uknown order_by attribute " + name,
                            Status.CLIENT_ERROR_BAD_REQUEST);
                } else if (descriptor instanceof GeometryDescriptor) {
                    throw new RestletException("order_by attribute " + name + " is a geometry, "
                            + "cannot sort on it", Status.CLIENT_ERROR_BAD_REQUEST);
                }

                sortBy[i] = FF.sort(name, order);
            }
            query.setSortBy(sortBy);
        }
    }

    private void applyOffset(Form form, Query query) {
        String offset = form.getFirstValue("offset");
        if (offset != null) {
            try {
                query.setStartIndex(Integer.parseInt(offset));
            } catch (NumberFormatException e) {
                throw new RestletException("Invalid offset expression: " + offset,
                        Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }
    }

    private void applyMaxFeatures(Form form, Query query) {
        String limit = form.getFirstValue("limit");
        if (limit == null) {
            limit = form.getFirstValue("maxfeatures");
        }
        if (limit != null) {
            try {
                query.setMaxFeatures(Integer.parseInt(limit));
            } catch (NumberFormatException e) {
                throw new RestletException("Invalid limit expression: " + limit,
                        Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }
    }

    private void applyFilter(Request request, SimpleFeatureType schema, Form form, Query query) {
        String fid = RESTUtils.getAttribute(request, "fid");
        if (fid != null) {
            final Id fidFilter = FF.id(Collections.singleton(FF.featureId(fid)));
            query.setFilter(fidFilter);
        } else {
            List<Filter> filters = new ArrayList<Filter>();

            // build the geometry filters
            filters.add(buildGeometryFilter(schema, form));
            filters.add(buildBBoxFilter(schema, form));
            filters.add(buildXYToleranceFilter(schema, form));

            // see if we have any non geometric one
            String queryable = form.getFirstValue("queryable");
            if (queryable != null) {
                String[] attributes = queryable.split("\\s*,\\s*");
                for (String name : attributes) {
                    AttributeDescriptor ad = schema.getDescriptor(name);
                    if (ad == null) {
                        throw new RestletException("Uknown queryable attribute " + name,
                                Status.CLIENT_ERROR_BAD_REQUEST);
                    } else if (ad instanceof GeometryDescriptor) {
                        throw new RestletException("queryable attribute " + name
                                + " is a geometry, " + "cannot perform non spatial filters on it",
                                Status.CLIENT_ERROR_BAD_REQUEST);
                    }

                    final PropertyName property = FF.property(name);
                    final String prefix = name + "__";
                    for (String paramName : form.getNames()) {
                        if (paramName.startsWith(prefix)) {
                            Literal value = FF.literal(form.getFirstValue(paramName));
                            String op = paramName.substring(prefix.length());
                            if ("eq".equals(op)) {
                                filters.add(FF.equals(property, value));
                            } else if ("ne".equals(op)) {
                                filters.add(FF.notEqual(property, value));
                            } else if ("lt".equals(op)) {
                                filters.add(FF.less(property, value));
                            } else if ("lte".equals(op)) {
                                filters.add(FF.lessOrEqual(property, value));
                            } else if ("ge".equals(op)) {
                                filters.add(FF.greater(property, value));
                            } else if ("gte".equals(op)) {
                                filters.add(FF.greaterOrEqual(property, value));
                            } else if ("like".equals(op)) {
                                String pattern = form.getFirstValue(paramName);
                                filters.add(FF.like(property, pattern, "%", "_", "\\", true));
                            } else if ("ilike".equals(op)) {
                                String pattern = form.getFirstValue(paramName);
                                filters.add(FF.like(property, pattern, "%", "_", "\\", false));
                            } else {
                                throw new RestletException("Uknown query operand '" + op + "'",
                                        Status.CLIENT_ERROR_BAD_REQUEST);
                            }
                        }
                    }
                }
            }

            if (filters.size() > 0) {
                // summarize all the filters
                Filter result = FF.and(filters);
                SimplifyingFilterVisitor simplifier = new SimplifyingFilterVisitor();
                result = (Filter) result.accept(simplifier, null);

                // if necessary, reproject the filters
                String crs = form.getFirstValue("crs");
                if (crs == null) {
                    crs = form.getFirstValue("epsg");
                }
                if (crs != null) {
                    try {
                        // apply the default srs into the spatial filters
                        CoordinateReferenceSystem sourceCrs = CRS.decode("EPSG:" + crs, true);
                        DefaultCRSFilterVisitor crsForcer = new DefaultCRSFilterVisitor(FF,
                                sourceCrs);
                        result = (Filter) result.accept(crsForcer, null);
                    } catch (Exception e) {
                        throw new RestletException("Uknown EPSG code '" + crs + "'",
                                Status.CLIENT_ERROR_BAD_REQUEST);
                    }
                }

                query.setFilter(result);
            }
        }
    }
    
    private double getTolerance(Form form) {
        String tolerance = form.getFirstValue("tolerance");
        if(tolerance != null) {
            double tolValue = parseDouble(tolerance, "tolerance");
            if(tolValue < 0) {
                throw new RestletException("Invalid tolerance, it should be zero or positive: " + tolValue ,
                        Status.CLIENT_ERROR_BAD_REQUEST);
            }
            return tolValue;
        } else {
            return 0d;
        }
    }

    private Filter buildXYToleranceFilter(SimpleFeatureType schema, Form form) {
        String x = form.getFirstValue("lon");
        String y = form.getFirstValue("lat");
        if (x == null && y == null) {
            return Filter.INCLUDE;
        }
        if (x == null || y == null) {
            throw new RestletException(
                    "Incomplete x/y specification, must provide both values",
                    Status.CLIENT_ERROR_BAD_REQUEST);
        }
        double ordx = parseDouble(x, "x");
        double ordy = parseDouble(y, "y");
        
        final Point centerPoint = new GeometryFactory().createPoint(new Coordinate(ordx, ordy));
        final double tolerance = getTolerance(form);
        return geometryFilter(schema, centerPoint, tolerance);

    }

    private Filter geometryFilter(SimpleFeatureType schema, Geometry geometry, double tolerance) {
        PropertyName defaultGeometry = FF.property(schema.getGeometryDescriptor().getLocalName());
        Literal center = FF.literal(geometry);
        
        if(tolerance == 0) {
            return FF.intersects(defaultGeometry, center);
        } else {
            return FF.dwithin(defaultGeometry, center, tolerance, null);
        }

    }

    double parseDouble(String value, String name) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new RestletException("Expected a number for " + name + " but got " + value,
                    Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

    private Filter buildBBoxFilter(SimpleFeatureType schema, Form form) {
        String bbox = form.getFirstValue("bbox");
        if (bbox == null) {
            return Filter.INCLUDE;
        } else {
            try {
                JSONArray ordinates = JSONArray.fromObject("[" + bbox + "]");
                String defaultGeomName = schema.getGeometryDescriptor().getLocalName();
                final double tolerance = getTolerance(form);
                double minx = ordinates.getDouble(0);
                double miny = ordinates.getDouble(1);
                double maxx = ordinates.getDouble(2);
                double maxy = ordinates.getDouble(3);
                if(tolerance > 0) {
                    minx -= tolerance;
                    miny -= tolerance;
                    maxx += tolerance;
                    maxy += tolerance;
                }
                return FF.bbox(defaultGeomName, minx, miny, maxx, maxy, null);
            } catch (Exception e) {
                throw new RestletException("Could not parse the bbox: " + e.getMessage(),
                        Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }
    }

    private Filter buildGeometryFilter(SimpleFeatureType schema, Form form) {
        String geometry = form.getFirstValue("geometry");
        if (geometry == null) {
            return Filter.INCLUDE;
        } else {
            try {
                Geometry geom = new GeometryJSON().read(geometry);
                final double tolerance = getTolerance(form);
                return geometryFilter(schema, geom, tolerance);
            } catch (IOException e) {
                throw new RestletException("Could not parse the geometry geojson: "
                        + e.getMessage(), Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }
    }

    SortOrder getSortOrder(String[] orders, int idx) {
        if (orders == null) {
            return SortOrder.ASCENDING;
        }
        String order = orders[idx];
        if (order == null) {
            return SortOrder.ASCENDING;
        } else if ("DESC".equals(order)) {
            return SortOrder.DESCENDING;
        } else if ("ASC".equals(order)) {
            return SortOrder.ASCENDING;
        } else {
            throw new RestletException("Unknown ordering direction: " + order,
                    Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }
}
