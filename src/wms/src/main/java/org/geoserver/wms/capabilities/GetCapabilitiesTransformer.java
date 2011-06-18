/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.apache.xalan.transformer.TransformerIdentityImpl;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LayerInfo.Type;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.ServiceException;
import org.geoserver.sld.GetStylesResponse;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilities;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.describelayer.DescribeLayerResponse;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.type.Name;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.springframework.util.Assert;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Geotools xml framework based encoder for a Capabilities WMS 1.1.1 document.
 * 
 * @author Gabriel Roldan
 * @version $Id
 * @see GetCapabilities#run(GetCapabilitiesRequest)
 * @see GetCapabilitiesResponse#write(Object, java.io.OutputStream,
 *      org.geoserver.platform.Operation)
 */
public class GetCapabilitiesTransformer extends TransformerBase {
    /** fixed MIME type for the returned capabilities document */
    public static final String WMS_CAPS_MIME = "application/vnd.ogc.wms_xml";

    /** the WMS supported exception formats */
    static final String[] EXCEPTION_FORMATS = { "application/vnd.ogc.se_xml",
            "application/vnd.ogc.se_inimage", };

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

    /**
     * Creates a new WMSCapsTransformer object.
     * 
     * @param wms
     * 
     * @param schemaBaseUrl
     *            the base URL of the current request (usually "http://host:port/geoserver")
     * @param getMapFormats
     *            the list of supported output formats to state for the GetMap request
     * @param getLegendGraphicFormats
     *            the list of supported output formats to state for the GetLegendGraphic request
     * 
     * @throws NullPointerException
     *             if <code>schemaBaseUrl</code> is null;
     */
    public GetCapabilitiesTransformer(WMS wms, String baseURL, Set<String> getMapFormats,
            Set<String> getLegendGraphicFormats,
            Collection<ExtendedCapabilitiesProvider> extCapsProviders) {
        super();
        Assert.notNull(wms);
        Assert.notNull(baseURL, "baseURL");
        Assert.notNull(getMapFormats, "getMapFormats");
        Assert.notNull(getLegendGraphicFormats, "getLegendGraphicFormats");

        this.wmsConfig = wms;
        this.getMapFormats = getMapFormats;
        this.getLegendGraphicFormats = getLegendGraphicFormats;
        this.baseURL = baseURL;
        this.extCapsProviders = extCapsProviders == null ? Collections.EMPTY_LIST
                : extCapsProviders;
        this.setNamespaceDeclarationEnabled(false);
        setIndentation(2);
        final Charset encoding = wms.getCharSet();
        setEncoding(encoding);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new CapabilitiesTranslator(handler, wmsConfig, getMapFormats,
                getLegendGraphicFormats, extCapsProviders);
    }

    /**
     * Gets the <code>Transformer</code> created by the overriden method in the superclass and adds
     * it the system DOCTYPE token pointing to the Capabilities DTD on this server instance.
     * 
     * <p>
     * The DTD is set at the fixed location given by the <code>schemaBaseUrl</code> passed to the
     * constructor <code>+
     * "wms/1.1.1/WMS_MS_Capabilities.dtd</code>.
     * </p>
     * 
     * @return a Transformer propoerly configured to produce DescribeLayer responses.h
     * 
     * @throws TransformerException
     *             if it is thrown by <code>super.createTransformer()</code>
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

        private static final Logger LOGGER = org.geotools.util.logging.Logging
                .getLogger(CapabilitiesTranslator.class.getPackage().getName());

        private static final String EPSG = "EPSG:";

        private static AttributesImpl wmsVersion = new AttributesImpl();

        private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

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

        /**
         * Creates a new CapabilitiesTranslator object.
         * 
         * @param handler
         *            content handler to send sax events to.
         * @param wmsConfig2
         */
        public CapabilitiesTranslator(ContentHandler handler, WMS wmsConfig,
                Set<String> getMapFormats, Set<String> getLegendGraphicFormats,
                Collection<ExtendedCapabilitiesProvider> extCapsProviders) {
            super(handler, null, null);
            this.wmsConfig = wmsConfig;
            this.getMapFormats = getMapFormats;
            this.getLegendGraphicFormats = getLegendGraphicFormats;
            this.extCapsProviders = extCapsProviders;
        }

