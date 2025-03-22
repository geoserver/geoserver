/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.opengis.wcs20.GetCapabilitiesType;
import org.geoserver.ExtendedCapabilitiesProvider;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.config.SettingsInfo;
import org.geoserver.crs.CapabilitiesCRSProvider;
import org.geoserver.data.InternationalContentHelper;
import org.geoserver.ows.URLMangler;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs2_0.WCS20Const;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.SrsSyntax;
import org.geotools.util.logging.Logging;
import org.geotools.wcs.v2_0.WCS;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.vfny.geoserver.global.CoverageInfoLabelComparator;
import org.vfny.geoserver.util.ResponseUtils;
import org.vfny.geoserver.util.WCSUtils;
import org.vfny.geoserver.wcs.WcsException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Transformer for GetCapabilities
 *
 * @author Emanuele Tajariol (etj) - GeoSolutions
 * @author Simone Giannecchini, GeoSolutions
 */
public class WCS20GetCapabilitiesTransformer extends TransformerBase {

    private static final Logger LOGGER = Logging.getLogger(WCS20GetCapabilitiesTransformer.class);

    protected static final String CUR_VERSION = WCS20Const.V201;

    private WCSInfo wcs;

    private final boolean skipMisconfigured;

    private CoverageResponseDelegateFinder responseFactory;

    /** {@link Enum} that identifies the various sections. */
    enum SECTIONS {
        ServiceIdentification,
        ServiceProvider,
        OperationsMetadata,
        ServiceMetadata,
        Contents,
        Languages,
        All;

        public static final Set<String> names;

        static {
            Set<String> tmp = new HashSet<>();
            for (SECTIONS section : SECTIONS.values()) {
                tmp.add(section.name());
            }
            names = Collections.unmodifiableSet(tmp);
        }
    }

