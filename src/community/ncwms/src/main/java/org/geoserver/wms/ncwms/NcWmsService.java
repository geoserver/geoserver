/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.ncwms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.featureinfo.LayerIdentifier;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.DateRange;
import org.geotools.util.SimpleInternationalString;
import org.springframework.beans.factory.DisposableBean;

/**
 * Implements the methods of the NcWMS service which are not included on the WMS standard. For the
 * moment, only GetTimeSeries method is supported.
 */
public class NcWmsService implements DisposableBean {

    public static final String WMS_CONFIG_KEY = "NcWMSInfo";

    public static final String TIME_SERIES_INFO_FORMAT_PARAM_NAME = "TIME_SERIES_INFO_FORMAT";

    public static final String GET_TIME_SERIES_REQUEST = "GetTimeSeries";

    private WMS wms;

    protected ExecutorService executor;

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

    /**
     * Returns the maximum number of time/elevation/dimensions the service should tolerate
     *
     * @param wms
     * @return
     */
    public static int getMaxDimensions(WMS wms) {
        // use the ncWMS local maximum
        NcWmsInfo ncWMSInfo =
                wms.getServiceInfo()
                        .getMetadata()
                        .get(NcWmsService.WMS_CONFIG_KEY, NcWmsInfo.class);
        return Optional.ofNullable(ncWMSInfo)
                .map(i -> i.getMaxTimeSeriesValues())
                .orElseGet(wms::getMaxRequestedDimensionValues);
    }

    enum DateFinder {
        NEAREST {
            // This is used when nearest match is supported.
            @Override
            List<DateRange> findDates(WMS wms, CoverageInfo coverage, List<Object> times)
                    throws IOException {
                TreeSet<Date> availableDates = wms.queryCoverageNearestMatchTimes(coverage, times);
                return availableDates.stream()
                        .map(date -> new DateRange(date, date))
                        .collect(Collectors.toList());
            }
        },
        QUERY {
            // When nearest match isn't enabled, let's query the coverage to identify
            // available dates only
            @Override
            List<DateRange> findDates(WMS wms, CoverageInfo coverage, List<Object> times)
                    throws IOException {
                TreeSet<Object> availableDates =
                        new TreeSet<>(ReaderDimensionsAccessor.TEMPORAL_COMPARATOR);
                final boolean isRange = times.get(0) instanceof DateRange;
                for (Object time : times) {
                    TreeSet<Object> foundTimes =
                            wms.queryCoverageTimes(
                                    coverage, getAsRange(time, isRange), Query.DEFAULT_MAX);
                    availableDates.addAll(foundTimes);
                }
                return availableDates.stream()
                        .map(
                                d ->
                                        d instanceof Date
                                                ? new DateRange((Date) d, (Date) d)
                                                : (DateRange) d)
                        .collect(Collectors.toList());
            }
        };

        // times should be a not null List. (Null Check is made before calling this method)
        abstract List<DateRange> findDates(WMS wms, CoverageInfo coverage, List<Object> times)
                throws IOException;

        protected DateRange getAsRange(Object time, boolean isRange) {
            return isRange ? (DateRange) time : new DateRange((Date) time, (Date) time);
        }
    }

    public NcWmsService(final WMS wms) {
        this.wms = wms;
        this.executor = getExecutorService(wms.getServiceInfo());
    }

