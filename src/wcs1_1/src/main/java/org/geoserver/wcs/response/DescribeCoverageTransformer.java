/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.opengis.wcs11.DescribeCoverageType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.kvp.GridType;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.LinearTransform;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Matrix;
import org.vfny.geoserver.util.ResponseUtils;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the job of encoding a WCS
 * 1.1.1 DescribeCoverage document.
 *
 * @author Andrea Aime, TOPP
 */
public class DescribeCoverageTransformer extends TransformerBase {
    protected static final Logger LOGGER =
            Logging.getLogger(DescribeCoverageTransformer.class.getPackage().getName());

    protected static final String WCS_URI = "http://www.opengis.net/wcs/1.1.1";

    protected static final String XSI_PREFIX = "xsi";

    protected static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    protected static final Map<String, String> METHOD_NAME_MAP = new HashMap<String, String>();

    static {
        METHOD_NAME_MAP.put("nearest neighbor", "nearest");
        METHOD_NAME_MAP.put("bilinear", "linear");
        METHOD_NAME_MAP.put("bicubic", "cubic");
    }

    protected WCSInfo wcs;

    protected Catalog catalog;

    protected CoverageResponseDelegateFinder responseFactory;

    /** Creates a new WFSCapsTransformer object. */
    public DescribeCoverageTransformer(
            WCSInfo wcs, Catalog catalog, CoverageResponseDelegateFinder responseFactory) {
        super();
        this.wcs = wcs;
        this.catalog = catalog;
        this.responseFactory = responseFactory;
        setNamespaceDeclarationEnabled(false);
    }

    public Translator createTranslator(ContentHandler handler) {
        return new WCS111DescribeCoverageTranslator(handler);
    }

    protected class WCS111DescribeCoverageTranslator extends TranslatorSupport {
        protected DescribeCoverageType request;

        protected String proxifiedBaseUrl;

        /** Creates a new WFSCapsTranslator object. */
        public WCS111DescribeCoverageTranslator(ContentHandler handler) {
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
                        new StringBuffer("Not a GetCapabilitiesType: ").append(o).toString());
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

            final String prefixDef = new StringBuffer("xmlns:").append(XSI_PREFIX).toString();
            attributes.addAttribute("", prefixDef, prefixDef, "", XSI_URI);

            final String locationAtt =
                    new StringBuffer(XSI_PREFIX).append(":schemaLocation").toString();

            final String locationDef =
                    WCS_URI
                            + " "
                            + buildSchemaURL(
                                    request.getBaseUrl(), "wcs/1.1.1/wcsDescribeCoverage.xsd");

            attributes.addAttribute("", locationAtt, locationAtt, "", locationDef);

            start("wcs:CoverageDescriptions", attributes);
            for (Iterator it = request.getIdentifier().iterator(); it.hasNext(); ) {
                String coverageId = (String) it.next();

                // check the coverage is known
                LayerInfo layer = catalog.getLayerByName(coverageId);
                if (layer == null || layer.getType() != PublishedType.RASTER) {
                    throw new WcsException(
                            "Could not find the specified coverage: " + coverageId,
                            WcsExceptionCode.InvalidParameterValue,
                            "identifiers");
                }

                CoverageInfo ci = catalog.getCoverageByName(coverageId);
                try {
                    handleCoverageDescription(ci);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Unexpected error occurred during describe coverage xml encoding", e);
                }
            }
            end("wcs:CoverageDescriptions");
        }

        protected void handleCoverageDescription(CoverageInfo ci) throws Exception {
            start("wcs:CoverageDescription");
            element("ows:Title", ci.getTitle());
            element("ows:Abstract", ci.getDescription());
            handleKeywords(ci.getKeywords());
            element("wcs:Identifier", ci.getStore().getWorkspace().getName() + ":" + ci.getName());
            handleMetadataLinks(ci.getMetadataLinks(), "simple");
            handleDomain(ci);
            handleRange(ci);
            handleSupportedCRSs(ci);
            handleSupportedFormats(ci);
            end("wcs:CoverageDescription");
        }

        // TODO: find a way to share this with the capabilities transfomer
        protected void handleMetadataLinks(List<MetadataLinkInfo> links, String linkType) {
            for (MetadataLinkInfo mdl : links) {
                if (mdl != null) {
                    handleMetadataLink(mdl, linkType);
                }
            }
        }