    public WCS20GetCapabilitiesTransformer(GeoServer gs, CoverageResponseDelegateFinder responseFactory) {
        this.wcs = gs.getService(WCSInfo.class);
        this.skipMisconfigured = ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                gs.getGlobal().getResourceErrorHandling());
        this.responseFactory = responseFactory;
        setNamespaceDeclarationEnabled(false);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new WCS20GetCapabilitiesTranslator(handler);
    }

    private class WCS20GetCapabilitiesTranslator extends TranslatorSupport {
        /**
         * @uml.property name="request"
         * @uml.associationEnd multiplicity="(0 1)"
         */
        private GetCapabilitiesType request;

        private List<WCSExtendedCapabilitiesProvider> extensions;
        private org.geoserver.ExtendedCapabilitiesProvider.Translator translator;
        private TranslatorHelper helper;

        private InternationalContentHelper internationalContentHelper;
        private boolean i18nRequested = false;

        /** Creates a new WFSCapsTranslator object. */
        public WCS20GetCapabilitiesTranslator(ContentHandler handler) {
            super(handler, null, null);
            this.helper = new TranslatorHelper();
            this.extensions = GeoServerExtensions.extensions(WCSExtendedCapabilitiesProvider.class);
            // register namespaces provided by extended capabilities
            NamespaceSupport namespaces = getNamespaceSupport();
            namespaces.declarePrefix("wcs", "http://www.opengis.net/wcs/2.0");
            namespaces.declarePrefix("int", "https://www.opengis.net/wcs/interpolation/1.0");

            namespaces.declarePrefix("crs", "http://www.opengis.net/wcs/crs/1.0");

            for (WCSExtendedCapabilitiesProvider cp : extensions) {
                cp.registerNamespaces(namespaces);
            }
            this.translator = new ExtendedCapabilitiesProvider.Translator() {

                @Override
                public void start(String element, Attributes attributes) {
                    WCS20GetCapabilitiesTranslator.this.start(element, attributes);
                }

                @Override
                public void start(String element) {
                    WCS20GetCapabilitiesTranslator.this.start(element);
                }

                @Override
                public void end(String element) {
                    WCS20GetCapabilitiesTranslator.this.end(element);
                }

                @Override
                public void chars(String text) {
                    WCS20GetCapabilitiesTranslator.this.chars(text);
                }
            };
        }

        /**
         * Encode the object.
         *
         * @param o The Object to encode.
         * @throws IllegalArgumentException if the Object is not encodeable.
         */
        @Override
        public void encode(Object o) throws IllegalArgumentException {
            if (!(o instanceof GetCapabilitiesType)) {
                throw new IllegalArgumentException("Not a GetCapabilitiesType: " + o != null ? o.toString() : "null");
            }
            this.request = (GetCapabilitiesType) o;
            Set<CoverageInfo> coverageInfos = getCoverages();
            this.i18nRequested = request.getAcceptLanguages() != null;
            this.internationalContentHelper =
                    new InternationalContentHelper(request.getAcceptLanguages(), wcs, new ArrayList<>(coverageInfos));
            // check the update sequence
            final long updateSequence = wcs.getGeoServer().getGlobal().getUpdateSequence();
            long requestedUpdateSequence = -1;
            if (request.getUpdateSequence() != null) {
                try {
                    requestedUpdateSequence = Long.parseLong(request.getUpdateSequence());
                } catch (NumberFormatException e) {
                    throw new WcsException(
                            "Invalid update sequence number format, " + "should be an integer",
                            WcsException.WcsExceptionCode.InvalidUpdateSequence,
                            "updateSequence");
                }
                if (requestedUpdateSequence > updateSequence) {
                    throw new WcsException(
                            "Invalid update sequence value, it's higher " + "than the current value, " + updateSequence,
                            WcsException.WcsExceptionCode.InvalidUpdateSequence,
                            "updateSequence");
                }
            }

            // check and init sections
            // handle the sections directive
            boolean allSections;
            List<String> sections;
            if (request.getSections() == null) {
                sections = Collections.emptyList();
                allSections = true;
            } else {
                sections = request.getSections().getSection();
                allSections = sections.contains(SECTIONS.All.name());

                for (String section : sections) {
                    if (!SECTIONS.names.contains(section))
                        throw new WcsException(
                                "Unknown section " + section,
                                WcsException.WcsExceptionCode.InvalidParameterValue,
                                "Sections");
                }
            }

            // Build the document

            final AttributesImpl attributes = WCS20Const.getDefaultNamespaces();
            attributes.addAttribute("", "version", "version", "", CUR_VERSION);
            attributes.addAttribute("", "updateSequence", "updateSequence", "", String.valueOf(updateSequence));
            helper.registerNamespaces(getNamespaceSupport(), attributes);

            // TODO: add a config to choose the canonical or local schema
            String location = buildSchemaLocation(
                    request.getBaseUrl(), WCS.NAMESPACE, "http://schemas.opengis.net/wcs/2.0/wcsGetCapabilities.xsd");

            // final String locationDef = WCS.NAMESPACE + " " + buildSchemaURL(request.getBaseUrl(),
            // "wcs/2.0/wcsGetCapabilities.xsd");//
            attributes.addAttribute("", "xsi:schemaLocation", "xsi:schemaLocation", "", location);

            start("wcs:Capabilities", attributes);

            // encode the actual capabilities contents taking into consideration
            // the sections
            if (requestedUpdateSequence < updateSequence) {
                if (allSections || sections.contains(SECTIONS.ServiceIdentification.name()))
                    handleServiceIdentification();
                if (allSections || sections.contains(SECTIONS.ServiceProvider.name())) handleServiceProvider();
                if (allSections || sections.contains(SECTIONS.OperationsMetadata.name())) handleOperationsMetadata();
                if (allSections || sections.contains(SECTIONS.ServiceMetadata.name())) handleServiceMetadata(request);
                if (allSections || sections.contains(SECTIONS.Contents.name())) handleContents(coverageInfos);
                if (allSections || sections.contains(SECTIONS.Languages.name())) handleLanguages();
            }

            end("wcs:Capabilities");
        }

        String buildSchemaLocation(String schemaBaseURL, String... locations) {
            for (WCSExtendedCapabilitiesProvider cp : extensions) {
                locations = helper.append(locations, cp.getSchemaLocations(schemaBaseURL));
            }

            return helper.buildSchemaLocation(locations);
        }

        private void handleServiceMetadata(GetCapabilitiesType ct) {
            start("wcs:ServiceMetadata");

            // formats are part of the document only starting version 2.0.1
            if (ct.getAcceptVersions() == null
                    || ct.getAcceptVersions().getVersion() == null
                    || ct.getAcceptVersions().getVersion().isEmpty()
                    || ct.getAcceptVersions().getVersion().contains("2.0.1")) {
                Set<String> formats = new TreeSet<>();
                for (String format : responseFactory.getOutputFormats()) {
                    CoverageResponseDelegate delegate = responseFactory.encoderFor(format);
                    String mime = delegate.getMimeType(format);
                    try {
                        new URI(mime);
                        formats.add(mime);
                    } catch (URISyntaxException e) {
                        // skip it
                    }
                }
                for (String format : formats) {
                    element("wcs:formatSupported", format);
                }
            }

            // the CRS extension requires us to declare the full list of supported CRS
            start("wcs:Extension");
            // add the supported CRS
            Collection<String> codes;
            if (wcs.getSRS() == null || wcs.getSRS().isEmpty()) {
                CapabilitiesCRSProvider crsProvider = new CapabilitiesCRSProvider();
                crsProvider.getAuthorityExclusions().add("CRS");
                crsProvider.setCodeMapper(
                        (authority, code) -> "http://www.opengis.net/def/crs/" + authority + "/0/" + code);
                codes = crsProvider.getCodes();
            } else {
                codes = wcs.getSRS().stream().map(code -> mapCode(code)).collect(Collectors.toList());
            }
            start("crs:CrsMetadata");
            for (String code : codes) {
                element("crs:crsSupported", code);
            }
            end("crs:CrsMetadata");

            // add the supported interpolation methods
            start("int:InterpolationMetadata");
            element("int:InterpolationSupported", "http://www.opengis.net/def/interpolation/OGC/1/nearest-neighbor");
            element("int:InterpolationSupported", "http://www.opengis.net/def/interpolation/OGC/1/linear");
            element("int:InterpolationSupported", "http://www.opengis.net/def/interpolation/OGC/1/cubic");
            end("int:InterpolationMetadata");

            end("wcs:Extension");

            end("wcs:ServiceMetadata");
        }

        private String mapCode(String code) {
            int idx = code.indexOf(":");
            if (idx == -1) {
                return "http://www.opengis.net/def/crs/EPSG/0/" + code;
            }
            String authority = code.substring(0, idx);
            String codeValue = code.substring(idx + 1);
            return "http://www.opengis.net/def/crs/" + authority + "/0/" + codeValue;
        }

        /** Handles the service identification of the capabilities document. */
        private void handleServiceIdentification() {
            start("ows:ServiceIdentification");

            handleServiceTitleAndAbstract();

            handleKeywords(wcs.getKeywords());

            element("ows:ServiceType", "urn:ogc:service:wcs"); // TODO: check this: some docs specify a "OGC WCS"
            // string
            element("ows:ServiceTypeVersion", WCS20Const.V201);
            element("ows:ServiceTypeVersion", WCS20Const.V111);
            element("ows:ServiceTypeVersion", WCS20Const.V110);

            element("ows:Profile", "http://www.opengis.net/spec/WCS/2.0/conf/core");
            element(
                    "ows:Profile",
                    "http://www.opengis.net/spec/WCS_protocol-binding_get-kvp/1.0.1"); // requirement #1 in OGC 09-147r3
            element("ows:Profile", "http://www.opengis.net/spec/WCS_protocol-binding_post-xml/1.0");

            // don't believe we support this one
            // element("ows:Profile",
            // "http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs-discrete-coverage");
            element(
                    "ows:Profile",
                    "http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs-gridded-coverage");

            // element("ows:Profile","http://www.opengis.net/spec/WCS_coverage-encoding/1.0/conf/coverage-encoding"); //
            // TODO: check specs and URL

            // === GeoTiff encoding extension
            element(
                    "ows:Profile",
                    " http://www.opengis.net/spec/WCS_geotiff-coverages/1.0/conf/geotiff-coverage"); // TODO: check
            // specs and URL

            // === GML encoding
            element("ows:Profile", "http://www.opengis.net/spec/GMLCOV/1.0/conf/gml-coverage");
            element("ows:Profile", "http://www.opengis.net/spec/GMLCOV/1.0/conf/special-format");
            element("ows:Profile", "http://www.opengis.net/spec/GMLCOV/1.0/conf/multipart");

            // === Scaling Extension
            element("ows:Profile", "http://www.opengis.net/spec/WCS_service-extension_scaling/1.0/conf/scaling");

            // === CRS Extension
            element("ows:Profile", "http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs");

            // === Interpolation
            element(
                    "ows:Profile",
                    "http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/interpolation");
            element(
                    "ows:Profile",
                    "http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/interpolation-per-axis"); // TODO for time axis
            element(
                    "ows:Profile",
                    "http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/nearest-neighbor");
            element("ows:Profile", "http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/linear");
            element("ows:Profile", "http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/cubic");

            // === Range Subsetting
            element(
                    "ows:Profile",
                    "http://www.opengis.net/spec/WCS_service-extension_range-subsetting/1.0/conf/record-subsetting");
            // TODO don't believe we support these
            // element("ows:Profile","http://www.opengis.net/spec/WCS_service-extension_array-subsetting/1.0/conf/array-subsetting");
            // element("ows:Profile","http://www.opengis.net/spec/WCS_service-extension_range-subsetting/1.0/conf/nested-subsetting");

            String fees = wcs.getFees();
            if (isBlank(fees)) {
                fees = "NONE";
            }
            element("ows:Fees", fees);

            String accessConstraints = wcs.getAccessConstraints();
            if (isBlank(accessConstraints)) {
                accessConstraints = "NONE";
            }
            element("ows:AccessConstraints", accessConstraints);
            end("ows:ServiceIdentification");
        }

        private void handleServiceTitleAndAbstract() {
            if (!i18nRequested) {
                element("ows:Title", wcs.getTitle());
                element("ows:Abstract", wcs.getAbstract());
            } else {
                element("ows:Title", internationalContentHelper.getTitle(wcs));
                element("ows:Abstract", internationalContentHelper.getAbstract(wcs));
            }
        }

        /** Handles the service provider of the capabilities document. */
        private void handleServiceProvider() {
            start("ows:ServiceProvider");
            SettingsInfo settings = wcs.getGeoServer().getSettings();
            handleServiceProvider(settings.getContact());
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(
                    "",
                    "xlink:href",
                    "xlink:href",
                    "",
                    settings.getOnlineResource() != null ? settings.getOnlineResource() : "");
            element("ows:ProviderSite", null, attributes);
            final GeoServer gs = wcs.getGeoServer();
            ContactInfo contact = gs.getSettings().getContact();
            encodeContact(contact, gs.getSettings().getOnlineResource());

            end("ows:ServiceProvider");
        }

        private void handleServiceProvider(ContactInfo contactInfo) {
            if (!i18nRequested) element("ows:ProviderName", contactInfo.getContactOrganization());
            else
                element(
                        "ows:ProviderName",
                        internationalContentHelper.getNullableString(
                                contactInfo.getInternationalContactOrganization()));
        }

        /**
         * Handles the OperationMetadata portion of the document, printing out the operations and where to bind to them.
         */
        private void handleOperationsMetadata() {
            start("ows:OperationsMetadata");
            handleOperation("GetCapabilities", null);
            handleOperation("DescribeCoverage", null);
            handleOperation("GetCoverage", null);

            // specify that we do support xml post encoding, clause 8.3.2.2 of
            // the WCS 1.1.1 spec
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(null, "name", "name", null, "PostEncoding");
            start("ows:Constraint", attributes);
            start("ows:AllowedValues");
            element("ows:Value", "XML");
            //            element("ows:Value", "text/xml");
            //            element("ows:Value", "application/xml");
            end("ows:AllowedValues");
            end("ows:Constraint");

            if (extensions != null && !extensions.isEmpty()) {
                try {
                    for (WCSExtendedCapabilitiesProvider provider : extensions) {
                        provider.encodeExtendedOperations(translator, wcs, request);
                    }
                } catch (Exception e) {
                    throw new ServiceException("Extended capabilities provider threw error", e);
                }
            }

            end("ows:OperationsMetadata");
        }

        private void handleOperation(String capabilityName, Map<String, List<String>> parameters) {
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(null, "name", "name", null, capabilityName);
            start("ows:Operation", attributes);

            final String url =
                    appendQueryString(buildURL(request.getBaseUrl(), "wcs", null, URLMangler.URLType.SERVICE), "");

            start("ows:DCP");
            start("ows:HTTP");
            attributes = new AttributesImpl();
            attributes.addAttribute("", "xlink:href", "xlink:href", "", url);
            element("ows:Get", null, attributes);
            end("ows:HTTP");
            end("ows:DCP");

            attributes = new AttributesImpl();
            attributes.addAttribute("", "xlink:href", "xlink:href", "", url);
            start("ows:DCP");
            start("ows:HTTP");
            element("ows:Post", null, attributes);
            end("ows:HTTP");
            end("ows:DCP");

            if (parameters != null && !parameters.isEmpty()) {
                for (Map.Entry<String, List<String>> param : parameters.entrySet()) {
                    attributes = new AttributesImpl();
                    attributes.addAttribute("", "name", "name", "", param.getKey());
                    start("ows:Parameter", attributes);
                    start("ows:AllowedValues");
                    for (String value : param.getValue()) {
                        element("ows:Value", value);
                    }
                    end("ows:AllowedValues");
                    end("ows:Parameter");
                }
            }

            end("ows:Operation");
        }

        /** */
        private void handleKeywords(List<KeywordInfo> kwords) {
            if (i18nRequested) kwords = internationalContentHelper.filterKeywords(kwords);
            if (kwords != null && !kwords.isEmpty()) {
                start("ows:Keywords");
                for (KeywordInfo kword : kwords) {
                    element("ows:Keyword", kword.getValue());
                }
                end("ows:Keywords");
            }
        }

        private void encodeContact(ContactInfo contactInfo, String onlineResource) {
            start("ows:ServiceContact");
            if (!i18nRequested) handleContact(contactInfo, onlineResource);
            else handleInternationalContact(contactInfo, onlineResource);
            end("ows:ServiceContact");
        }

        /** Handles contacts. */
        private void handleContact(ContactInfo contact, String onlineResource) {
            elementIfNotEmpty("ows:IndividualName", contact.getContactPerson());
            elementIfNotEmpty("ows:PositionName", contact.getContactPosition());

            start("ows:ContactInfo");
            start("ows:Phone");
            elementIfNotEmpty("ows:Voice", contact.getContactVoice());
            elementIfNotEmpty("ows:Facsimile", contact.getContactFacsimile());
            end("ows:Phone");
            start("ows:Address");
            elementIfNotEmpty("ows:DeliveryPoint", contact.getAddress());
            elementIfNotEmpty("ows:City", contact.getAddressCity());
            elementIfNotEmpty("ows:AdministrativeArea", contact.getAddressState());
            elementIfNotEmpty("ows:PostalCode", contact.getAddressPostalCode());
            elementIfNotEmpty("ows:Country", contact.getAddressCountry());
            elementIfNotEmpty("ows:ElectronicMailAddress", contact.getContactEmail());
            end("ows:Address");

            handleSettingOnlineRes(onlineResource);

            end("ows:ContactInfo");
        }

        /** Handles contacts. */
        private void handleInternationalContact(ContactInfo contact, String onlineResource) {
            elementIfNotEmpty(
                    "ows:IndividualName",
                    internationalContentHelper.getNullableString(contact.getInternationalContactPerson()));
            elementIfNotEmpty(
                    "ows:PositionName",
                    internationalContentHelper.getNullableString(contact.getInternationalContactPosition()));

            start("ows:ContactInfo");
            start("ows:Phone");
            elementIfNotEmpty(
                    "ows:Voice", internationalContentHelper.getNullableString(contact.getInternationalContactVoice()));
            elementIfNotEmpty(
                    "ows:Facsimile",
                    internationalContentHelper.getNullableString(contact.getInternationalContactFacsimile()));
            end("ows:Phone");
            start("ows:Address");
            elementIfNotEmpty(
                    "ows:DeliveryPoint",
                    internationalContentHelper.getNullableString(contact.getInternationalAddress()));
            elementIfNotEmpty(
                    "ows:City", internationalContentHelper.getNullableString(contact.getInternationalAddressCity()));
            elementIfNotEmpty(
                    "ows:AdministrativeArea",
                    internationalContentHelper.getNullableString(contact.getInternationalAddressState()));
            elementIfNotEmpty(
                    "ows:PostalCode",
                    internationalContentHelper.getNullableString(contact.getInternationalAddressPostalCode()));
            elementIfNotEmpty(
                    "ows:Country",
                    internationalContentHelper.getNullableString(contact.getInternationalAddressCountry()));
            elementIfNotEmpty(
                    "ows:ElectronicMailAddress",
                    internationalContentHelper.getNullableString(contact.getInternationalContactEmail()));
            end("ows:Address");

            handleSettingOnlineRes(onlineResource);

            end("ows:ContactInfo");
        }

        private void handleSettingOnlineRes(String onlineResource) {
            if (isNotBlank(onlineResource)) {
                AttributesImpl attributes = new AttributesImpl();
                attributes.addAttribute("", "xlink:href", "xlink:href", "", onlineResource);
                start("ows:OnlineResource", attributes);
                end("OnlineResource");
            }
        }

        private void handleWGS84BoundingBox(BoundingBox envelope) {
            start("ows:WGS84BoundingBox");
            element(
                    "ows:LowerCorner",
                    new StringBuilder(Double.toString(envelope.getLowerCorner().getOrdinate(0)))
                            .append(" ")
                            .append(envelope.getLowerCorner().getOrdinate(1))
                            .toString());
            element(
                    "ows:UpperCorner",
                    new StringBuilder(Double.toString(envelope.getUpperCorner().getOrdinate(0)))
                            .append(" ")
                            .append(envelope.getUpperCorner().getOrdinate(1))
                            .toString());
            end("ows:WGS84BoundingBox");
        }

        private void handleContents(Set<CoverageInfo> coverages) {
            start("wcs:Contents");

            for (CoverageInfo cv : coverages) {
                try {
                    mark();
                    handleCoverageSummary(cv);
                    commit();
                } catch (Exception e) {
                    if (skipMisconfigured) {
                        reset();
                        LOGGER.log(
                                Level.SEVERE,
                                "Skipping coverage " + cv.prefixedName() + " as its capabilities generation failed",
                                e);
                    } else {
                        throw new RuntimeException(
                                "Capabilities document generation failed on coverage " + cv.prefixedName(), e);
                    }
                }
            }

            if (extensions != null && !extensions.isEmpty()) {
                start("wcs:Extension");
                try {
                    for (WCSExtendedCapabilitiesProvider provider : extensions) {
                        provider.encodeExtendedContents(translator, wcs, new ArrayList<>(coverages), request);
                    }
                } catch (Exception e) {
                    throw new ServiceException("Extended capabilities provider threw error", e);
                }
                end("wcs:Extension");
            }

            end("wcs:Contents");
        }

        private Set<CoverageInfo> getCoverages() {
            final Set<CoverageInfo> coverages = new TreeSet<>(new CoverageInfoLabelComparator());
            coverages.addAll(wcs.getGeoServer().getCatalog().getCoverages());

            // filter out disabled coverages
            for (Iterator<CoverageInfo> it = coverages.iterator(); it.hasNext(); ) {
                CoverageInfo cv = it.next();
                if (!cv.enabled()) {
                    it.remove();
                }
            }
            return coverages;
        }

        private void handleCoverageSummary(CoverageInfo cv) throws Exception {
            start("wcs:CoverageSummary");
            handleTitleAndAbstract(cv);
            handleKeywords(cv.getKeywords());
            String covId = NCNameResourceCodec.encode(cv);
            element("wcs:CoverageId", covId);
            element("wcs:CoverageSubtype", "RectifiedGridCoverage"); // TODO make this parametric

            GridCoverage2DReader reader = (GridCoverage2DReader) cv.getGridCoverageReader(null, null);
            ReferencedEnvelope envelope = WCSUtils.fitEnvelope(cv, reader);
            handleBoundingBox(envelope);
            // should we "fit" this one too?
            handleWGS84BoundingBox(cv.getLatLonBoundingBox());
            cv.getMetadataLinks().forEach(this::handleMetadataLink);

            end("wcs:CoverageSummary");
        }

        private void handleTitleAndAbstract(CoverageInfo cv) {
            if (!i18nRequested) {
                elementIfNotEmpty("ows:Title", cv.getTitle());
                elementIfNotEmpty("ows:Abstract", cv.getAbstract());
            } else {
                elementIfNotEmpty("ows:Title", internationalContentHelper.getTitle(cv));
                elementIfNotEmpty("ows:Abstract", internationalContentHelper.getAbstract(cv));
            }
        }

        /**
         * Spits out the boundingbox for the current coverage taking into account the reprojection policy.
         *
         * @param boundingBox an instance of reference
         * @throws Exception in case we don't manage to retrieve the CRS EPSG code for this bbox (It should not happen!)
         */
        private void handleBoundingBox(BoundingBox boundingBox) throws Exception {
            // CRS for this bbox
            final AttributesImpl attributes = new AttributesImpl();
            // full scan needed for property based CRS plugins (e.g. IAU)
            String identifier = ResourcePool.lookupIdentifier(boundingBox.getCoordinateReferenceSystem(), true);
            if (identifier != null) {
                String srs = SrsSyntax.OGC_HTTP_URI.getSRS(identifier);
                attributes.addAttribute("", "crs", "crs", "", srs);
            }
            start("ows:BoundingBox", attributes);
            // LowerCorner
            element(
                    "ows:LowerCorner",
                    new StringBuilder(
                                    Double.toString(boundingBox.getLowerCorner().getOrdinate(0)))
                            .append(" ")
                            .append(boundingBox.getLowerCorner().getOrdinate(1))
                            .toString());
            // UpperCorner
            element(
                    "ows:UpperCorner",
                    new StringBuilder(
                                    Double.toString(boundingBox.getUpperCorner().getOrdinate(0)))
                            .append(" ")
                            .append(boundingBox.getUpperCorner().getOrdinate(1))
                            .toString());
            end("ows:BoundingBox");
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

        private void handleLanguages() {
            if (i18nRequested) {
                start("ows:Languages");
                List<String> languages = internationalContentHelper.getSupportedLocales().stream()
                        .map(l -> l.toLanguageTag())
                        .collect(Collectors.toList());
                for (String lang : languages) {
                    element("ows:Language", lang);
                }
                end("ows:Languages");
            }
        }

        /** Writes the element if and only if the content is not null and not empty */
        private void elementIfNotEmpty(String elementName, String content) {
            if (isNotBlank(content)) element(elementName, content);
        }
    }
}
