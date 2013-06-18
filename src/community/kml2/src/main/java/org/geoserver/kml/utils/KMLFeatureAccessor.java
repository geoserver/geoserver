/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.kml.regionate.RegionatingStrategy;
import org.geoserver.kml.regionate.RegionatingStrategyFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.IllegalFilterException;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Class encapsulating the logic to query features for a given layer in the current GetMap KML
 * request
 * 
 * @author Andrea Aime - GeoSolutions
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class KMLFeatureAccessor {

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.kml");

    /**
     * Factory used to create filter objects
     */
    private static FilterFactory filterFactory = (FilterFactory) CommonFactoryFinder
            .getFilterFactory(null);

    /**
     * Loads the feature collection based on the current styling and the scale denominator. If no
     * feature is going to be returned a null feature collection will be returned instead
     * 
     * @param layer
     * @param mapContent
     * @param wms
     * @param scaleDenominator
     * @return
     * @throws Exception
     */
    public SimpleFeatureCollection loadFeatureCollection(Layer layer, WMSMapContent mapContent,
            WMS wms, double scaleDenominator) throws Exception {
        SimpleFeatureSource featureSource = (SimpleFeatureSource) layer.getFeatureSource();
        Query q = getFeaturesQuery(layer, mapContent, wms, scaleDenominator);

        // make sure we output in 4326 since that's what KML mandates
        CoordinateReferenceSystem wgs84;
        try {
            wgs84 = CRS.decode("EPSG:4326", true);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot decode EPSG:4326, the CRS subsystem must be badly broken...", e);
        }
        SimpleFeatureCollection features = featureSource.getFeatures(q);
        SimpleFeatureType schema = featureSource.getSchema();
        CoordinateReferenceSystem nativeCRS = schema.getCoordinateReferenceSystem();
        if (nativeCRS != null && !CRS.equalsIgnoreMetadata(wgs84, nativeCRS)) {
            features = new ReprojectFeatureResults(features, wgs84);
        }

        return features;
    }

    /**
     * Counts how many features will be returned for the specified layer in the current request
     * 
     * @param layer
     * @param mapContent
     * @param wms
     * @param scaleDenominator
     * @return
     * @throws Exception
     */
    public int getFeatureCount(Layer layer, WMSMapContent mapContent, WMS wms,
            double scaleDenominator) throws Exception {
        Query q = getFeaturesQuery(layer, mapContent, wms, scaleDenominator);

        SimpleFeatureSource featureSource = (SimpleFeatureSource) layer.getFeatureSource();
        int count = featureSource.getCount(q);
        if (count == -1) {
            count = featureSource.getFeatures(q).size();
        }

        return count;
    }

    /**
     * Builds the Query object that will return the features for the specified layer and scale
     * denominator, based also on the current WMS configuration
     * 
     * @param layer
     * @param mapContent
     * @param wms
     * @param scaleDenominator
     * @param schema
     * @return
     * @throws TransformException
     * @throws FactoryException
     */
    private Query getFeaturesQuery(Layer layer, WMSMapContent mapContent, WMS wms,
            double scaleDenominator) throws TransformException, FactoryException {
        SimpleFeatureType schema = ((SimpleFeatureSource) layer.getFeatureSource()).getSchema();

        // create a bbox filter in the source crs (from the GetMap request bbox)
        ReferencedEnvelope aoi = mapContent.getRenderingArea();
        CoordinateReferenceSystem sourceCrs = schema.getCoordinateReferenceSystem();
        if ((sourceCrs != null)
                && !CRS.equalsIgnoreMetadata(aoi.getCoordinateReferenceSystem(), sourceCrs)) {
            aoi = aoi.transform(sourceCrs, true);
        }

        Filter filter = createBBoxFilter(schema, aoi);

        // now build the query using only the attributes and the bounding box needed
        Query q = new Query(schema.getTypeName());
        q.setFilter(filter);

        // now, if a definition query has been established for this layer,
        // be sure to respect it by combining it with the bounding box one.
        q = DataUtilities.mixQueries(q, layer.getQuery(), "KMLEncoder");

        // check the regionating strategy
        RegionatingStrategy regionatingStrategy = null;
        String stratname = (String) mapContent.getRequest().getFormatOptions().get("regionateBy");
        if (("auto").equals(stratname)) {
            Catalog catalog = wms.getGeoServer().getCatalog();
            Name name = layer.getFeatureSource().getName();
            stratname = catalog.getFeatureTypeByName(name).getMetadata()
                    .get("kml.regionateStrategy", String.class);
            if (stratname == null || "".equals(stratname)) {
                stratname = "best_guess";
                LOGGER.log(Level.FINE, "No default regionating strategy has been configured in "
                        + name + "; using automatic best-guess strategy.");
            }
        }

        Filter regionatingFilter = Filter.INCLUDE;
        if (stratname != null) {
            regionatingStrategy = findStrategyByName(stratname);

            // if a strategy was specified but we did not find it, let the user
            // know
            if (regionatingStrategy == null) {
                throw new ServiceException("Unknown regionating strategy " + stratname);
            } else {
                regionatingFilter = regionatingStrategy.getFilter(mapContent, layer);
            }
        }

        // try to load less features by leveraging regionating strategy and the SLD
        Filter ruleFilter = getStyleFilter(schema, layer.getStyle(), scaleDenominator);
        Filter finalFilter = joinFilters(q.getFilter(), ruleFilter, regionatingFilter);
        if (finalFilter == Filter.EXCLUDE) {
            // if we don't have any feature to return
            return null;
        }
        q.setFilter(finalFilter);
        return q;
    }

    private RegionatingStrategy findStrategyByName(String name) {
        List<RegionatingStrategyFactory> factories = GeoServerExtensions
                .extensions(RegionatingStrategyFactory.class);
        Iterator<RegionatingStrategyFactory> it = factories.iterator();
        while (it.hasNext()) {
            RegionatingStrategyFactory factory = it.next();
            if (factory.canHandle(name)) {
                return factory.createStrategy();
            }
        }

        return null;
    }

    private Filter getStyleFilter(SimpleFeatureType schema, Style style, double scaleDenominator) {
        // first, simplify the style and get only the applicable rules
        ScaleStyleVisitor sdSimplifier = new ScaleStyleVisitor(scaleDenominator, schema);
        style.accept(sdSimplifier);
        Style simplified = sdSimplifier.getSimplifiedStyle();
        // then collect the filter equivalent to all the rules
        RuleFiltersCollector collector = new RuleFiltersCollector();
        simplified.accept(collector);
        return collector.getSummaryFilter();
    }

    /**
     * Creates the bounding box filters (one for each geometric attribute) needed to query a
     * <code>Layer</code>'s feature source to return just the features for the target rendering
     * extent
     * 
     * @param schema the layer's feature source schema
     * @param bbox the expression holding the target rendering bounding box
     * @return an or'ed list of bbox filters, one for each geometric attribute in
     *         <code>attributes</code>. If there are just one geometric attribute, just returns its
     *         corresponding <code>GeometryFilter</code>.
     * @throws IllegalFilterException if something goes wrong creating the filter
     */
    private Filter createBBoxFilter(SimpleFeatureType schema, Envelope bbox)
            throws IllegalFilterException {
        List<Filter> filters = new ArrayList<Filter>();
        for (int j = 0; j < schema.getAttributeCount(); j++) {
            AttributeDescriptor attType = schema.getDescriptor(j);

            if (attType instanceof GeometryDescriptor) {
                Filter gfilter = filterFactory.bbox(attType.getLocalName(), bbox.getMinX(),
                        bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY(), null);
                filters.add(gfilter);
            }
        }

        if (filters.size() == 0)
            return Filter.INCLUDE;
        else if (filters.size() == 1)
            return (Filter) filters.get(0);
        else
            return filterFactory.or(filters);
    }

    /**
     * Joins the provided filters in a single one by and-ing them (and then, simplifying them)
     * 
     * @param filters
     * @return
     */
    private Filter joinFilters(Filter... filters) {
        if (filters == null || filters.length == 0) {
            return Filter.EXCLUDE;
        }

        Filter result = null;
        if (filters.length > 0) {
            FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
            result = ff.and(Arrays.asList(filters));
        } else if (filters.length == 1) {
            result = filters[0];
        }

        SimplifyingFilterVisitor visitor = new SimplifyingFilterVisitor();
        return (Filter) result.accept(visitor, null);
    }
}
