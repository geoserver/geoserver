/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import static org.geoserver.ows.util.ResponseUtils.appendPath;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wcs10.CapabilitiesSectionType;
import net.opengis.wcs10.GetCapabilitiesType;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.wcs.WCSInfo;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.vfny.geoserver.global.CoverageInfoLabelComparator;
import org.vfny.geoserver.util.ResponseUtils;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the job of encoding a WCS
 * 1.0.0 Capabilities document.
 *
 * @author Alessio Fabiani (alessio.fabiani@gmail.com)
 * @author Simone Giannecchini (simboss1@gmail.com)
 * @author Andrea Aime, TOPP
 */
public class Wcs10CapsTransformer extends TransformerBase {
    private static final Logger LOGGER =
            Logging.getLogger(Wcs10CapsTransformer.class.getPackage().getName());

    protected static final String WCS_URI = "http://www.opengis.net/wcs";

    protected static final String CUR_VERSION = "1.0.0";

    protected static final String XSI_PREFIX = "xsi";

    protected static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    private WCSInfo wcs;

    private Catalog catalog;

    private final boolean skipMisconfigured;

    /** Creates a new WFSCapsTransformer object. */
    public Wcs10CapsTransformer(GeoServer geoServer) {
        super();
        this.wcs = geoServer.getService(WCSInfo.class);
        this.catalog = geoServer.getCatalog();
        setNamespaceDeclarationEnabled(false);
        this.skipMisconfigured =
                ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                        geoServer.getGlobal().getResourceErrorHandling());
    }

    public Translator createTranslator(ContentHandler handler) {
        return new WCS100CapsTranslator(handler);
    }

    private class WCS100CapsTranslator extends TranslatorSupport {
        // the path that does contain the GeoServer internal XML schemas
        public static final String SCHEMAS = "schemas";

        private GetCapabilitiesType request;

        /** Creates a new WCS100CapsTranslator object. */
        public WCS100CapsTranslator(ContentHandler handler) {
            super(handler, null, null);
        }

        /**
         * Encode the object.
         *
         * @param o The Object to encode.
         * @throws IllegalArgumentException if the Object is not encodeable.
         */
        public void encode(Object o) throws IllegalArgumentException {
            if (!(o instanceof GetCapabilitiesType)) {
                throw new IllegalArgumentException(
                        new StringBuffer("Not a GetCapabilitiesType: ").append(o).toString());
            }

            this.request = (GetCapabilitiesType) o;

            // check the update sequence
            final long updateSequence = wcs.getGeoServer().getGlobal().getUpdateSequence();
            long requestedUpdateSequence = -1;
            if (request.getUpdateSequence() != null) {
                try {
                    requestedUpdateSequence = Long.parseLong(request.getUpdateSequence());
                } catch (NumberFormatException e) {
                    if (request.getUpdateSequence().length() == 0) requestedUpdateSequence = 0;
                    else
                        throw new WcsException(
                                "Invalid update sequence number format, " + "should be an integer",
                                WcsExceptionCode.InvalidUpdateSequence,
                                "updateSequence");
                }
                if (requestedUpdateSequence > updateSequence) {
                    throw new WcsException(
                            "Invalid update sequence value, it's higher "
                                    + "than the current value, "
                                    + updateSequence,
                            WcsExceptionCode.InvalidUpdateSequence,
                            "updateSequence");
                }

                if (requestedUpdateSequence == updateSequence) {
                    throw new WcsException(
                            "WCS capabilities document is current (updateSequence = "
                                    + updateSequence
                                    + ")",
                            WcsExceptionCode.CurrentUpdateSequence,
                            "");
                }
            }

            final AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute("", "version", "version", "", CUR_VERSION);
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

            // proxifiedBaseUrl = RequestUtils.proxifiedBaseURL(request.getBaseUrl(), wcs
            // .getGeoServer().getProxyBaseUrl());
            // final String locationDef = WCS_URI + " " + proxifiedBaseUrl +
            // "schemas/wcs/1.0.0/wcsCapabilities.xsd";
            final String locationDef =
                    WCS_URI
                            + " "
                            + buildURL(
                                    request.getBaseUrl(),
                                    appendPath(SCHEMAS, "wcs/1.0.0/wcsCapabilities.xsd"),
                                    null,
                                    URLType.RESOURCE);

            attributes.addAttribute("", locationAtt, locationAtt, "", locationDef);
            attributes.addAttribute(
                    "", "updateSequence", "updateSequence", "", String.valueOf(updateSequence));
            start("wcs:WCS_Capabilities", attributes);

            // handle the sections directive
            boolean allSections;
            CapabilitiesSectionType section;
            if (request.getSection() == null) {
                allSections = true;
                section = CapabilitiesSectionType.get("/");
            } else {
                section = request.getSection();
                allSections = (section.get("/").equals(section));
            }
            final Set<String> knownSections =
                    new HashSet<String>(
                            Arrays.asList(
                                    "/",
                                    "/WCS_Capabilities/Service",
                                    "/WCS_Capabilities/Capability",
                                    "/WCS_Capabilities/ContentMetadata"));

            if (!knownSections.contains(section.getLiteral()))
                throw new WcsException(
                        "Unknown section " + section,
                        WcsExceptionCode.InvalidParameterValue,
                        "Sections");

            // encode the actual capabilities contents taking into consideration
            // the sections
            if (requestedUpdateSequence < updateSequence) {
                if (allSections
                        || section.equals(
                                CapabilitiesSectionType.WCS_CAPABILITIES_SERVICE_LITERAL)) {
                    handleService(allSections);
                }

                if (allSections
                        || section.equals(
                                CapabilitiesSectionType.WCS_CAPABILITIES_CAPABILITY_LITERAL))
                    handleCapabilities(allSections);

                if (allSections
                        || section.equals(
                                CapabilitiesSectionType.WCS_CAPABILITIES_CONTENT_METADATA_LITERAL))
                    handleContentMetadata(allSections);
            }

            end("wcs:WCS_Capabilities");
        }

        /**
         * Handles the service section of the capabilities document.
         *
         * @throws SAXException For any errors.
         */
        private void handleService(boolean allSections) {
            AttributesImpl attributes = new AttributesImpl();
            if (!allSections) {
                attributes.addAttribute("", "version", "version", "", CUR_VERSION);
            }
            start("wcs:Service", attributes);
            if (wcs.getMetadataLink() != null) {
                handleMetadataLink(wcs.getMetadataLink(), "simple");
            }
            element("wcs:description", wcs.getAbstract());
            element("wcs:name", wcs.getName());
            element("wcs:label", wcs.getTitle());
            handleKeywords(wcs.getKeywords());
            handleContact();

            String fees = wcs.getFees();

            if ((fees == null) || "".equals(fees)) {
                fees = "NONE";
            }

            element("wcs:fees", fees);

            String accessConstraints = wcs.getAccessConstraints();

            if ((accessConstraints == null) || "".equals(accessConstraints)) {
                accessConstraints = "NONE";
            }

            element("wcs:accessConstraints", accessConstraints);
            end("wcs:Service");
        }

        private void handleMetadataLink(MetadataLinkInfo mdl, String linkType) {
            AttributesImpl attributes = new AttributesImpl();

            if (StringUtils.isNotBlank(mdl.getAbout())) {
                attributes.addAttribute("", "about", "about", "", mdl.getAbout());
            }

            if (StringUtils.isNotBlank(linkType)) {
                attributes.addAttribute("", "xlink:type", "xlink:type", "", linkType);
            }

            if (StringUtils.isNotBlank(mdl.getMetadataType())) {
                attributes.addAttribute(
                        "", "metadataType", "metadataType", "", mdl.getMetadataType());
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
        private void handleKeywords(List kwords) {
            if (kwords == null || kwords.isEmpty()) {
                return;
            }

            start("wcs:keywords");

            if (kwords != null) {
                for (Iterator it = kwords.iterator(); it.hasNext(); ) {
                    element("wcs:keyword", it.next().toString());
                }
            }

            end("wcs:keywords");
        }

        /** Handles contacts. */
        private void handleContact() {
            final GeoServer gs = wcs.getGeoServer();
            String tmp = "";

            SettingsInfo settings = gs.getSettings();
            ContactInfo contact = settings.getContact();

            if (contact != null
                    && (StringUtils.isNotBlank(contact.getContactPerson())
                            || (StringUtils.isNotBlank(contact.getContactOrganization())))) {
                start("wcs:responsibleParty");

                tmp = contact.getContactPerson();
                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:individualName", tmp);
                } else {
                    // not optional
                    element("wcs:individualName", "");
                }

                tmp = contact.getContactOrganization();
                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:organisationName", tmp);
                }

                tmp = contact.getContactPosition();

                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:positionName", tmp);
                }

                start("wcs:contactInfo");

                start("wcs:phone");
                tmp = contact.getContactVoice();

                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:voice", tmp);
                }

                tmp = contact.getContactFacsimile();

                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:facsimile", tmp);
                }

                end("wcs:phone");

                start("wcs:address");
                tmp = contact.getAddressType();

                if (StringUtils.isNotBlank(tmp)) {
                    String addr = "";
                    addr = contact.getAddress();

                    if (StringUtils.isNotBlank(addr)) {
                        element("wcs:deliveryPoint", tmp + " " + addr);
                    }
                } else {
                    tmp = contact.getAddress();

                    if (StringUtils.isNotBlank(tmp)) {
                        element("wcs:deliveryPoint", tmp);
                    }
                }

                tmp = contact.getAddressCity();

                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:city", tmp);
                }

                tmp = contact.getAddressState();

                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:administrativeArea", tmp);
                }

                tmp = contact.getAddressPostalCode();

                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:postalCode", tmp);
                }

                tmp = contact.getAddressCountry();

                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:country", tmp);
                }

                tmp = contact.getContactEmail();

                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:electronicMailAddress", tmp);
                }

                end("wcs:address");

                tmp = contact.getOnlineResource();

                if (StringUtils.isNotBlank(tmp)) {
                    AttributesImpl attributes = new AttributesImpl();
                    attributes.addAttribute("", "xlink:href", "xlink:href", "", tmp);
                    start("wcs:onlineResource", attributes);
                    end("wcs:onlineResource");
                }

                end("wcs:contactInfo");

                end("wcs:responsibleParty");
            }
        }

        /** */
        private void handleCapabilities(boolean allSections) {
            start("wcs:Capability");
            handleRequest();
            handleExceptions();
            handleVendorSpecifics();
            end("wcs:Capability");
        }

        /**
         * Handles the request portion of the document, printing out the capabilities and where to
         * bind to them.
         */
        private void handleRequest() {
            start("wcs:Request");
            handleCapability("wcs:GetCapabilities");
            handleCapability("wcs:DescribeCoverage");
            handleCapability("wcs:GetCoverage");
            end("wcs:Request");
        }

        private void handleCapability(String capabilityName) {
            AttributesImpl attributes = new AttributesImpl();
            start(capabilityName);

            start("wcs:DCPType");
            start("wcs:HTTP");

            // String baseURL = RequestUtils.proxifiedBaseURL(request.getBaseUrl(),
            // wcs.getGeoServer().getGlobal().getProxyBaseUrl());
            String url = buildURL(request.getBaseUrl(), "wcs", null, URLType.SERVICE);
            url = makeURLAppendable(url);
            attributes.addAttribute("", "xlink:href", "xlink:href", "", url);

            start("wcs:Get");
            start("wcs:OnlineResource", attributes);
            end("wcs:OnlineResource");
            end("wcs:Get");
            end("wcs:HTTP");
            end("wcs:DCPType");

            attributes = new AttributesImpl();
            attributes.addAttribute("", "xlink:href", "xlink:href", "", url);

            start("wcs:DCPType");
            start("wcs:HTTP");
            start("wcs:Post");
            start("wcs:OnlineResource", attributes);
            end("wcs:OnlineResource");
            end("wcs:Post");
            end("wcs:HTTP");
            end("wcs:DCPType");
            end(capabilityName);
        }

        /**
         * Makes sure the URL can simply be used by appending to it, without checking if it ends by
         * ?, & or nothing
         */
        private String makeURLAppendable(String url) {
            if (url.endsWith("?") || url.endsWith("&")) {
                return url;
            } else if (url.contains("?")) {
                return url + "&";
            } else {
                return url + "?";
            }
        }

        /**
         * Handles the printing of the exceptions information, prints the formats that GeoServer can
         * return exceptions in.
         */
        private void handleExceptions() {
            start("wcs:Exception");

            List<String> exceptionFormats = wcs.getExceptionFormats();

            if (exceptionFormats == null) exceptionFormats = new ArrayList<>();
            if (exceptionFormats.isEmpty()) exceptionFormats.add("application/vnd.ogc.se_xml");

            for (String format : exceptionFormats) {
                element("wcs:Format", format);
            }

            end("wcs:Exception");
        }

        /** Handles the vendor specific capabilities. Right now there are none, so we do nothing. */
        private void handleVendorSpecifics() {}

        private void handleEnvelope(
                ReferencedEnvelope referencedEnvelope,
                DimensionInfo timeInfo,
                ReaderDimensionsAccessor dimensions)
                throws IOException {
            AttributesImpl attributes = new AttributesImpl();

            attributes.addAttribute(
                    "",
                    "srsName",
                    "srsName",
                    "", /* "WGS84(DD)" */
                    "urn:ogc:def:crs:OGC:1.3:CRS84");
            start("wcs:lonLatEnvelope", attributes);
            final StringBuffer minCP =
                    new StringBuffer(Double.toString(referencedEnvelope.getMinX()))
                            .append(" ")
                            .append(referencedEnvelope.getMinY());
            final StringBuffer maxCP =
                    new StringBuffer(Double.toString(referencedEnvelope.getMaxX()))
                            .append(" ")
                            .append(referencedEnvelope.getMaxY());

            element("gml:pos", minCP.toString());
            element("gml:pos", maxCP.toString());

            // are we going to report time?
            if (timeInfo != null && timeInfo.isEnabled()) {
                SimpleDateFormat timeFormat = dimensions.getTimeFormat();
                element("gml:timePosition", timeFormat.format(dimensions.getMinTime()));
                element("gml:timePosition", timeFormat.format(dimensions.getMaxTime()));
            }

            end("wcs:lonLatEnvelope");
        }

        /** @param originalArray */
        private String[] orderDoubleArray(String[] originalArray) {
            List finalArray = Arrays.asList(originalArray);

            Collections.sort(
                    finalArray,
                    new Comparator<String>() {

                        public int compare(String o1, String o2) {
                            if (o1.equals(o2)) return 0;

                            return (Double.parseDouble(o1) > Double.parseDouble(o2) ? 1 : -1);
                        }
                    });

            return (String[]) finalArray.toArray(new String[1]);
        }

        /** @param originalArray */
        private String[] orderTimeArray(String[] originalArray) {
            List finalArray = Arrays.asList(originalArray);

            Collections.sort(
                    finalArray,
                    new Comparator<String>() {
                        /** All patterns that are correct regarding the ISO-8601 norm. */
                        final String[] PATTERNS = {
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            "yyyy-MM-dd'T'HH:mm:sss'Z'",
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            "yyyy-MM-dd'T'HH:mm'Z'",
                            "yyyy-MM-dd'T'HH'Z'",
                            "yyyy-MM-dd",
                            "yyyy-MM",
                            "yyyy"
                        };

                        public int compare(String o1, String o2) {
                            if (o1.equals(o2)) return 0;

                            Date d1 = getDate(o1);
                            Date d2 = getDate(o2);

                            if (d1 == null || d2 == null) return 0;

                            return (d1.getTime() > d2.getTime() ? 1 : -1);
                        }

                        private Date getDate(final String value) {

                            // special handling for current keyword
                            if (value.equalsIgnoreCase("current")) return null;
                            for (int i = 0; i < PATTERNS.length; i++) {
                                // rebuild formats at each parse, date formats are not thread safe
                                SimpleDateFormat format =
                                        new SimpleDateFormat(PATTERNS[i], Locale.CANADA);

                                /* We do not use the standard method DateFormat.parse(String), because if the parsing
                                 * stops before the end of the string, the remaining characters are just ignored and
                                 * no exception is thrown. So we have to ensure that the whole string is correct for
                                 * the format.
                                 */
                                ParsePosition pos = new ParsePosition(0);
                                Date time = format.parse(value, pos);
                                if (pos.getIndex() == value.length()) {
                                    return time;
                                }
                            }

                            return null;
                        }
                    });

            return (String[]) finalArray.toArray(new String[1]);
        }

        /** */
        private void handleContentMetadata(boolean allSections) {
            AttributesImpl attributes = new AttributesImpl();
            if (!allSections) {
                attributes.addAttribute("", "version", "version", "", CUR_VERSION);
            }

            start("wcs:ContentMetadata", attributes);

            List<CoverageInfo> coverages = catalog.getCoverages();
            Collections.sort(coverages, new CoverageInfoLabelComparator());
            for (CoverageInfo cvInfo : coverages) {
                try {
                    mark();
                    handleCoverageOfferingBrief(cvInfo);
                    commit();
                } catch (Exception e) {
                    if (skipMisconfigured) {
                        reset();
                        LOGGER.log(
                                Level.SEVERE,
                                "Skipping coverage: "
                                        + cvInfo.prefixedName()
                                        + " as its capabilities generation failed",
                                e);
                    } else {
                        throw new RuntimeException(
                                "Capabilities document generation failed on coverage "
                                        + cvInfo.prefixedName(),
                                e);
                    }
                }
            }

            end("wcs:ContentMetadata");
        }

        /** */
        private void handleCoverageOfferingBrief(CoverageInfo cv) throws IOException {
            if (cv.isEnabled()) {
                start("wcs:CoverageOfferingBrief");

                String tmp;

                for (MetadataLinkInfo mdl : cv.getMetadataLinks())
                    handleMetadataLink(mdl, "simple");

                tmp = cv.getDescription();

                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:description", tmp);
                }

                tmp = cv.prefixedName();

                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:name", tmp);
                }

                tmp = cv.getTitle();

                if (StringUtils.isNotBlank(tmp)) {
                    element("wcs:label", tmp);
                }

                CoverageStoreInfo csinfo = cv.getStore();

                if (csinfo == null) {
                    throw new WcsException(
                            "Unable to acquire coverage store resource for coverage: "
                                    + cv.getName());
                }

                GridCoverage2DReader reader = null;
                try {
                    reader =
                            (GridCoverage2DReader)
                                    cv.getGridCoverageReader(null, GeoTools.getDefaultHints());
                } catch (IOException e) {
                    LOGGER.severe(
                            "Unable to acquire a reader for this coverage with format: "
                                    + csinfo.getFormat().getName());
                }

                if (reader == null)
                    throw new WcsException(
                            "Unable to acquire a reader for this coverage with format: "
                                    + csinfo.getFormat().getName());

                DimensionInfo timeInfo =
                        cv.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
                ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
                handleEnvelope(cv.getLatLonBoundingBox(), timeInfo, dimensions);
                handleKeywords(cv.getKeywords());

                end("wcs:CoverageOfferingBrief");
            }
        }

        /** Writes the element if and only if the content is not null and not empty */
        private void elementIfNotEmpty(String elementName, String content) {
            if (content != null && !"".equals(content.trim())) element(elementName, content);
        }
    }
}