        /**
         * @param o
         *            the {@link GetCapabilitiesRequest}
         * @throws IllegalArgumentException
         *             if {@code o} is not of the expected type
         */
        public void encode(Object o) throws IllegalArgumentException {
            if (!(o instanceof GetCapabilitiesRequest)) {
                throw new IllegalArgumentException();
            }

            this.request = (GetCapabilitiesRequest) o;

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(new StringBuffer("producing a capabilities document for ").append(
                        request).toString());
            }

            AttributesImpl rootAtts = new AttributesImpl(wmsVersion);
            rootAtts.addAttribute("", "updateSequence", "updateSequence", "",
                    wmsConfig.getUpdateSequence() + "");
            start("WMT_MS_Capabilities", rootAtts);
            handleService();
            handleCapability();
            end("WMT_MS_Capabilities");
        }

        /**
         * Encodes the service metadata section of a WMS capabilities document.
         */
        private void handleService() {
            start("Service");

            final WMSInfo serviceInfo = wmsConfig.getServiceInfo();
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
                    LOGGER.log(Level.WARNING, "WMS online resource seems to be an invalid URL: '"
                            + onlineResource + "'");
                }
            }
            orAtts.addAttribute("", "xlink:href", "xlink:href", "", onlineResource);
            element("OnlineResource", null, orAtts);

            GeoServer geoServer = wmsConfig.getGeoServer();
            ContactInfo contact = geoServer.getGlobal().getContact();
            handleContactInfo(contact);

            element("Fees", serviceInfo.getFees());
            element("AccessConstraints", serviceInfo.getAccessConstraints());
            end("Service");
        }

        /**
         * Encodes contact information in the WMS capabilities document
         * 
         * @param geoServer
         */
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

        /**
         * Turns the keyword list to XML
         * 
         * @param keywords
         */
        private void handleKeywordList(List<String> keywords) {
            start("KeywordList");

            if (keywords != null) {
                for (Iterator<String> it = keywords.iterator(); it.hasNext();) {
                    element("Keyword", it.next());
                }
            }

            end("KeywordList");
        }

        /**
         * Turns the metadata URL list to XML
         * 
         * @param keywords
         */
        private void handleMetadataList(Collection<MetadataLinkInfo> metadataURLs) {
            if (metadataURLs == null) {
                return;
            }

            for (MetadataLinkInfo link : metadataURLs) {

                AttributesImpl lnkAtts = new AttributesImpl();
                lnkAtts.addAttribute("", "type", "type", "", link.getMetadataType());
                start("MetadataURL", lnkAtts);

                element("Format", link.getType());

                AttributesImpl orAtts = new AttributesImpl();
                orAtts.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                orAtts.addAttribute(XLINK_NS, "xlink:type", "xlink:type", "", "simple");
                orAtts.addAttribute("", "xlink:href", "xlink:href", "", link.getContent());
                element("OnlineResource", null, orAtts);

                end("MetadataURL");
            }
        }

        /**
         * Encodes the capabilities metadata section of a WMS capabilities document
         */
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
            element("Format", WMS_CAPS_MIME);

            // build the service URL and make sure it ends with &
            String serviceUrl = buildURL(request.getBaseUrl(), "wms", params("SERVICE", "WMS"),
                    URLType.SERVICE);
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
            for (Iterator<String> it = sortedFormats.iterator(); it.hasNext();) {
                element("Format", String.valueOf(it.next()));
            }

            handleDcpType(serviceUrl, null);
            end("GetMap");

            start("GetFeatureInfo");

            for (String format : wmsConfig.getAvailableFeatureInfoFormats()) {
                element("Format", format);
            }

            handleDcpType(serviceUrl, serviceUrl);
            end("GetFeatureInfo");

            start("DescribeLayer");
            element("Format", DescribeLayerResponse.DESCLAYER_MIME_TYPE);
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
         * @param getUrl
         *            the URL of the onlineresource for HTTP GET method requests
         * @param postUrl
         *            the URL of the onlineresource for HTTP POST method requests
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
            for (ExtendedCapabilitiesProvider cp : extCapsProviders) {
                List<String> roots = cp.getVendorSpecificCapabilitiesRoots(request);
                if (roots == null || roots.size() == 0) {
                    return;
                }
            }

            start("VendorSpecificCapabilities");
            for (ExtendedCapabilitiesProvider cp : extCapsProviders) {
                try {
                    cp.encode(new ExtendedCapabilitiesProvider.Translator() {
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
                    }, wmsConfig.getServiceInfo(), request);
                } catch (Exception e) {
                    throw new ServiceException("Extended capabilities provider threw error", e);
                }
            }
            end("VendorSpecificCapabilities");
        }

        /**
         * Handles the encoding of the layers elements.
         * 
         * <p>
         * This method does a search over the SRS of all the layers to see if there are at least a
         * common one, as needed by the spec: "<i>The root Layer element shall include a sequence of
         * zero or more &lt;SRS&gt; elements listing all SRSes that are common to all subsidiary
         * layers. Use a single SRS element with empty content (like so: "&lt;SRS&gt;&lt;/SRS&gt;")
         * if there is no common SRS."</i>
         * </p>
         * 
         * <p>
         * By the other hand, this search is also used to collecto the whole latlon bbox, as stated
         * by the spec: <i>"The bounding box metadata in Capabilities XML specify the minimum
         * enclosing rectangle for the layer as a whole."</i>
         * </p>
         * 
         * @task TODO: manage this differently when we have the layer list of the WMS service
         *       decoupled from the feature types configured for the server instance. (This involves
         *       nested layers, gridcoverages, etc)
         */
        private void handleLayers() {
            start("Layer");

            final List<LayerInfo> layers;

            // filter the layers if a namespace filter has been set
            if (request.getNamespace() != null) {
                final List<LayerInfo> allLayers = wmsConfig.getLayers();
                layers = new ArrayList<LayerInfo>();

                String namespace = wmsConfig.getNamespaceByPrefix(request.getNamespace());
                for (LayerInfo layer : allLayers) {
                    Name name = layer.getResource().getQualifiedName();
                    if (name.getNamespaceURI().equals(namespace)) {
                        layers.add(layer);
                    }
                }
            } else {
                layers = wmsConfig.getLayers();
            }

            WMSInfo serviceInfo = wmsConfig.getServiceInfo();
            element("Title", serviceInfo.getTitle());
            element("Abstract", serviceInfo.getAbstract());

            List<String> srsList = serviceInfo.getSRS();
            Set<String> srs = new HashSet<String>();
            if (srsList != null) {
                srs.addAll(srsList);
            }
            handleRootCrsList(srs);

            handleRootBbox(layers);

            // now encode each layer individually
            LayerTree featuresLayerTree = new LayerTree(layers);
            handleLayerTree(featuresLayerTree);

            try {
                List<LayerGroupInfo> layerGroups = wmsConfig.getLayerGroups();
                handleLayerGroups(new ArrayList<LayerGroupInfo>(layerGroups));
            } catch (FactoryException e) {
                throw new RuntimeException("Can't obtain Envelope of Layer-Groups: "
                        + e.getMessage(), e);
            } catch (TransformException e) {
                throw new RuntimeException("Can't obtain Envelope of Layer-Groups: "
                        + e.getMessage(), e);
            }

            end("Layer");
        }

        /**
         * Called by <code>handleLayers()</code>, writes down list of supported CRS's for the root
         * Layer.
         * <p>
         * If <code>epsgCodes</code> is not empty, the list of supported CRS identifiers written
         * down to the capabilities document is limited to those in the <code>epsgCodes</code> list.
         * Otherwise, all the GeoServer supported CRS identifiers are used.
         * </p>
         * 
         * @param epsgCodes
         *            possibly empty set of CRS identifiers to limit the number of SRS elements to.
         */
        private void handleRootCrsList(final Set<String> epsgCodes) {
            final Set<String> capabilitiesCrsIdentifiers;
            if (epsgCodes.isEmpty()) {
                comment("All supported EPSG projections:");
                capabilitiesCrsIdentifiers = new LinkedHashSet<String>();
                for (String code : CRS.getSupportedCodes("AUTO")) {
                    if ("WGS84(DD)".equals(code))
                        continue;
                    capabilitiesCrsIdentifiers.add("AUTO:" + code);
                }
                capabilitiesCrsIdentifiers.addAll(CRS.getSupportedCodes("EPSG"));
            } else {
                comment("Limited list of EPSG projections:");
                capabilitiesCrsIdentifiers = new TreeSet<String>(epsgCodes);
            }

            try {
                Iterator<String> it = capabilitiesCrsIdentifiers.iterator();
                String currentSRS;

                while (it.hasNext()) {
                    currentSRS = it.next();
                    if (currentSRS.indexOf(':') == -1) {
                        currentSRS = EPSG + currentSRS;
                    }
                    element("SRS", currentSRS);
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
         * @param ftypes
         *            the collection of FeatureTypeInfo and CoverageInfo objects to traverse
         */
        private void handleRootBbox(Collection<LayerInfo> layers) {

            Envelope latlonBbox = new Envelope();
            Envelope layerBbox = null;

            LOGGER.finer("Collecting summarized latlonbbox and common SRS...");

            for (LayerInfo layer : layers) {
                ResourceInfo resource = layer.getResource();
                layerBbox = resource.getLatLonBoundingBox();
                latlonBbox.expandToInclude(layerBbox);
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Summarized LatLonBBox is " + latlonBbox);
            }

            handleLatLonBBox(latlonBbox);
        }

        /**
         * @param layerTree
         */
        private void handleLayerTree(final LayerTree layerTree) {
            final List<LayerInfo> data = new ArrayList<LayerInfo>(layerTree.getData());
            final Collection<LayerTree> children = layerTree.getChildrens();

            Collections.sort(data, new Comparator<LayerInfo>() {
                public int compare(LayerInfo o1, LayerInfo o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            for (LayerInfo layer : data) {
                // no sense in exposing a geometryless layer through wms...
                boolean wmsExposable = false;
                if (layer.getType() == Type.RASTER || layer.getType() == Type.WMS) {
                    wmsExposable = true;
                } else {
                    try {
                        wmsExposable = layer.getType() == Type.VECTOR
                                && ((FeatureTypeInfo) layer.getResource()).getFeatureType()
                                        .getGeometryDescriptor() != null;
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "An error occurred trying to determine if"
                                + " the layer is geometryless", e);
                    }
                }

                // ask for enabled() instead of isEnabled() to account for disabled resource/store
                if (layer.enabled() && wmsExposable) {
                    try {
                        handleLayer(layer);
                    } catch (Exception e) {
                        // report what layer we failed on to help the admin locate and fix it
                        throw new ServiceException(
                                "Error occurred trying to write out metadata for layer: "
                                        + layer.getName(), e);
                    }
                }
            }

            for (LayerTree childLayerTree : children) {
                start("Layer");
                element("Name", childLayerTree.getName());
                element("Title", childLayerTree.getName());
                handleLayerTree(childLayerTree);
                end("Layer");
            }
        }

        /**
         * Calls super.handleFeatureType to add common FeatureType content such as Name, Title and
         * LatLonBoundingBox, and then writes WMS specific layer properties as Styles, Scale Hint,
         * etc.
         * 
         * @throws IOException
         * 
         * @task TODO: write wms specific elements.
         */
        @SuppressWarnings("deprecation")
        protected void handleLayer(final LayerInfo layer) {
            // HACK: by now all our layers are queryable, since they reference
            // only featuretypes managed by this server
            AttributesImpl qatts = new AttributesImpl();
            boolean queryable = wmsConfig.isQueryable(layer);
            qatts.addAttribute("", "queryable", "queryable", "", queryable ? "1" : "0");
            start("Layer", qatts);
            element("Name", layer.getResource().getNamespace().getPrefix() + ":" + layer.getName());
            // REVISIT: this is bad, layer should have title and anbstract by itself
            element("Title", layer.getResource().getTitle());
            element("Abstract", layer.getResource().getAbstract());
            handleKeywordList(layer.getResource().getKeywords());

            /**
             * @task REVISIT: should getSRS() return the full URL? no - the spec says it should be a
             *       set of <SRS>EPSG:#</SRS>...
             */
            final String srs = layer.getResource().getSRS();
            element("SRS", srs);

            // DJB: I want to be nice to the people reading the capabilities
            // file - I'm going to get the
            // human readable name and stick it in the capabilities file
            // NOTE: this isnt well done because "comment()" isnt in the
            // ContentHandler interface...
            try {
                CoordinateReferenceSystem crs = layer.getResource().getCRS();
                String desc = "WKT definition of this CRS:\n" + crs;
                comment(desc);
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
            }

            Envelope bbox;
            try {
                bbox = layer.getResource().boundingBox();
            } catch (Exception e) {
                throw new RuntimeException("Unexpected error obtaining bounding box for layer "
                        + layer.getName(), e);
            }
            Envelope llbbox = layer.getResource().getLatLonBoundingBox();

            handleLatLonBBox(llbbox);
            // the native bbox might be null
            if (bbox != null) {
                handleBBox(bbox, srs);
            }

            // handle dimensions
            String timeMetadata = null;
            String elevationMetadata = null;
            if (layer.getType() == Type.RASTER) {
                CoverageInfo cvinfo = ((CoverageInfo) layer.getResource());

                if (cvinfo == null)
                    throw new RuntimeException("Unable to acquire coverage resource for layer: "
                            + layer.getName());

                Catalog catalog = cvinfo.getCatalog();

                if (catalog == null)
                    throw new RuntimeException("Unable to acquire catalog resource for layer: "
                            + layer.getName());

                CoverageStoreInfo csinfo = cvinfo.getStore();

                if (csinfo == null)
                    throw new RuntimeException(
                            "Unable to acquire coverage store resource for layer: "
                                    + layer.getName());

                AbstractGridCoverage2DReader reader = null;
                try {
                    reader = (AbstractGridCoverage2DReader) catalog.getResourcePool()
                            .getGridCoverageReader(csinfo, GeoTools.getDefaultHints());
                } catch (Throwable t) {
                    LOGGER.severe("Unable to acquire a reader for this coverage with format: "
                            + csinfo.getFormat().getName());
                }

                if (reader == null)
                    throw new RuntimeException(
                            "Unable to acquire a reader for this coverage with format: "
                                    + csinfo.getFormat().getName());

                final String[] metadataNames = reader.getMetadataNames();

                if (metadataNames != null && metadataNames.length > 0) {
                    // TIME DIMENSION
                    timeMetadata = reader.getMetadataValue("TIME_DOMAIN");

                    if (timeMetadata != null) {
                        AttributesImpl timeDim = new AttributesImpl();
                        timeDim.addAttribute("", "name", "name", "", "time");
                        timeDim.addAttribute("", "units", "units", "", "ISO8601");
                        element("Dimension", null, timeDim);
                    }

                    // ELEVATION DIMENSION
                    elevationMetadata = reader.getMetadataValue("ELEVATION_DOMAIN");

                    if (elevationMetadata != null) {
                        AttributesImpl elevDim = new AttributesImpl();
                        elevDim.addAttribute("", "name", "name", "", "elevation");
                        elevDim.addAttribute("", "units", "units", "", "EPSG:5030");
                        element("Dimension", null, elevDim);
                    }
                }
            }

            // handle extensions
            if (timeMetadata != null && timeMetadata.length() > 0) {
                AttributesImpl timeDim = new AttributesImpl();
                timeDim.addAttribute("", "name", "name", "", "time");
                timeDim.addAttribute("", "default", "default", "", "current");
                element("Extent", timeMetadata, timeDim);
            }

            if (elevationMetadata != null && elevationMetadata.length() > 0) {
                final String[] elevationLevels = elevationMetadata.split(",");
                AttributesImpl elevDim = new AttributesImpl();
                elevDim.addAttribute("", "name", "name", "", "elevation");
                elevDim.addAttribute("", "default", "default", "", elevationLevels[0]);
                element("Extent", elevationMetadata, elevDim);
            }

            // handle data attribution
            handleAttribution(layer);

            // handle metadata URLs
            handleMetadataList(layer.getResource().getMetadataLinks());

            if (layer.getResource() instanceof WMSLayerInfo) {
                // do nothing for the moment, we may want to list the set of cascaded named styles
                // in the future (when we add support for that)
            } else {
                // add the layer style
                start("Style");

                StyleInfo defaultStyle = layer.getDefaultStyle();
                if (defaultStyle == null) {
                    throw new NullPointerException("Layer " + layer.getName()
                            + " has no default style");
                }
                Style ftStyle;
                try {
                    ftStyle = defaultStyle.getStyle();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                element("Name", defaultStyle.getName());
                element("Title", ftStyle.getTitle());
                element("Abstract", ftStyle.getAbstract());
                handleLegendURL(layer.getName(), layer.getLegend(), null);
                end("Style");

                Set<StyleInfo> styles = layer.getStyles();

                for (StyleInfo styleInfo : styles) {
                    try {
                        ftStyle = styleInfo.getStyle();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    start("Style");
                    element("Name", styleInfo.getName());
                    element("Title", ftStyle.getTitle());
                    element("Abstract", ftStyle.getAbstract());
                    handleLegendURL(layer.getName(), null, styleInfo);
                    end("Style");
                }
            }

            end("Layer");
        }

        // /**
        // *
        // * @param originalArray
        // * @return
        // */
        // private static String[] orderDoubleArray(String[] originalArray) {
        // List finalArray = Arrays.asList(originalArray);
        //
        // Collections.sort(finalArray, new Comparator<String>() {
        //
        // public int compare(String o1, String o2) {
        // if (o1.equals(o2))
        // return 0;
        //
        // return (Double.parseDouble(o1) > Double.parseDouble(o2) ? 1 : -1);
        // }
        //
        // });
        //
        // return (String[]) finalArray.toArray(new String[1]);
        // }

        // /**
        // *
        // * @param originalArray
        // * @return
        // */
        // private static String[] orderTimeArray(String[] originalArray) {
        // List finalArray = Arrays.asList(originalArray);
        //
        // Collections.sort(finalArray, new Comparator<String>() {
        // /**
        // * All patterns that are correct regarding the ISO-8601 norm.
        // */
        // final String[] PATTERNS = {
        // "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        // "yyyy-MM-dd'T'HH:mm:sss'Z'",
        // "yyyy-MM-dd'T'HH:mm:ss'Z'",
        // "yyyy-MM-dd'T'HH:mm'Z'",
        // "yyyy-MM-dd'T'HH'Z'",
        // "yyyy-MM-dd",
        // "yyyy-MM",
        // "yyyy"
        // };
        //
        // public int compare(String o1, String o2) {
        // if (o1.equals(o2))
        // return 0;
        //
        // Date d1 = getDate(o1);
        // Date d2 = getDate(o2);
        //
        // if (d1 == null || d2 == null)
        // return 0;
        //
        // return (d1.getTime() > d2.getTime() ? 1 : -1);
        // }
        //
        // private Date getDate(final String value) {
        //
        // // special handling for current keyword
        // if(value.equalsIgnoreCase("current"))
        // return null;
        // for (int i=0; i<PATTERNS.length; i++) {
        // // rebuild formats at each parse, date formats are not thread safe
        // SimpleDateFormat format = new SimpleDateFormat(PATTERNS[i], Locale.CANADA);
        //
        // /* We do not use the standard method DateFormat.parse(String), because if the parsing
        // * stops before the end of the string, the remaining characters are just ignored and
        // * no exception is thrown. So we have to ensure that the whole string is correct for
        // * the format.
        // */
        // ParsePosition pos = new ParsePosition(0);
        // Date time = format.parse(value, pos);
        // if (pos.getIndex() == value.length()) {
        // return time;
        // }
        // }
        //
        // return null;
        // }
        //
        //
        // });
        //
        // return (String[]) finalArray.toArray(new String[1]);
        // }

        protected void handleLayerGroups(List<LayerGroupInfo> layerGroups) throws FactoryException,
                TransformException {
            if (layerGroups == null || layerGroups.size() == 0) {
                return;
            }

            Collections.sort(layerGroups, new Comparator<LayerGroupInfo>() {
                public int compare(LayerGroupInfo o1, LayerGroupInfo o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            for (LayerGroupInfo layerGroup : layerGroups) {
                String layerName = layerGroup.getName();

                AttributesImpl qatts = new AttributesImpl();
                boolean queryable = wmsConfig.isQueryable(layerGroup);
                qatts.addAttribute("", "queryable", "queryable", "", queryable? "1" : "0");
                // qatts.addAttribute("", "opaque", "opaque", "", "1");
                // qatts.addAttribute("", "cascaded", "cascaded", "", "1");
                start("Layer", qatts);
                element("Name", layerName);
                element("Title", layerName);
                element("Abstract", "Layer-Group type layer: " + layerName);

                final ReferencedEnvelope layerGroupBounds = layerGroup.getBounds();
                final ReferencedEnvelope latLonBounds = layerGroupBounds.transform(
                        DefaultGeographicCRS.WGS84, true);

                String authority = layerGroupBounds.getCoordinateReferenceSystem().getIdentifiers()
                        .toArray()[0].toString();

                element("SRS", authority);

                handleLatLonBBox(latLonBounds);
                handleBBox(layerGroupBounds, authority);

                // Aggregated metadata links (see GEOS-4500)
                List<LayerInfo> layers = layerGroup.getLayers();
                Set<MetadataLinkInfo> aggregatedLinks = new HashSet<MetadataLinkInfo>();
                for (LayerInfo layer : layers) {
                    List<MetadataLinkInfo> metadataLinks = layer.getResource().getMetadataLinks();
                    if (metadataLinks != null) {
                        aggregatedLinks.addAll(metadataLinks);
                    }
                }
                handleMetadataList(aggregatedLinks);

                // the layer style is not provided since the group does just have
                // one possibility, the lack of styles that will make it use
                // the default ones for each layer

                end("Layer");
            }
        }

        protected void handleAttribution(LayerInfo layer) {
            AttributionInfo attribution = layer.getAttribution();

            String title = attribution.getTitle();
            String url = attribution.getHref();
            String logoURL = attribution.getLogoURL();
            String logoType = attribution.getLogoType();
            int logoWidth = attribution.getLogoWidth();
            int logoHeight = attribution.getLogoHeight();

            boolean titleGood = (title != null), urlGood = (url != null), logoGood = (logoURL != null
                    && logoType != null && logoWidth > 0 && logoHeight > 0);

            if (titleGood || urlGood || logoGood) {
                start("Attribution");
                if (titleGood)
                    element("Title", title);

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

        /**
         * Writes layer LegendURL pointing to the user supplied icon URL, if any, or to the proper
         * GetLegendGraphic operation if an URL was not supplied by configuration file.
         * 
         * <p>
         * It is common practice to supply a URL to a WMS accesible legend graphic when it is
         * difficult to create a dynamic legend for a layer.
         * </p>
         * 
         * @param ft
         *            The FeatureTypeInfo that holds the legendURL to write out, or<code>null</code>
         *            if dynamically generated.
         * 
         * @task TODO: figure out how to unhack legend parameters such as WIDTH, HEIGHT and FORMAT
         */
        protected void handleLegendURL(String layerName, LegendInfo legend, StyleInfo style) {
            if (legend != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("using user supplied legend URL");
                }

                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "width", "width", "", String.valueOf(legend.getWidth()));
                attrs.addAttribute("", "height", "height", "", String.valueOf(legend.getHeight()));

                start("LegendURL", attrs);

                element("Format", legend.getFormat());
                attrs.clear();
                attrs.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                attrs.addAttribute(XLINK_NS, "type", "xlink:type", "", "simple");
                attrs.addAttribute(XLINK_NS, "href", "xlink:href", "", legend.getOnlineResource());

                element("OnlineResource", null, attrs);

                end("LegendURL");
            } else {
                String defaultFormat = GetLegendGraphicRequest.DEFAULT_FORMAT;

                if (null == wmsConfig.getLegendGraphicOutputFormat(defaultFormat)) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(new StringBuffer("Default legend format (")
                                .append(defaultFormat)
                                .append(")is not supported (jai not available?), can't add LegendURL element")
                                .toString());
                    }

                    return;
                }

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Adding GetLegendGraphic call as LegendURL");
                }

                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "width", "width", "",
                        String.valueOf(GetLegendGraphicRequest.DEFAULT_WIDTH));

                // DJB: problem here is that we do not know the size of the
                // legend apriori - we need
                // to make one and find its height. Not the best way, but it
                // would work quite well.
                // This was advertising a 20*20 icon, but actually producing
                // ones of a different size.
                // An alternative is to just scale the resulting icon to what
                // the server requested, but this isnt
                // the nicest thing since warped images dont look nice. The
                // client should do the warping.

                // however, to actually estimate the size is a bit difficult.
                // I'm going to do the scaling
                // so it obeys the what the request says. For people with a
                // problem with that should consider
                // changing the default size here so that the request is for the
                // correct size.
                attrs.addAttribute("", "height", "height", "",
                        String.valueOf(GetLegendGraphicRequest.DEFAULT_HEIGHT));

                start("LegendURL", attrs);

                element("Format", defaultFormat);
                attrs.clear();

                Map<String, String> params = params("request", "GetLegendGraphic", "format",
                        defaultFormat, "width",
                        String.valueOf(GetLegendGraphicRequest.DEFAULT_WIDTH), "height",
                        String.valueOf(GetLegendGraphicRequest.DEFAULT_HEIGHT), "layer", layerName);
                if (style != null) {
                    params.put("style", style.getName());
                }
                String legendURL = buildURL(request.getBaseUrl(), "wms", params, URLType.SERVICE);

                attrs.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                attrs.addAttribute(XLINK_NS, "type", "xlink:type", "", "simple");
                attrs.addAttribute(XLINK_NS, "href", "xlink:href", "", legendURL);
                element("OnlineResource", null, attrs);

                end("LegendURL");
            }
        }

        /**
         * Encodes a LatLonBoundingBox for the given Envelope.
         * 
         * @param bbox
         */
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

        /**
         * adds a comment to the output xml file. THIS IS A BIG HACK. TODO: do this in the correct
         * manner!
         * 
         * @param comment
         */
        public void comment(String comment) {
            if (contentHandler instanceof TransformerIdentityImpl) // HACK HACK
                                                                   // HACK --
                                                                   // not sure
                                                                   // of the
                                                                   // proper
                                                                   // way to do
                                                                   // this.
            {
                try {
                    TransformerIdentityImpl ch = (TransformerIdentityImpl) contentHandler;
                    ch.comment(comment.toCharArray(), 0, comment.length());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Encodes a BoundingBox for the given Envelope.
         * 
         * @param bbox
         */
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
    }
}
