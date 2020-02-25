/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.geoserver.catalog.Predicates.asc;
import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;
import static org.geoserver.wms.capabilities.CapabilityUtil.validateLegendInfo;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.awt.Dimension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.sld.GetStylesResponse;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilities;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.capabilities.DimensionHelper.Mode;
import org.geoserver.wms.describelayer.XMLDescribeLayerResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.geotools.util.NumberRange;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.locationtech.jts.geom.Envelope;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.InternationalString;
import org.springframework.util.Assert;
import org.vfny.geoserver.util.ResponseUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Geotools xml framework based encoder for a Capabilities WMS 1.1.1 document.
 *
 * @author Gabriel Roldan
 * @version $Id
 * @see GetCapabilities#run(GetCapabilitiesRequest)
 * @see GetCapabilitiesResponse#write(Object, java.io.OutputStream,
 *     org.geoserver.platform.Operation)
 */
public class GetCapabilitiesTransformer extends TransformerBase {
    /** default MIME type for the returned capabilities document */
    public static final String WMS_CAPS_DEFAULT_MIME = "application/vnd.ogc.wms_xml";

    // available MIME types for the returned capabilities document
    public static final String[] WMS_CAPS_AVAIL_MIME = {WMS_CAPS_DEFAULT_MIME, "text/xml"};

    /** the WMS supported exception formats */
    static final String[] EXCEPTION_FORMATS = {
        "application/vnd.ogc.se_xml",
        "application/vnd.ogc.se_inimage",
        "application/vnd.ogc.se_blank",
        "application/json"
    };

