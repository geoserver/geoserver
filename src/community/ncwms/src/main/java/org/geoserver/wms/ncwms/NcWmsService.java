/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.ncwms;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.LayerIdentifier;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.DateRange;
import org.geotools.util.SimpleInternationalString;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Implements the methods of the NcWMS service which are not included on the WMS standard. For the
 * moment, only GetTimeSeries method is supported.
 */
public class NcWmsService {

    public static final String TIME_SERIES_INFO_FORMAT_PARAM_NAME = "TIME_SERIES_INFO_FORMAT";

    public static final String GET_TIME_SERIES_REQUEST = "GetTimeSeries";

    private WMS wms;

    /** Simple helper to enforce rendering timeout */
    private static class CountdownClock {
        long end;

        long maxRenderingTime;

        CountdownClock(long maxRenderingTime) {
            this.maxRenderingTime = maxRenderingTime;
            if (maxRenderingTime > 0) {
                this.end = System.currentTimeMillis() + maxRenderingTime;
            }
        }

        public void checkTimeout() {
            if (maxRenderingTime > 0 && System.currentTimeMillis() > end) {
                throw new ServiceException(
                        "This request used more time than allowed and has been forcefully stopped. "
                                + "Max rendering time is "
                                + (maxRenderingTime / 1000.0)
                                + "s");
            }
        }
    }

    public NcWmsService(final WMS wms) {
        this.wms = wms;
    }

    private LayerIdentifier getLayerIdentifier(MapLayerInfo layer) {
        List<LayerIdentifier> identifiers = GeoServerExtensions.extensions(LayerIdentifier.class);
        for (LayerIdentifier identifier : identifiers) {
            if (identifier.canHandle(layer)) {
                return identifier;
            }
        }

        throw new ServiceException(
                "Could not find any identifier that can handle layer "
                        + layer.getLayerInfo().prefixedName()
                        + " among these identifiers: "
                        + identifiers);
    }

    /**
     * Implements the GetTimeSeries method, which can retrieve a time series of values on a certain
     * point, using a syntax similar to the GetFeatureInfo operation.
     *
     * @param request
     * @return
     */
    @SuppressWarnings("rawtypes")
    public FeatureCollectionType getTimeSeries(GetFeatureInfoRequest request) {
        FeatureCollectionType result = WfsFactory.eINSTANCE.createFeatureCollectionType();
        WfsFactory.eINSTANCE.createFeatureCollectionType();
        result.setTimeStamp(Calendar.getInstance());

        // Process the request only if we have a time range
        if (request.getGetMapRequest().getTime() == null
                || request.getGetMapRequest().getTime().size() != 1) {
            throw new ServiceException(
                    "The TIME parameter was not a valid WMS time range or was missing");
        }

        Object queryRangePlain = (DateRange) request.getGetMapRequest().getTime().get(0);
        if (queryRangePlain == null || !(queryRangePlain instanceof DateRange)) {
            throw new ServiceException("The TIME parameter was not a valid WMS time range");
        }

        DateRange queryRange = (DateRange) queryRangePlain;
        final List<MapLayerInfo> requestedLayers = request.getQueryLayers();
        // Process the request only if we have single layer
        if (requestedLayers.size() != 1) {
            throw new ServiceException(
                    "The QUERY_LAYERS parameter must specify a single coverage layer for the GetTimeSeries operation");
        }

        final MapLayerInfo layer = requestedLayers.get(0);
        CoverageInfo coverage;
        try {
            coverage = layer.getCoverage();
        } catch (Exception cex) {
            throw new ServiceException(
                    "The GetTimeSeries operation is only defined for coverage layers");
        }

        // control how much time we spend doing queries to gather times and values
        int maxRenderingTime = wms.getMaxRenderingTime(request.getGetMapRequest());
        CountdownClock countdownClock = new CountdownClock(maxRenderingTime);
        LayerIdentifier identifier = getLayerIdentifier(layer);
        SimpleFeatureBuilder featureBuilder =
                getResultFeatureBuilder(layer.getName(), buildTypeDescription(layer));
        try {
            // Get available dates, then perform an identify operation per each date in the range
            TreeSet availableDates =
                    wms.queryCoverageTimes(coverage, queryRange, Query.DEFAULT_MAX);
            ListFeatureCollection features =
                    new ListFeatureCollection(featureBuilder.getFeatureType());
            for (Object d : availableDates) {
                // check timeout
                countdownClock.checkTimeout();

                // run query
                Date date = (Date) d;
                DateRange currentDate = new DateRange(date, date);
                request.getGetMapRequest().getTime().remove(0);
                request.getGetMapRequest().getTime().add(currentDate);
                FeatureInfoRequestParameters requestParams =
                        new FeatureInfoRequestParameters(request);
                List<FeatureCollection> identifiedCollections =
                        identifier.identify(requestParams, 1);

                // collect the data
                if (identifiedCollections != null) {
                    for (FeatureCollection c : identifiedCollections) {
                        try (FeatureIterator featIter = c.features()) {
                            if (featIter.hasNext()) { // no need to loop, we just want one value
                                Feature inFeat = featIter.next();
                                Iterator<Property> propIter = inFeat.getProperties().iterator();
                                if (propIter.hasNext()) {
                                    Property prop = propIter.next();
                                    featureBuilder.add(date);
                                    featureBuilder.add(prop.getValue());
                                    SimpleFeature newFeat = featureBuilder.buildFeature(null);
                                    features.add(newFeat);
                                }
                            }
                        }
                    }
                }
            }
            result.getFeature().add(features);
        } catch (Exception e) {
            throw new ServiceException("Error processing the operation", e);
        } finally {
            // restore the original range
            request.getGetMapRequest().getTime().remove(0);
            request.getGetMapRequest().getTime().add(queryRange);
        }

        return result;
    }

    public String buildTypeDescription(MapLayerInfo layer) {
        String name = layer.getName();
        if (layer.getCoverage() != null
                && layer.getCoverage().getDimensions().size() == 1
                && layer.getCoverage().getDimensions().get(0).getName() != null
                && layer.getCoverage().getDimensions().get(0).getUnit() != null) {
            name =
                    layer.getCoverage().getDimensions().get(0).getName()
                            + " ("
                            + layer.getCoverage().getDimensions().get(0).getUnit()
                            + ")";
        }
        return name;
    }

    private SimpleFeatureBuilder getResultFeatureBuilder(String name, String description) {
        // create the builder
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

        // set global state
        builder.setName(name);
        builder.setDescription(new SimpleInternationalString(description));
        builder.setNamespaceURI("http://www.geoserver.org/");
        builder.setSRS("EPSG:4326");

        // add attributes
        builder.add("date", Date.class);
        builder.add("value", Double.class);
        SimpleFeatureType featureType = builder.buildFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        return featureBuilder;
    }
}
