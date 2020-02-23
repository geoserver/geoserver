/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import static org.geoserver.ows.util.ResponseUtils.appendPath;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import net.opengis.wcs10.DescribeCoverageType;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.wcs.WCSInfo;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.util.ResponseUtils;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the job of encoding a WCS
 * 1.0.0 DescribeCoverage document.
 *
 * @author Andrea Aime, TOPP
 * @author Alessio Fabiani, GeoSolutions
 */
public class Wcs10DescribeCoverageTransformer extends TransformerBase {
    private static final Logger LOGGER =
            Logging.getLogger(Wcs10DescribeCoverageTransformer.class.getPackage().getName());

    private static final String WCS_URI = "http://www.opengis.net/wcs";

    private static final String XSI_PREFIX = "xsi";

    private static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    private static final Map<String, String> METHOD_NAME_MAP = new HashMap<String, String>();

    private final boolean skipMisconfigured;

    static {
        METHOD_NAME_MAP.put("nearest neighbor", "nearest");
        METHOD_NAME_MAP.put("bilinear", "linear");
        METHOD_NAME_MAP.put("bicubic", "cubic");
    }

    private Catalog catalog;

    /** Creates a new WFSCapsTransformer object. */
    public Wcs10DescribeCoverageTransformer(WCSInfo wcs, Catalog catalog) {
        super();
        this.catalog = catalog;
        this.skipMisconfigured =
                ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                        wcs.getGeoServer().getGlobal().getResourceErrorHandling());
        setNamespaceDeclarationEnabled(false);
    }

    public Translator createTranslator(ContentHandler handler) {
        return new WCS100DescribeCoverageTranslator(handler);
    }

    private class WCS100DescribeCoverageTranslator extends TranslatorSupport {
        // the path that does contain the GeoServer internal XML schemas
        public static final String SCHEMAS = "schemas";

        private DescribeCoverageType request;

        /** Creates a new WFSCapsTranslator object. */
        public WCS100DescribeCoverageTranslator(ContentHandler handler) {
            super(handler, null, null);
        }

        /**
         * Encode the object.
         *
         * @param o The Object to encode.
         * @throws IllegalArgumentException if the Object is not encodeable.
         */
        public void encode(Object o) throws IllegalArgumentException {
            // try {
            if (!(o instanceof DescribeCoverageType)) {
                throw new IllegalArgumentException(
                        new StringBuilder("Not a GetCapabilitiesType: ").append(o).toString());
            }

            this.request = (DescribeCoverageType) o;

            final AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute("", "xmlns:wcs", "xmlns:wcs", "", WCS_URI);
            attributes.addAttribute(
                    "", "xmlns:xlink", "xmlns:xlink", "", "http://www.w3.org/1999/xlink");
            attributes.addAttribute("", "xmlns:ogc", "xmlns:ogc", "", "http://www.opengis.net/ogc");
            attributes.addAttribute(
                    "", "xmlns:ows", "xmlns:ows", "", "http://www.opengis.net/ows/1.1");
            attributes.addAttribute("", "xmlns:gml", "xmlns:gml", "", "http://www.opengis.net/gml");

            final String prefixDef = new StringBuilder("xmlns:").append(XSI_PREFIX).toString();
            attributes.addAttribute("", prefixDef, prefixDef, "", XSI_URI);

            final String locationAtt =
                    new StringBuilder(XSI_PREFIX).append(":schemaLocation").toString();

            // proxifiedBaseUrl = RequestUtils.proxifiedBaseURL(request.getBaseUrl(),
            // wcs.getGeoServer().getGlobal().getProxyBaseUrl());
            // final String locationDef = WCS_URI + " " + proxifiedBaseUrl +
            // "schemas/wcs/1.0.0/describeCoverage.xsd";
            final String locationDef =
                    WCS_URI
                            + " "
                            + buildURL(
                                    request.getBaseUrl(),
                                    appendPath(SCHEMAS, "wcs/1.0.0/describeCoverage.xsd"),
                                    null,
                                    URLType.RESOURCE);
            attributes.addAttribute("", locationAtt, locationAtt, "", locationDef);

            attributes.addAttribute("", "version", "version", "", "1.0.0");

            start("wcs:CoverageDescription", attributes);

            List<CoverageInfo> coverages;
            final boolean skipMisconfiguredThisTime;
            if (request.getCoverage() == null || request.getCoverage().size() == 0) {
                skipMisconfiguredThisTime = skipMisconfigured;
                coverages = catalog.getCoverages();
            } else {
                skipMisconfiguredThisTime =
                        false; // NEVER skip layers when the user requested specific ones
                coverages = new ArrayList<CoverageInfo>();
                for (Iterator it = request.getCoverage().iterator(); it.hasNext(); ) {
                    String coverageId = (String) it.next();
                    // check the coverage is known
                    LayerInfo layer = catalog.getLayerByName(coverageId);
                    if (layer == null || layer.getType() != PublishedType.RASTER) {
                        throw new WcsException(
                                "Could not find the specified coverage: " + coverageId,
                                WcsExceptionCode.InvalidParameterValue,
                                "coverage");
                    }
                    coverages.add(catalog.getCoverageByName(coverageId));
                }
            }
            for (Iterator it = coverages.iterator(); it.hasNext(); ) {
                CoverageInfo coverage = (CoverageInfo) it.next();
                try {
                    mark();
                    handleCoverageOffering(coverage);
                    commit();
                } catch (Exception e) {
                    if (skipMisconfiguredThisTime) {
                        reset();
                    } else {
                        throw new RuntimeException(
                                "Unexpected error occurred during describe coverage xml encoding",
                                e);
                    }
                }
            }
            end("wcs:CoverageDescription");
        }

        private void handleCoverageOffering(CoverageInfo ci) throws Exception {
            start("wcs:CoverageOffering");
            for (MetadataLinkInfo mdl : ci.getMetadataLinks()) handleMetadataLink(mdl, "simple");
            element("wcs:description", ci.getDescription());
            element("wcs:name", ci.prefixedName());
            element("wcs:label", ci.getTitle());
            handleLonLatEnvelope(ci, ci.getLatLonBoundingBox());
            handleKeywords(ci.getKeywords());

            handleDomain(ci);
            handleRange(ci);

            handleSupportedCRSs(ci);
            handleSupportedFormats(ci);
            handleSupportedInterpolations(ci);
            end("wcs:CoverageOffering");
        }

        private void handleMetadataLink(MetadataLinkInfo mdl, String linkType) {
            AttributesImpl attributes = new AttributesImpl();

            if (StringUtils.isNotBlank(mdl.getAbout())) {
                attributes.addAttribute("", "about", "about", "", mdl.getAbout());
            }

            if (StringUtils.isNotBlank(mdl.getMetadataType())) {
                attributes.addAttribute(
                        "", "metadataType", "metadataType", "", mdl.getMetadataType());
            }

            if ((linkType != null) && (linkType != "")) {
                attributes.addAttribute("", "xlink:type", "xlink:type", "", linkType);
            }

            if (StringUtils.isNotBlank(mdl.getContent())) {
                attributes.addAttribute(
                        "",
                        "xlink:href",
                        "xlink:href",
                        "",
                        ResponseUtils.proxifyMetadataLink(mdl, request.getBaseUrl()));
            }

            if (attributes.getLength() > 0) {
                element("wcs:metadataLink", null, attributes);
            }
        }

        /** */
        private void handleLonLatEnvelope(CoverageInfo ci, ReferencedEnvelope referencedEnvelope)
                throws IOException {

            CoverageStoreInfo csinfo = ci.getStore();

            if (csinfo == null)
                throw new WcsException(
                        "Unable to acquire coverage store resource for coverage: " + ci.getName());

            GridCoverage2DReader reader = null;
            try {
                reader =
                        (GridCoverage2DReader)
                                ci.getGridCoverageReader(null, GeoTools.getDefaultHints());
            } catch (IOException e) {
                LOGGER.severe(
                        "Unable to acquire a reader for this coverage with format: "
                                + csinfo.getFormat().getName());
            }

            if (reader == null)
                throw new WcsException(
                        "Unable to acquire a reader for this coverage with format: "
                                + csinfo.getFormat().getName());

            if (referencedEnvelope != null) {
                AttributesImpl attributes = new AttributesImpl();
                attributes.addAttribute(
                        "",
                        "srsName",
                        "srsName",
                        "", /* "WGS84(DD)" */
                        "urn:ogc:def:crs:OGC:1.3:CRS84");

                start("wcs:lonLatEnvelope", attributes);

                final String minCP =
                        referencedEnvelope.getMinX() + " " + referencedEnvelope.getMinY();
                final String maxCP =
                        referencedEnvelope.getMaxX() + " " + referencedEnvelope.getMaxY();
                element("gml:pos", minCP);
                element("gml:pos", maxCP);

                // are we going to report time?
                DimensionInfo timeInfo =
                        ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
                if (timeInfo != null && timeInfo.isEnabled()) {
                    ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
                    SimpleDateFormat format = dimensions.getTimeFormat();
                    element("gml:timePosition", format.format(dimensions.getMinTime()));
                    element("gml:timePosition", format.format(dimensions.getMaxTime()));
                }

                end("wcs:lonLatEnvelope");
            }
        }

        /** */
        private void handleKeywords(List kwords) {
            start("wcs:keywords");

            if (kwords != null) {
                for (Iterator it = kwords.iterator(); it.hasNext(); ) {
                    element("wcs:keyword", it.next().toString());
                }
            }

            end("wcs:keywords");
        }

        private void handleDomain(CoverageInfo ci) throws Exception {
            CoverageStoreInfo csinfo = ci.getStore();

            if (csinfo == null)
                throw new WcsException(
                        "Unable to acquire coverage store resource for coverage: " + ci.getName());

            GridCoverage2DReader reader = null;
            try {
                reader =
                        (GridCoverage2DReader)
                                ci.getGridCoverageReader(null, GeoTools.getDefaultHints());
            } catch (IOException e) {
                LOGGER.severe(
                        "Unable to acquire a reader for this coverage with format: "
                                + csinfo.getFormat().getName());
            }

            if (reader == null) {
                throw new WcsException(
                        "Unable to acquire a reader for this coverage with format: "
                                + csinfo.getFormat().getName());
            }

            DimensionInfo timeInfo = ci.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
            ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
            start("wcs:domainSet");
            start("wcs:spatialDomain");
            handleBoundingBox(ci.getSRS(), ci.getNativeBoundingBox(), timeInfo, dimensions);
            handleGrid(ci);
            end("wcs:spatialDomain");
            if (timeInfo != null && timeInfo.isEnabled()) {
                handleTemporalDomain(ci, timeInfo, dimensions);
            }
            end("wcs:domainSet");
        }

        /** */
        private void handleBoundingBox(
                String srsName,
                ReferencedEnvelope referencedEnvelope,
                DimensionInfo timeInfo,
                ReaderDimensionsAccessor dimensions)
                throws IOException {
            if (referencedEnvelope != null) {
                AttributesImpl attributes = new AttributesImpl();
                attributes.addAttribute("", "srsName", "srsName", "", srsName);

                final String minCP =
                        referencedEnvelope.getMinX() + " " + referencedEnvelope.getMinY();
                final String maxCP =
                        referencedEnvelope.getMaxX() + " " + referencedEnvelope.getMaxY();

                String minTime = null;
                String maxTime = null;
                if (timeInfo != null && timeInfo.isEnabled()) {
                    SimpleDateFormat timeFormat = dimensions.getTimeFormat();
                    minTime = timeFormat.format(dimensions.getMinTime());
                    maxTime = timeFormat.format(dimensions.getMaxTime());
                }

                if (minTime != null && maxTime != null) {
                    start("gml:EnvelopeWithTimePeriod", attributes);
                    element("gml:pos", minCP);
                    element("gml:pos", maxCP);

                    element("gml:timePosition", minTime);
                    element("gml:timePosition", maxTime);
                    end("gml:EnvelopeWithTimePeriod");
                } else {
                    start("gml:Envelope", attributes);
                    element("gml:pos", minCP);
                    element("gml:pos", maxCP);
                    end("gml:Envelope");
                }
            }
        }

        /** */
        private void handleTemporalDomain(
                CoverageInfo ci, DimensionInfo timeInfo, ReaderDimensionsAccessor dimensions)
                throws IOException {
            SimpleDateFormat timeFormat = dimensions.getTimeFormat();
            start("wcs:temporalDomain");
            if (timeInfo.getPresentation() == DimensionPresentation.LIST) {
                for (Object item : dimensions.getTimeDomain()) {
                    if (item instanceof Date) {
                        element("gml:timePosition", timeFormat.format((Date) item));
                    } else {
                        DateRange range = (DateRange) item;
                        start("wcs:timePeriod");
                        String minTime = timeFormat.format(range.getMinValue());
                        String maxTime = timeFormat.format(range.getMaxValue());
                        element("wcs:beginPosition", minTime);
                        element("wcs:endPosition", maxTime);
                        end("wcs:timePeriod");
                    }
                }
            } else {
                String minTime = timeFormat.format(dimensions.getMinTime());
                String maxTime = timeFormat.format(dimensions.getMaxTime());
                start("wcs:timePeriod");
                element("wcs:beginPosition", minTime);
                element("wcs:endPosition", maxTime);
                if (timeInfo.getPresentation() == DimensionPresentation.DISCRETE_INTERVAL) {
                    BigDecimal resolution = timeInfo.getResolution();
                    if (resolution == null) {
                        resolution =
                                new BigDecimal(
                                        dimensions.getMaxTime().getTime()
                                                - dimensions.getMinTime().getTime());
                    }
                    // this will format the time period properly
                    element(
                            "wcs:timeResolution",
                            new DefaultPeriodDuration(resolution.longValue()).toString());
                }
                end("wcs:timePeriod");
            }
            end("wcs:temporalDomain");
        }

        /** */
        private void handleGrid(CoverageInfo ci) throws Exception {
            final GridGeometry originalGrid = ci.getGrid();
            final GridEnvelope gridRange = originalGrid.getGridRange();
            final AffineTransform2D gridToCRS = (AffineTransform2D) originalGrid.getGridToCRS();
            final int gridDimension = gridToCRS.getSourceDimensions();

            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(
                    "", "dimension", "dimension", "", String.valueOf(gridDimension));
            attributes.addAttribute("", "srsName", "srsName", "", ci.getSRS());

            // RectifiedGrid
            start("gml:RectifiedGrid", attributes);

            // Grid Envelope
            String lowers = "";
            String uppers = "";
            for (int r = 0; r < gridDimension; r++) {
                if (gridToCRS.getSourceDimensions() > r) {
                    lowers += (gridRange.getLow(r) + " ");
                    uppers += (gridRange.getHigh(r) + " ");
                } else {
                    lowers += (0 + " ");
                    uppers += (0 + " ");
                }
            }

            start("gml:limits");
            start("gml:GridEnvelope");
            element("gml:low", lowers.trim());
            element("gml:high", uppers.trim());
            end("gml:GridEnvelope");
            end("gml:limits");

            // Grid Axes
            for (int dn = 0; dn < ci.getCRS().getCoordinateSystem().getDimension(); dn++) {
                String axisName = ci.getCRS().getCoordinateSystem().getAxis(dn).getAbbreviation();
                axisName = axisName.toLowerCase().startsWith("lon") ? "x" : axisName;
                axisName = axisName.toLowerCase().startsWith("lat") ? "y" : axisName;
                element("gml:axisName", axisName);
            }

            // Grid Origins
            final StringBuilder origins = new StringBuilder();
            origins.append(gridToCRS.getTranslateX()).append(" ").append(gridToCRS.getTranslateY());
            start("gml:origin");
            element("gml:pos", origins.toString());
            end("gml:origin");

            // Grid Offsets
            final StringBuilder offsetX = new StringBuilder();
            offsetX.append(gridToCRS.getScaleX()).append(" ").append(gridToCRS.getShearX());
            element("gml:offsetVector", offsetX.toString());
            final StringBuilder offsetY = new StringBuilder();
            offsetY.append(gridToCRS.getShearY()).append(" ").append(gridToCRS.getScaleY());
            element("gml:offsetVector", offsetY.toString());

            end("gml:RectifiedGrid");
        }

        /** */
        private void handleRange(CoverageInfo ci) throws IOException {
            // rangeSet
            start("wcs:rangeSet");
            start("wcs:RangeSet");
            element("wcs:name", ci.getName());
            element("wcs:label", ci.getTitle());

            start("wcs:axisDescription");
            start("wcs:AxisDescription");

            //
            // STANDARD BANDS
            //
            int numSampleDimensions = ci.getDimensions().size();

            element("wcs:name", "Band");
            element("wcs:label", "Band");
            start("wcs:values");
            if (numSampleDimensions > 1) {
                start("wcs:interval");
                element("wcs:min", "1");
                element("wcs:max", String.valueOf(numSampleDimensions));
                end("wcs:interval");
            } else {
                element("wcs:singleValue", "1");
            }
            end("wcs:values");

            end("wcs:AxisDescription");
            end("wcs:axisDescription");

            //
            // ELEVATION
            //
            // now get possible elevation
            DimensionInfo elevationInfo =
                    ci.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
            if (elevationInfo != null && elevationInfo.isEnabled()) {
                GridCoverage2DReader reader = null;
                try {
                    reader =
                            (GridCoverage2DReader)
                                    ci.getGridCoverageReader(null, GeoTools.getDefaultHints());
                } catch (IOException e) {
                    LOGGER.severe(
                            "Unable to acquire a reader for this coverage with format: "
                                    + ci.getStore().getFormat().getName());
                }
                if (reader == null) {
                    throw new WcsException(
                            "Unable to acquire a reader for this coverage with format: "
                                    + ci.getStore().getFormat().getName());
                }

                ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
                if (dimensions.hasElevation()) {
                    // we can only report values one at a time here, there is no interval concept
                    start("wcs:axisDescription");
                    start("wcs:AxisDescription");
                    element("wcs:name", "ELEVATION");
                    element("wcs:label", "ELEVATION");
                    start("wcs:values");

                    TreeSet<Object> rawElevations = dimensions.getElevationDomain();
                    // we cannot expose ranges, so if we find them, we turn them into
                    // their mid point
                    TreeSet<Double> elevations = new TreeSet<Double>();
                    for (Object raw : rawElevations) {
                        if (raw instanceof Double) {
                            elevations.add((Double) raw);
                        } else {
                            NumberRange<Double> range = (NumberRange<Double>) raw;
                            double midValue = (range.getMinimum() + range.getMaximum()) / 2;
                            elevations.add(midValue);
                        }
                    }
                    for (Double elevation : elevations) {
                        element("wcs:singleValue", Double.toString(elevation));
                    }
                    element("wcs:default", Double.toString(elevations.first()));

                    end("wcs:values");

                    end("wcs:AxisDescription");
                    end("wcs:axisDescription");
                }
            }

            end("wcs:RangeSet");
            end("wcs:rangeSet");
        }

        /** @param ci */
        private void handleSupportedCRSs(CoverageInfo ci) throws Exception {
            Set supportedCRSs = new LinkedHashSet();
            if (ci.getRequestSRS() != null) supportedCRSs.addAll(ci.getRequestSRS());
            if (ci.getResponseSRS() != null) supportedCRSs.addAll(ci.getResponseSRS());
            start("wcs:supportedCRSs");
            for (Iterator it = supportedCRSs.iterator(); it.hasNext(); ) {
                String crsName = (String) it.next();
                CoordinateReferenceSystem crs = CRS.decode(crsName, true);
                // element("requestResponseCRSs", urnIdentifier(crs));
                element("wcs:requestResponseCRSs", CRS.lookupIdentifier(crs, false));
            }
            end("wcs:supportedCRSs");
        }

        private String urnIdentifier(final CoordinateReferenceSystem crs) throws FactoryException {
            String authorityAndCode = CRS.lookupIdentifier(crs, false);
            String code = authorityAndCode.substring(authorityAndCode.lastIndexOf(":") + 1);
            // we don't specify the version, but we still need to put a space
            // for it in the urn form, that's why we have :: before the code
            return "urn:ogc:def:crs:EPSG::" + code;
        }

        /** @param ci */
        private void handleSupportedFormats(CoverageInfo ci) throws Exception {
            final String nativeFormat =
                    (((ci.getNativeFormat() != null)
                                    && ci.getNativeFormat().equalsIgnoreCase("GEOTIFF"))
                            ? "GeoTIFF"
                            : ci.getNativeFormat());

            final AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute("", "nativeFormat", "nativeFormat", "", nativeFormat);

            // gather all the formats for this coverage
            Set<String> formats = new HashSet<String>();
            for (Iterator it = ci.getSupportedFormats().iterator(); it.hasNext(); ) {
                String format = (String) it.next();
                formats.add(format);
            }
            // sort them
            start("wcs:supportedFormats", attributes);
            List<String> sortedFormats = new ArrayList<String>(formats);
            Collections.sort(sortedFormats);
            for (String format : sortedFormats) {
                element("wcs:formats", format.equalsIgnoreCase("GEOTIFF") ? "GeoTIFF" : format);
            }
            end("wcs:supportedFormats");
        }

        /** @param ci */
        private void handleSupportedInterpolations(CoverageInfo ci) {
            if (ci.getDefaultInterpolationMethod() != null) {
                final AttributesImpl attributes = new AttributesImpl();
                attributes.addAttribute(
                        "", "default", "default", "", ci.getDefaultInterpolationMethod());

                start("wcs:supportedInterpolations", attributes);
            } else {
                start("wcs:supportedInterpolations");
            }

            for (Iterator it = ci.getInterpolationMethods().iterator(); it.hasNext(); ) {
                String method = (String) it.next();
                if (method != null) element("wcs:interpolationMethod", method);
            }
            end("wcs:supportedInterpolations");
        }
    }
}
