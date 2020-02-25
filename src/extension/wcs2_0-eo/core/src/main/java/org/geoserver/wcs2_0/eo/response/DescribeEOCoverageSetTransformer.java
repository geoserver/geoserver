/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.response;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wcs20.DescribeEOCoverageSetType;
import net.opengis.wcs20.DimensionTrimType;
import net.opengis.wcs20.Section;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.OWS20Exception.OWSExceptionCode;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs2_0.GetCoverage;
import org.geoserver.wcs2_0.eo.EOCoverageResourceCodec;
import org.geoserver.wcs2_0.eo.WCSEOMetadata;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.response.WCS20DescribeCoverageTransformer;
import org.geoserver.wcs2_0.response.WCS20DescribeCoverageTransformer.WCS20DescribeCoverageTranslator;
import org.geoserver.wcs2_0.response.WCSDimensionsHelper;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.MaxFeaturesFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.geotools.xml.impl.DatatypeConverterImpl;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Encodes a DescribeEOCoverageSet response
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DescribeEOCoverageSetTransformer extends TransformerBase {

    static Logger LOGGER = Logging.getLogger(WCS20DescribeEOCoverageSetTranslator.class);

    EOCoverageResourceCodec codec;

    WCS20DescribeCoverageTransformer dcTransformer;

    EnvelopeAxesLabelsMapper envelopeAxisMapper;

    WCSInfo wcs;

    public DescribeEOCoverageSetTransformer(
            WCSInfo wcs,
            EOCoverageResourceCodec codec,
            EnvelopeAxesLabelsMapper envelopeAxesMapper,
            WCS20DescribeCoverageTransformer dcTransformer) {
        this.wcs = wcs;
        this.codec = codec;
        this.envelopeAxisMapper = envelopeAxesMapper;
        this.dcTransformer = dcTransformer;
        setIndentation(2);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        WCS20DescribeCoverageTranslator dcTranslator = dcTransformer.createTranslator(handler);
        return new WCS20DescribeEOCoverageSetTranslator(handler, dcTranslator);
    }

    public class WCS20DescribeEOCoverageSetTranslator extends TranslatorSupport {

        private WCS20DescribeCoverageTranslator dcTranslator;

        public WCS20DescribeEOCoverageSetTranslator(
                ContentHandler handler, WCS20DescribeCoverageTranslator dcTranslator) {
            super(handler, null, null);
            this.dcTranslator = dcTranslator;
        }

        /** Encode the object. */
        @Override
        public void encode(Object o) throws IllegalArgumentException {
            DescribeEOCoverageSetType dcs = (DescribeEOCoverageSetType) o;

            List<CoverageInfo> coverages = getCoverages(dcs);
            List<CoverageGranules> coverageGranules = getCoverageGranules(dcs, coverages);
            int granuleCount = getGranuleCount(coverageGranules);
            Integer maxCoverages = getMaxCoverages(dcs);
            int returned;
            if (maxCoverages != null) {
                returned = granuleCount < maxCoverages ? granuleCount : maxCoverages;
            } else {
                returned = granuleCount;
            }

            String eoSchemaLocation =
                    ResponseUtils.buildSchemaURL(dcs.getBaseUrl(), "wcseo/1.0/wcsEOAll.xsd");
            Attributes atts =
                    atts(
                            "xmlns:eop",
                            "http://www.opengis.net/eop/2.0", //
                            "xmlns:ows",
                            "http://www.opengis.net/ows/2.0",
                            "xmlns:gml",
                            "http://www.opengis.net/gml/3.2", //
                            "xmlns:wcsgs",
                            "http://www.geoserver.org/wcsgs/2.0", //
                            "xmlns:gmlcov",
                            "http://www.opengis.net/gmlcov/1.0",
                            "xmlns:om",
                            "http://www.opengis.net/om/2.0",
                            "xmlns:swe",
                            "http://www.opengis.net/swe/2.0",
                            "xmlns:wcs",
                            "http://www.opengis.net/wcs/2.0",
                            "xmlns:wcseo",
                            "http://www.opengis.net/wcseo/1.0",
                            "xmlns:xlink",
                            "http://www.w3.org/1999/xlink",
                            "xmlns:xsi",
                            "http://www.w3.org/2001/XMLSchema-instance",
                            "numberMatched",
                            String.valueOf(granuleCount),
                            "numberReturned",
                            String.valueOf(returned),
                            "xsi:schemaLocation",
                            "http://www.opengis.net/wcseo/1.0 " + eoSchemaLocation);

            start("wcseo:EOCoverageSetDescription", atts);

            if (!coverageGranules.isEmpty()) {
                List<CoverageGranules> reducedGranules = coverageGranules;
                if (maxCoverages != null) {
                    reducedGranules = applyMaxCoverages(coverageGranules, maxCoverages);
                }

                boolean allSections =
                        dcs.getSections() == null
                                || dcs.getSections().getSection() == null
                                || dcs.getSections().getSection().contains(Section.ALL);
                if (allSections
                        || dcs.getSections().getSection().contains(Section.COVERAGEDESCRIPTIONS)) {
                    handleCoverageDescriptions(reducedGranules);
                }
                if (allSections
                        || dcs.getSections()
                                .getSection()
                                .contains(Section.DATASETSERIESDESCRIPTIONS)) {
                    handleDatasetSeriesDescriptions(coverageGranules);
                }
            }

            end("wcseo:EOCoverageSetDescription");
        }

        /** Returns the max number of coverages to return, if any (null otherwise) */
        private Integer getMaxCoverages(DescribeEOCoverageSetType dcs) {
            if (dcs.getCount() > 0) {
                return dcs.getCount();
            }

            // fall back on the the default value, it's ok if it's null
            return wcs.getMetadata().get(WCSEOMetadata.COUNT_DEFAULT.key, Integer.class);
        }

        private List<CoverageGranules> applyMaxCoverages(
                List<CoverageGranules> coverageGranules, Integer maxCoverages) {
            List<CoverageGranules> result =
                    new ArrayList<DescribeEOCoverageSetTransformer.CoverageGranules>();
            for (CoverageGranules cg : coverageGranules) {
                int size = cg.granules.size();
                if (size > maxCoverages) {
                    cg.granules =
                            DataUtilities.simple(
                                    new MaxFeaturesFeatureCollection<
                                            SimpleFeatureType, SimpleFeature>(
                                            cg.granules, maxCoverages));
                }
                result.add(cg);
                maxCoverages -= size;
                if (maxCoverages <= 0) {
                    break;
                }
            }

            return result;
        }

        private void handleDatasetSeriesDescriptions(List<CoverageGranules> coverages) {
            start("wcseo:DatasetSeriesDescriptions");
            for (CoverageGranules gc : coverages) {
                CoverageInfo ci = gc.coverage;
                String datasetId = codec.getDatasetName(ci);
                start("wcseo:DatasetSeriesDescription", atts("gml:id", datasetId));

                try {
                    GridCoverage2DReader reader =
                            (GridCoverage2DReader) ci.getGridCoverageReader(null, null);

                    // encode the bbox
                    encodeDatasetBounds(ci, reader);

                    // the dataset series id
                    element("wcseo:DatasetSeriesId", datasetId);

                    // encode the time
                    DimensionInfo time =
                            ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
                    WCSDimensionsHelper timeHelper =
                            new WCSDimensionsHelper(time, reader, datasetId);
                    dcTranslator.encodeTimePeriod(
                            timeHelper.getBeginTime(),
                            timeHelper.getEndTime(),
                            datasetId + "_timeperiod",
                            null,
                            null);

                    end("wcseo:DatasetSeriesDescription");
                } catch (IOException e) {
                    throw new WCS20Exception(
                            "Failed to build the description for dataset series "
                                    + codec.getDatasetName(ci),
                            e);
                }
            }
            end("wcseo:DatasetSeriesDescriptions");
        }

        private void encodeDatasetBounds(CoverageInfo ci, GridCoverage2DReader reader)
                throws IOException {
            // get the crs and look for an EPSG code
            final CoordinateReferenceSystem crs = ci.getCRS();
            GeneralEnvelope envelope = reader.getOriginalEnvelope();
            List<String> axesNames = envelopeAxisMapper.getAxesNames(envelope, true);

            // lookup EPSG code
            Integer EPSGCode = null;
            try {
                EPSGCode = CRS.lookupEpsgCode(crs, false);
            } catch (FactoryException e) {
                throw new IllegalStateException(
                        "Unable to lookup epsg code for this CRS:" + crs, e);
            }
            if (EPSGCode == null) {
                throw new IllegalStateException("Unable to lookup epsg code for this CRS:" + crs);
            }
            final String srsName = GetCoverage.SRS_STARTER + EPSGCode;
            // handle axes swap for geographic crs
            final boolean axisSwap = CRS.getAxisOrder(crs).equals(AxisOrder.EAST_NORTH);

            final StringBuilder builder = new StringBuilder();
            for (String axisName : axesNames) {
                builder.append(axisName).append(" ");
            }
            String axesLabel = builder.substring(0, builder.length() - 1);
            dcTranslator.handleBoundedBy(envelope, axisSwap, srsName, axesLabel, null);
        }

        private void handleCoverageDescriptions(List<CoverageGranules> coverageGranules) {
            start("wcs:CoverageDescriptions");
            for (CoverageGranules cg : coverageGranules) {
                SimpleFeatureIterator features = cg.granules.features();
                try {
                    while (features.hasNext()) {
                        SimpleFeature f = features.next();
                        String granuleId = codec.getGranuleId(cg.coverage, f.getID());
                        dcTranslator.handleCoverageDescription(
                                granuleId,
                                new GranuleCoverageInfo(cg.coverage, f, cg.dimensionDescriptors));
                    }
                } finally {
                    if (features != null) {
                        features.close();
                    }
                }
            }
            end("wcs:CoverageDescriptions");
        }

        private int getGranuleCount(List<CoverageGranules> granules) {
            int sum = 0;
            for (CoverageGranules cg : granules) {
                sum += cg.granules.size();
            }

            return sum;
        }

        private List<CoverageInfo> getCoverages(DescribeEOCoverageSetType dcs) {
            List<CoverageInfo> results = new ArrayList<CoverageInfo>();
            for (String id : dcs.getEoId()) {
                CoverageInfo ci = codec.getDatasetCoverage(id);
                if (ci == null) {
                    throw new IllegalArgumentException(
                            "The dataset id is invalid, should have been checked earlier?");
                }
                results.add(ci);
            }

            return results;
        }

        private List<CoverageGranules> getCoverageGranules(
                DescribeEOCoverageSetType dcs, List<CoverageInfo> coverages) {
            List<CoverageGranules> results = new ArrayList<CoverageGranules>();
            for (CoverageInfo ci : coverages) {
                GranuleSource source = null;
                try {
                    StructuredGridCoverage2DReader reader =
                            (StructuredGridCoverage2DReader)
                                    ci.getGridCoverageReader(null, GeoTools.getDefaultHints());
                    String name = codec.getCoverageName(ci);
                    source = reader.getGranules(name, true);

                    Query q = buildQueryFromDimensionTrims(dcs, reader, name);
                    SimpleFeatureCollection collection = source.getGranules(q);

                    // only report in output coverages that have at least one matched granule
                    if (!collection.isEmpty()) {
                        List<DimensionDescriptor> descriptors =
                                getActiveDimensionDescriptor(ci, reader, name);
                        CoverageGranules granules =
                                new CoverageGranules(ci, name, reader, collection, descriptors);
                        results.add(granules);
                    }
                } catch (IOException e) {
                    throw new WCS20Exception(
                            "Failed to load the coverage granules for covearge "
                                    + ci.prefixedName(),
                            e);
                } finally {
                    try {
                        if (source != null) {
                            source.dispose();
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.FINE, "Failed to dispose granule source", e);
                    }
                }
            }
            return results;
        }

        private List<DimensionDescriptor> getActiveDimensionDescriptor(
                CoverageInfo ci, StructuredGridCoverage2DReader reader, String name)
                throws IOException {
            // map the source descriptors for easy retrieval
            Map<String, DimensionDescriptor> sourceDescriptors =
                    new HashMap<String, DimensionDescriptor>();
            for (DimensionDescriptor dimensionDescriptor : reader.getDimensionDescriptors(name)) {
                sourceDescriptors.put(
                        dimensionDescriptor.getName().toUpperCase(), dimensionDescriptor);
            }
            // select only those that have been activated vai the GeoServer GUI
            List<DimensionDescriptor> enabledDescriptors = new ArrayList<DimensionDescriptor>();
            for (Entry<String, Serializable> entry : ci.getMetadata().entrySet()) {
                if (entry.getValue() instanceof DimensionInfo) {
                    DimensionInfo di = (DimensionInfo) entry.getValue();
                    if (di.isEnabled()) {
                        String dimensionName = entry.getKey();
                        if (dimensionName.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                            dimensionName =
                                    dimensionName.substring(
                                            ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                        }
                        DimensionDescriptor selected =
                                sourceDescriptors.get(dimensionName.toUpperCase());
                        if (selected != null) {
                            enabledDescriptors.add(selected);
                        }
                    }
                }
            }

            return enabledDescriptors;
        }

        private Query buildQueryFromDimensionTrims(
                DescribeEOCoverageSetType dcs,
                StructuredGridCoverage2DReader reader,
                String coverageName)
                throws IOException {
            // no selection, get all
            if (dcs.getDimensionTrim() == null || dcs.getDimensionTrim().size() == 0) {
                return Query.ALL;
            }

            // find out what the selection is about
            DateRange timeRange = null;
            NumberRange<Double> lonRange = null;
            NumberRange<Double> latRange = null;
            for (DimensionTrimType trim : dcs.getDimensionTrim()) {
                String name = trim.getDimension();
                if ("Long".equals(name)) {
                    if (lonRange != null) {
                        throw new WCS20Exception(
                                "Long trim specified more than once",
                                OWSExceptionCode.InvalidParameterValue,
                                "subset");
                    }
                    lonRange = parseNumberRange(trim);
                } else if ("Lat".equals(name)) {
                    if (latRange != null) {
                        throw new WCS20Exception(
                                "Lat trim specified more than once",
                                OWSExceptionCode.InvalidParameterValue,
                                "subset");
                    }
                    latRange = parseNumberRange(trim);
                } else if ("phenomenonTime".equals(name)) {
                    if (timeRange != null) {
                        throw new WCS20Exception(
                                "phenomenonTime trim specified more than once",
                                OWSExceptionCode.InvalidParameterValue,
                                "subset");
                    }
                    timeRange = parseDateRange(trim);
                } else {
                    throw new WCS20Exception(
                            "Invalid dimension name "
                                    + name
                                    + ", the only valid values "
                                    + "by WCS EO spec are Long, Lat and phenomenonTime",
                            OWSExceptionCode.InvalidParameterValue,
                            "subset");
                }
            }

            // check the desired containment
            boolean overlaps;
            String containment = dcs.getContainmentType();
            if (containment == null || "overlaps".equals(containment)) {
                overlaps = true;
            } else if ("contains".equals(containment)) {
                overlaps = false;
            } else {
                throw new WCS20Exception(
                        "Invalid containment value "
                                + containment
                                + ", the only valid values by WCS EO spec are contains and overlaps",
                        OWSExceptionCode.InvalidParameterValue,
                        "containment");
            }

            // spatial subset
            FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
            Filter filter = null;
            if (lonRange != null || latRange != null) {
                try {
                    // we are going to intersect the trims with the original envelope
                    // since the trims can only be expressed in wgs84 we need to reproject back and
                    // forth
                    ReferencedEnvelope original =
                            new ReferencedEnvelope(reader.getOriginalEnvelope())
                                    .transform(DefaultGeographicCRS.WGS84, true);
                    if (lonRange != null) {
                        ReferencedEnvelope lonTrim =
                                new ReferencedEnvelope(
                                        lonRange.getMinimum(),
                                        lonRange.getMaximum(),
                                        -90,
                                        90,
                                        DefaultGeographicCRS.WGS84);
                        original =
                                new ReferencedEnvelope(
                                        original.intersection(lonTrim), DefaultGeographicCRS.WGS84);
                    }
                    if (latRange != null) {
                        ReferencedEnvelope latTrim =
                                new ReferencedEnvelope(
                                        -180,
                                        180,
                                        latRange.getMinimum(),
                                        latRange.getMaximum(),
                                        DefaultGeographicCRS.WGS84);
                        original =
                                new ReferencedEnvelope(
                                        original.intersection(latTrim), DefaultGeographicCRS.WGS84);
                    }
                    if (original.isEmpty()) {
                        filter = Filter.EXCLUDE;
                    } else {
                        Polygon llPolygon = JTS.toGeometry(original);
                        GeometryDescriptor geom =
                                reader.getGranules(coverageName, true)
                                        .getSchema()
                                        .getGeometryDescriptor();
                        PropertyName geometryProperty = ff.property(geom.getLocalName());
                        Geometry nativeCRSPolygon =
                                JTS.transform(
                                        llPolygon,
                                        CRS.findMathTransform(
                                                DefaultGeographicCRS.WGS84,
                                                reader.getCoordinateReferenceSystem()));
                        Literal polygonLiteral = ff.literal(nativeCRSPolygon);
                        if (overlaps) {
                            filter = ff.intersects(geometryProperty, polygonLiteral);
                        } else {
                            filter = ff.within(geometryProperty, polygonLiteral);
                        }
                    }
                } catch (Exception e) {
                    throw new WCS20Exception(
                            "Failed to translate the spatial trim into a native filter", e);
                }
            }

            // temporal subset
            if (timeRange != null && filter != Filter.EXCLUDE) {
                DimensionDescriptor timeDescriptor =
                        WCSDimensionsHelper.getDimensionDescriptor(reader, coverageName, "TIME");
                String start = timeDescriptor.getStartAttribute();
                String end = timeDescriptor.getEndAttribute();

                Filter timeFilter;
                if (end == null) {
                    // single value time
                    timeFilter =
                            ff.between(
                                    ff.property(start),
                                    ff.literal(timeRange.getMinValue()),
                                    ff.literal(timeRange.getMaxValue()));
                } else {
                    // range value, we need to account for containment then
                    if (overlaps) {
                        Filter f1 =
                                ff.lessOrEqual(
                                        ff.property(start), ff.literal(timeRange.getMaxValue()));
                        Filter f2 =
                                ff.greaterOrEqual(
                                        ff.property(end), ff.literal(timeRange.getMinValue()));
                        timeFilter = ff.and(Arrays.asList(f1, f2));
                    } else {
                        Filter f1 =
                                ff.greaterOrEqual(
                                        ff.property(start), ff.literal(timeRange.getMinValue()));
                        Filter f2 =
                                ff.lessOrEqual(
                                        ff.property(end), ff.literal(timeRange.getMaxValue()));
                        timeFilter = ff.and(Arrays.asList(f1, f2));
                    }
                }

                if (filter == null) {
                    filter = timeFilter;
                } else {
                    filter = ff.and(filter, timeFilter);
                }
            }

            return new Query(null, filter);
        }

        private DateRange parseDateRange(DimensionTrimType trim) {
            DatatypeConverterImpl xmlTimeConverter = DatatypeConverterImpl.getInstance();
            try {
                // Use the lenient parameter
                final Date low = xmlTimeConverter.parseDateTime(trim.getTrimLow(), true).getTime();
                final Date high =
                        xmlTimeConverter.parseDateTime(trim.getTrimHigh(), true).getTime();

                // low > high???
                if (low.compareTo(high) > 0) {
                    throw new WCS20Exception(
                            "Low greater than High in trim for dimension: " + trim.getDimension(),
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                            "dimensionTrim");
                }

                return new DateRange(low, high);
            } catch (IllegalArgumentException e) {
                throw new WCS20Exception(
                        "Invalid date value",
                        OWSExceptionCode.InvalidParameterValue,
                        "dimensionTrim",
                        e);
            }
        }

        private NumberRange<Double> parseNumberRange(DimensionTrimType trim) {
            try {
                double low = Double.parseDouble(trim.getTrimLow());
                double high = Double.parseDouble(trim.getTrimHigh());

                // low > high???
                if (low > high) {
                    throw new WCS20Exception(
                            "Low greater than High in trim for dimension: " + trim.getDimension(),
                            WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                            "dimensionTrim");
                }

                return new NumberRange<Double>(Double.class, low, high);
            } catch (NumberFormatException e) {
                throw new WCS20Exception(
                        "Invalid numeric value",
                        OWSExceptionCode.InvalidParameterValue,
                        "dimensionTrim",
                        e);
            }
        }

        Attributes atts(String... atts) {
            AttributesImpl attributes = new AttributesImpl();
            for (int i = 0; i < atts.length; i += 2) {
                attributes.addAttribute(null, atts[i], atts[i], null, atts[i + 1]);
            }
            return attributes;
        }
    }

    static class CoverageGranules {
        CoverageInfo coverage;

        StructuredGridCoverage2DReader reader;

        SimpleFeatureCollection granules;

        private String name;

        private List<DimensionDescriptor> dimensionDescriptors;

        public CoverageGranules(
                CoverageInfo coverage,
                String name,
                StructuredGridCoverage2DReader reader,
                SimpleFeatureCollection granules,
                List<DimensionDescriptor> dimensionDescriptors) {
            this.coverage = coverage;
            this.name = name;
            this.reader = reader;
            this.granules = granules;
            this.dimensionDescriptors = dimensionDescriptors;
        }
    }
}
