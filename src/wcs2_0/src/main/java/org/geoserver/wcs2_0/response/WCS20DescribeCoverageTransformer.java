/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.opengis.wcs20.DescribeCoverageType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs2_0.GetCoverage;
import org.geoserver.wcs2_0.WCS20Const;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geoserver.wcs2_0.util.RequestUtils;
import org.geoserver.wcs2_0.util.WCS20DescribeCoverageExtension;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.util.logging.Logging;
import org.geotools.wcs.v2_0.WCS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.vfny.geoserver.util.ResponseUtils;
import org.vfny.geoserver.wcs.WcsException;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the job of encoding a WCS
 * 2.0.1 DescribeCoverage document.
 *
 * @author Emanuele Tajariol (etj) - GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public class WCS20DescribeCoverageTransformer extends GMLTransformer {
    public static final Logger LOGGER =
            Logging.getLogger(WCS20DescribeCoverageTransformer.class.getPackage().getName());

    private MIMETypeMapper mimemapper;

    private Catalog catalog;

    /** Available extension points for DescribeCoverage */
    private List<WCS20DescribeCoverageExtension> wcsDescribeCoverageExtensions;

    /**
     * Boolean indicating that at least an extension point for the DescribeCoverage operation is
     * available
     */
    private boolean availableDescribeCoverageExtensions;

    /** Creates a new WFSCapsTransformer object. */
    public WCS20DescribeCoverageTransformer(
            Catalog catalog,
            EnvelopeAxesLabelsMapper envelopeDimensionsMapper,
            MIMETypeMapper mimemapper) {
        super(envelopeDimensionsMapper);
        this.catalog = catalog;
        this.mimemapper = mimemapper;
        setNamespaceDeclarationEnabled(false);
        setIndentation(2);
        this.wcsDescribeCoverageExtensions =
                GeoServerExtensions.extensions(WCS20DescribeCoverageExtension.class);
        this.availableDescribeCoverageExtensions =
                wcsDescribeCoverageExtensions != null && !wcsDescribeCoverageExtensions.isEmpty();
    }

    public WCS20DescribeCoverageTranslator createTranslator(ContentHandler handler) {
        return new WCS20DescribeCoverageTranslator(handler);
    }

    public class WCS20DescribeCoverageTranslator extends GMLTranslator {
        private DescribeCoverageType request;

        public WCS20DescribeCoverageTranslator(ContentHandler handler) {
            super(handler);
        }

        /** Encode the object. */
        @Override
        public void encode(Object o) throws IllegalArgumentException {

            if (!(o instanceof DescribeCoverageType)) {
                throw new IllegalArgumentException(
                        new StringBuffer("Not a DescribeCoverageType: ").append(o).toString());
            }

            this.request = (DescribeCoverageType) o;

            // collect coverages
            List<CoverageInfo> coverages = new ArrayList<CoverageInfo>();

            List<String> covIds = new ArrayList<String>();
            for (String encodedCoverageId : (List<String>) request.getCoverageId()) {
                String newCoverageID = encodedCoverageId;
                // Extension point for encoding the coverageId
                if (availableDescribeCoverageExtensions) {
                    for (WCS20DescribeCoverageExtension ext : wcsDescribeCoverageExtensions) {
                        newCoverageID = ext.handleCoverageId(newCoverageID);
                    }
                }
                LayerInfo layer = NCNameResourceCodec.getCoverage(catalog, newCoverageID);
                if (layer != null) {
                    coverages.add((CoverageInfo) layer.getResource());
                    covIds.add(encodedCoverageId);
                } else {
                    // if we get there there is an internal error, the coverage existence is
                    // checked before creating the transformer
                    throw new IllegalArgumentException(
                            "Failed to locate coverage "
                                    + encodedCoverageId
                                    + ", unexpected, the coverage existance has been "
                                    + "checked earlier in the request lifecycle");
                }
            }

            // register namespaces provided by extended capabilities
            NamespaceSupport namespaces = getNamespaceSupport();
            namespaces.declarePrefix("swe", "http://www.opengis.net/swe/2.0");
            namespaces.declarePrefix("wcsgs", "http://www.geoserver.org/wcsgs/2.0");
            for (WCS20CoverageMetadataProvider cp : extensions) {
                cp.registerNamespaces(namespaces);
            }

            // ok: build the response
            final AttributesImpl attributes = WCS20Const.getDefaultNamespaces();
            helper.registerNamespaces(getNamespaceSupport(), attributes);
            String location =
                    buildSchemaLocation(
                            request.getBaseUrl(),
                            WCS.NAMESPACE,
                            "http://schemas.opengis.net/wcs/2.0/wcsDescribeCoverage.xsd",
                            "http://www.geoserver.org/wcsgs/2.0",
                            buildSchemaURL(request.getBaseUrl(), "wcs/2.0/wcsgs.xsd"));
            attributes.addAttribute("", "xsi:schemaLocation", "xsi:schemaLocation", "", location);
            start("wcs:CoverageDescriptions", attributes);
            int coverageIndex = 0;
            for (CoverageInfo ci : coverages) {
                try {
                    String encodedId = NCNameResourceCodec.encode(ci);
                    CoverageInfo ciNew = ci;
                    String newCoverageID = covIds.get(coverageIndex);
                    // Extension point for encoding the coverageId
                    if (availableDescribeCoverageExtensions) {
                        for (WCS20DescribeCoverageExtension ext : wcsDescribeCoverageExtensions) {
                            newCoverageID = ext.handleEncodedId(location, newCoverageID);
                            ciNew = ext.handleCoverageInfo(covIds.get(coverageIndex), ci);
                        }
                    } else {
                        newCoverageID = encodedId;
                    }
                    handleCoverageDescription(newCoverageID, ciNew);
                    coverageIndex++;
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Unexpected error occurred during describe coverage xml encoding", e);
                }
            }
            end("wcs:CoverageDescriptions");
        }

        String buildSchemaLocation(String schemaBaseURL, String... locations) {
            for (WCS20CoverageMetadataProvider cp : extensions) {
                locations = helper.append(locations, cp.getSchemaLocations(schemaBaseURL));
            }

            return helper.buildSchemaLocation(locations);
        }

        /** @param ci */
        public void handleCoverageDescription(String encodedId, CoverageInfo ci) {

            try {
                GridCoverage2DReader reader =
                        (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
                if (reader == null) {
                    throw new WCS20Exception("Unable to read sample coverage for " + ci.getName());
                }

                // see if we have to handle time, elevation and additional dimensions
                Map<String, DimensionInfo> dimensionsMap =
                        WCSDimensionsHelper.getDimensionsFromMetadata(ci.getMetadata());
                WCSDimensionsHelper dimensionsHelper = null;
                if (dimensionsMap != null && !dimensionsMap.isEmpty()) {
                    dimensionsHelper =
                            WCSDimensionsHelper.getWCSDimensionsHelper(encodedId, ci, reader);
                }

                // get the crs and look for an EPSG code
                final CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
                List<String> axesNames =
                        envelopeDimensionsMapper.getAxesNames(reader.getOriginalEnvelope(), true);

                // lookup EPSG code
                Integer EPSGCode = null;
                try {
                    EPSGCode = CRS.lookupEpsgCode(crs, false);
                } catch (FactoryException e) {
                    throw new IllegalStateException(
                            "Unable to lookup epsg code for this CRS:" + crs, e);
                }
                if (EPSGCode == null) {
                    throw new IllegalStateException(
                            "Unable to lookup epsg code for this CRS:" + crs);
                }
                final String srsName = GetCoverage.SRS_STARTER + EPSGCode;
                // handle axes swap for geographic crs
                final boolean axisSwap =
                        !CRS.getAxisOrder(CRS.decode(srsName)).equals(AxisOrder.EAST_NORTH);

                // encoding ID of the coverage
                final AttributesImpl coverageAttributes = new AttributesImpl();
                coverageAttributes.addAttribute("", "gml:id", "gml:id", "", encodedId);

                // starting encoding
                start("wcs:CoverageDescription", coverageAttributes);
                elementSafe("gml:description", ci.getDescription());
                elementSafe("gml:name", ci.getTitle());

                // handle domain
                final StringBuilder builder = new StringBuilder();
                for (String axisName : axesNames) {
                    builder.append(axisName).append(" ");
                }
                if (dimensionsHelper != null && dimensionsHelper.getElevationDimension() != null) {
                    builder.append("elevation ");
                }

                if (dimensionsHelper != null && dimensionsHelper.getTimeDimension() != null) {
                    builder.append("time ");
                }
                String axesLabel = builder.substring(0, builder.length() - 1);
                GeneralEnvelope envelope = reader.getOriginalEnvelope();
                handleBoundedBy(envelope, axisSwap, srsName, axesLabel, dimensionsHelper);

                // coverage id
                element("wcs:CoverageId", encodedId);

                // handle coverage function
                handleCoverageFunction((GridEnvelope2D) reader.getOriginalGridRange(), axisSwap);

                // metadata
                handleMetadata(ci, dimensionsHelper);

                // handle domain
                builder.setLength(0);
                axesNames =
                        envelopeDimensionsMapper.getAxesNames(reader.getOriginalEnvelope(), false);
                for (String axisName : axesNames) {
                    builder.append(axisName).append(" ");
                }
                axesLabel = builder.substring(0, builder.length() - 1);
                GridGeometry2D gg =
                        new GridGeometry2D(
                                reader.getOriginalGridRange(),
                                reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER),
                                reader.getCoordinateReferenceSystem());
                handleDomainSet(gg, 2, encodedId, srsName, axisSwap);

                // handle rangetype
                handleRangeType(ci.getDimensions());

                // service parameters
                handleServiceParameters(ci);

                end("wcs:CoverageDescription");
            } catch (Exception e) {
                throw new WcsException(e);
            }
        }

        private GridSampleDimension[] getSampleDimensions(GridCoverage2DReader reader)
                throws Exception {
            GridCoverage2D coverage = null;
            try {
                coverage = RequestUtils.readSampleGridCoverage(reader);
                return coverage.getSampleDimensions();
            } finally {
                if (coverage != null) {
                    CoverageCleanerCallback.addCoverages(coverage);
                }
            }
        }

        @Override
        protected void handleAdditionalMetadata(Object context) {
            if (context instanceof CoverageInfo) {
                CoverageInfo ci = (CoverageInfo) context;
                List<KeywordInfo> keywords = ci.getKeywords();
                if (keywords != null && !keywords.isEmpty()) {
                    start("ows:Keywords");
                    keywords.forEach(kw -> element("ows:Keyword", kw.getValue()));
                    end("ows:Keywords");
                }
                ci.getMetadataLinks().forEach(this::handleMetadataLink);
            }
        }

        private void handleMetadataLink(MetadataLinkInfo mdl) {
            if (isNotBlank(mdl.getContent())) {
                String url = ResponseUtils.proxifyMetadataLink(mdl, request.getBaseUrl());
                AttributesImpl attributes = new AttributesImpl();
                if (isNotBlank(mdl.getAbout())) {
                    attributes.addAttribute("", "about", "about", "", mdl.getAbout());
                }
                attributes.addAttribute("", "xlink:type", "xlink:type", "", "simple");
                attributes.addAttribute("", "xlink:href", "xlink:href", "", url);
                element("ows:Metadata", null, attributes);
            }
        }

        private void handleServiceParameters(CoverageInfo ci) throws IOException {
            start("wcs:ServiceParameters");
            element("wcs:CoverageSubtype", "RectifiedGridCoverage");

            String mapNativeFormat = mimemapper.mapNativeFormat(ci);
            element("wcs:nativeFormat", mapNativeFormat);
            end("wcs:ServiceParameters");
        }

        /**
         * Encodes the RangeType as per the {@link DescribeCoverageType}WCS spec of the provided
         * {@link GridCoverage2D}
         *
         * <p>e.g.:
         *
         * <pre>{@code
         * <gmlcov:rangeType>
         *    <swe:DataRecord>
         *        <swe:field name="singleBand">
         *           <swe:Quantity definition="http://www.opengis.net/def/property/OGC/0/Radiance">
         *               <gml:description>Panchromatic Channel</gml:description>
         *               <gml:name>single band</gml:name>
         *               <swe:uom code="W/cm2"/>
         *               <swe:constraint>
         *                   <swe:AllowedValues>
         *                       <swe:interval>0 255</swe:interval>
         *                       <swe:significantFigures>3</swe:significantFigures>
         *                   </swe:AllowedValues>
         *               </swe:constraint>
         *           </swe:Quantity>
         *        </swe:field>
         *    </swe:DataRecord>
         * </gmlcov:rangeType>
         * }</pre>
         */
        public void handleRangeType(final List<CoverageDimensionInfo> bands) {
            start("gmlcov:rangeType");
            start("swe:DataRecord");

            // handle bands
            for (CoverageDimensionInfo sd : bands) {
                final AttributesImpl fieldAttr = new AttributesImpl();
                fieldAttr.addAttribute("", "name", "name", "", sd.getName());
                start("swe:field", fieldAttr);

                start("swe:Quantity");

                // Description
                start("swe:description");
                chars(sd.getName()); // TODO can we make up something better??
                end("swe:description");

                // nil values
                List<Double> nullValues = sd.getNullValues();
                if (nullValues != null && !nullValues.isEmpty()) {
                    final int size = nullValues.size();
                    double[] noDataValues = new double[size];
                    for (int i = 0; i < size; i++) {
                        noDataValues[i] = nullValues.get(i);
                    }
                    handleSampleDimensionNilValues(null, noDataValues);
                }

                // UoM
                final AttributesImpl uomAttr = new AttributesImpl();
                final String unit = sd.getUnit();
                uomAttr.addAttribute("", "code", "code", "", unit == null ? "W.m-2.Sr-1" : unit);
                start("swe:uom", uomAttr);
                end("swe:uom");

                // constraint on values
                start("swe:constraint");
                start("swe:AllowedValues");
                handleSampleDimensionRange(sd); // TODO make  this generic
                end("swe:AllowedValues");
                end("swe:constraint");

                end("swe:Quantity");
                end("swe:field");
            }

            end("swe:DataRecord");
            end("gmlcov:rangeType");
        }
    }
}