    private ExecutorService getExecutorService(WMSInfo wms) {
        NcWmsInfo info = wms.getMetadata().get(WMS_CONFIG_KEY, NcWmsInfo.class);
        int poolSize =
                Optional.ofNullable(info)
                        .map(i -> i.getTimeSeriesPoolSize())
                        .filter(size -> size > 0)
                        .orElseGet(() -> Runtime.getRuntime().availableProcessors());

        return Executors.newFixedThreadPool(
                poolSize,
                new ThreadFactory() {

                    private final AtomicInteger counter = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "gs-ncwms-" + counter.incrementAndGet());
                    }
                });
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }

    public synchronized void configurationChanged(WMSInfo wms) {
        executor.shutdownNow();
        this.executor = getExecutorService(wms);
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
     */
    public FeatureCollectionType getTimeSeries(GetFeatureInfoRequest request) {
        FeatureCollectionType result = WfsFactory.eINSTANCE.createFeatureCollectionType();
        WfsFactory.eINSTANCE.createFeatureCollectionType();
        result.setTimeStamp(Calendar.getInstance());

        // Process the request only if we have a time range
        List<Object> times = request.getGetMapRequest().getTime();
        if (times == null || times.size() == 0) {
            throw new ServiceException("The TIME parameter was missing");
        }

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

        // we'll just pick the first band anyways, no need to read them all
        if (request.getPropertyNames() == null
                || request.getPropertyNames().size() == 0
                || request.getPropertyNames().get(0).isEmpty()) {
            String firstBand = coverage.getDimensions().get(0).getName();
            request.setPropertyNames(Arrays.asList(Arrays.asList(firstBand)));
        }

        // control how much time we spend doing queries to gather times and values
        int maxRenderingTime = wms.getMaxRenderingTime(request.getGetMapRequest());
        CountdownClock countdownClock = new CountdownClock(maxRenderingTime);
        LayerIdentifier identifier = getLayerIdentifier(layer);
        SimpleFeatureType resultType = getResultType(layer.getName(), buildTypeDescription(layer));

        // 1) Use a concurrent hash map for collecting date -> feature in parallel
        // 2) Use a fixed thread pool sized by default by the number of CPUs?
        // 3) Map in advance the requests to FeatureInfoRequestParameters, then use only them in the
        // concurrent part
        // 4) Configuration GUI with custom panel for number of threads? stored in the WMSInfo
        // metadata map
        Map<Date, SimpleFeature> features = new ConcurrentHashMap<>();

        try {
            // Get available dates, then perform an identify operation for each date in the range
            List<DateRange> availableDates = getAvailableDates(coverage, times);
            request.setExcludeNodataResults(true);
            List<Callable<Void>> callables = new ArrayList<>();
            for (DateRange date : availableDates) {
                // this modifies the request and then transforms it into a feature info
                // request parameter, which, inside, does not contain a reference
                // to the request anymore
                request.getGetMapRequest().setTime(Collections.singletonList(date));
                FeatureInfoRequestParameters requestParams =
                        new FeatureInfoRequestParameters(request);
                callables.add(
                        () -> {
                            // check timeout and then run identify
                            countdownClock.checkTimeout();
                            identifyDate(identifier, resultType, features, date, requestParams);
                            return null;
                        });
            }

            // invoke and get all to make sure no exception was raised
            List<Future<Void>> futures = executor.invokeAll(callables);
            for (Future<Void> future : futures) {
                future.get();
            }

            // sort by time and accumulate values
            List<SimpleFeature> featureList =
                    features.entrySet().stream()
                            .sorted(Comparator.comparing(e -> e.getKey()))
                            .map(e -> e.getValue())
                            .collect(Collectors.toList());

            result.getFeature().add(new ListFeatureCollection(resultType, featureList));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Error processing the operation", e);
        } finally {
            // restore the original times
            request.getGetMapRequest().setTime(times);
        }

        return result;
    }

    private void identifyDate(
            LayerIdentifier identifier,
            SimpleFeatureType resultType,
            Map<Date, SimpleFeature> features,
            DateRange date,
            FeatureInfoRequestParameters requestParams)
            throws Exception {
        // identify
        List<FeatureCollection> identifiedCollections = identifier.identify(requestParams, 1);

        // collect the data
        if (identifiedCollections != null) {
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(resultType);
            for (FeatureCollection c : identifiedCollections) {
                try (FeatureIterator featIter = c.features()) {
                    // no need to loop, we just want one value
                    if (featIter.hasNext()) {
                        Feature inFeat = featIter.next();
                        Iterator<Property> propIter = inFeat.getProperties().iterator();
                        if (propIter.hasNext()) {
                            Property prop = propIter.next();
                            Date dateValue = date.getMinValue();
                            // check for duplicates while updating the collection
                            if (!features.containsKey(dateValue)) {
                                featureBuilder.add(dateValue);
                                featureBuilder.add(prop.getValue());
                                SimpleFeature newFeat = featureBuilder.buildFeature(null);
                                features.put(dateValue, newFeat);
                            }
                        }
                    }
                }
            }
        }
    }

    private List<DateRange> getAvailableDates(CoverageInfo coverage, List<Object> times)
            throws IOException {

        // We have 3 input cases here:
        // (A) Simple range: e.g. 2018-01-01/2020-01-01
        // (B) Single times list: e.g. 2018-01,2018-02,2018-03,2018-04
        // (C) Range with period: e.g. 2018-01-01/2020-01-01/P1D

        // (A) and (B) will result into a list of DateRange but (A) will only have 1 element
        // (C) will result into a list of Dates

        DimensionInfo timeDimension =
                coverage.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (timeDimension == null || !timeDimension.isEnabled()) {
            throw new ServiceException(
                    "Layer " + coverage.prefixedName() + " does not have time support enabled");
        }

        // We have already checked before invoking this method that the list isn't null nor empty
        final boolean nearestMatch = timeDimension.isNearestMatchEnabled();

        final boolean simpleRange = times.size() == 1 && times.get(0) instanceof DateRange;
        List<DateRange> results;
        if (nearestMatch && simpleRange) {
            results = handleSimpleInterval(coverage, times);
        } else {
            DateFinder finder = nearestMatch ? DateFinder.NEAREST : DateFinder.QUERY;
            results = finder.findDates(wms, coverage, times);
        }

        // check they are not too many
        int maxDimensions = NcWmsService.getMaxDimensions(wms);

        if (maxDimensions > 0 && results.size() > maxDimensions)
            throw new ServiceException(
                    "This request would process "
                            + results.size()
                            + " times, while the maximum allowed is "
                            + maxDimensions
                            + ". Please reduce the size of the requested time range.",
                    "InvalidParameterValue",
                    "time");

        return results;
    }

    private List<DateRange> handleSimpleInterval(CoverageInfo coverage, List<Object> times)
            throws IOException {
        DateFinder finder = DateFinder.QUERY;
        List<DateRange> results = finder.findDates(wms, coverage, times);
        if (results.size() == 0) results = DateFinder.NEAREST.findDates(wms, coverage, times);
        return results;
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

    private SimpleFeatureType getResultType(String name, String description) {
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
        return builder.buildFeatureType();
    }
}