        protected void handleMetadataLink(MetadataLinkInfo mdl, String linkType) {
            AttributesImpl attributes = new AttributesImpl();

            if (isNotBlank(mdl.getAbout())) {
                attributes.addAttribute("", "about", "about", "", mdl.getAbout());
            }

            if (isNotBlank(mdl.getMetadataType())) {
                attributes.addAttribute(
                        "", "metadataType", "metadataType", "", mdl.getMetadataType());
            }

            if (isNotBlank(linkType)) {
                attributes.addAttribute("", "xlink:type", "xlink:type", "", linkType);
            }

            if (isNotBlank(mdl.getContent())) {
                attributes.addAttribute(
                        "",
                        "xlink:href",
                        "xlink:href",
                        "",
                        ResponseUtils.proxifyMetadataLink(mdl, request.getBaseUrl()));
            }

            if (attributes.getLength() > 0) {
                element("ows:Metadata", null, attributes);
            }
        }

        // TODO: find a way to share this with the capabilities transfomer
        protected void handleKeywords(List kwords) {
            start("ows:Keywords");

            if (kwords != null) {
                for (Iterator it = kwords.iterator(); it.hasNext(); ) {
                    element("ows:Keyword", it.next().toString());
                }
            }

            end("ows:Keywords");
        }

        protected void handleDomain(CoverageInfo ci) throws Exception {
            start("wcs:Domain");
            start("wcs:SpatialDomain");
            handleBoundingBox(ci.getLatLonBoundingBox(), true);
            handleBoundingBox(ci.boundingBox(), false);
            handleGridCRS(ci);
            end("wcs:SpatialDomain");
            end("wcs:Domain");
        }

        protected void handleGridCRS(CoverageInfo ci) throws Exception {
            start("wcs:GridCRS");
            element("wcs:GridBaseCRS", urnIdentifier(ci.getCRS()));
            element("wcs:GridType", GridType.GT2dGridIn2dCrs.getXmlConstant());
            // TODO: go back to using the metadata once they can be trusted
            final LinearTransform tx = (LinearTransform) ci.getGrid().getGridToCRS();
            final Matrix matrix = tx.getMatrix();
            // origin
            StringBuffer origins = new StringBuffer();
            for (int i = 0; i < matrix.getNumRow() - 1; i++) {
                origins.append(matrix.getElement(i, matrix.getNumCol() - 1));
                if (i < matrix.getNumRow() - 2) origins.append(" ");
            }
            element("wcs:GridOrigin", origins.toString());
            // offsets
            StringBuffer offsets = new StringBuffer();
            for (int i = 0; i < matrix.getNumRow() - 1; i++) {
                for (int j = 0; j < matrix.getNumCol() - 1; j++) {
                    offsets.append(matrix.getElement(i, j));
                    if (j < matrix.getNumCol() - 2) offsets.append(" ");
                }
                if (i < matrix.getNumRow() - 2) offsets.append(" ");
            }
            element("wcs:GridOffsets", offsets.toString());
            element("wcs:GridCS", "urn:ogc:def:cs:OGC:0.0:Grid2dSquareCS");
            end("wcs:GridCRS");
        }

        protected void handleBoundingBox(ReferencedEnvelope encodedEnvelope, boolean wgsLonLat)
                throws Exception {
            final AttributesImpl attributes = new AttributesImpl();
            final CoordinateReferenceSystem crs = encodedEnvelope.getCoordinateReferenceSystem();
            if (wgsLonLat) {
                attributes.addAttribute("", "crs", "crs", "", "urn:ogc:def:crs:OGC:1.3:CRS84");
            } else {
                String urnIdentifier = urnIdentifier(crs);
                CoordinateReferenceSystem latlonCrs = CRS.decode(urnIdentifier);
                encodedEnvelope = new ReferencedEnvelope(CRS.transform(encodedEnvelope, latlonCrs));
                attributes.addAttribute("", "crs", "crs", "", urnIdentifier);
            }
            attributes.addAttribute(
                    "",
                    "dimensions",
                    "dimensions",
                    "",
                    Integer.toString(crs.getCoordinateSystem().getDimension()));
            start("ows:BoundingBox", attributes);
            element(
                    "ows:LowerCorner",
                    new StringBuffer(
                                    Double.toString(
                                            encodedEnvelope.getLowerCorner().getOrdinate(0)))
                            .append(" ")
                            .append(encodedEnvelope.getLowerCorner().getOrdinate(1))
                            .toString());
            element(
                    "ows:UpperCorner",
                    new StringBuffer(
                                    Double.toString(
                                            encodedEnvelope.getUpperCorner().getOrdinate(0)))
                            .append(" ")
                            .append(encodedEnvelope.getUpperCorner().getOrdinate(1))
                            .toString());
            end("ows:BoundingBox");
        }