    /**
     * Set of supported metadta link types. Links of any other type will be ignored to honor the DTD
     * rule: {@code <!ATTLIST MetadataURL type ( TC211 | FGDC ) #REQUIRED>}
     */
    private static final Set<String> SUPPORTED_MDLINK_TYPES =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("FGDC", "TC211")));

    /**
     * The geoserver base URL to append it the schemas/wms/1.1.1/WMS_MS_Capabilities.dtd DTD
     * location
     */
    private String baseURL;

    /** The list of output formats to state as supported for the GetMap request */
    private Set<String> getMapFormats;

    /** The list of output formats to state as supported for the GetLegendGraphic request */
    private Set<String> getLegendGraphicFormats;

    private WMS wmsConfig;

    private Collection<ExtendedCapabilitiesProvider> extCapsProviders;

    /** if true, forces always including a root Layer element * */
    private Boolean includeRootLayer = null;

    /**
     * Creates a new WMSCapsTransformer object.
     *
     * @param baseURL the base URL of the current request (usually "http://host:port/geoserver")
     * @param getMapFormats the list of supported output formats to state for the GetMap request
     * @param getLegendGraphicFormats the list of supported output formats to state for the
     *     GetLegendGraphic request
     * @param extCapsProviders collection of providers of extended capabilities content
     * @throws NullPointerException if <code>schemaBaseUrl</code> is null;
     */
    public GetCapabilitiesTransformer(
            WMS wms,
            String baseURL,
            Set<String> getMapFormats,
            Set<String> getLegendGraphicFormats,
            Collection<ExtendedCapabilitiesProvider> extCapsProviders) {
        super();
        Assert.notNull(wms, "wms");
        Assert.notNull(baseURL, "baseURL");
        Assert.notNull(getMapFormats, "getMapFormats");
        Assert.notNull(getLegendGraphicFormats, "getLegendGraphicFormats");

        this.wmsConfig = wms;
        this.getMapFormats = getMapFormats;
        this.getLegendGraphicFormats = getLegendGraphicFormats;
        this.baseURL = baseURL;
        this.extCapsProviders =
                extCapsProviders == null ? Collections.EMPTY_LIST : extCapsProviders;
        this.setNamespaceDeclarationEnabled(false);
        setIndentation(2);
        final Charset encoding = wms.getCharSet();
        setEncoding(encoding);
    }

    /**
     * Optional root layer include / exclude flag
     *
     * @param includeRootLayer whether to always include root Layer element , also if there is a
     *     already single top Layer element
     */
    public void setIncludeRootLayer(Boolean includeRootLayer) {
        this.includeRootLayer = includeRootLayer;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new CapabilitiesTranslator(
                handler,
                wmsConfig,
                getMapFormats,
                getLegendGraphicFormats,
                extCapsProviders,
                includeRootLayer);
    }

    /**
     * Gets the <code>Transformer</code> created by the overriden method in the superclass and adds
     * it the system DOCTYPE token pointing to the Capabilities DTD on this server instance.
     *
     * <p>The DTD is set at the fixed location given by the <code>schemaBaseUrl</code> passed to the
     * constructor <code>+
     * "wms/1.1.1/WMS_MS_Capabilities.dtd</code>.
     *
     * @return a Transformer propoerly configured to produce DescribeLayer responses.h
     * @throws TransformerException if it is thrown by <code>super.createTransformer()</code>
     */
    @Override
    public Transformer createTransformer() throws TransformerException {
        Transformer transformer = super.createTransformer();
        String dtdUrl = buildSchemaURL(baseURL, "wms/1.1.1/WMS_MS_Capabilities.dtd");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, dtdUrl);

        return transformer;
    }

    /**
     * @author Gabriel Roldan
     * @version $Id
     */
    private static class CapabilitiesTranslator extends TranslatorSupport {

        private static final Logger LOGGER =
                org.geotools.util.logging.Logging.getLogger(
                        CapabilitiesTranslator.class.getPackage().getName());

        private static final String MIN_DENOMINATOR_ATTR = "min";

        private static final String MAX_DENOMINATOR_ATTR = "max";

        private static final String EPSG = "EPSG:";

        private static AttributesImpl wmsVersion = new AttributesImpl();

        private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

        DimensionHelper dimensionHelper;

        private LegendSample legendSample;

        static {
            wmsVersion.addAttribute("", "version", "version", "", "1.1.1");
        }

        /**
         * The request from wich all the information needed to produce the capabilities document can
         * be obtained
         */
        private GetCapabilitiesRequest request;

        private Set<String> getMapFormats;

        private Set<String> getLegendGraphicFormats;

        private WMS wmsConfig;

        private Collection<ExtendedCapabilitiesProvider> extCapsProviders;

        private final boolean skipping;

        private WMSInfo serviceInfo;

        private Boolean includeRootLayer;

        /**
         * Creates a new CapabilitiesTranslator object.
         *
         * @param handler content handler to send sax events to.
         */
        public CapabilitiesTranslator(
                ContentHandler handler,
                WMS wmsConfig,
                Set<String> getMapFormats,
                Set<String> getLegendGraphicFormats,
                Collection<ExtendedCapabilitiesProvider> extCapsProviders,
                Boolean includeRootlayer) {
            super(handler, null, null);
            this.wmsConfig = wmsConfig;
            this.getMapFormats = getMapFormats;
            this.getLegendGraphicFormats = getLegendGraphicFormats;
            this.extCapsProviders = extCapsProviders;
            this.serviceInfo = wmsConfig.getServiceInfo();

            this.dimensionHelper =
                    new DimensionHelper(Mode.WMS11, wmsConfig) {

                        @Override
                        protected void element(String element, String content, Attributes atts) {
                            CapabilitiesTranslator.this.element(element, content, atts);
                        }

                        @Override
                        protected void element(String element, String content) {
                            CapabilitiesTranslator.this.element(element, content);
                        }
                    };
            legendSample = GeoServerExtensions.bean(LegendSample.class);
            this.skipping =
                    ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                            wmsConfig.getGeoServer().getGlobal().getResourceErrorHandling());
            this.includeRootLayer = includeRootlayer;
        }

        /**
         * @param o the {@link GetCapabilitiesRequest}
         * @throws IllegalArgumentException if {@code o} is not of the expected type
         */
        public void encode(Object o) throws IllegalArgumentException {
            if (!(o instanceof GetCapabilitiesRequest)) {
                throw new IllegalArgumentException();
            }

            this.request = (GetCapabilitiesRequest) o;

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        new StringBuffer("producing a capabilities document for ")
                                .append(request)
                                .toString());
            }

            AttributesImpl rootAtts = new AttributesImpl(wmsVersion);
            rootAtts.addAttribute(
                    "", "updateSequence", "updateSequence", "", wmsConfig.getUpdateSequence() + "");
            start("WMT_MS_Capabilities", rootAtts);
            handleService();
            handleCapability();
            end("WMT_MS_Capabilities");
        }

        /** Encodes the service metadata section of a WMS capabilities document. */
        private void handleService() {
            start("Service");

            element("Name", "OGC:WMS");
            element("Title", serviceInfo.getTitle());
            element("Abstract", serviceInfo.getAbstract());

            handleKeywordList(serviceInfo.getKeywords());

            AttributesImpl orAtts = new AttributesImpl();
            orAtts.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
            orAtts.addAttribute(XLINK_NS, "xlink:type", "xlink:type", "", "simple");

            String onlineResource = serviceInfo.getOnlineResource();
            if (onlineResource == null || onlineResource.trim().length() == 0) {
                String requestBaseUrl = request.getBaseUrl();
                onlineResource = buildURL(requestBaseUrl, null, null, URLType.SERVICE);
            } else {
                try {
                    new URL(onlineResource);
                } catch (MalformedURLException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "WMS online resource seems to be an invalid URL: '"
                                    + onlineResource
                                    + "'");
                }
            }
            orAtts.addAttribute("", "xlink:href", "xlink:href", "", onlineResource);
            element("OnlineResource", null, orAtts);

            GeoServer geoServer = wmsConfig.getGeoServer();
            ContactInfo contact = geoServer.getSettings().getContact();
            handleContactInfo(contact);

            String fees = serviceInfo.getFees();
            element("Fees", fees == null ? "none" : fees);
            String constraints = serviceInfo.getAccessConstraints();
            element("AccessConstraints", constraints == null ? "none" : constraints);
            end("Service");
        }

        /** Encodes contact information in the WMS capabilities document */
        public void handleContactInfo(ContactInfo contact) {
            start("ContactInformation");

            start("ContactPersonPrimary");
            element("ContactPerson", contact.getContactPerson());
            element("ContactOrganization", contact.getContactOrganization());
            end("ContactPersonPrimary");

            element("ContactPosition", contact.getContactPosition());

            start("ContactAddress");
            element("AddressType", contact.getAddressType());
            element("Address", contact.getAddress());
            element("City", contact.getAddressCity());
            element("StateOrProvince", contact.getAddressState());
            element("PostCode", contact.getAddressPostalCode());
            element("Country", contact.getAddressCountry());
            end("ContactAddress");

            element("ContactVoiceTelephone", contact.getContactVoice());
            element("ContactFacsimileTelephone", contact.getContactFacsimile());
            element("ContactElectronicMailAddress", contact.getContactEmail());

            end("ContactInformation");
        }

        /** Turns the keyword list to XML */
        private void handleKeywordList(List<KeywordInfo> keywords) {
            start("KeywordList");

            if (keywords != null) {
                for (Iterator<KeywordInfo> it = keywords.iterator(); it.hasNext(); ) {
                    element("Keyword", it.next().getValue());
                }
            }

            end("KeywordList");
        }

        /** Turns the metadata URL list to XML */
        private void handleMetadataList(Collection<MetadataLinkInfo> metadataURLs) {
            if (metadataURLs == null) {
                return;
            }

            for (MetadataLinkInfo link : metadataURLs) {
                if (!SUPPORTED_MDLINK_TYPES.contains(link.getMetadataType())) {
                    continue;
                }
                AttributesImpl lnkAtts = new AttributesImpl();
                lnkAtts.addAttribute("", "type", "type", "", link.getMetadataType());
                start("MetadataURL", lnkAtts);

                element("Format", link.getType());

                String content = ResponseUtils.proxifyMetadataLink(link, request.getBaseUrl());

                AttributesImpl orAtts = new AttributesImpl();
                orAtts.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                orAtts.addAttribute(XLINK_NS, "xlink:type", "xlink:type", "", "simple");
                orAtts.addAttribute("", "xlink:href", "xlink:href", "", content);
                element("OnlineResource", null, orAtts);

                end("MetadataURL");
            }
        }

        /** Turns the data URL list to XML */
        private void handleDataList(Collection<DataLinkInfo> dataURLs) {
            if (dataURLs == null) {
                return;
            }

            for (DataLinkInfo link : dataURLs) {

                start("DataURL");

                element("Format", link.getType());

                String content = ResponseUtils.proxifyDataLink(link, request.getBaseUrl());

                AttributesImpl orAtts = new AttributesImpl();
                orAtts.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                orAtts.addAttribute(XLINK_NS, "xlink:type", "xlink:type", "", "simple");
                orAtts.addAttribute("", "xlink:href", "xlink:href", "", content);
                element("OnlineResource", null, orAtts);

                end("DataURL");
            }
        }

        /** Encodes the capabilities metadata section of a WMS capabilities document */
        private void handleCapability() {
            start("Capability");
            handleRequest();
            handleException();
            handleVendorSpecificCapabilities();
            handleSLD();
            handleLayers();
            end("Capability");
        }

        private void handleRequest() {
            start("Request");

            start("GetCapabilities");

            // add all the supported MIME types for the capabilities document
            for (String mimeType : WMS_CAPS_AVAIL_MIME) {
                element("Format", mimeType);
            }

            // build the service URL and make sure it ends with &
            String serviceUrl =
                    buildURL(
                            request.getBaseUrl(), "wms", params("SERVICE", "WMS"), URLType.SERVICE);
            serviceUrl = appendQueryString(serviceUrl, "");

            handleDcpType(serviceUrl, serviceUrl);
            end("GetCapabilities");

            start("GetMap");

            List<String> sortedFormats = new ArrayList<String>(getMapFormats);
            Collections.sort(sortedFormats);
            // this is a hack necessary to make cite tests pass: we need an output format
            // that is equal to the mime type as the first one....
            if (sortedFormats.contains("image/png")) {
                sortedFormats.remove("image/png");
                sortedFormats.add(0, "image/png");
            }
            for (Iterator<String> it = sortedFormats.iterator(); it.hasNext(); ) {
                element("Format", String.valueOf(it.next()));
            }

            handleDcpType(serviceUrl, null);
            end("GetMap");

            start("GetFeatureInfo");

            for (String format : wmsConfig.getAllowedFeatureInfoFormats()) {
                element("Format", format);
            }

            handleDcpType(serviceUrl, serviceUrl);
            end("GetFeatureInfo");

            start("DescribeLayer");
            element("Format", XMLDescribeLayerResponse.DESCLAYER_MIME_TYPE);
            handleDcpType(serviceUrl, null);
            end("DescribeLayer");

            start("GetLegendGraphic");

            for (String format : getLegendGraphicFormats) {
                element("Format", format);
            }

            handleDcpType(serviceUrl, null);
            end("GetLegendGraphic");

            start("GetStyles");
            element("Format", GetStylesResponse.SLD_MIME_TYPE);
            handleDcpType(serviceUrl, null);
            end("GetStyles");

            end("Request");
        }

        /**
         * Encodes a <code>DCPType</code> fragment for HTTP GET and POST methods.
         *
         * @param getUrl the URL of the onlineresource for HTTP GET method requests
         * @param postUrl the URL of the onlineresource for HTTP POST method requests
         */
        private void handleDcpType(String getUrl, String postUrl) {
            AttributesImpl orAtts = new AttributesImpl();
            orAtts.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
            orAtts.addAttribute("", "xlink:type", "xlink:type", "", "simple");
            orAtts.addAttribute("", "xlink:href", "xlink:href", "", getUrl);
            start("DCPType");
            start("HTTP");

            if (getUrl != null) {
                start("Get");
                element("OnlineResource", null, orAtts);
                end("Get");
            }

            if (postUrl != null) {
                orAtts.setAttribute(2, "", "xlink:href", "xlink:href", "", postUrl);
                start("Post");
                element("OnlineResource", null, orAtts);
                end("Post");
            }

            end("HTTP");
            end("DCPType");
        }

        private void handleException() {
            start("Exception");

            for (String exceptionFormat : GetCapabilitiesTransformer.EXCEPTION_FORMATS) {
                element("Format", exceptionFormat);
            }
            if (JSONType.isJsonpEnabled()) {
                element("Format", JSONType.jsonp);
            }
            end("Exception");
        }

        private void handleSLD() {
            AttributesImpl sldAtts = new AttributesImpl();

            String supportsSLD = wmsConfig.supportsSLD() ? "1" : "0";
            String supportsUserLayer = wmsConfig.supportsUserLayer() ? "1" : "0";
            String supportsUserStyle = wmsConfig.supportsUserStyle() ? "1" : "0";
            String supportsRemoteWFS = wmsConfig.supportsRemoteWFS() ? "1" : "0";
            sldAtts.addAttribute("", "SupportSLD", "SupportSLD", "", supportsSLD);
            sldAtts.addAttribute("", "UserLayer", "UserLayer", "", supportsUserLayer);
            sldAtts.addAttribute("", "UserStyle", "UserStyle", "", supportsUserStyle);
            sldAtts.addAttribute("", "RemoteWFS", "RemoteWFS", "", supportsRemoteWFS);

            start("UserDefinedSymbolization", sldAtts);
            // djb: this was removed, even though they are correct - the CITE tests have an
            // incorrect DTD
            // element("SupportedSLDVersion","1.0.0"); //djb: added that we support this. We support
            // partial 1.1
            end("UserDefinedSymbolization");

            // element("UserDefinedSymbolization", null, sldAtts);
        }

        private void handleVendorSpecificCapabilities() {
            /*
             * Check whether some caps provider contributes to the internal DTD. If not, there's no
             * need to output the VendorSpecificCapabilities element. Moreover, the document will
             * not validate if it's there but not declared in the internal DTD
             */
            int numberRoots = 0;
            for (ExtendedCapabilitiesProvider cp : extCapsProviders) {
                List<String> roots = cp.getVendorSpecificCapabilitiesRoots(request);
                if (roots != null) {
                    numberRoots += roots.size();
                }
            }
            if (numberRoots == 0) {
                return;
            }

            start("VendorSpecificCapabilities");
            for (ExtendedCapabilitiesProvider cp : extCapsProviders) {
                try {
                    cp.encode(
                            new ExtendedCapabilitiesProvider.Translator() {
                                public void start(String element) {
                                    CapabilitiesTranslator.this.start(element);
                                }

                                public void start(String element, Attributes attributes) {
                                    CapabilitiesTranslator.this.start(element, attributes);
                                }

                                public void chars(String text) {
                                    CapabilitiesTranslator.this.chars(text);
                                }

                                public void end(String element) {
                                    CapabilitiesTranslator.this.end(element);
                                }
                            },
                            serviceInfo,
                            request);
                } catch (Exception e) {
                    throw new ServiceException("Extended capabilities provider threw error", e);
                }
            }
            end("VendorSpecificCapabilities");
        }

        /**
         * Handles the encoding of the layers elements.
         *
         * <p>This method does a search over the SRS of all the layers to see if there are at least
         * a common one, as needed by the spec: "<i>The root Layer element shall include a sequence
         * of zero or more &lt;SRS&gt; elements listing all SRSes that are common to all subsidiary
         * layers. Use a single SRS element with empty content (like so: "&lt;SRS&gt;&lt;/SRS&gt;")
         * if there is no common SRS."</i>
         *
         * <p>By the other hand, this search is also used to collecto the whole latlon bbox, as
         * stated by the spec: <i>"The bounding box metadata in Capabilities XML specify the minimum
         * enclosing rectangle for the layer as a whole."</i>
         *
         * @task TODO: manage this differently when we have the layer list of the WMS service
         *     decoupled from the feature types configured for the server instance. (This involves
         *     nested layers, gridcoverages, etc)
         */
        private void handleLayers() {
            // get filtered and ordered layers:
            final List<LayerInfo> layers = getOrderedLayers();
            final List<LayerGroupInfo> layerGroups = getOrderedLayerGroups();
            Set<LayerInfo> layersAlreadyProcessed =
                    getLayersInGroups(new ArrayList<LayerGroupInfo>(layerGroups));

            if (includeRootLayer(layers, layerGroups, layersAlreadyProcessed)) {
                start("Layer");

                // WMSInfo serviceInfo = wmsConfig.getServiceInfo();
                if (StringUtils.isBlank(serviceInfo.getRootLayerTitle())) {
                    element("Title", serviceInfo.getTitle());
                } else {
                    element("Title", serviceInfo.getRootLayerTitle());
                }
                if (StringUtils.isBlank(serviceInfo.getRootLayerAbstract())) {
                    element("Abstract", serviceInfo.getAbstract());
                } else {
                    element("Abstract", serviceInfo.getRootLayerAbstract());
                }
                Set<String> srs = getServiceSRSList();
                handleRootCrsList(srs);

                handleRootBbox(layers);

                // handle AuthorityURL
                handleAuthorityURL(serviceInfo.getAuthorityURLs());

                // handle identifiers
                handleLayerIdentifiers(serviceInfo.getIdentifiers());

                // encode layer groups
                try {
                    handleLayerGroups(new ArrayList<LayerGroupInfo>(layerGroups), false);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Can't obtain Envelope of Layer-Groups: " + e.getMessage(), e);
                }

                // now encode each layer individually
                LayerTree featuresLayerTree = new LayerTree(layers);
                handleLayerTree(featuresLayerTree, layersAlreadyProcessed, false);

                end("Layer");
            } else {
                if (layerGroups.size() > 0) {
                    try {
                        handleLayerGroups(new ArrayList<LayerGroupInfo>(layerGroups), true);
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Can't obtain Envelope of Layer-Groups: " + e.getMessage(), e);
                    }
                } else {
                    // now encode the single layer
                    LayerTree featuresLayerTree = new LayerTree(layers);
                    handleLayerTree(featuresLayerTree, layersAlreadyProcessed, true);
                }
            }
        }

        private boolean includeRootLayer(
                List<LayerInfo> layers,
                List<LayerGroupInfo> layerGroups,
                Set<LayerInfo> layersAlreadyProcessed) {
            final PublishedInfo singleRoot =
                    getSingleRoot(layers, layerGroups, layersAlreadyProcessed);
            // is there a single top element? if not, we have to include root
            if (singleRoot != null) {
                // first we check if the user has specified a rootLayer param
                if (includeRootLayer != null) {
                    return includeRootLayer.booleanValue();
                }
                // then we check for layer / group level setting
                Boolean layerIncludeRoot =
                        singleRoot
                                .getMetadata()
                                .get(PublishedInfo.ROOT_IN_CAPABILITIES, Boolean.class);
                if (layerIncludeRoot != null) {
                    return layerIncludeRoot.booleanValue();
                }
                // finally we return global WMS setting
                return wmsConfig.isRootLayerInCapabilitesEnabled();
            }
            return true;
        }

        private PublishedInfo getSingleRoot(
                List<LayerInfo> layers,
                List<LayerGroupInfo> layerGroups,
                Set<LayerInfo> layersAlreadyProcessed) {
            List<LayerInfo> rootLayers =
                    layers.stream()
                            .filter(layer -> includeLayer(layersAlreadyProcessed, layer))
                            .collect(Collectors.toList());
            List<LayerGroupInfo> rootGroups = filterNestedGroups(layerGroups);
            if (rootLayers.size() == 1 && rootGroups.size() == 0) {
                return rootLayers.get(0);
            }
            if (rootLayers.size() == 0 && rootGroups.size() == 1) {
                return rootGroups.get(0);
            }
            return null;
        }

        private Set<String> getServiceSRSList() {
            List<String> srsList = serviceInfo.getSRS();
            Set<String> srs = new LinkedHashSet<String>();
            if (srsList != null) {
                srs.addAll(srsList);
            }
            for (ExtendedCapabilitiesProvider provider : extCapsProviders) {
                provider.customizeRootCrsList(srs);
            }
            return srs;
        }

        /**
         * Returns a list of name-ordered LayerGroupInfo, and filtered by namespace if needed
         *
         * @return LayerGroupInfo list
         */
        private List<LayerGroupInfo> getOrderedLayerGroups() {
            Catalog catalog = wmsConfig.getCatalog();
            // namespace filter
            Filter filter = Predicates.acceptAll();
            addNameSpaceFilterIfNeed(filter, "workspace.name");
            // order by name ASC
            SortBy order = asc("name");
            // get list from iterator
            try (CloseableIterator<LayerGroupInfo> iter =
                    catalog.list(LayerGroupInfo.class, filter, null, null, order)) {
                return Lists.newArrayList(iter);
            }
        }

        /**
         * Returns a list of name-ordered LayerInfo, and filtered by namespace if needed
         *
         * @return LayerInfo list
         */
        private List<LayerInfo> getOrderedLayers() {
            Catalog catalog = wmsConfig.getCatalog();
            Filter filter = equal("enabled", Boolean.TRUE);
            // namespace filter
            addNameSpaceFilterIfNeed(filter, "resource.namespace.prefix");
            // order by name ASC
            SortBy order = asc("name");
            // get list:
            try (CloseableIterator<LayerInfo> iter =
                    catalog.list(LayerInfo.class, filter, null, null, order)) {
                return Lists.newArrayList(iter);
            }
        }

        /**
         * If the current request contains a namespace we build a filter using the provided property
         * and request namespace and adds it to the provided filter. If the request doesn't contain
         * a namespace the original filter is returned as is.
         */
        private Filter addNameSpaceFilterIfNeed(Filter filter, String nameSpaceProperty) {
            String nameSpacePrefix = request.getNamespace();
            if (nameSpacePrefix == null) {
                return filter;
            }
            Filter equals = Predicates.equal(nameSpaceProperty, nameSpacePrefix);
            return Predicates.and(filter, equals);
        }

        /**
         * Called by <code>handleLayers()</code>, writes down list of supported CRS's for the root
         * Layer.
         *
         * <p>If <code>epsgCodes</code> is not empty, the list of supported CRS identifiers written
         * down to the capabilities document is limited to those in the <code>epsgCodes</code> list.
         * Otherwise, all the GeoServer supported CRS identifiers are used.
         *
         * @param epsgCodes possibly empty set of CRS identifiers to limit the number of SRS
         *     elements to.
         */
        private void handleRootCrsList(final Set<String> epsgCodes) {
            final Set<String> capabilitiesCrsIdentifiers;
            if (epsgCodes.isEmpty()) {
                comment("All supported EPSG projections:");
                capabilitiesCrsIdentifiers = new LinkedHashSet<String>();
                for (String code : CRS.getSupportedCodes("AUTO")) {
                    if ("WGS84(DD)".equals(code)) continue;
                    capabilitiesCrsIdentifiers.add("AUTO:" + code);
                }
                capabilitiesCrsIdentifiers.addAll(CRS.getSupportedCodes("EPSG"));
            } else {
                comment("Limited list of EPSG projections:");
                capabilitiesCrsIdentifiers = new LinkedHashSet<String>(epsgCodes);
            }

            try {
                Iterator<String> it = capabilitiesCrsIdentifiers.iterator();
                String currentSRS;

                while (it.hasNext()) {
                    String code = it.next();
                    if (!"WGS84(DD)".equals(code)) {
                        currentSRS = qualifySRS(code);
                        element("SRS", currentSRS);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        /**
         * Called by <code>handleLayers()</code>, iterates over the available featuretypes and
         * coverages to summarize their LatLonBBox'es and write the aggregated bounds for the root
         * layer.
         *
         * @param layers the collection of LayerInfo objects to traverse
         */
        private void handleRootBbox(Collection<LayerInfo> layers) {

            Envelope latlonBbox = new Envelope();
            Envelope layerBbox = null;

            LOGGER.finer("Collecting summarized latlonbbox and common SRS...");

            for (LayerInfo layer : layers) {
                ResourceInfo resource = layer.getResource();
                layerBbox = resource.getLatLonBoundingBox();
                if (layerBbox != null) latlonBbox.expandToInclude(layerBbox);
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Summarized LatLonBBox is " + latlonBbox);
            }

            handleLatLonBBox(latlonBbox);
            handleAdditionalBBox(
                    new ReferencedEnvelope(latlonBbox, DefaultGeographicCRS.WGS84), null, null);
        }

        private boolean isExposable(LayerInfo layer) {
            if (!layer.isEnabled()) {
                return false;
            }
            return WMS.isWmsExposable(layer);
        }

        /** @param layerTree */
        private void handleLayerTree(
                final LayerTree layerTree, Set<LayerInfo> layersAlreadyProcessed, boolean isRoot) {
            final List<LayerInfo> data = new ArrayList<LayerInfo>(layerTree.getData());
            final Collection<LayerTree> children = layerTree.getChildrens();

            Collections.sort(
                    data,
                    new Comparator<LayerInfo>() {
                        public int compare(LayerInfo o1, LayerInfo o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });

            for (LayerInfo layer : data) {
                // ask for enabled() instead of isEnabled() to account for disabled resource/store
                // don't expose a geometryless layer through wms
                if (includeLayer(layersAlreadyProcessed, layer)) {
                    try {
                        mark();
                        handleLayer(layer, isRoot);
                        commit();
                    } catch (Exception e) {
                        if (skipping) {
                            reset();
                            LOGGER.log(
                                    Level.WARNING,
                                    "Error writing metadata; skipping layer: " + layer.getName(),
                                    e);
                        } else {
                            // report what layer we failed on to help the admin locate and fix it
                            throw new ServiceException(
                                    "Error occurred trying to write out metadata for layer: "
                                            + layer.getName(),
                                    e);
                        }
                    }
                }
            }

            for (LayerTree childLayerTree : children) {
                start("Layer");
                element("Name", childLayerTree.getName());
                element("Title", childLayerTree.getName());
                handleLayerTree(childLayerTree, layersAlreadyProcessed, false);
                end("Layer");
            }
        }

        private boolean includeLayer(Set<LayerInfo> layersAlreadyProcessed, LayerInfo layer) {
            return layer.enabled() && !layersAlreadyProcessed.contains(layer) && isExposable(layer);
        }

        /**
         * Calls super.handleFeatureType to add common FeatureType content such as Name, Title and
         * LatLonBoundingBox, and then writes WMS specific layer properties as Styles, Scale Hint,
         * etc.
         *
         * @task TODO: write wms specific elements.
         */
        @SuppressWarnings("deprecation")
        protected void handleLayer(final LayerInfo layer, boolean isRoot) throws IOException {
            // HACK: by now all our layers are queryable, since they reference
            // only featuretypes managed by this server
            AttributesImpl qatts = new AttributesImpl();
            boolean queryable = wmsConfig.isQueryable(layer);
            qatts.addAttribute("", "queryable", "queryable", "", queryable ? "1" : "0");
            boolean opaque = wmsConfig.isOpaque(layer);
            qatts.addAttribute("", "opaque", "opaque", "", opaque ? "1" : "0");
            Integer cascaded = wmsConfig.getCascadedHopCount(layer);
            if (cascaded != null) {
                qatts.addAttribute("", "cascaded", "cascaded", "", String.valueOf(cascaded));
            }

            start("Layer", qatts);
            element("Name", layer.prefixedName());
            // REVISIT: this is bad, layer should have title and anbstract by itself
            element("Title", layer.getResource().getTitle());
            element("Abstract", layer.getResource().getAbstract());
            handleKeywordList(layer.getResource().getKeywords());

            /**
             * @task REVISIT: should getSRS() return the full URL? no - the spec says it should be a
             *     set of <SRS>EPSG:#</SRS>...
             */
            final String srs = layer.getResource().getSRS();
            if (isRoot) {
                Set<String> srsList = getServiceSRSList();
                handleRootCrsList(srsList);
            } else {
                element("SRS", srs);
            }

            ReferencedEnvelope bbox;
            try {
                bbox = layer.getResource().boundingBox();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Unexpected error obtaining bounding box for layer " + layer.getName(), e);
            }
            Envelope llbbox = layer.getResource().getLatLonBoundingBox();

            handleLatLonBBox(llbbox);
            // the native bbox might be null
            if (bbox != null) {
                handleBBox(bbox, srs);
                handleAdditionalBBox(bbox, srs, layer);
            }

            // handle dimensions
            if (layer.getType() == PublishedType.VECTOR) {
                dimensionHelper.handleVectorLayerDimensions(layer);
            } else if (layer.getType() == PublishedType.RASTER) {
                dimensionHelper.handleRasterLayerDimensions(layer);
            } else if (layer.getType() == PublishedType.WMTS) {
                dimensionHelper.handleWMTSLayerDimensions(layer);
            }

            // handle data attribution
            handleAttribution(layer);

            // handle AuthorityURL
            if (isRoot) {
                handleAuthorityURL(serviceInfo.getAuthorityURLs());
            }
            handleAuthorityURL(layer.getAuthorityURLs());

            // handle identifiers
            handleLayerIdentifiers(layer.getIdentifiers());

            // handle metadata URLs
            handleMetadataList(layer.getResource().getMetadataLinks());

            // handle DataURLs
            handleDataList(layer.getResource().getDataLinks());

            // if WMTS layer do nothing for the moment, we may want to list the set of
            // cascaded named styles
            // in the future (when we add support for that)
            if (!(layer.getResource() instanceof WMTSLayerInfo)) {
                // add the layer style
                start("Style");

                StyleInfo defaultStyle = layer.getDefaultStyle();
                if (defaultStyle == null) {
                    throw new NullPointerException(
                            "Layer " + layer.getName() + " has no default style");
                }
                handleCommonStyleElements(defaultStyle);
                handleLegendURL(layer, defaultStyle.getLegend(), null, defaultStyle);
                end("Style");

                for (StyleInfo styleInfo : layer.getStyles()) {
                    start("Style");
                    handleCommonStyleElements(styleInfo);
                    handleLegendURL(layer, styleInfo.getLegend(), styleInfo, styleInfo);
                    end("Style");
                }
            }
            handleScaleHint(layer);

            end("Layer");
        }

        private void handleCommonStyleElements(StyleInfo styleInfo) {
            Style ftStyle;
            try {
                ftStyle = styleInfo.getStyle();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            element("Name", styleInfo.prefixedName());
            if (ftStyle.getDescription() != null) {
                if (ftStyle.getDescription().getTitle() != null) {
                    element("Title", ftStyle.getDescription().getTitle());
                } else {
                    // Title is not required by the SLD spec, but it is required
                    // by the WMS 1.1 DTD so we have to provide something.
                    element("Title", styleInfo.prefixedName());
                }
                element("Abstract", ftStyle.getDescription().getAbstract());
            }
        }

        private void element(String element, InternationalString is) {
            if (is != null) {
                element(element, is.toString());
            }
        }

        /**
         * Inserts the ScaleHint element in the layer information.
         *
         * <p>The process is consistent with the following criteria:
         *
         * <pre>
         * a) min = 0.0, max= infinity => ScaleHint is not generated
         * b) max=value => <ScaleHint min=0 max=value/>
         * c) min=value => <ScaleHint min=value max=infinity/>
         * </pre>
         */
        private void handleScaleHint(PublishedInfo layer) {

            try {
                NumberRange<Double> scaleDenominators =
                        CapabilityUtil.searchMinMaxScaleDenominator(layer);

                // allow extension points to customize
                for (ExtendedCapabilitiesProvider provider : extCapsProviders) {
                    scaleDenominators =
                            provider.overrideScaleDenominators(layer, scaleDenominators);
                }
                // makes the element taking into account that if the min and max denominators have
                // got the default
                // values the ScaleHint element is not generated
                if ((scaleDenominators.getMinimum() == 0.0)
                        && (scaleDenominators.getMaximum() == Double.POSITIVE_INFINITY)) {
                    return;
                }

                Double minScaleHint;
                Double maxScaleHint;
                boolean scaleUnitPixel =
                        wmsConfig.getScalehintUnitPixel() != null
                                && wmsConfig.getScalehintUnitPixel();
                if (scaleUnitPixel) {
                    // makes the scalehint computation taking into account the OGC standardized
                    // rendering pixel size" that is 0.28mm × 0.28mm (millimeters).
                    minScaleHint = CapabilityUtil.computeScaleHint(scaleDenominators.getMinValue());
                    maxScaleHint = CapabilityUtil.computeScaleHint(scaleDenominators.getMaxValue());
                } else {
                    minScaleHint = scaleDenominators.getMinValue();
                    maxScaleHint = scaleDenominators.getMaxValue();
                }

                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute(
                        "",
                        MIN_DENOMINATOR_ATTR,
                        MIN_DENOMINATOR_ATTR,
                        "",
                        String.valueOf(minScaleHint));
                attrs.addAttribute(
                        "",
                        MAX_DENOMINATOR_ATTR,
                        MAX_DENOMINATOR_ATTR,
                        "",
                        String.valueOf(maxScaleHint));

                element("ScaleHint", null, attrs);

            } catch (IOException e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

        private String qualifySRS(String srs) {
            if (srs.indexOf(':') == -1) {
                srs = EPSG + srs;
            }
            return srs;
        }

        protected void handleLayerGroup(LayerGroupInfo layerGroup, boolean isRoot)
                throws TransformException, FactoryException, IOException {
            // String layerName = layerGroup.getName();
            String layerName = layerGroup.prefixedName();

            AttributesImpl qatts = new AttributesImpl();
            boolean queryable = wmsConfig.isQueryable(layerGroup);
            qatts.addAttribute("", "queryable", "queryable", "", queryable ? "1" : "0");
            // qatts.addAttribute("", "opaque", "opaque", "", "1");
            // qatts.addAttribute("", "cascaded", "cascaded", "", "1");
            start("Layer", qatts);

            if (!LayerGroupInfo.Mode.CONTAINER.equals(layerGroup.getMode())) {
                element("Name", layerName);
            }

            if (StringUtils.isEmpty(layerGroup.getTitle())) {
                element("Title", layerName);
            } else {
                element("Title", layerGroup.getTitle());
            }

            if (StringUtils.isEmpty(layerGroup.getAbstract())) {
                element("Abstract", "Layer-Group type layer: " + layerName);
            } else {
                element("Abstract", layerGroup.getAbstract());
            }

            // handle keywords
            handleKeywordList(layerGroup.getKeywords());

            final ReferencedEnvelope layerGroupBounds = layerGroup.getBounds();
            final ReferencedEnvelope latLonBounds =
                    layerGroupBounds.transform(DefaultGeographicCRS.WGS84, true);

            String authority =
                    layerGroupBounds
                            .getCoordinateReferenceSystem()
                            .getIdentifiers()
                            .toArray()[0]
                            .toString();
            if (isRoot) {
                Set<String> srsList = getServiceSRSList();
                handleRootCrsList(srsList);
            } else {
                element("SRS", authority);
            }

            handleLatLonBBox(latLonBounds);
            handleBBox(layerGroupBounds, authority);
            handleAdditionalBBox(layerGroupBounds, authority, layerGroup);

            if (LayerGroupInfo.Mode.EO.equals(layerGroup.getMode())) {
                LayerInfo rootLayer = layerGroup.getRootLayer();

                // handle dimensions
                if (rootLayer.getType() == PublishedType.VECTOR) {
                    dimensionHelper.handleVectorLayerDimensions(rootLayer);
                } else if (rootLayer.getType() == PublishedType.RASTER) {
                    dimensionHelper.handleRasterLayerDimensions(rootLayer);
                }
            }

            // handle data attribution
            handleAttribution(layerGroup);

            // handle AuthorityURL
            if (isRoot) {
                handleAuthorityURL(serviceInfo.getAuthorityURLs());
            }
            handleAuthorityURL(layerGroup.getAuthorityURLs());

            // handle identifiers
            handleLayerIdentifiers(layerGroup.getIdentifiers());

            Collection<MetadataLinkInfo> metadataLinks = layerGroup.getMetadataLinks();
            if (metadataLinks == null || metadataLinks.isEmpty()) {
                // Aggregated metadata links (see GEOS-4500)
                Set<MetadataLinkInfo> aggregatedLinks = new HashSet<MetadataLinkInfo>();
                for (LayerInfo layer : Iterables.filter(layerGroup.getLayers(), LayerInfo.class)) {
                    List<MetadataLinkInfo> metadataLinksLayer =
                            layer.getResource().getMetadataLinks();
                    if (metadataLinksLayer != null) {
                        aggregatedLinks.addAll(metadataLinksLayer);
                    }
                }
                metadataLinks = aggregatedLinks;
            }
            handleMetadataList(metadataLinks);

            // handle children layers and groups
            if (!LayerGroupInfo.Mode.OPAQUE_CONTAINER.equals(layerGroup.getMode())
                    && !LayerGroupInfo.Mode.SINGLE.equals(layerGroup.getMode())) {
                for (PublishedInfo child : layerGroup.getLayers()) {
                    if (child instanceof LayerInfo) {
                        LayerInfo layer = (LayerInfo) child;
                        if (isExposable(layer)) {
                            handleLayer((LayerInfo) child, false);
                        }
                    } else {
                        handleLayerGroup((LayerGroupInfo) child, false);
                    }
                }
            }

            // the layer style is not provided since the group does just have
            // one possibility, the lack of styles that will make it use
            // the default ones for each layer

            handleScaleHint(layerGroup);

            end("Layer");
        }

        protected Set<LayerInfo> getLayersInGroups(List<LayerGroupInfo> layerGroups) {
            Set<LayerInfo> layersAlreadyProcessed = new HashSet<LayerInfo>();

            if (layerGroups == null || layerGroups.size() == 0) {
                return layersAlreadyProcessed;
            }

            List<LayerGroupInfo> topLevelGroups = filterNestedGroups(layerGroups);

            for (LayerGroupInfo layerGroup : topLevelGroups) {
                getLayersInGroup(layerGroup, layersAlreadyProcessed);
            }

            return layersAlreadyProcessed;
        }

        private void getLayersInGroup(
                LayerGroupInfo layerGroup, Set<LayerInfo> layersAlreadyProcessed) {

            if (LayerGroupInfo.Mode.EO.equals(layerGroup.getMode())) {
                layersAlreadyProcessed.add(layerGroup.getRootLayer());
            }

            // handle children layers and groups
            if (LayerGroupInfo.Mode.OPAQUE_CONTAINER.equals(layerGroup.getMode())) {
                // just hide the layers in the group
                layersAlreadyProcessed.addAll(layerGroup.layers());
            } else if (!LayerGroupInfo.Mode.SINGLE.equals(layerGroup.getMode())) {
                for (PublishedInfo child : layerGroup.getLayers()) {
                    if (child instanceof LayerInfo) {
                        LayerInfo layer = (LayerInfo) child;
                        if (isExposable(layer)) {
                            layersAlreadyProcessed.add((LayerInfo) child);
                        }
                    } else {
                        getLayersInGroup((LayerGroupInfo) child, layersAlreadyProcessed);
                    }
                }
            }
        }

        protected void handleLayerGroups(List<LayerGroupInfo> layerGroups, boolean isRoot)
                throws FactoryException, TransformException, IOException {
            if (layerGroups != null) {
                List<LayerGroupInfo> topLevelGropus = filterNestedGroups(layerGroups);

                for (LayerGroupInfo layerGroup : topLevelGropus) {
                    try {
                        mark();
                        handleLayerGroup(layerGroup, isRoot);
                        commit();
                    } catch (Exception e) {
                        // report what layer we failed on to help the admin locate and fix it
                        if (skipping) {
                            if (layerGroup != null) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Skipping layer group "
                                                + layerGroup.getName()
                                                + " as its caps document element failed to generate",
                                        e);
                            } else {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Skipping a null layer group during caps document generation",
                                        e);
                            }

                            reset();
                        } else {
                            throw new ServiceException(
                                    "Error occurred trying to write out metadata for layer group: "
                                            + layerGroup.getName(),
                                    e);
                        }
                    }
                }
            }
        }

        /**
         * Returns a list of top level groups, that is, the ones that are not nested within other
         * layer groups
         */
        private List<LayerGroupInfo> filterNestedGroups(List<LayerGroupInfo> allGroups) {
            LinkedHashSet<LayerGroupInfo> result = new LinkedHashSet<LayerGroupInfo>(allGroups);
            for (LayerGroupInfo group : allGroups) {
                for (PublishedInfo pi : group.getLayers()) {
                    if (pi instanceof LayerGroupInfo) {
                        result.remove(pi);
                    }
                }
            }

            return new ArrayList<LayerGroupInfo>(result);
        }

        protected void handleAttribution(PublishedInfo layer) {
            AttributionInfo attribution = layer.getAttribution();

            if (attribution != null) {

                String title = attribution.getTitle();
                String url = attribution.getHref();
                String logoURL = attribution.getLogoURL();
                String logoType = attribution.getLogoType();
                int logoWidth = attribution.getLogoWidth();
                int logoHeight = attribution.getLogoHeight();

                boolean titleGood = (title != null),
                        urlGood = (url != null),
                        logoGood =
                                (logoURL != null
                                        && logoType != null
                                        && logoWidth > 0
                                        && logoHeight > 0);

                if (titleGood || urlGood || logoGood) {
                    start("Attribution");
                    if (titleGood) element("Title", title);

                    if (urlGood) {
                        AttributesImpl urlAttributes = new AttributesImpl();
                        urlAttributes.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                        urlAttributes.addAttribute(XLINK_NS, "type", "xlink:type", "", "simple");
                        urlAttributes.addAttribute(XLINK_NS, "href", "xlink:href", "", url);
                        element("OnlineResource", null, urlAttributes);
                    }

                    if (logoGood) {
                        AttributesImpl logoAttributes = new AttributesImpl();
                        logoAttributes.addAttribute("", "", "height", "", "" + logoHeight);
                        logoAttributes.addAttribute("", "", "width", "", "" + logoWidth);
                        start("LogoURL", logoAttributes);
                        element("Format", logoType);

                        AttributesImpl urlAttributes = new AttributesImpl();
                        urlAttributes.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                        urlAttributes.addAttribute(XLINK_NS, "type", "xlink:type", "", "simple");
                        urlAttributes.addAttribute(XLINK_NS, "href", "xlink:href", "", logoURL);

                        element("OnlineResource", null, urlAttributes);
                        end("LogoURL");
                    }

                    end("Attribution");
                }
            }
        }

        /**
         * Writes layer LegendURL pointing to the user supplied icon URL, if any, or to the proper
         * GetLegendGraphic operation if an URL was not supplied by configuration file.
         *
         * <p>It is common practice to supply a URL to a WMS accesible legend graphic when it is
         * difficult to create a dynamic legend for a layer.
         *
         * @param layer The LayerInfo that holds the legendURL to write out, or<code>null</code> if
         *     dynamically generated.
         * @task TODO: figure out how to unhack legend parameters such as WIDTH, HEIGHT and FORMAT
         */
        protected void handleLegendURL(
                LayerInfo layer, LegendInfo legend, StyleInfo style, StyleInfo sampleStyle) {
            // add CapabilityUtil.validateLegendInfo
            int legendWidth = GetLegendGraphicRequest.DEFAULT_WIDTH;
            int legendHeight = GetLegendGraphicRequest.DEFAULT_HEIGHT;

            String defaultFormat = GetLegendGraphicRequest.DEFAULT_FORMAT;

            if (validateLegendInfo(legend)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("using user supplied legend URL");
                }
                // reading sizes of external graphics
                legendWidth = legend.getWidth();
                legendHeight = legend.getHeight();
                // remove any charset info
                defaultFormat = legend.getFormat().replaceFirst(";charset=utf-8", "");

            } else if (sampleStyle != null) {
                // delegate to legendSample the calculus of proper legend size for
                // the given style
                Dimension dimension;
                try {
                    dimension = legendSample.getLegendURLSize(sampleStyle);
                    if (dimension != null) {
                        legendWidth = (int) dimension.getWidth();
                        legendHeight = (int) dimension.getHeight();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error getting LegendURL dimensions from sample", e);
                }
            }

            if (null == wmsConfig.getLegendGraphicOutputFormat(defaultFormat)) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning(
                            new StringBuffer("Default legend format (")
                                    .append(defaultFormat)
                                    .append(
                                            ")is not supported (jai not available?), can't add LegendURL element")
                                    .toString());
                }

                return;
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Adding GetLegendGraphic call as LegendURL");
            }

            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "width", "width", "", String.valueOf(legendWidth));

            attrs.addAttribute("", "height", "height", "", String.valueOf(legendHeight));

            start("LegendURL", attrs);

            element("Format", defaultFormat);
            attrs.clear();

            String layerName = layer.prefixedName();
            Map<String, String> params =
                    params(
                            "request",
                            "GetLegendGraphic",
                            "format",
                            defaultFormat,
                            "width",
                            String.valueOf(GetLegendGraphicRequest.DEFAULT_WIDTH),
                            "height",
                            String.valueOf(GetLegendGraphicRequest.DEFAULT_HEIGHT),
                            "layer",
                            layerName);
            if (style != null) {
                params.put("style", style.getName());
            }
            String legendURL = buildURL(request.getBaseUrl(), "wms", params, URLType.SERVICE);
            CapabilityUtil.addGetLegendAttributes(attrs, legendURL, XLINK_NS);
            element("OnlineResource", null, attrs);

            end("LegendURL");
        }

        /** Encodes a LatLonBoundingBox for the given Envelope. */
        private void handleLatLonBBox(Envelope bbox) {
            String minx = String.valueOf(bbox.getMinX());
            String miny = String.valueOf(bbox.getMinY());
            String maxx = String.valueOf(bbox.getMaxX());
            String maxy = String.valueOf(bbox.getMaxY());

            AttributesImpl bboxAtts = new AttributesImpl();
            bboxAtts.addAttribute("", "minx", "minx", "", minx);
            bboxAtts.addAttribute("", "miny", "miny", "", miny);
            bboxAtts.addAttribute("", "maxx", "maxx", "", maxx);
            bboxAtts.addAttribute("", "maxy", "maxy", "", maxy);

            element("LatLonBoundingBox", null, bboxAtts);
        }

        /** Encodes a BoundingBox for the given Envelope. */
        private void handleBBox(Envelope bbox, String SRS) {
            String minx = String.valueOf(bbox.getMinX());
            String miny = String.valueOf(bbox.getMinY());
            String maxx = String.valueOf(bbox.getMaxX());
            String maxy = String.valueOf(bbox.getMaxY());

            AttributesImpl bboxAtts = new AttributesImpl();
            bboxAtts.addAttribute("", "SRS", "SRS", "", SRS);
            bboxAtts.addAttribute("", "minx", "minx", "", minx);
            bboxAtts.addAttribute("", "miny", "miny", "", miny);
            bboxAtts.addAttribute("", "maxx", "maxx", "", maxx);
            bboxAtts.addAttribute("", "maxy", "maxy", "", maxy);

            element("BoundingBox", null, bboxAtts);
        }

        private void handleAdditionalBBox(
                ReferencedEnvelope bbox, String srs, PublishedInfo layer) {
            // TODO: this method is copied from wms 1.3 caps (along with a lot of things), we
            // should refactor
            // WMSInfo info = wmsConfig.getServiceInfo();
            if (serviceInfo.isBBOXForEachCRS() && !serviceInfo.getSRS().isEmpty()) {
                // output bounding box for each supported service srs
                for (String crs : serviceInfo.getSRS()) {
                    crs = qualifySRS(crs);
                    if (srs != null && srs.equals(crs)) {
                        continue; // already did this one
                    }

                    CoordinateReferenceSystem targetCrs = null;
                    try {
                        targetCrs = CRS.decode(crs);
                        ReferencedEnvelope tbbox = bbox.transform(targetCrs, true);
                        handleBBox(tbbox, crs);
                    } catch (Exception e) {
                        // An exception is occurred during transformation. Try using a
                        // ProjectionHandler
                        try {
                            // Try transformation with a ProjectionHandler
                            CapabilitiesTransformerProjectionHandler handler =
                                    CapabilitiesTransformerProjectionHandler.create(
                                            targetCrs, bbox.getCoordinateReferenceSystem());
                            if (handler == null) {
                                // Still no luck. Report the original issue
                                LOGGER.warning(
                                        String.format(
                                                "Unable to transform bounding box for '%s' layer"
                                                        + " to %s",
                                                layer != null ? layer.getName() : "root", crs));
                                if (LOGGER.isLoggable(Level.FINE)) {
                                    LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                                }
                            } else {
                                ReferencedEnvelope tbbox =
                                        handler.transformEnvelope(bbox, targetCrs);
                                handleBBox(tbbox, crs);
                            }
                        } catch (FactoryException | TransformException e1) {
                            LOGGER.warning(
                                    String.format(
                                            "Unable to transform bounding box for '%s' layer"
                                                    + " to %s",
                                            layer != null ? layer.getName() : "root", crs));
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.log(Level.FINE, e1.getLocalizedMessage(), e1);
                            }
                        }
                    }
                }
            }
        }

        /**
         * e.g. {@code <AuthorityURL name="gcmd"><OnlineResource xlink:href="some_url" ...
         * /></AuthorityURL>}
         */
        private void handleAuthorityURL(List<AuthorityURLInfo> authorityURLs) {
            if (authorityURLs == null || authorityURLs.isEmpty()) {
                return;
            }

            String name;
            String href;
            AttributesImpl atts = new AttributesImpl();
            for (AuthorityURLInfo url : authorityURLs) {
                name = url.getName();
                href = url.getHref();
                if (name == null || href == null) {
                    LOGGER.warning("Ignoring AuthorityURL, name: " + name + ", href: " + href);
                    continue;
                }
                atts.clear();
                atts.addAttribute("", "name", "name", "", name);
                start("AuthorityURL", atts);

                atts.clear();
                atts.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                atts.addAttribute("", "xlink:href", "xlink:href", "", href);
                element("OnlineResource", null, atts);
                end("AuthorityURL");
            }
        }

        /** e.g. {@code <Identifier authority="gcmd">id_value</Identifier>} */
        private void handleLayerIdentifiers(List<LayerIdentifierInfo> identifiers) {
            if (identifiers == null || identifiers.isEmpty()) {
                return;
            }

            String authority;
            String id;
            AttributesImpl atts = new AttributesImpl();
            for (LayerIdentifierInfo identifier : identifiers) {
                authority = identifier.getAuthority();
                id = identifier.getIdentifier();
                if (authority == null || id == null) {
                    LOGGER.warning(
                            "Ignoring layer Identifier, authority: "
                                    + authority
                                    + ", identifier: "
                                    + id);
                    continue;
                }
                atts.clear();
                atts.addAttribute("", "authority", "authority", "", authority);
                element("Identifier", id, atts);
            }
        }
    }
}
