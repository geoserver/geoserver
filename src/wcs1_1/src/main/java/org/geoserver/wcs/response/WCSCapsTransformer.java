/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.geoserver.ows.util.ResponseUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wcs11.GetCapabilitiesType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.wcs.WCSInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.vfny.geoserver.global.CoverageInfoLabelComparator;
import org.vfny.geoserver.util.ResponseUtils;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the job of encoding a WCS
 * 1.1 Capabilities document.
 *
 * @author Alessio Fabiani (alessio.fabiani@gmail.com)
 * @author Simone Giannecchini (simboss1@gmail.com)
 * @author Andrea Aime, TOPP
 */
public class WCSCapsTransformer extends TransformerBase {
    protected static final Logger LOGGER =
            Logging.getLogger(WCSCapsTransformer.class.getPackage().getName());

    protected static final String WCS_URI = "http://www.opengis.net/wcs/1.1.1";

    protected static final String CUR_VERSION = "1.1.1";

    protected static final String XSI_PREFIX = "xsi";

    protected static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    protected WCSInfo wcs;

    protected Catalog catalog;

    protected final boolean skipMisconfigured;

    /** Creates a new WFSCapsTransformer object. */
    public WCSCapsTransformer(GeoServer gs) {
        super();
        this.wcs = gs.getService(WCSInfo.class);
        this.catalog = gs.getCatalog();
        this.skipMisconfigured =
                ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                        gs.getGlobal().getResourceErrorHandling());
        setNamespaceDeclarationEnabled(false);
    }

    public Translator createTranslator(ContentHandler handler) {
        return new WCS111CapsTranslator(handler);
    }

    protected class WCS111CapsTranslator extends TranslatorSupport {
        /**
         * @uml.property name="request"
         * @uml.associationEnd multiplicity="(0 1)"
         */
        protected GetCapabilitiesType request;

        /** Creates a new WFSCapsTranslator object. */
        public WCS111CapsTranslator(ContentHandler handler) {
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

            final String locationDef =
                    WCS_URI
                            + " "
                            + buildSchemaURL(
                                    request.getBaseUrl(), "wcs/1.1.1/wcsGetCapabilities.xsd");

            attributes.addAttribute("", locationAtt, locationAtt, "", locationDef);
            attributes.addAttribute(
                    "", "updateSequence", "updateSequence", "", String.valueOf(updateSequence));
            start("wcs:Capabilities", attributes);

            // handle the sections directive
            boolean allSections;
            List<String> sections;
            if (request.getSections() == null) {
                allSections = true;
                sections = Collections.emptyList();
            } else {
                sections = request.getSections().getSection();
                allSections = sections.contains("All");
            }
            final Set<String> knownSections =
                    new HashSet<String>(
                            Arrays.asList(
                                    "ServiceIdentification",
                                    "ServiceProvider",
                                    "OperationsMetadata",
                                    "Contents",
                                    "All"));
            for (String section : sections) {
                if (!knownSections.contains(section))
                    throw new WcsException(
                            "Unknown section " + section,
                            WcsExceptionCode.InvalidParameterValue,
                            "Sections");
            }

            // encode the actual capabilities contents taking into consideration
            // the sections
            if (requestedUpdateSequence < updateSequence) {
                if (allSections || sections.contains("ServiceIdentification"))
                    handleServiceIdentification();
                if (allSections || sections.contains("ServiceProvider")) handleServiceProvider();
                if (allSections || sections.contains("OperationsMetadata"))
                    handleOperationsMetadata();
                if (allSections || sections.contains("Contents")) handleContents();
            }

            end("wcs:Capabilities");
        }

        /** Handles the service identification of the capabilities document. */
        protected void handleServiceIdentification() {
            start("ows:ServiceIdentification");
            element("ows:Title", wcs.getTitle());
            element("ows:Abstract", wcs.getAbstract());
            handleKeywords(wcs.getKeywords());
            element("ows:ServiceType", "WCS");
            element("ows:ServiceTypeVersion", "1.1.0");
            element("ows:ServiceTypeVersion", "1.1.1");

            String fees = wcs.getFees();
            if ((fees == null) || "".equals(fees)) {
                fees = "NONE";
            }
            element("ows:Fees", fees);

            String accessConstraints = wcs.getAccessConstraints();
            if ((accessConstraints == null) || "".equals(accessConstraints)) {
                accessConstraints = "NONE";
            }
            element("ows:AccessConstraints", accessConstraints);
            end("ows:ServiceIdentification");
        }

        /** Handles the service provider of the capabilities document. */
        protected void handleServiceProvider() {
            start("ows:ServiceProvider");
            SettingsInfo settings = wcs.getGeoServer().getSettings();
            element("ows:ProviderName", settings.getContact().getContactOrganization());
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(
                    "",
                    "xlink:href",
                    "xlink:href",
                    "",
                    settings.getOnlineResource() != null ? settings.getOnlineResource() : "");
            element("ows:ProviderSite", null, attributes);

            handleContact();

            end("ows:ServiceProvider");
        }

        /**
         * Handles the OperationMetadata portion of the document, printing out the operations and
         * where to bind to them.
         */
        protected void handleOperationsMetadata() {
            start("ows:OperationsMetadata");
            handleOperation("GetCapabilities", null);
            handleOperation("DescribeCoverage", null);
            handleOperation(
                    "GetCoverage",
                    new HashMap<String, List<String>>() {
                        {
                            put("store", Arrays.asList("True", "False"));
                        }
                    });

            // specify that we do support xml post encoding, clause 8.3.2.2 of
            // the WCS 1.1.1 spec
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(null, "name", "name", null, "PostEncoding");
            start("ows:Constraint", attributes);
            start("ows:AllowedValues");
            element("ows:Value", "XML");
            end("ows:AllowedValues");
            end("ows:Constraint");

            end("ows:OperationsMetadata");
        }

        protected void handleOperation(
                String capabilityName, Map<String, List<String>> parameters) {
            AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(null, "name", "name", null, capabilityName);
            start("ows:Operation", attributes);

            final String url =
                    appendQueryString(
                            buildURL(request.getBaseUrl(), "wcs", null, URLType.SERVICE), "");

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
        protected void handleKeywords(List kwords) {
            if (kwords != null && kwords.size() > 0) {
                start("ows:Keywords");

                if (kwords != null) {
                    for (Iterator it = kwords.iterator(); it.hasNext(); ) {
                        element("ows:Keyword", it.next().toString());
                    }
                }

                end("ows:Keywords");
            }
        }

        /** Handles contacts. */
        protected void handleContact() {
            final GeoServer gs = wcs.getGeoServer();
            start("ows:ServiceContact");

            ContactInfo contact = gs.getSettings().getContact();
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

            String or = gs.getSettings().getOnlineResource();
            if ((or != null) && !"".equals(or.trim())) {
                AttributesImpl attributes = new AttributesImpl();
                attributes.addAttribute("", "xlink:href", "xlink:href", "", or);
                start("ows:OnlineResource", attributes);
                end("OnlineResource");
            }

            end("ows:ContactInfo");
            end("ows:ServiceContact");
        }

        protected void handleEnvelope(ReferencedEnvelope envelope) {
            start("ows:WGS84BoundingBox");
            element(
                    "ows:LowerCorner",
                    new StringBuffer(Double.toString(envelope.getLowerCorner().getOrdinate(0)))
                            .append(" ")
                            .append(envelope.getLowerCorner().getOrdinate(1))
                            .toString());
            element(
                    "ows:UpperCorner",
                    new StringBuffer(Double.toString(envelope.getUpperCorner().getOrdinate(0)))
                            .append(" ")
                            .append(envelope.getUpperCorner().getOrdinate(1))
                            .toString());
            end("ows:WGS84BoundingBox");
        }

        protected void handleContents() {
            start("wcs:Contents");

            List<CoverageInfo> coverages =
                    new ArrayList<CoverageInfo>(wcs.getGeoServer().getCatalog().getCoverages());

            // filter out disabled coverages
            for (Iterator<CoverageInfo> it = coverages.iterator(); it.hasNext(); ) {
                CoverageInfo cv = (CoverageInfo) it.next();
                if (!cv.enabled()) {
                    it.remove();
                }
            }

            // filter out coverages that are not in the requested namespace
            if (request.getNamespace() != null) {
                String namespace = request.getNamespace();
                for (Iterator it = coverages.iterator(); it.hasNext(); ) {
                    CoverageInfo cv = (CoverageInfo) it.next();
                    if (!namespace.equals(cv.getStore().getWorkspace().getName())) it.remove();
                }
            }

            Collections.sort(coverages, new CoverageInfoLabelComparator());
            for (Iterator i = coverages.iterator(); i.hasNext(); ) {
                CoverageInfo cv = (CoverageInfo) i.next();
                try {
                    mark();
                    handleCoverageSummary(cv);
                    commit();
                } catch (Exception e) {
                    if (skipMisconfigured) {
                        reset();
                        LOGGER.log(
                                Level.SEVERE,
                                "Skipping coverage "
                                        + cv.prefixedName()
                                        + " as its capabilities generation failed",
                                e);
                    } else {
                        throw new RuntimeException(
                                "Capabilities document generation failed on coverage "
                                        + cv.prefixedName(),
                                e);
                    }
                }
            }

            end("wcs:Contents");
        }

        protected void handleCoverageSummary(CoverageInfo cv) {
            start("wcs:CoverageSummary");
            elementIfNotEmpty("ows:Title", cv.getTitle());
            elementIfNotEmpty("ows:Abstract", cv.getDescription());
            handleKeywords(cv.getKeywords());
            handleMetadataLinks(cv.getMetadataLinks(), "simple");
            handleEnvelope(cv.getLatLonBoundingBox());
            element("wcs:Identifier", cv.prefixedName());

            end("wcs:CoverageSummary");
        }

        /**
         * Converts each metadata URL to XML.
         *
         * @param links a collection of links
         * @param linkType the type of links
         */
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
                element("ows:Metadata", null, attributes);
            }
        }

        /** Writes the element if and only if the content is not null and not empty */
        protected void elementIfNotEmpty(String elementName, String content) {
            if (content != null && !"".equals(content.trim())) element(elementName, content);
        }
    }
}