        protected void handleRange(CoverageInfo ci) {
            start("wcs:Range");
            // at the moment we only handle single field coverages
            start("wcs:Field");
            List<CoverageDimensionInfo> dimensions = ci.getDimensions();
            element("wcs:Identifier", "contents");
            // the output domain of the field
            start("wcs:Definition");
            NumberRange range = getCoverageRange(dimensions);
            if (range == null || range.isEmpty()) {
                element("ows:AnyValue", "");
            } else {
                start("ows:AllowedValues");
                start("ows:Range");
                element("ows:MinimumValue", Double.toString(range.getMinimum()));
                element("ows:MaximumValue", Double.toString(range.getMaximum()));
                end("ows:Range");
                end("ows:AllowedValues");
            }
            end("wcs:Definition");
            handleNullValues(dimensions);
            handleInterpolationMethods(ci);
            handleAxis(ci);
            end("wcs:Field");
            end("wcs:Range");
        }

        protected void handleAxis(CoverageInfo ci) {
            final AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute("", "identifier", "identifier", "", "Bands");
            start("wcs:Axis", attributes);
            start("wcs:AvailableKeys");
            List<CoverageDimensionInfo> dimensions = ci.getDimensions();
            for (CoverageDimensionInfo cd : dimensions) {
                element("wcs:Key", cd.getName().replace(' ', '_'));
            }
            end("wcs:AvailableKeys");
            end("wcs:Axis");
        }

        /**
         * Given a set of sample dimensions, this will return a valid range only if all sample
         * dimensions have one, otherwise null
         */
        protected NumberRange getCoverageRange(List<CoverageDimensionInfo> dimensions) {
            NumberRange range = null;
            for (CoverageDimensionInfo dimension : dimensions) {
                if (dimension.getRange() == null) return null;
                else if (range == null) range = dimension.getRange();
                else range.union(dimension.getRange());
            }
            return range;
        }

        protected void handleNullValues(List<CoverageDimensionInfo> dimensions) {
            for (CoverageDimensionInfo cd : dimensions) {
                List<Double> nulls = cd.getNullValues();
                if (nulls == null) return;
                if (nulls.size() == 1) {
                    element("wcs:NullValue", nulls.get(0).toString());
                } else if (nulls.size() >= 1) {
                    // the new specification allows only for a list of values,
                    // Can we assume min and max are two integer numbers and
                    // make up a list out of them? For the moment, just fail
                    throw new IllegalArgumentException(
                            "Cannot encode a range of null values, "
                                    + "only single values are handled");
                }
            }
        }

        protected void handleInterpolationMethods(CoverageInfo ci) {
            start("wcs:InterpolationMethods");
            for (Iterator it = ci.getInterpolationMethods().iterator(); it.hasNext(); ) {
                String method = (String) it.next();
                String converted = METHOD_NAME_MAP.get(method);
                if (converted != null) element("wcs:InterpolationMethod", converted);
            }
            elementIfNotEmpty("wcs:Default", ci.getDefaultInterpolationMethod());
            end("wcs:InterpolationMethods");
        }

        protected void handleSupportedFormats(CoverageInfo ci) throws Exception {
            // gather all the formats for this coverage
            Set<String> formats = new LinkedHashSet<String>();
            for (Iterator it = ci.getSupportedFormats().iterator(); it.hasNext(); ) {
                String format = (String) it.next();
                // wcs 1.1 requires mime types, not format names
                try {
                    CoverageResponseDelegate delegate = responseFactory.encoderFor(format);
                    String formatMime = delegate.getMimeType(format);
                    if (formatMime != null) formats.add(formatMime);
                } catch (Exception e) {
                    // no problem, we just want to avoid people writing HALLABALOOLA in the
                    // supported formats section of the coverage config and then break the
                    // describe response
                }
            }
            // sort them
            for (String format : formats) {
                element("wcs:SupportedFormat", format);
            }
        }

        protected void handleSupportedCRSs(CoverageInfo ci) throws Exception {
            Set supportedCRSs = new LinkedHashSet();
            if (ci.getRequestSRS() != null) supportedCRSs.addAll(ci.getRequestSRS());
            if (ci.getResponseSRS() != null) supportedCRSs.addAll(ci.getResponseSRS());
            for (Iterator it = supportedCRSs.iterator(); it.hasNext(); ) {
                String crsName = (String) it.next();
                CoordinateReferenceSystem crs = CRS.decode(crsName);
                element("wcs:SupportedCRS", urnIdentifier(crs));
                element("wcs:SupportedCRS", crsName);
            }
        }

        protected String urnIdentifier(final CoordinateReferenceSystem crs)
                throws FactoryException {
            String authorityAndCode = CRS.lookupIdentifier(crs, false);
            String code = authorityAndCode.substring(authorityAndCode.lastIndexOf(":") + 1);
            // we don't specify the version, but we still need to put a space
            // for it in the urn form, that's why we have :: before the code
            return "urn:ogc:def:crs:EPSG::" + code;
        }

        /** Writes the element if and only if the content is not null and not empty */
        protected void elementIfNotEmpty(String elementName, String content) {
            if (content != null && !"".equals(content.trim())) element(elementName, content);
        }
    }
}
