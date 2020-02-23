/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.CapabilitiesTransformer.WFS1_1.CapabilitiesTranslator1_1;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.v1_0.OGC;
import org.geotools.filter.v2_0.FES;
import org.geotools.gml3.GML;
import org.geotools.util.Version;
import org.geotools.xlink.XLINK;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.geotools.xs.XS;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.Schema;
import org.opengis.filter.capability.FunctionName;
import org.opengis.parameter.Parameter;
import org.vfny.geoserver.global.FeatureTypeInfoTitleComparator;
import org.vfny.geoserver.util.ResponseUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the job of encoding a WFS
 * 1.0 Capabilities document.
 *
 * @author Gabriel Roldan, Axios Engineering
 * @author Chris Holmes
 * @author Justin Deoliveira
 * @version $Id$
 */
public abstract class CapabilitiesTransformer extends TransformerBase {
    /** logger */
    protected static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(
                    CapabilitiesTransformer.class.getPackage().getName());
    // Constants for constraints (it's case sensitive)
    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";

    enum Sections {
        ServiceIdentification,
        ServiceProvider,
        OperationsMetadata,
        FeatureTypeList,
        Filter_Capabilities,
        All
    };

    static final Set<Sections> ALL_SECTIONS =
            Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    Sections.ServiceIdentification,
                                    Sections.ServiceProvider,
                                    Sections.OperationsMetadata,
                                    Sections.FeatureTypeList,
                                    Sections.Filter_Capabilities)));

    /** identifer of a http get + post request */
    protected static final String HTTP_GET = "Get";

    protected static final String HTTP_POST = "Post";

    /** wfs namespace */
    protected static final String WFS_URI = "http://www.opengis.net/wfs";

    /** xml schema namespace + prefix */
    protected static final String XSI_PREFIX = "xsi";

    protected static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    /** filter namesapce + prefix */
    protected static final String OGC_PREFIX = "ogc";

    protected static final String OGC_URI = OGC.NAMESPACE;

    /** wfs service */
    protected WFSInfo wfs;

    /** wfs version */
    protected WFSInfo.Version version;

    /** catalog */
    protected Catalog catalog;

    /** Creates a new CapabilitiesTransformer object. */
    public CapabilitiesTransformer(WFSInfo wfs, WFSInfo.Version version, Catalog catalog) {
        super();
        setNamespaceDeclarationEnabled(false);

        this.wfs = wfs;
        this.version = version;
        this.catalog = catalog;
    }

    /**
     * It turns out that the he WFS 1.0 and 1.1 specifications don't actually support an
     * updatesequence-based getcapabilities operation. There's no mention of an updatesequence
     * request parameter in the getcapabilities operation, and there's no normative behaviour
     * description for what the updatesequence parameter in the capabilities document should *do*.
     *
     * <p>So this behaviour is not used right now, at all (as of Jan 2007)
     */
    public void verifyUpdateSequence(GetCapabilitiesRequest request) throws ServiceException {
        long reqUS = -1;
        if (request.getUpdateSequence() != null) {
            try {
                reqUS = Long.parseLong(request.getUpdateSequence());
            } catch (NumberFormatException nfe) {
                throw new ServiceException(
                        "GeoServer only accepts numbers in the updateSequence parameter");
            }
        }
        long geoUS = wfs.getGeoServer().getGlobal().getUpdateSequence();
        if (reqUS > geoUS) {
            throw new ServiceException(
                    "Client supplied an updateSequence that is greater than the current sever updateSequence",
                    "InvalidUpdateSequence");
        }
        if (reqUS == geoUS) {
            throw new ServiceException(
                    "WFS capabilities document is current (updateSequence = " + geoUS + ")",
                    "CurrentUpdateSequence");
        }
    }

    protected Set<FunctionName> getAvailableFunctionNames() {
        // Sort them up for easier visual inspection
        SortedSet sortedFunctions =
                new TreeSet(
                        new Comparator() {
                            public int compare(Object o1, Object o2) {
                                String n1 = ((FunctionName) o1).getName();
                                String n2 = ((FunctionName) o2).getName();

                                return n1.toLowerCase().compareTo(n2.toLowerCase());
                            }
                        });

        Set<FunctionFactory> factories = CommonFactoryFinder.getFunctionFactories(null);
        for (FunctionFactory factory : factories) {
            sortedFunctions.addAll(factory.getFunctionNames());
        }

        return sortedFunctions;
    }

    protected String[] getAvailableOutputFormatNames(String first, Version wfsVersion) {
        List<String> oflist = new ArrayList<String>();
        Collection featureProducers =
                GeoServerExtensions.extensions(WFSGetFeatureOutputFormat.class);
        for (Iterator i = featureProducers.iterator(); i.hasNext(); ) {
            WFSGetFeatureOutputFormat format = (WFSGetFeatureOutputFormat) i.next();
            if (format.canHandle(wfsVersion)) {
                for (Iterator f = format.getOutputFormats().iterator(); f.hasNext(); ) {
                    oflist.add(f.next().toString());
                }
            }
        }
        Collections.sort(oflist);
        if (oflist.contains(first)) {
            oflist.remove(first);
            oflist.add(0, first);
        }
        return (String[]) oflist.toArray(new String[oflist.size()]);
    }

    protected void updateSequence(AttributesImpl attributes) {
        attributes.addAttribute(
                "",
                "updateSequence",
                "updateSequence",
                "",
                wfs.getGeoServer().getGlobal().getUpdateSequence() + "");
    }

    protected void registerNamespaces(AttributesImpl attributes) {
        List<NamespaceInfo> namespaces = catalog.getNamespaces();
        for (NamespaceInfo namespace : namespaces) {
            String prefix = namespace.getPrefix();
            String uri = namespace.getURI();

            // ignore xml prefix
            if ("xml".equals(prefix)) {
                continue;
            }

            String prefixDef = "xmlns:" + prefix;

            attributes.addAttribute("", prefixDef, prefixDef, "", uri);
        }
    }

    protected AttributesImpl attributes(String... nameValues) {
        AttributesImpl atts = new AttributesImpl();

        for (int i = 0; i < nameValues.length; i += 2) {
            String name = nameValues[i];
            String valu = nameValues[i + 1];

            atts.addAttribute(null, null, name, null, valu);
        }

        return atts;
    }

    protected Map.Entry parameter(final String name, final Object value) {
        return new Map.Entry() {
            public Object getKey() {
                return name;
            }

            public Object getValue() {
                return value;
            }

            public Object setValue(Object value) {
                return null;
            }
        };
    }

    /** Returns the list of requested sections (never null or emtpy) */
    protected Set<Sections> getSections(GetCapabilitiesRequest request) {
        List<String> sectionNames = request.getSections();
        if (sectionNames == null || sectionNames.isEmpty()) {
            return ALL_SECTIONS;
        }
        Set<Sections> sections = new HashSet<>();
        for (String sectionName : sectionNames) {
            try {
                Sections section = Sections.valueOf(sectionName);
                sections.add(section);
            } catch (IllegalArgumentException e) {
                WFSException exception =
                        new WFSException(
                                request, "Unknown section " + sectionName, "InvalidParameterValue");
                exception.setLocator("sections");
                throw exception;
            }
        }

        if (sections.contains(Sections.All)) {
            return ALL_SECTIONS;
        }
        return sections;
    }

    /** Transformer for wfs 1.0 capabilities document. */
    public static class WFS1_0 extends CapabilitiesTransformer {
        protected final boolean skipMisconfigured;

        public WFS1_0(WFSInfo wfs, Catalog catalog) {
            super(wfs, WFSInfo.Version.V_10, catalog);
            this.skipMisconfigured =
                    ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                            wfs.getGeoServer().getGlobal().getResourceErrorHandling());
        }

        public Translator createTranslator(ContentHandler handler) {
            return new CapabilitiesTranslator1_0(handler);
        }

        protected class CapabilitiesTranslator1_0 extends TranslatorSupport {
            protected GetCapabilitiesRequest request;

            public CapabilitiesTranslator1_0(ContentHandler handler) {
                super(handler, null, null);
            }

            public void encode(Object object) throws IllegalArgumentException {
                request = GetCapabilitiesRequest.adapt(object);

                // Not used.  WFS 1.1 and 1.0 don't actually support updatesequence
                // verifyUpdateSequence(request);

                AttributesImpl attributes = new AttributesImpl();
                attributes.addAttribute("", "version", "version", "", "1.0.0");
                attributes.addAttribute("", "xmlns", "xmlns", "", WFS_URI);

                List<NamespaceInfo> namespaces = catalog.getNamespaces();

                for (NamespaceInfo namespace : namespaces) {
                    String prefix = namespace.getPrefix();
                    String uri = namespace.getURI();

                    if ("xml".equals(prefix)) {
                        continue;
                    }

                    String prefixDef = "xmlns:" + prefix;
                    attributes.addAttribute("", prefixDef, prefixDef, "", uri);
                }

                // filter
                attributes.addAttribute(
                        "", "xmlns:" + OGC_PREFIX, "xmlns:" + OGC_PREFIX, "", OGC_URI);

                // xml schema
                attributes.addAttribute(
                        "", "xmlns:" + XSI_PREFIX, "xmlns:" + XSI_PREFIX, "", XSI_URI);

                String locationAtt = XSI_PREFIX + ":schemaLocation";
                String locationDef =
                        WFS_URI
                                + " "
                                + (wfs.isCanonicalSchemaLocation()
                                        ? org.geoserver
                                                .wfs
                                                .xml
                                                .v1_0_0
                                                .WFS
                                                .CANONICAL_SCHEMA_LOCATION_CAPABILITIES
                                        : buildSchemaURL(
                                                request.getBaseUrl(),
                                                "wfs/1.0.0/WFS-capabilities.xsd"));
                attributes.addAttribute("", locationAtt, locationAtt, "", locationDef);

                start("WFS_Capabilities", attributes);

                handleService();
                handleCapability();
                handleFeatureTypes();
                handleFilterCapabilities();

                end("WFS_Capabilities");
            }

            /**
             * Encodes the wfs:Service element.
             *
             * <pre>
             * &lt;xsd:complexType name="ServiceType"&gt;
             *         &lt;xsd:sequence&gt;
             *                 &lt;xsd:element name="Name" type="xsd:string"/&gt;
             *                 &lt;xsd:element ref="wfs:Title"/&gt;
             *                 &lt;xsd:element ref="wfs:Abstract" minOccurs="0"/&gt;
             *                 &lt;xsd:element ref="wfs:Keywords" minOccurs="0"/&gt;
             *                 &lt;xsd:element ref="wfs:OnlineResource"/&gt;
             *                 &lt;xsd:element ref="wfs:Fees" minOccurs="0"/&gt;
             *                 &lt;xsd:element ref="wfs:AccessConstraints" minOccurs="0"/&gt;
             *          &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             *
             *         </pre>
             */
            protected void handleService() {
                start("Service");
                element("Name", wfs.getName());
                element("Title", wfs.getTitle());
                element("Abstract", wfs.getAbstract());

                handleKeywords(wfs.getKeywords());

                element(
                        "OnlineResource",
                        buildURL(request.getBaseUrl(), "wfs", null, URLType.SERVICE));
                element("Fees", wfs.getFees());
                element("AccessConstraints", wfs.getAccessConstraints());
                end("Service");
            }

            /**
             * Encodes the wfs:Keywords element.
             *
             * <p>
             *
             * <pre>
             *         &lt;!-- Short words to help catalog searching.
             *                  Currently, no controlled vocabulary has
             *                 been defined. --&gt;
             *         &lt;xsd:element name="Keywords" type="xsd:string"/&gt;
             *                 </pre>
             */
            protected void handleKeywords(String[] kwlist) {
                if (kwlist == null) {
                    handleKeywords((List) null);
                } else {
                    handleKeywords(Arrays.asList(kwlist));
                }
            }

            /**
             * Encodes the wfs:Keywords element.
             *
             * <p>
             *
             * <pre>
             *         &lt;!-- Short words to help catalog searching.
             *                  Currently, no controlled vocabulary has
             *                 been defined. --&gt;
             *         &lt;xsd:element name="Keywords" type="xsd:string"/&gt;
             *                 </pre>
             */
            protected void handleKeywords(List kwlist) {
                StringBuffer kwds = new StringBuffer();

                for (int i = 0; (kwlist != null) && (i < kwlist.size()); i++) {
                    kwds.append(kwlist.get(i));

                    if (i != (kwlist.size() - 1)) {
                        kwds.append(", ");
                    }
                }

                element("Keywords", kwds.toString());
            }

            /**
             * Encodes the wfs:Capability element.
             *
             * <p>
             *
             * <pre>
             * &lt;xsd:complexType name="CapabilityType"&gt;
             *         &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="Request" type="wfs:RequestType"/&gt;
             *                &lt;!-- The optional VendorSpecificCapabilities element lists any
             *                                 capabilities unique to a particular server.  Because the
             *                                 information is not known a priori, it cannot be constrained
             *                                 by this particular schema document.  A vendor-specific schema
             *                                 fragment must be supplied at the start of the XML capabilities
             *                                 document, after the reference to the general WFS_Capabilities
             *                                 schema. --&gt;
             *                &lt;xsd:element ref="wfs:VendorSpecificCapabilities" minOccurs="0"/&gt;
             *        &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             *                 </pre>
             */
            protected void handleCapability() {
                start("Capability");
                start("Request");
                handleGetCapabilities();
                handleDescribeFT();
                handleGetFeature();

                if (wfs.getServiceLevel().contains(WFSInfo.ServiceLevel.TRANSACTIONAL)) {
                    handleTransaction();
                }

                if (wfs.getServiceLevel().contains(WFSInfo.ServiceLevel.COMPLETE)) {
                    handleLock();
                    handleFeatureWithLock();
                }

                end("Request");
                end("Capability");
            }

            /**
             * Encodes the wfs:GetCapabilities elemnt.
             *
             * <p>
             *
             * <pre>
             * &lt;xsd:complexType name="GetCapabilitiesType"&gt;
             *        &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="DCPType" type="wfs:DCPTypeType" maxOccurs="unbounded"/&gt;
             *        &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             *                 </pre>
             */
            protected void handleGetCapabilities() {
                String capName = "GetCapabilities";
                start(capName);
                handleDcpType(capName, HTTP_GET);
                handleDcpType(capName, HTTP_POST);
                end(capName);
            }

            /**
             * Encodes the wfs:DescribeFeatureType element.
             *
             * <p>
             *
             * <pre>
             * &lt;xsd:complexType name="DescribeFeatureTypeType"&gt;
             *         &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="SchemaDescriptionLanguage"
             *                        type="wfs:SchemaDescriptionLanguageType"/&gt;
             *                &lt;xsd:element name="DCPType"
             *         type="wfs:DCPTypeType" maxOccurs="unbounded"/&gt;
             *        &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             * </pre>
             */
            protected void handleDescribeFT() {
                String capName = "DescribeFeatureType";
                start(capName);
                start("SchemaDescriptionLanguage");
                element("XMLSCHEMA", null);
                end("SchemaDescriptionLanguage");

                handleDcpType(capName, HTTP_GET);
                handleDcpType(capName, HTTP_POST);
                end(capName);
            }

            /**
             * Encodes the wfs:GetFeature element.
             *
             * <p>&lt;xsd:complexType name="GetFeatureTypeType"&gt; &lt;xsd:sequence&gt;
             * &lt;xsd:element name="ResultFormat" type="wfs:ResultFormatType"/&gt; &lt;xsd:element
             * name="DCPType" type="wfs:DCPTypeType" maxOccurs="unbounded"/&gt;
             * &lt;/xsd:sequence&gt; &lt;/xsd:complexType&gt;
             */
            protected void handleGetFeature() {
                String capName = "GetFeature";
                start(capName);

                String resultFormat = "ResultFormat";
                start(resultFormat);

                // we accept numerous formats, but cite only allows you to have GML2
                if (wfs.isCiteCompliant()) {
                    element("GML2", null);
                } else {
                    // FULL MONTY
                    Collection featureProducers =
                            GeoServerExtensions.extensions(WFSGetFeatureOutputFormat.class);

                    Map dupes = new HashMap();
                    for (Iterator i = featureProducers.iterator(); i.hasNext(); ) {
                        WFSGetFeatureOutputFormat format = (WFSGetFeatureOutputFormat) i.next();
                        for (String name : format.getCapabilitiesElementNames()) {
                            if (!dupes.containsKey(name)) {
                                element(name, null);
                                dupes.put(name, new Object());
                            }
                        }
                    }
                }

                end(resultFormat);

                handleDcpType(capName, HTTP_GET);
                handleDcpType(capName, HTTP_POST);
                end(capName);
            }

            /**
             * Encodes the wfs:Transaction element.
             *
             * <p>
             *
             * <pre>
             *  &lt;xsd:complexType name="TransactionType"&gt;
             *          &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="DCPType" type="wfs:DCPTypeType" maxOccurs="unbounded"/&gt;
             *          &lt;/xsd:sequence&gt;
             *  &lt;/xsd:complexType&gt;
             * </pre>
             */
            protected void handleTransaction() {
                String capName = "Transaction";
                start(capName);
                handleDcpType(capName, HTTP_GET);
                handleDcpType(capName, HTTP_POST);
                end(capName);
            }

            /**
             * Encodes the wfs:LockFeature element.
             *
             * <p>
             *
             * <pre>
             *  &lt;xsd:complexType name="LockFeatureTypeType"&gt;
             *          &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="DCPType" type="wfs:DCPTypeType" maxOccurs="unbounded"/&gt;
             *         &lt;/xsd:sequence&gt;
             *        &lt;/xsd:complexType&gt;
             * </pre>
             */
            protected void handleLock() {
                String capName = "LockFeature";
                start(capName);
                handleDcpType(capName, HTTP_GET);
                handleDcpType(capName, HTTP_POST);
                end(capName);
            }

            /**
             * Encodes the wfs:GetFeatureWithLock element.
             *
             * <p>&lt;xsd:complexType name="GetFeatureTypeType"&gt; &lt;xsd:sequence&gt;
             * &lt;xsd:element name="ResultFormat" type="wfs:ResultFormatType"/&gt; &lt;xsd:element
             * name="DCPType" type="wfs:DCPTypeType" maxOccurs="unbounded"/&gt;
             * &lt;/xsd:sequence&gt; &lt;/xsd:complexType&gt;
             */
            protected void handleFeatureWithLock() {
                String capName = "GetFeatureWithLock";
                start(capName);
                start("ResultFormat");
                // TODO: output format extensions
                element("GML2", null);
                end("ResultFormat");
                handleDcpType(capName, HTTP_GET);
                handleDcpType(capName, HTTP_POST);
                end(capName);
            }

            /**
             * Encodes a <code>DCPType</code> element.
             *
             * <p>
             *
             * <pre>
             *  &lt;!-- Available Distributed Computing Platforms (DCPs) are
             *                listed here.  At present, only HTTP is defined. --&gt;
             *        &lt;xsd:complexType name="DCPTypeType"&gt;
             *                &lt;xsd:sequence&gt;
             *                        &lt;xsd:element name="HTTP" type="wfs:HTTPType"/&gt;
             *                &lt;/xsd:sequence&gt;
             *        &lt;/xsd:complexType&gt;
             *
             *        </pre>
             *
             * @param capabilityName the URL of the onlineresource for HTTP GET method requests
             * @param httpMethod the URL of the onlineresource for HTTP POST method requests
             */
            protected void handleDcpType(String capabilityName, String httpMethod) {
                String onlineResource;
                if (HTTP_GET.equals(httpMethod)) {
                    onlineResource =
                            buildURL(
                                    request.getBaseUrl(),
                                    "wfs",
                                    params("request", capabilityName),
                                    URLType.SERVICE);
                } else {
                    // make sure it ends with ?
                    onlineResource = buildURL(request.getBaseUrl(), "wfs", null, URLType.SERVICE);
                    appendQueryString(onlineResource, "");
                }

                start("DCPType");
                start("HTTP");

                AttributesImpl atts = new AttributesImpl();
                atts.addAttribute("", "onlineResource", "onlineResource", "", onlineResource);
                element(httpMethod, null, atts);

                end("HTTP");
                end("DCPType");
            }

            /**
             * Encodes the wfs:FeatureTYpeList element.
             *
             * <p>
             *
             * <pre>
             * &lt;xsd:complexType name="FeatureTypeListType"&gt;
             *        &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="Operations"
             *         type="wfs:OperationsType" minOccurs="0"/&gt;
             *                &lt;xsd:element name="FeatureType"
             *         type="wfs:FeatureTypeType" maxOccurs="unbounded"/&gt;
             *        &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             *         </pre>
             */
            protected void handleFeatureTypes() {
                start("FeatureTypeList");
                start("Operations");

                if ((wfs.getServiceLevel().contains(WFSInfo.ServiceLevel.BASIC))) {
                    element("Query", null);
                }

                if ((wfs.getServiceLevel()
                        .getOps()
                        .contains(WFSInfo.Operation.TRANSACTION_INSERT))) {
                    element("Insert", null);
                }

                if ((wfs.getServiceLevel()
                        .getOps()
                        .contains(WFSInfo.Operation.TRANSACTION_UPDATE))) {
                    element("Update", null);
                }

                if ((wfs.getServiceLevel()
                        .getOps()
                        .contains(WFSInfo.Operation.TRANSACTION_DELETE))) {
                    element("Delete", null);
                }

                if ((wfs.getServiceLevel().getOps().contains(WFSInfo.Operation.LOCKFEATURE))) {
                    element("Lock", null);
                }

                end("Operations");

                List featureTypes = new ArrayList(catalog.getFeatureTypes());

                // filter out disabled feature types
                for (Iterator it = featureTypes.iterator(); it.hasNext(); ) {
                    FeatureTypeInfo ft = (FeatureTypeInfo) it.next();
                    if (!ft.enabled()) it.remove();
                }

                // filter the layers if a namespace filter has been set
                if (request.getNamespace() != null) {
                    String namespace = request.getNamespace();
                    for (Iterator it = featureTypes.iterator(); it.hasNext(); ) {
                        FeatureTypeInfo ft = (FeatureTypeInfo) it.next();
                        if (!namespace.equals(ft.getNamespace().getPrefix())) it.remove();
                    }
                }

                Collections.sort(featureTypes, new FeatureTypeInfoTitleComparator());
                for (Iterator it = featureTypes.iterator(); it.hasNext(); ) {
                    FeatureTypeInfo ftype = (FeatureTypeInfo) it.next();
                    try {
                        mark();
                        handleFeatureType(ftype);
                        commit();
                    } catch (RuntimeException e) {
                        if (skipMisconfigured) {
                            reset();
                            LOGGER.log(
                                    Level.WARNING,
                                    "Couldn't encode WFS Capabilities entry for FeatureType: "
                                            + ftype.prefixedName(),
                                    e);
                        } else {
                            throw e;
                        }
                    }
                }

                end("FeatureTypeList");
            }

            /**
             * Default handle of a FeatureTypeInfo content that writes the latLongBBox as well as
             * the GlobalBasic's parameters
             *
             * <p>
             *
             * <pre>
             * &lt;xsd:complexType name="FeatureTypeType"&gt;
             *        &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="Name" type="xsd:QName"/&gt;
             *                &lt;xsd:element ref="wfs:Title" minOccurs="0"/&gt;
             *                &lt;xsd:element ref="wfs:Abstract" minOccurs="0"/&gt;
             *                &lt;xsd:element ref="wfs:Keywords" minOccurs="0"/&gt;
             *                &lt;xsd:element ref="wfs:SRS"/&gt;
             *                &lt;xsd:element name="Operations"
             *         type="wfs:OperationsType" minOccurs="0"/&gt;
             *                &lt;xsd:element name="LatLongBoundingBox"
             *         type="wfs:LatLongBoundingBoxType"
             *         minOccurs="0" maxOccurs="unbounded"/&gt;
             *                &lt;xsd:element name="MetadataURL"
             *         type="wfs:MetadataURLType"
             *         minOccurs="0" maxOccurs="unbounded"/&gt;
             *        &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             *         </pre>
             *
             * @param info The FeatureType configuration to report capabilities on.
             * @throws RuntimeException For any errors.
             */
            protected void handleFeatureType(FeatureTypeInfo info) {
                Envelope bbox = null;
                bbox = info.getLatLonBoundingBox();

                start("FeatureType");
                element("Name", info.prefixedName());
                element("Title", info.getTitle());
                element("Abstract", info.getAbstract());
                handleKeywords(info.getKeywords());

                element("SRS", info.getSRS());

                String minx = String.valueOf(bbox.getMinX());
                String miny = String.valueOf(bbox.getMinY());
                String maxx = String.valueOf(bbox.getMaxX());
                String maxy = String.valueOf(bbox.getMaxY());

                AttributesImpl bboxAtts = new AttributesImpl();
                bboxAtts.addAttribute("", "minx", "minx", "", minx);
                bboxAtts.addAttribute("", "miny", "miny", "", miny);
                bboxAtts.addAttribute("", "maxx", "maxx", "", maxx);
                bboxAtts.addAttribute("", "maxy", "maxy", "", maxy);

                element("LatLongBoundingBox", null, bboxAtts);

                end("FeatureType");
            }

            /**
             * Encodes the ogc:Filter_Capabilities element.
             *
             * <p>
             *
             * <pre>
             * &lt;xsd:element name="Filter_Capabilities"&gt;
             *        &lt;xsd:complexType&gt;
             *          &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="Spatial_Capabilities" type="ogc:Spatial_CapabilitiesType"/&gt;
             *                &lt;xsd:element name="Scalar_Capabilities" type="ogc:Scalar_CapabilitiesType"/&gt;
             *          &lt;/xsd:sequence&gt;
             *        &lt;/xsd:complexType&gt;
             * &lt;/xsd:element&gt;
             * </pre>
             */
            protected void handleFilterCapabilities() {
                String ogc = "ogc:";

                // REVISIT: for now I"m just prepending ogc onto the name element.
                // Is the proper way to only do that for the qname?  I guess it
                // would only really matter if we're going to be producing capabilities
                // documents that aren't qualified, and I don't see any reason to
                // do that.
                start(ogc + "Filter_Capabilities");
                start(ogc + "Spatial_Capabilities");
                start(ogc + "Spatial_Operators");
                element(ogc + "Disjoint", null);
                element(ogc + "Equals", null);
                element(ogc + "DWithin", null);
                element(ogc + "Beyond", null);
                element(ogc + "Intersect", null);
                element(ogc + "Touches", null);
                element(ogc + "Crosses", null);
                element(ogc + "Within", null);
                element(ogc + "Contains", null);
                element(ogc + "Overlaps", null);
                element(ogc + "BBOX", null);
                end(ogc + "Spatial_Operators");
                end(ogc + "Spatial_Capabilities");

                start(ogc + "Scalar_Capabilities");
                element(ogc + "Logical_Operators", null);
                start(ogc + "Comparison_Operators");
                element(ogc + "Simple_Comparisons", null);
                element(ogc + "Between", null);
                element(ogc + "Like", null);
                element(ogc + "NullCheck", null);
                end(ogc + "Comparison_Operators");
                start(ogc + "Arithmetic_Operators");
                element(ogc + "Simple_Arithmetic", null);

                handleFunctions(ogc); // djb: list functions

                end(ogc + "Arithmetic_Operators");
                end(ogc + "Scalar_Capabilities");
                end(ogc + "Filter_Capabilities");
            }

            /**
             * &lt;xsd:complexType name="FunctionsType"&gt; &lt;xsd:sequence&gt; &lt;xsd:element
             * name="Function_Names" type="ogc:Function_NamesType"/&gt; &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             */
            protected void handleFunctions(String prefix) {
                start(prefix + "Functions");
                start(prefix + "Function_Names");

                Set<FunctionName> functions = getAvailableFunctionNames();
                Iterator it = functions.iterator();

                while (it.hasNext()) {
                    FunctionName fname = (FunctionName) it.next();
                    AttributesImpl atts = new AttributesImpl();
                    atts.addAttribute("", "nArgs", "nArgs", "", fname.getArgumentCount() + "");

                    element(prefix + "Function_Name", fname.getName(), atts);
                }

                end(prefix + "Function_Names");
                end(prefix + "Functions");
            }
        }
    }

    /** Transformer for wfs 1.1 capabilities document. */
    public static class WFS1_1 extends CapabilitiesTransformer {

        public static final Version VERSION_11 = new Version("1.1.0");

        static final Set<String> VALID_LINKS_METADATATYPES =
                new HashSet<String>(Arrays.asList("TC211", "FGDC", "19115", "13139"));

        static final Set<String> VALID_LINKS_FORMATS =
                new HashSet<String>(
                        Arrays.asList("text/xml", "text/html", "text/sgml", "text/plain"));

        protected final boolean skipMisconfigured;
        protected final Collection<WFSExtendedCapabilitiesProvider> extCapsProviders;
        protected final String baseUrl;

        public WFS1_1(
                WFSInfo wfs,
                String baseUrl,
                Catalog catalog,
                Collection<WFSExtendedCapabilitiesProvider> extCapsProviders) {
            this(wfs, WFSInfo.Version.V_11, baseUrl, catalog, extCapsProviders);
        }

        public WFS1_1(
                WFSInfo wfs,
                WFSInfo.Version version,
                String baseUrl,
                Catalog catalog,
                Collection<WFSExtendedCapabilitiesProvider> extCapsProviders) {
            super(wfs, version, catalog);
            skipMisconfigured =
                    ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                            wfs.getGeoServer().getGlobal().getResourceErrorHandling());

            this.extCapsProviders = extCapsProviders;
            this.baseUrl = baseUrl;
        }

        public Translator createTranslator(ContentHandler handler) {
            return new CapabilitiesTranslator1_1(handler, baseUrl, wfs, extCapsProviders);
        }

        protected class CapabilitiesTranslator1_1 extends TranslatorSupport {
            protected static final String GML_3_1_1_FORMAT = "text/xml; subtype=gml/3.1.1";
            GetCapabilitiesRequest request;
            protected Collection<WFSExtendedCapabilitiesProvider> extCapsProviders;
            protected final WFSInfo wfs;
            protected final String schemaBaseURL;

            public CapabilitiesTranslator1_1(
                    ContentHandler handler,
                    String baseUrl,
                    WFSInfo wfs,
                    Collection<WFSExtendedCapabilitiesProvider> extCapsProviders) {
                super(handler, null, null);
                this.wfs = wfs;
                this.extCapsProviders = extCapsProviders;
                this.schemaBaseURL = baseUrl;

                // register namespaces provided by extended capabilities
                for (WFSExtendedCapabilitiesProvider cp : extCapsProviders) {
                    cp.registerNamespaces(getNamespaceSupport());
                }
            }

            public void encode(Object object) throws IllegalArgumentException {
                request = GetCapabilitiesRequest.adapt(object);

                verifyUpdateSequence(request);

                StringBuilder schemaLocation = new StringBuilder();
                schemaLocation.append(org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE);
                schemaLocation.append(" ");
                if (wfs.isCanonicalSchemaLocation()) {
                    schemaLocation.append(
                            org.geoserver.wfs.xml.v1_1_0.WFS.CANONICAL_SCHEMA_LOCATION);
                } else {
                    schemaLocation.append(
                            buildSchemaURL(request.getBaseUrl(), "wfs/1.1.0/wfs.xsd"));
                }
                addExtensionSchemaLocation(schemaLocation);

                AttributesImpl attributes =
                        attributes(
                                new String[] {
                                    "version",
                                    "1.1.0",
                                    "xmlns:xsi",
                                    XSI_URI,
                                    "xmlns",
                                    WFS_URI,
                                    "xmlns:wfs",
                                    WFS_URI,
                                    "xmlns:ows",
                                    OWS.NAMESPACE,
                                    "xmlns:gml",
                                    GML.NAMESPACE,
                                    "xmlns:ogc",
                                    OGC.NAMESPACE,
                                    "xmlns:xlink",
                                    XLINK.NAMESPACE,
                                    "xsi:schemaLocation",
                                    schemaLocation.toString()
                                });

                @SuppressWarnings("rawtypes")
                Enumeration prefixes = getNamespaceSupport().getPrefixes();
                while (prefixes.hasMoreElements()) {
                    String prefix = (String) prefixes.nextElement();
                    if ("xml".equals(prefix)) continue;
                    attributes.addAttribute(
                            null,
                            null,
                            "xmlns:" + prefix,
                            null,
                            getNamespaceSupport().getURI(prefix));
                }
                registerNamespaces(attributes);
                updateSequence(attributes);

                start("wfs:WFS_Capabilities", attributes);

                Set<Sections> sections = getSections(request);
                if (sections.contains(Sections.ServiceIdentification)) {
                    serviceIdentification();
                }
                if (sections.contains(Sections.ServiceProvider)) {
                    serviceProvider(wfs.getGeoServer());
                }
                if (sections.contains(Sections.OperationsMetadata)) {
                    operationsMetadata();
                }
                if (sections.contains(Sections.FeatureTypeList)) {
                    featureTypeList();
                }
                if (sections.contains(Sections.Filter_Capabilities)) {
                    filterCapabilities();
                }

                end("wfs:WFS_Capabilities");
            }

            protected String addExtensionSchemaLocation(StringBuilder schemaLocation) {
                for (WFSExtendedCapabilitiesProvider cp : extCapsProviders) {
                    String[] locations = cp.getSchemaLocations(schemaBaseURL);
                    try {
                        for (int i = 0; i < locations.length - 1; i += 2) {
                            schemaLocation.append(" ");
                            schemaLocation.append(schemaLocation(locations[i], locations[i + 1]));
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new ServiceException(
                                "Extended capabilities provider returned improper "
                                        + "set of namespace,location pairs from getSchemaLocations()",
                                e);
                    }
                }

                return schemaLocation.toString();
            }

            protected String schemaLocation(String namespace, String uri) {
                String location = null;
                try {
                    new URL(uri);

                    // external location
                    location = uri;
                } catch (MalformedURLException e) {
                    // means the url is relative
                    location = buildSchemaURL(schemaBaseURL, uri);
                }

                return namespace + " " + location;
            }

            /**
             * Encodes the ows:ServiceIdentification element.
             *
             * <p>
             *
             * <pre>
             * &lt;complexType&gt;
             *         &lt;complexContent&gt;
             *           &lt;extension base="ows:DescriptionType"&gt;
             *       &lt;sequence&gt;
             *         &lt;element name="ServiceType" type="ows:CodeType"&gt;
             *         &lt;annotation&gt;
             *             &lt;documentation&gt;A service type name from a registry of services.
             *             For example, the values of the codeSpace URI and name and code string may
             *             be "OGC" and "catalogue." This type name is normally used for
             *             machine-to-machine communication.&lt;/documentation&gt;
             *         &lt;/annotation&gt;
             *         &lt;/element&gt;
             *         &lt;element name="ServiceTypeVersion" type="ows:VersionType" maxOccurs="unbounded"&gt;
             *         &lt;annotation&gt;
             *             &lt;documentation&gt;Unordered list of one or more versions of this service
             *             type implemented by this server. This information is not adequate for
             *             version negotiation, and shall not be used for that purpose. &lt;/documentation&gt;
             *         &lt;/annotation&gt;
             *         &lt;/element&gt;
             *         &lt;element ref="ows:Fees" minOccurs="0"&gt;
             *         &lt;annotation&gt;
             *               &lt;documentation&gt;If this element is omitted, no meaning is implied. &lt;/documentation&gt;
             *         &lt;/annotation&gt;
             *         &lt;/element&gt;
             *         &lt;element ref="ows:AccessConstraints" minOccurs="0" maxOccurs="unbounded"&gt;
             *         &lt;annotation&gt;
             *               &lt;documentation&gt;Unordered list of access constraints applied to assure
             *               the protection of privacy or intellectual property, and any other
             *               restrictions on retrieving or using data from or otherwise using this
             *               server. The reserved value NONE (case insensitive) shall be used to
             *               mean no access constraints are imposed. If this element is omitted,
             *               no meaning is implied. &lt;/documentation&gt;
             *               &lt;/annotation&gt;
             *               &lt;/element&gt;
             *             &lt;/sequence&gt;
             *     &lt;/extension&gt;
             *   &lt;/complexContent&gt;
             * &lt;/complexType&gt;
             * </pre>
             */
            protected void serviceIdentification() {
                serviceIdentification("1.1.0");
            }

            protected void serviceIdentification(String version) {
                start("ows:ServiceIdentification");

                element("ows:Title", wfs.getTitle());
                element("ows:Abstract", wfs.getAbstract());

                keywords(wfs.getKeywords());

                element("ows:ServiceType", "WFS");
                element("ows:ServiceTypeVersion", version);

                // advertise eventual profiles that might be implemented (only since OWS 1.1, thus
                // WFS 2.0)
                Version gtVersion = new Version(version);
                if (gtVersion.compareTo(new Version("2")) >= 0) {
                    LinkedHashSet<String> profiles = new LinkedHashSet<>();
                    for (WFSExtendedCapabilitiesProvider provider : extCapsProviders) {
                        List<String> providerProfiles = provider.getProfiles(gtVersion);
                        profiles.addAll(providerProfiles);
                    }
                    for (String profile : profiles) {
                        element("ows:Profile", profile);
                    }
                }

                element("ows:Fees", wfs.getFees());
                element("ows:AccessConstraints", wfs.getAccessConstraints());

                end("ows:ServiceIdentification");
            }

            /**
             * Encodes the ows:ServiceProvider element.
             *
             * <p>
             *
             * <pre>
             * &lt;complexType&gt;
             *        &lt;sequence&gt;
             *           &lt;element name="ProviderName" type="string"&gt;
             *           &lt;annotation&gt;
             *        &lt;documentation&gt;A unique identifier for the service provider organization. &lt;/documentation&gt;
             *     &lt;/annotation&gt;
             *     &lt;/element&gt;
             *           &lt;element name="ProviderSite" type="ows:OnlineResourceType" minOccurs="0"&gt;
             *     &lt;annotation&gt;
             *        &lt;documentation&gt;Reference to the most relevant web site of the service provider. &lt;/documentation&gt;
             *     &lt;/annotation&gt;
             *     &lt;/element&gt;
             *     &lt;element name="ServiceContact" type="ows:ResponsiblePartySubsetType"&gt;
             *     &lt;annotation&gt;
             *        &lt;documentation&gt;Information for contacting the service provider. The
             *        OnlineResource element within this ServiceContact element should not be used
             *        to reference a web site of the service provider. &lt;/documentation&gt;
             *     &lt;/annotation&gt;
             *     &lt;/element&gt;
             *  &lt;/sequence&gt;
             * &lt;/complexType&gt;
             * </pre>
             */
            protected void serviceProvider(GeoServer gs) {
                ContactInfo contact = gs.getSettings().getContact();
                start("ows:ServiceProvider");

                element("ows:ProviderName", contact.getContactOrganization());
                start("ows:ServiceContact");
                /*
                <sequence>
                <element ref="ows:IndividualName" minOccurs="0"/>
                <element ref="ows:OrganisationName" minOccurs="0"/>
                <element ref="ows:PositionName" minOccurs="0"/>
                <element ref="ows:ContactInfo" minOccurs="0"/>
                <element ref="ows:Role"/>
                </sequence>
                */
                element("ows:IndividualName", contact.getContactPerson());
                element("ows:PositionName", contact.getContactPosition());

                start("ows:ContactInfo");
                /*
                 <sequence>
                       <element name="Phone" type="ows:TelephoneType" minOccurs="0">
                       <element name="Address" type="ows:AddressType" minOccurs="0">
                       <element name="OnlineResource" type="ows:OnlineResourceType" minOccurs="0">
                       <element name="HoursOfService" type="string" minOccurs="0">
                       <element name="ContactInstructions" type="string" minOccurs="0">
                </sequence>
                */
                start("ows:Phone");
                element("ows:Voice", contact.getContactVoice());
                element("ows:Facsimile", contact.getContactFacsimile());
                end("ows:Phone");

                start("ows:Address");
                /*
                <element name="DeliveryPoint" type="string" minOccurs="0" maxOccurs="unbounded">
                <element name="City" type="string" minOccurs="0">
                <element name="AdministrativeArea" type="string" minOccurs="0">
                <element name="PostalCode" type="string" minOccurs="0">
                <element name="Country" type="string" minOccurs="0">
                <element name="ElectronicMailAddress" type="string" minOccurs="0" maxOccurs="unbounded">
                 */
                element("ows:DeliveryPoint", contact.getAddressDeliveryPoint());
                element("ows:City", contact.getAddressCity());
                element("ows:AdministrativeArea", contact.getAddressState());
                element("ows:PostalCode", contact.getAddressPostalCode());
                element("ows:Country", contact.getAddressCountry());
                element("ows:ElectronicMailAddress", contact.getContactEmail());

                /* Currently disregarding the maxOccurs for DeliveryPoint and ElectronicMailAddress,
                 * because this can create issues with the XML serializer. */

                end("ows:Address");

                end("ows:ContactInfo");

                end("ows:ServiceContact");

                end("ows:ServiceProvider");
            }

            /**
             * Encodes the ows:OperationsMetadata element.
             *
             * <p>
             *
             * <pre>
             * &lt;complexType&gt;
             *        &lt;sequence&gt;
             *                &lt;element ref="ows:Operation" minOccurs="2" maxOccurs="unbounded"&gt;
             *                &lt;annotation&gt;
             *                &lt;documentation&gt;Metadata for unordered list of all the (requests for) operations
             *                that this server interface implements. The list of required and optional
             *                operations implemented shall be specified in the Implementation Specification
             *                for this service. &lt;/documentation&gt;
             *                &lt;/annotation&gt;
             *                &lt;/element&gt;
             *                &lt;element name="Parameter" type="ows:DomainType" minOccurs="0" maxOccurs="unbounded"&gt;
             *                &lt;annotation&gt;
             *                        &lt;documentation&gt;Optional unordered list of parameter valid domains that each
             *                        apply to one or more operations which this server interface implements. The
             *                        list of required and optional parameter domain limitations shall be specified
             *                        in the Implementation Specification for this service. &lt;/documentation&gt;
             *                &lt;/annotation&gt;
             *                &lt;/element&gt;
             *                &lt;element name="Constraint" type="ows:DomainType" minOccurs="0" maxOccurs="unbounded"&gt;
             *                &lt;annotation&gt;
             *                        &lt;documentation&gt;Optional unordered list of valid domain constraints on
             *                        non-parameter quantities that each apply to this server. The list of
             *                        required and optional constraints shall be specified in the Implementation
             *                        Specification for this service. &lt;/documentation&gt;
             *                &lt;/annotation&gt;
             *                &lt;/element&gt;
             *                &lt;element ref="ows:ExtendedCapabilities" minOccurs="0"/&gt;
             *        &lt;/sequence&gt;
             * &lt;/complexType&gt;
             * </pre>
             */
            protected void operationsMetadata() {
                start("ows:OperationsMetadata");

                List<OperationMetadata> operations = new ArrayList<>();
                operations.add(getCapabilities());
                operations.add(describeFeatureType());
                operations.add(getFeature());
                operations.add(getGmlObject());

                if (wfs.getServiceLevel().contains(WFSInfo.ServiceLevel.COMPLETE)) {
                    operations.add(lockFeature());
                    operations.add(getFeatureWithLock());
                }

                if (wfs.getServiceLevel().contains(WFSInfo.ServiceLevel.TRANSACTIONAL)) {
                    operations.add(transaction());
                }

                // allow extension points to manipulate/modify the list
                for (WFSExtendedCapabilitiesProvider provider : extCapsProviders) {
                    provider.updateOperationMetadata(VERSION_11, operations);
                }
                // declare metadata for each operation
                operations.forEach(o -> operation(o));

                extendedCapabilities();

                end("ows:OperationsMetadata");
            }

            /** Encodes the GetCapabilities ows:Operation element. */
            protected OperationMetadata getCapabilities() {
                OperationMetadata operation = new OperationMetadata("GetCapabilities", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("AcceptVersions", new String[] {"1.0.0", "1.1.0"}));
                operation
                        .getParameters()
                        .add(new DomainType("AcceptFormats", new String[] {"text/xml"}));
                operation
                        .getParameters()
                        .add(
                                new DomainType(
                                        "Sections",
                                        new String[] {
                                            "ServiceIdentification",
                                            "ServiceProvider",
                                            "OperationsMetadata",
                                            "FeatureTypeList",
                                            "Filter_Capabilities"
                                        }));
                return operation;
            }

            /** Encodes the DescribeFeatureType ows:Operation element. */
            protected OperationMetadata describeFeatureType() {
                OperationMetadata operation =
                        new OperationMetadata("DescribeFeatureType", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("outputFormat", new String[] {GML_3_1_1_FORMAT}));
                return operation;
            }

            /** Encodes the GetFeature ows:Operation element. */
            protected OperationMetadata getFeature() {
                String[] formats = getoutputFormatNames();
                OperationMetadata operation = new OperationMetadata("GetFeature", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("resultType", new String[] {"results", "hits"}));
                operation.getParameters().add(new DomainType("outputFormat", formats));
                operation
                        .getConstraints()
                        .add(new DomainType("LocalTraverseXLinkScope", new String[] {"2"}));
                return operation;
            }

            protected String[] getoutputFormatNames() {
                return getAvailableOutputFormatNames(GML_3_1_1_FORMAT, VERSION_11);
            }

            /** Encodes the GetFeatureWithLock ows:Operation element. */
            protected OperationMetadata getFeatureWithLock() {
                String[] formats = getoutputFormatNames();
                OperationMetadata operation =
                        new OperationMetadata("GetFeatureWithLock", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("resultType", new String[] {"results", "hits"}));
                operation.getParameters().add(new DomainType("outputFormat", formats));
                return operation;
            }

            /** Encodes the LockFeature ows:Operation element. */
            protected OperationMetadata lockFeature() {
                OperationMetadata operation = new OperationMetadata("LockFeature", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("releaseAction", new String[] {"ALL", "SOME"}));
                return operation;
            }

            /** Encodes the Transaction ows:Operation element. */
            protected OperationMetadata transaction() {
                OperationMetadata operation = new OperationMetadata("Transaction", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("inputFormat", new String[] {GML_3_1_1_FORMAT}));
                operation
                        .getParameters()
                        .add(
                                new DomainType(
                                        "idgen",
                                        new String[] {
                                            "GenerateNew", "UseExisting", "ReplaceDuplicate"
                                        }));
                operation
                        .getParameters()
                        .add(new DomainType("releaseAction", new String[] {"ALL", "SOME"}));
                return operation;
            }

            /** Encodes the GetGmlObject ows:Operation element. */
            protected OperationMetadata getGmlObject() {
                return new OperationMetadata("GetGmlObject", true, true);
            }

            protected void extendedCapabilities() {
                for (WFSExtendedCapabilitiesProvider cp : this.extCapsProviders) {
                    try {
                        cp.encode(
                                new WFSExtendedCapabilitiesProvider.Translator() {
                                    public void start(String element) {
                                        CapabilitiesTranslator1_1.this.start(element);
                                    }

                                    public void start(String element, Attributes attributes) {
                                        CapabilitiesTranslator1_1.this.start(element, attributes);
                                    }

                                    public void chars(String text) {
                                        CapabilitiesTranslator1_1.this.chars(text);
                                    }

                                    public void end(String element) {
                                        CapabilitiesTranslator1_1.this.end(element);
                                    }
                                },
                                wfs,
                                request);
                    } catch (Exception e) {
                        throw new ServiceException("Extended capabilities provider threw error", e);
                    }
                }
            }

            /**
             * Encdoes the wfs:FeatureTypeList element.
             *
             * <p>
             *
             * <pre>
             * &lt;xsd:complexType name="FeatureTypeListType"&gt;
             *      &lt;xsd:annotation&gt;
             *         &lt;xsd:documentation&gt;
             *            A list of feature types available from  this server.
             *         &lt;/xsd:documentation&gt;
             *      &lt;/xsd:annotation&gt;
             *      &lt;xsd:sequence&gt;
             *         &lt;xsd:element name="Operations"
             *                      type="wfs:OperationsType"
             *                      minOccurs="0"/&gt;
             *         &lt;xsd:element name="FeatureType"
             *                      type="wfs:FeatureTypeType"
             *                      maxOccurs="unbounded"/&gt;
             *      &lt;/xsd:sequence&gt;
             *   &lt;/xsd:complexType&gt;
             * </pre>
             */
            protected void featureTypeList() {
                start("FeatureTypeList");

                start("Operations");

                if ((wfs.getServiceLevel().contains(WFSInfo.ServiceLevel.BASIC))) {
                    element("Operation", "Query");
                }

                if ((wfs.getServiceLevel()
                        .getOps()
                        .contains(WFSInfo.Operation.TRANSACTION_INSERT))) {
                    element("Operation", "Insert");
                }

                if ((wfs.getServiceLevel()
                        .getOps()
                        .contains(WFSInfo.Operation.TRANSACTION_UPDATE))) {
                    element("Operation", "Update");
                }

                if ((wfs.getServiceLevel()
                        .getOps()
                        .contains(WFSInfo.Operation.TRANSACTION_DELETE))) {
                    element("Operation", "Delete");
                }

                if ((wfs.getServiceLevel().getOps().contains(WFSInfo.Operation.LOCKFEATURE))) {
                    element("Operation", "Lock");
                }

                end("Operations");

                featureTypes();

                end("FeatureTypeList");
            }

            protected void featureTypes() {
                // featureTypes(false, "urn:x-ogc:def:crs:", request.getNamespace());
                featureTypes(false, request.getNamespace());
            }

            protected void featureTypes(boolean crs, String namespace) {
                List featureTypes = new ArrayList(catalog.getFeatureTypes());

                // filter out disabled feature types
                for (Iterator it = featureTypes.iterator(); it.hasNext(); ) {
                    FeatureTypeInfo ft = (FeatureTypeInfo) it.next();
                    if (!ft.enabled()) it.remove();
                }

                // filter the layers if a namespace filter has been set
                if (namespace != null) {
                    for (Iterator it = featureTypes.iterator(); it.hasNext(); ) {
                        FeatureTypeInfo ft = (FeatureTypeInfo) it.next();
                        if (!namespace.equals(ft.getNamespace().getPrefix())) it.remove();
                    }
                }

                Collections.sort(featureTypes, new FeatureTypeInfoTitleComparator());
                for (Iterator i = featureTypes.iterator(); i.hasNext(); ) {
                    FeatureTypeInfo featureType = (FeatureTypeInfo) i.next();
                    if (featureType.enabled()) {
                        try {
                            mark();
                            featureType(featureType, crs);
                            commit();
                        } catch (RuntimeException ex) {
                            if (skipMisconfigured) {
                                reset();
                                LOGGER.log(
                                        Level.WARNING,
                                        "Couldn't encode WFS capabilities entry for featuretype: "
                                                + featureType.prefixedName(),
                                        ex);
                            } else {
                                throw ex;
                            }
                        }
                    }
                }
            }

            /**
             * Encodes the wfs:FeatureType element.
             *
             * <p>
             *
             * <pre>
             * &lt;xsd:complexType name="FeatureTypeType"&gt;
             *      &lt;xsd:annotation&gt;
             *         &lt;xsd:documentation&gt;
             *            An element of this type that describes a feature in an application
             *            namespace shall have an xml xmlns specifier, e.g.
             *            xmlns:bo="http://www.BlueOx.org/BlueOx"
             *         &lt;/xsd:documentation&gt;
             *      &lt;/xsd:annotation&gt;
             *      &lt;xsd:sequence&gt;
             *         &lt;xsd:element name="Name" type="xsd:QName"&gt;
             *            &lt;xsd:annotation&gt;
             *               &lt;xsd:documentation&gt;
             *                  Name of this feature type, including any namespace prefix
             *               &lt;/xsd:documentation&gt;
             *            &lt;/xsd:annotation&gt;
             *         &lt;/xsd:element&gt;
             *         &lt;xsd:element name="Title" type="xsd:string"&gt;
             *            &lt;xsd:annotation&gt;
             *               &lt;xsd:documentation&gt;
             *                  Title of this feature type, normally used for display
             *                  to a human.
             *               &lt;/xsd:documentation&gt;
             *            &lt;/xsd:annotation&gt;
             *         &lt;/xsd:element&gt;
             *         &lt;xsd:element name="Abstract" type="xsd:string" minOccurs="0"&gt;*            &lt;xsd:annotation&gt;
             *               &lt;xsd:documentation&gt;
             *                  Brief narrative description of this feature type, normally*                  used for display to a human.
             *               &lt;/xsd:documentation&gt;
             *            &lt;/xsd:annotation&gt;
             *         &lt;/xsd:element&gt;
             *         &lt;xsd:element ref="ows:Keywords" minOccurs="0" maxOccurs="unbounded"/&gt;
             *         &lt;xsd:choice&gt;
             *            &lt;xsd:sequence&gt;
             *               &lt;xsd:element name="DefaultSRS"
             *                            type="xsd:anyURI"&gt;
             *                  &lt;xsd:annotation&gt;
             *                     &lt;xsd:documentation&gt;
             *                        The DefaultSRS element indicated which spatial
             *                        reference system shall be used by a WFS to
             *                        express the state of a spatial feature if not
             *                        otherwise explicitly identified within a query
             *                        or transaction request.  The SRS may be indicated
             *                        using either the EPSG form (EPSG:posc code) or
             *                        the URL form defined in subclause 4.3.2 of
             *                        refernce[2].
             *                     &lt;/xsd:documentation&gt;
             *                  &lt;/xsd:annotation&gt;
             *               &lt;/xsd:element&gt;
             *               &lt;xsd:element name="OtherSRS"
             *                            type="xsd:anyURI"
             *                            minOccurs="0" maxOccurs="unbounded"&gt;
             *                  &lt;xsd:annotation&gt;
             *                     &lt;xsd:documentation&gt;
             *                        The OtherSRS element is used to indicate other
             *                        supported SRSs within query and transaction
             *                        operations.  A supported SRS means that the
             *                        WFS supports the transformation of spatial
             *                        properties between the OtherSRS and the internal
             *                        storage SRS.  The effects of such transformations
             *                        must be considered when determining and declaring
             *                        the guaranteed data accuracy.
             *                     &lt;/xsd:documentation&gt;
             *                  &lt;/xsd:annotation&gt;
             *               &lt;/xsd:element&gt;
             *            &lt;/xsd:sequence&gt;
             *            &lt;xsd:element name="NoSRS"&gt;
             *              &lt;xsd:complexType/&gt;
             *            &lt;/xsd:element&gt;
             *         &lt;/xsd:choice&gt;
             *         &lt;xsd:element name="Operations"
             *                      type="wfs:OperationsType"
             *                      minOccurs="0"/&gt;
             *         &lt;xsd:element name="OutputFormats"
             *                      type="wfs:OutputFormatListType"
             *                      minOccurs="0"/&gt;
             *         &lt;xsd:element ref="ows:WGS84BoundingBox"
             *                      minOccurs="1" maxOccurs="unbounded"/&gt;
             *         &lt;xsd:element name="MetadataURL"
             *                      type="wfs:MetadataURLType"
             *                      minOccurs="0" maxOccurs="unbounded"/&gt;
             *      &lt;/xsd:sequence&gt;
             *   &lt;/xsd:complexType&gt;
             *         </pre>
             */
            protected void featureType(FeatureTypeInfo featureType, boolean crs) {
                GMLInfo gml = wfs.getGML().get(version);

                String prefix = featureType.getNamespace().getPrefix();
                String uri = featureType.getNamespace().getURI();

                start("FeatureType", attributes(new String[] {"xmlns:" + prefix, uri}));

                element("Name", featureType.prefixedName());
                element("Title", featureType.getTitle());
                element("Abstract", featureType.getAbstract());
                keywords(featureType.getKeywords());

                String srs = featureType.getSRS();
                srs = applySRSNameStyle(gml, srs);

                // default srs
                if (crs) {
                    // wfs 2.0
                    element("DefaultCRS", srs);
                } else {
                    element("DefaultSRS", srs);
                }

                // other srs
                List<String> otherSRSes = getOtherSRS(featureType);
                for (String otherSRS : otherSRSes) {
                    if (otherSRS != null) {
                        otherSRS = applySRSNameStyle(gml, otherSRS);
                        if (!otherSRS.equals(srs)) {
                            if (crs) {
                                element("OtherCRS", otherSRS);
                            } else {
                                element("OtherSRS", otherSRS);
                            }
                        }
                    }
                }

                Envelope bbox = null;
                bbox = featureType.getLatLonBoundingBox();

                start("ows:WGS84BoundingBox");

                element("ows:LowerCorner", bbox.getMinX() + " " + bbox.getMinY());
                element("ows:UpperCorner", bbox.getMaxX() + " " + bbox.getMaxY());

                end("ows:WGS84BoundingBox");

                List<MetadataLinkInfo> mlinks = featureType.getMetadataLinks();
                if (mlinks != null && !mlinks.isEmpty()) {
                    for (MetadataLinkInfo link : mlinks) {
                        metadataLink(link);
                    }
                }

                end("FeatureType");
            }

            private String applySRSNameStyle(GMLInfo gml, String srs) {
                if (srs != null) {
                    String prefix = gml.getSrsNameStyle().getPrefix();
                    if (srs.matches("(?ui)EPSG:[0-9]+")) {
                        srs = prefix + srs.substring(5);
                    } else {
                        srs = prefix + srs;
                    }
                }
                return srs;
            }

            protected List<String> getOtherSRS(FeatureTypeInfo featureType) {
                List<String> extraSRS;
                if (featureType.isOverridingServiceSRS()) {
                    extraSRS = featureType.getResponseSRS();
                } else {
                    extraSRS = wfs.getSRS();
                }

                return extraSRS;
            }

            protected void metadataLink(MetadataLinkInfo link) {
                // extract format and metadata type, make sure they abide the WFS 1.1
                // restrictions
                String format = link.getType();
                String metadataType = link.getMetadataType();
                if ("ISO19115:2003".equals(metadataType)) {
                    metadataType = "19115";
                }
                if (!VALID_LINKS_FORMATS.contains(format)) {
                    LOGGER.log(
                            Level.FINE,
                            "Skipping metadata link "
                                    + link.getContent()
                                    + ", format "
                                    + format
                                    + " is not valid in WFS 1.1, supported types are: "
                                    + VALID_LINKS_FORMATS);
                    return;
                }
                if (!VALID_LINKS_METADATATYPES.contains(metadataType)) {
                    LOGGER.log(
                            Level.FINE,
                            "Skipping metadata link "
                                    + link.getContent()
                                    + ", metadata type "
                                    + metadataType
                                    + " is not valid in WFS 1.1, supported types are: "
                                    + VALID_LINKS_METADATATYPES);
                    return;
                }
                if ((link.getContent() == null) || link.getContent().isEmpty()) {
                    return;
                }
                AttributesImpl mtAtts = attributes("type", metadataType, "format", format);
                element(
                        "MetadataURL",
                        ResponseUtils.proxifyMetadataLink(link, request.getBaseUrl()),
                        mtAtts);
            }

            /**
             * Encodes the wfs:SupportsGMLObjectTypeList element.
             *
             * <p>
             *
             * <pre>
             * &lt;xsd:complexType name="GMLObjectTypeListType"&gt;
             *        &lt;xsd:sequence&gt;
             *             &lt;xsd:element name="GMLObjectType" type="wfs:GMLObjectTypeType"
             *               maxOccurs="unbounded"&gt;
             *                &lt;xsd:annotation&gt;
             *                   &lt;xsd:documentation&gt;
             *                      Name of this GML object type, including any namespace prefix
             *                   &lt;/xsd:documentation&gt;
             *                &lt;/xsd:annotation&gt;
             *             &lt;/xsd:element&gt;
             *          &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             *        </pre>
             */
            protected void supportsGMLObjectTypeList() {
                element("SupportsGMLObjectTypeList", null);
            }

            /**
             * Encodes the ogc:Filter_Capabilities element.
             *
             * <p>
             *
             * <pre>
             * *&lt;xsd:element name="Filter_Capabilities"&gt;
             *      &lt;xsd:complexType&gt;
             *         &lt;xsd:sequence&gt;
             *            &lt;xsd:element name="Spatial_Capabilities"
             *                         type="ogc:Spatial_CapabilitiesType"/&gt;
             *            &lt;xsd:element name="Scalar_Capabilities"
             *                         type="ogc:Scalar_CapabilitiesType"/&gt;
             *            &lt;xsd:element name="Id_Capabilities"
             *                         type="ogc:Id_CapabilitiesType"/&gt;
             *         &lt;/xsd:sequence&gt;
             *      &lt;/xsd:complexType&gt;
             *   &lt;/xsd:element&gt;
             * </pre>
             */
            protected void filterCapabilities() {
                start("ogc:Filter_Capabilities");

                start("ogc:Spatial_Capabilities");

                start("ogc:GeometryOperands");
                element("ogc:GeometryOperand", "gml:Envelope");
                element("ogc:GeometryOperand", "gml:Point");
                element("ogc:GeometryOperand", "gml:LineString");
                element("ogc:GeometryOperand", "gml:Polygon");
                end("ogc:GeometryOperands");

                start("ogc:SpatialOperators");
                element("ogc:SpatialOperator", null, attributes(new String[] {"name", "Disjoint"}));
                element("ogc:SpatialOperator", null, attributes(new String[] {"name", "Equals"}));
                element("ogc:SpatialOperator", null, attributes(new String[] {"name", "DWithin"}));
                element("ogc:SpatialOperator", null, attributes(new String[] {"name", "Beyond"}));
                element(
                        "ogc:SpatialOperator",
                        null,
                        attributes(new String[] {"name", "Intersects"}));
                element("ogc:SpatialOperator", null, attributes(new String[] {"name", "Touches"}));
                element("ogc:SpatialOperator", null, attributes(new String[] {"name", "Crosses"}));
                element("ogc:SpatialOperator", null, attributes(new String[] {"name", "Within"}));
                element("ogc:SpatialOperator", null, attributes(new String[] {"name", "Contains"}));
                element("ogc:SpatialOperator", null, attributes(new String[] {"name", "Overlaps"}));
                element("ogc:SpatialOperator", null, attributes(new String[] {"name", "BBOX"}));
                end("ogc:SpatialOperators");

                end("ogc:Spatial_Capabilities");

                start("ogc:Scalar_Capabilities");

                element("ogc:LogicalOperators", null);

                start("ogc:ComparisonOperators");
                element("ogc:ComparisonOperator", "LessThan");
                element("ogc:ComparisonOperator", "GreaterThan");
                element("ogc:ComparisonOperator", "LessThanEqualTo");
                element("ogc:ComparisonOperator", "GreaterThanEqualTo");
                element("ogc:ComparisonOperator", "EqualTo");
                element("ogc:ComparisonOperator", "NotEqualTo");
                element("ogc:ComparisonOperator", "Like");
                element("ogc:ComparisonOperator", "Between");
                element("ogc:ComparisonOperator", "NullCheck");
                end("ogc:ComparisonOperators");

                start("ogc:ArithmeticOperators");
                element("ogc:SimpleArithmetic", null);

                functions();

                end("ogc:ArithmeticOperators");

                end("ogc:Scalar_Capabilities");

                start("ogc:Id_Capabilities");
                element("ogc:FID", null);
                element("ogc:EID", null);
                end("ogc:Id_Capabilities");

                end("ogc:Filter_Capabilities");
            }

            protected void functions() {
                start("ogc:Functions");

                Set<FunctionName> functions = getAvailableFunctionNames();
                if (!functions.isEmpty()) {
                    start("ogc:FunctionNames");

                    for (FunctionName fe : functions) {
                        element(
                                "ogc:FunctionName",
                                fe.getName(),
                                attributes(new String[] {"nArgs", "" + fe.getArgumentCount()}));
                    }

                    end("ogc:FunctionNames");
                }

                end("ogc:Functions");
            }

            /**
             * Encodes the ows:Keywords element.
             *
             * <p>
             *
             * <pre>
             * &lt;complexType name="KeywordsType"&gt;
             *     &lt;annotation&gt;
             *          &lt;documentation&gt;Unordered list of one or more commonly used or formalised word(s) or phrase(s) used to describe the subject. When needed, the optional "type" can name the type of the associated list of keywords that shall all have the same type. Also when needed, the codeSpace attribute of that "type" can reference the type name authority and/or thesaurus. &lt;/documentation&gt;
             *          &lt;documentation&gt;For OWS use, the optional thesaurusName element was omitted as being complex information that could be referenced by the codeSpace attribute of the Type element. &lt;/documentation&gt;
             *     &lt;/annotation&gt;
             *     &lt;sequence&gt;
             *          &lt;element name="Keyword" type="string" maxOccurs="unbounded"/&gt;
             *          &lt;element name="Type" type="ows:CodeType" minOccurs="0"/&gt;
             *     &lt;/sequence&gt;
             * &lt;/complexType&gt;
             * </pre>
             */
            protected void keywords(KeywordInfo[] keywords) {
                if ((keywords == null) || (keywords.length == 0)) {
                    return;
                }

                start("ows:Keywords");

                for (int i = 0; i < keywords.length; i++) {
                    element("ows:Keyword", keywords[i].getValue());
                }

                end("ows:Keywords");
            }

            protected void keywords(List keywords) {
                if (keywords != null) {
                    keywords((KeywordInfo[]) keywords.toArray(new KeywordInfo[keywords.size()]));
                }
            }

            /**
             * @see {@link #operation(String, java.util.Map.Entry[], java.util.Map.Entry[], boolean,
             *     boolean)}
             */
            protected void operation(
                    String name, Map.Entry[] parameters, boolean get, boolean post) {
                operation(name, parameters, null, get, post);
            }

            /**
             * Encodes the ows:Operation element.
             *
             * <p>
             *
             * <pre>
             * &lt;complexType&gt;
             *      &lt;sequence&gt;
             *        &lt;element ref="ows:DCP" maxOccurs="unbounded"&gt;
             *          &lt;annotation&gt;
             *            &lt;documentation&gt;Unordered list of Distributed Computing Platforms (DCPs) supported for this operation. At present, only the HTTP DCP is defined, so this element will appear only once. &lt;/documentation&gt;
             *          &lt;/annotation&gt;
             *        &lt;/element&gt;
             *        &lt;element name="Parameter" type="ows:DomainType" minOccurs="0" maxOccurs="unbounded"&gt;
             *          &lt;annotation&gt;
             *            &lt;documentation&gt;Optional unordered list of parameter domains that each apply to this operation which this server implements. If one of these Parameter elements has the same "name" attribute as a Parameter element in the OperationsMetadata element, this Parameter element shall override the other one for this operation. The list of required and optional parameter domain limitations for this operation shall be specified in the Implementation Specification for this service. &lt;/documentation&gt;
             *          &lt;/annotation&gt;
             *        &lt;/element&gt;
             *        &lt;element name="Constraint" type="ows:DomainType" minOccurs="0" maxOccurs="unbounded"&gt;
             *          &lt;annotation&gt;
             *            &lt;documentation&gt;Optional unordered list of valid domain constraints on non-parameter quantities that each apply to this operation. If one of these Constraint elements has the same "name" attribute as a Constraint element in the OperationsMetadata element, this Constraint element shall override the other one for this operation. The list of required and optional constraints for this operation shall be specified in the Implementation Specification for this service. &lt;/documentation&gt;
             *          &lt;/annotation&gt;
             *        &lt;/element&gt;
             *        &lt;element ref="ows:Metadata" minOccurs="0" maxOccurs="unbounded"&gt;
             *          &lt;annotation&gt;
             *            &lt;documentation&gt;Optional unordered list of additional metadata about this operation and its' implementation. A list of required and optional metadata elements for this operation should be specified in the Implementation Specification for this service. (Informative: This metadata might specify the operation request parameters or provide the XML Schemas for the operation request.) &lt;/documentation&gt;
             *          &lt;/annotation&gt;
             *        &lt;/element&gt;
             *      &lt;/sequence&gt;
             *      &lt;attribute name="name" type="string" use="required"&gt;
             *        &lt;annotation&gt;
             *          &lt;documentation&gt;Name or identifier of this operation (request) (for example, GetCapabilities). The list of required and optional operations implemented shall be specified in the Implementation Specification for this service. &lt;/documentation&gt;
             *        &lt;/annotation&gt;
             *      &lt;/attribute&gt;
             *    &lt;/complexType&gt;
             * </pre>
             */
            protected void operation(
                    String name,
                    Map.Entry[] parameters,
                    Map.Entry[] constraints,
                    boolean get,
                    boolean post) {
                start("ows:Operation", attributes(new String[] {"name", name}));
                String serviceURL = buildURL(request.getBaseUrl(), "wfs", null, URLType.SERVICE);

                // dcp
                dcp(serviceURL, get, post);

                // parameters
                for (int i = 0; i < parameters.length; i++) {
                    String pname = (String) parameters[i].getKey();
                    String[] pvalues = (String[]) parameters[i].getValue();

                    start("ows:Parameter", attributes(new String[] {"name", pname}));

                    for (int j = 0; j < pvalues.length; j++) {
                        element("ows:Value", pvalues[j]);
                    }

                    end("ows:Parameter");
                }

                // constraints
                for (int i = 0; constraints != null && i < constraints.length; i++) {
                    String cname = (String) constraints[i].getKey();
                    String[] cvalues = (String[]) constraints[i].getValue();

                    start("ows:Constraint", attributes(new String[] {"name", cname}));

                    for (int j = 0; j < cvalues.length; j++) {
                        element("ows:Value", cvalues[j]);
                    }

                    end("ows:Constraint");
                }

                end("ows:Operation");
            }

            protected void dcp(String serviceURL, boolean get, boolean post) {
                start("ows:DCP");
                start("ows:HTTP");

                if (get) {
                    element("ows:Get", null, attributes(new String[] {"xlink:href", serviceURL}));
                }

                if (post) {
                    element("ows:Post", null, attributes(new String[] {"xlink:href", serviceURL}));
                }

                end("ows:HTTP");
                end("ows:DCP");
            }

            protected void operation(OperationMetadata operation) {
                start("ows:Operation", attributes(new String[] {"name", operation.getName()}));

                String serviceURL = buildURL(request.getBaseUrl(), "wfs", null, URLType.SERVICE);

                // dcp
                dcp(serviceURL, operation.isGet(), operation.isPost());

                // parameters
                for (DomainType parameter : operation.getParameters()) {
                    domainType("ows:Parameter", parameter);
                }
                // constraints
                for (DomainType constraint : operation.getConstraints()) {
                    domainType("ows:Constraint", constraint);
                }
                end("ows:Operation");
            }

            protected void domainType(String elementName, DomainType domainType) {
                start(elementName, attributes(new String[] {"name", domainType.getName()}));
                if (domainType.getDefaultValue() != null) {
                    element("ows:Value", domainType.getDefaultValue());
                }
                if (!domainType.getAllowedValues().isEmpty()) {
                    for (String v : domainType.getAllowedValues()) {
                        element("ows:Value", v);
                    }
                }
                end(elementName);
            }
        }
    }

    /** Transformer for wfs 2.0 capabilities document. */
    public static class WFS2_0 extends CapabilitiesTransformer {
        public static final Version VERSION_20 = new Version("2.0.0");
        /** wfs namespace uri */
        static String WFS20_URI = "http://www.opengis.net/wfs/2.0";
        /** gml 3.2 mime type */
        protected static final String GML32_FORMAT = "application/gml+xml; version=3.2";

        /** filter namespace + prefix */
        protected static final String FES_PREFIX = "fes";

        protected static final String FES_URI = FES.NAMESPACE;
        protected final Collection<WFSExtendedCapabilitiesProvider> extCapsProviders;
        protected final String baseUrl;

        public WFS2_0(
                WFSInfo wfs,
                String baseUrl,
                Catalog catalog,
                Collection<WFSExtendedCapabilitiesProvider> extCapsProviders) {
            super(wfs, WFSInfo.Version.V_20, catalog);
            this.extCapsProviders = extCapsProviders;
            this.baseUrl = baseUrl;
        }

        @Override
        public Translator createTranslator(ContentHandler handler) {
            return new CapabilitiesTranslator2_0(handler, baseUrl, wfs, this.extCapsProviders);
        }

        protected class CapabilitiesTranslator2_0 extends TranslatorSupport {

            protected GetCapabilitiesRequest request;
            protected WFS1_1.CapabilitiesTranslator1_1 delegate;

            public CapabilitiesTranslator2_0(
                    ContentHandler handler,
                    String baseUrl,
                    WFSInfo wfs,
                    Collection<WFSExtendedCapabilitiesProvider> extCapsProviders) {
                super(handler, null, null);

                // register schema mappings for function return + argument types
                getNamespaceSupport().declarePrefix("xs", XS.NAMESPACE);
                getNamespaceSupport().declarePrefix("gml", org.geotools.gml3.v3_2.GML.NAMESPACE);

                // register namespaces provided by extended capabilities
                for (WFSExtendedCapabilitiesProvider cp : extCapsProviders) {
                    cp.registerNamespaces(getNamespaceSupport());
                }

                // wfs 1.1 already does a lot of the capabilities work, use that transformer
                // as a delegate
                WFS1_1 wfs1_1 =
                        new WFS1_1(wfs, version, baseUrl, catalog, extCapsProviders) {

                            @Override
                            public Translator createTranslator(ContentHandler handler) {
                                return new CapabilitiesTranslator1_1_v2MetadataLinks(
                                        handler, baseUrl, wfs, extCapsProviders);
                            }

                            class CapabilitiesTranslator1_1_v2MetadataLinks
                                    extends CapabilitiesTranslator1_1 {

                                public CapabilitiesTranslator1_1_v2MetadataLinks(
                                        ContentHandler handler,
                                        String baseUrl,
                                        WFSInfo wfs,
                                        Collection<WFSExtendedCapabilitiesProvider>
                                                extCapsProviders) {
                                    super(handler, baseUrl, wfs, extCapsProviders);
                                }

                                @Override
                                protected void metadataLink(MetadataLinkInfo link) {
                                    // WFS 2.0 metadata url is different than the v1.1 one, indeed
                                    // it just
                                    // has an href
                                    if ((link.getContent() == null)
                                            || link.getContent().isEmpty()) {
                                        return;
                                    }
                                    AttributesImpl mtAtts =
                                            attributes(
                                                    "xlink:href",
                                                    ResponseUtils.proxifyMetadataLink(
                                                            link, request.getBaseUrl()));
                                    start("MetadataURL", mtAtts);
                                    end("MetadataURL");
                                }
                            }
                        };
                delegate = (CapabilitiesTranslator1_1) wfs1_1.createTranslator(handler);
            }

            public void encode(Object o) throws IllegalArgumentException {
                request = GetCapabilitiesRequest.adapt(o);
                delegate.request = request;

                StringBuilder schemaLocation = new StringBuilder();
                schemaLocation.append(WFS20_URI);
                schemaLocation.append(" ");
                if (wfs.isCanonicalSchemaLocation()) {
                    schemaLocation.append(org.geotools.wfs.v2_0.WFS.CANONICAL_SCHEMA_LOCATION);
                } else {
                    schemaLocation.append(buildSchemaURL(request.getBaseUrl(), "wfs/2.0/wfs.xsd"));
                }

                delegate.addExtensionSchemaLocation(schemaLocation);

                AttributesImpl attributes =
                        attributes(
                                new String[] {
                                    "version",
                                    "2.0.0",
                                    "xmlns:xsi",
                                    XSI_URI,
                                    "xmlns",
                                    WFS20_URI,
                                    "xmlns:wfs",
                                    WFS20_URI,
                                    "xmlns:ows",
                                    org.geotools.ows.v1_1.OWS.NAMESPACE,
                                    "xmlns:gml",
                                    org.geotools.gml3.v3_2.GML.NAMESPACE,
                                    "xmlns:fes",
                                    FES_URI,
                                    "xmlns:xlink",
                                    XLINK.NAMESPACE,
                                    "xmlns:xs",
                                    XS.NAMESPACE,
                                    "xsi:schemaLocation",
                                    schemaLocation.toString()
                                });

                @SuppressWarnings("rawtypes")
                Enumeration prefixes = getNamespaceSupport().getPrefixes();
                while (prefixes.hasMoreElements()) {
                    String prefix = (String) prefixes.nextElement();
                    attributes.addAttribute(
                            null,
                            null,
                            "xmlns:" + prefix,
                            null,
                            getNamespaceSupport().getURI(prefix));
                }

                registerNamespaces(attributes);
                updateSequence(attributes);

                start("wfs:WFS_Capabilities", attributes);

                Set<Sections> sections = getSections(request);
                if (sections.isEmpty() || sections.contains(Sections.ServiceIdentification)) {
                    delegate.serviceIdentification("2.0.0");
                }
                if (sections.isEmpty() || sections.contains(Sections.ServiceProvider)) {
                    delegate.serviceProvider(wfs.getGeoServer());
                }
                if (sections.isEmpty() || sections.contains(Sections.OperationsMetadata)) {
                    operationsMetadata();
                }
                if (sections.isEmpty() || sections.contains(Sections.FeatureTypeList)) {
                    featureTypeList();
                }
                if (sections.isEmpty() || sections.contains(Sections.Filter_Capabilities)) {
                    filterCapabilities();
                }

                end("wfs:WFS_Capabilities");
            }

            protected void operationsMetadata() {
                start("ows:OperationsMetadata");

                // setup basic operations
                List<OperationMetadata> operations = new ArrayList<>();
                operations.add(getCapabilities());
                operations.add(describeFeatureType());
                operations.add(getFeature());
                operations.add(getPropertyValue());
                operations.add(listStoredQueries());
                operations.add(describeStoredQueries());
                operations.add(createStoredQuery());
                operations.add(dropStoredQuery());
                if (wfs.getServiceLevel().contains(WFSInfo.ServiceLevel.COMPLETE)) {
                    operations.add(lockFeature());
                    operations.add(getFeatureWithLock());
                }
                if (wfs.getServiceLevel().contains(WFSInfo.ServiceLevel.TRANSACTIONAL)) {
                    operations.add(transaction());
                }

                // allow extension points to manipulate/modify the list
                for (WFSExtendedCapabilitiesProvider provider : extCapsProviders) {
                    provider.updateOperationMetadata(VERSION_20, operations);
                }
                // declare metadata for each operation
                operations.forEach(o -> operation(o));

                constraints();

                delegate.extendedCapabilities();

                end("ows:OperationsMetadata");
            }

            /** Encodes the GetCapabilities ows:Operation element. */
            protected OperationMetadata getCapabilities() {
                OperationMetadata operation = new OperationMetadata("GetCapabilities", true, true);
                operation
                        .getParameters()
                        .add(
                                new DomainType(
                                        "AcceptVersions",
                                        new String[] {"1.0.0", "1.1.0", "2.0.0"}));
                operation
                        .getParameters()
                        .add(new DomainType("AcceptFormats", new String[] {"text/xml"}));
                operation
                        .getParameters()
                        .add(
                                new DomainType(
                                        "Sections",
                                        new String[] {
                                            "ServiceIdentification",
                                            "ServiceProvider",
                                            "OperationsMetadata",
                                            "FeatureTypeList",
                                            "Filter_Capabilities"
                                        }));
                return operation;
            }

            /** Encodes the DescribeFeatureType ows:Operation element. */
            protected OperationMetadata describeFeatureType() {
                OperationMetadata operation =
                        new OperationMetadata("DescribeFeatureType", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("outputFormat", new String[] {GML32_FORMAT}));
                return operation;
            }

            /** Encodes the GetFeature ows:Operation element. */
            protected OperationMetadata getFeature() {
                String[] formats = getAvailableOutputFormatNames(GML32_FORMAT, VERSION_20);
                OperationMetadata operation = new OperationMetadata("GetFeature", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("resultType", new String[] {"results", "hits"}));
                operation.getParameters().add(new DomainType("outputFormat", formats));
                operation.getConstraints().add(new DomainType("PagingIsTransactionSafe", FALSE));
                operation
                        .getConstraints()
                        .add(new DomainType("CountDefault", String.valueOf(wfs.getMaxFeatures())));
                operation
                        .getParameters()
                        .add(new DomainType("resolve", new String[] {"none", "local"}));
                return operation;
            }

            /** Encodes the GetFeatureWithLock ows:Operation element. */
            protected OperationMetadata getFeatureWithLock() {
                String[] formats = getAvailableOutputFormatNames(GML32_FORMAT, VERSION_20);
                OperationMetadata operation =
                        new OperationMetadata("GetFeatureWithLock", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("resultType", new String[] {"results", "hits"}));
                operation.getParameters().add(new DomainType("outputFormat", formats));
                operation
                        .getParameters()
                        .add(new DomainType("resolve", new String[] {"none", "local"}));
                return operation;
            }

            protected OperationMetadata getPropertyValue() {
                OperationMetadata operation = new OperationMetadata("GetPropertyValue", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("resolve", new String[] {"none", "local"}));
                operation
                        .getParameters()
                        .add(new DomainType("outputFormat", new String[] {GML32_FORMAT}));
                return operation;
            }

            /** Encodes the LockFeature ows:Operation element. */
            protected OperationMetadata lockFeature() {
                OperationMetadata operation = new OperationMetadata("LockFeature", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("releaseAction", new String[] {"ALL", "SOME"}));
                return operation;
            }

            /** Encodes the Transaction ows:Operation element. */
            protected OperationMetadata transaction() {
                OperationMetadata operation = new OperationMetadata("Transaction", true, true);
                operation
                        .getParameters()
                        .add(new DomainType("inputFormat", new String[] {GML32_FORMAT}));
                operation
                        .getParameters()
                        .add(new DomainType("releaseAction", new String[] {"ALL", "SOME"}));
                return operation;
            }

            /** Encodes the ListStoredQueries ows:Operation element. */
            protected OperationMetadata listStoredQueries() {
                return new OperationMetadata("ListStoredQueries", true, true);
            }

            /** Encodes the ListStoredQueries ows:Operation element. */
            protected OperationMetadata describeStoredQueries() {
                return new OperationMetadata("DescribeStoredQueries", true, true);
            }

            /** Encodes the CreateStoredQuery ows:Operation element. */
            protected OperationMetadata createStoredQuery() {
                OperationMetadata operation =
                        new OperationMetadata("CreateStoredQuery", false, true);
                operation
                        .getParameters()
                        .add(
                                new DomainType(
                                        "language",
                                        new String[] {CreateStoredQuery.DEFAULT_LANGUAGE}));
                return operation;
            }

            /** Encodes the DropStoredQuery ows:Operation element. */
            protected OperationMetadata dropStoredQuery() {
                return new OperationMetadata("DropStoredQuery", true, true);
            }

            /** Encodes service constraints. */
            protected void constraints() {
                List<DomainType> constraints = new ArrayList<>();
                constraints.add(new DomainType("ImplementsBasicWFS", TRUE));
                WFSInfo.ServiceLevel serviceLevel = wfs.getServiceLevel();
                constraints.add(
                        new DomainType(
                                "ImplementsTransactionalWFS",
                                serviceLevel.contains(WFSInfo.ServiceLevel.TRANSACTIONAL)
                                        ? TRUE
                                        : FALSE));
                constraints.add(
                        new DomainType(
                                "ImplementsLockingWFS",
                                serviceLevel.contains(WFSInfo.ServiceLevel.COMPLETE)
                                        ? TRUE
                                        : FALSE));
                constraints.add(new DomainType("KVPEncoding", TRUE));
                constraints.add(new DomainType("XMLEncoding", TRUE));
                constraints.add(new DomainType("SOAPEncoding", TRUE));
                constraints.add(new DomainType("ImplementsInheritance", FALSE));
                constraints.add(new DomainType("ImplementsRemoteResolve", FALSE));
                constraints.add(new DomainType("ImplementsResultPaging", TRUE));
                constraints.add(new DomainType("ImplementsStandardJoins", TRUE));
                constraints.add(new DomainType("ImplementsSpatialJoins", TRUE));
                constraints.add(new DomainType("ImplementsTemporalJoins", TRUE));
                constraints.add(new DomainType("ImplementsFeatureVersioning", FALSE));
                constraints.add(new DomainType("ManageStoredQueries", TRUE));

                // capacity constraints
                constraints.add(new DomainType("PagingIsTransactionSafe", FALSE));
                constraints.add(
                        new DomainType(
                                "QueryExpressions", new String[] {"wfs:Query", "wfs:StoredQuery"}));

                // allow extension points to alter the constraints
                Version serviceVersion = VERSION_20;
                for (WFSExtendedCapabilitiesProvider provider : extCapsProviders) {
                    provider.updateRootOperationConstraints(serviceVersion, constraints);
                }
                for (DomainType constraint : constraints) {
                    constraint(constraint);
                }
            }

            protected void constraint(DomainType constraint) {
                domainType("ows:Constraint", constraint);
            }

            protected void featureTypeList() {
                if (catalog.getFeatureTypes().isEmpty()) {
                    return;
                }

                start("FeatureTypeList");

                // TODO: namespace filtering
                delegate.featureTypes(true, request.getNamespace());
                end("FeatureTypeList");
            }

            protected void filterCapabilities() {
                start("fes:Filter_Capabilities");
                start("fes:Conformance");
                start("fes:Constraint", attributes(new String[] {"name", "ImplementsQuery"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                start("fes:Constraint", attributes(new String[] {"name", "ImplementsAdHocQuery"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                start("fes:Constraint", attributes(new String[] {"name", "ImplementsFunctions"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                start("fes:Constraint", attributes(new String[] {"name", "ImplementsResourceId"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                start(
                        "fes:Constraint",
                        attributes(new String[] {"name", "ImplementsMinStandardFilter"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                start(
                        "fes:Constraint",
                        attributes(new String[] {"name", "ImplementsStandardFilter"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                start(
                        "fes:Constraint",
                        attributes(new String[] {"name", "ImplementsMinSpatialFilter"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                start(
                        "fes:Constraint",
                        attributes(new String[] {"name", "ImplementsSpatialFilter"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                start(
                        "fes:Constraint",
                        attributes(new String[] {"name", "ImplementsMinTemporalFilter"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                start(
                        "fes:Constraint",
                        attributes(new String[] {"name", "ImplementsTemporalFilter"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                start("fes:Constraint", attributes(new String[] {"name", "ImplementsVersionNav"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", FALSE);
                end("fes:Constraint");
                start("fes:Constraint", attributes(new String[] {"name", "ImplementsSorting"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                start(
                        "fes:Constraint",
                        attributes(new String[] {"name", "ImplementsExtendedOperators"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", FALSE);
                end("fes:Constraint");
                start(
                        "fes:Constraint",
                        attributes(new String[] {"name", "ImplementsMinimumXPath"}));
                element("ows:NoValues", null);
                element("ows:DefaultValue", TRUE);
                end("fes:Constraint");
                end("fes:Conformance");

                start("fes:Id_Capabilities");
                element(
                        "fes:ResourceIdentifier",
                        null,
                        attributes(new String[] {"name", "fes:ResourceId"}));
                end("fes:Id_Capabilities");

                start("fes:Scalar_Capabilities");
                element("fes:LogicalOperators", null);
                start("fes:ComparisonOperators");
                element(
                        "fes:ComparisonOperator",
                        null,
                        attributes(new String[] {"name", "PropertyIsLessThan"}));
                element(
                        "fes:ComparisonOperator",
                        null,
                        attributes(new String[] {"name", "PropertyIsGreaterThan"}));
                element(
                        "fes:ComparisonOperator",
                        null,
                        attributes(new String[] {"name", "PropertyIsLessThanOrEqualTo"}));
                element(
                        "fes:ComparisonOperator",
                        null,
                        attributes(new String[] {"name", "PropertyIsGreaterThanOrEqualTo"}));
                element(
                        "fes:ComparisonOperator",
                        null,
                        attributes(new String[] {"name", "PropertyIsEqualTo"}));
                element(
                        "fes:ComparisonOperator",
                        null,
                        attributes(new String[] {"name", "PropertyIsNotEqualTo"}));
                element(
                        "fes:ComparisonOperator",
                        null,
                        attributes(new String[] {"name", "PropertyIsLike"}));
                element(
                        "fes:ComparisonOperator",
                        null,
                        attributes(new String[] {"name", "PropertyIsBetween"}));
                element(
                        "fes:ComparisonOperator",
                        null,
                        attributes(new String[] {"name", "PropertyIsNull"}));
                element(
                        "fes:ComparisonOperator",
                        null,
                        attributes(new String[] {"name", "PropertyIsNil"}));
                end("fes:ComparisonOperators");
                end("fes:Scalar_Capabilities");

                start("fes:Spatial_Capabilities");
                start("fes:GeometryOperands");
                element(
                        "fes:GeometryOperand",
                        null,
                        attributes(new String[] {"name", "gml:Envelope"}));
                element(
                        "fes:GeometryOperand",
                        null,
                        attributes(new String[] {"name", "gml:Point"}));
                element(
                        "fes:GeometryOperand",
                        null,
                        attributes(new String[] {"name", "gml:MultiPoint"}));
                element(
                        "fes:GeometryOperand",
                        null,
                        attributes(new String[] {"name", "gml:LineString"}));
                element(
                        "fes:GeometryOperand",
                        null,
                        attributes(new String[] {"name", "gml:MultiLineString"}));
                element(
                        "fes:GeometryOperand",
                        null,
                        attributes(new String[] {"name", "gml:Polygon"}));
                element(
                        "fes:GeometryOperand",
                        null,
                        attributes(new String[] {"name", "gml:MultiPolygon"}));
                element(
                        "fes:GeometryOperand",
                        null,
                        attributes(new String[] {"name", "gml:MultiGeometry"}));
                end("fes:GeometryOperands");
                start("fes:SpatialOperators");
                element("fes:SpatialOperator", null, attributes(new String[] {"name", "Disjoint"}));
                element("fes:SpatialOperator", null, attributes(new String[] {"name", "Equals"}));
                element("fes:SpatialOperator", null, attributes(new String[] {"name", "DWithin"}));
                element("fes:SpatialOperator", null, attributes(new String[] {"name", "Beyond"}));
                element(
                        "fes:SpatialOperator",
                        null,
                        attributes(new String[] {"name", "Intersects"}));
                element("fes:SpatialOperator", null, attributes(new String[] {"name", "Touches"}));
                element("fes:SpatialOperator", null, attributes(new String[] {"name", "Crosses"}));
                element("fes:SpatialOperator", null, attributes(new String[] {"name", "Within"}));
                element("fes:SpatialOperator", null, attributes(new String[] {"name", "Contains"}));
                element("fes:SpatialOperator", null, attributes(new String[] {"name", "Overlaps"}));
                element("fes:SpatialOperator", null, attributes(new String[] {"name", "BBOX"}));
                end("fes:SpatialOperators");
                end("fes:Spatial_Capabilities");

                start("fes:Temporal_Capabilities");
                start("fes:TemporalOperands");
                element(
                        "fes:TemporalOperand",
                        null,
                        attributes(new String[] {"name", "gml:TimeInstant"}));
                element(
                        "fes:TemporalOperand",
                        null,
                        attributes(new String[] {"name", "gml:TimePeriod"}));
                // element("fes:TemporalOperand", null, attributes(new String[] { "name",
                // "gml:validTime" }));
                // element("fes:TemporalOperand", null, attributes(new String[] { "name",
                // "gml:timePosition" }));
                // element("fes:TemporalOperand", null, attributes(new String[] { "name",
                // "gml:timeInterval" }));
                // element("fes:TemporalOperand", null, attributes(new String[] { "name",
                // "gml:duration" }));
                end("fes:TemporalOperands");
                start("fes:TemporalOperators");
                element("fes:TemporalOperator", null, attributes(new String[] {"name", "After"}));
                element("fes:TemporalOperator", null, attributes(new String[] {"name", "Before"}));
                element("fes:TemporalOperator", null, attributes(new String[] {"name", "Begins"}));
                element("fes:TemporalOperator", null, attributes(new String[] {"name", "BegunBy"}));
                element(
                        "fes:TemporalOperator",
                        null,
                        attributes(new String[] {"name", "TContains"}));
                element("fes:TemporalOperator", null, attributes(new String[] {"name", "During"}));
                element("fes:TemporalOperator", null, attributes(new String[] {"name", "TEquals"}));
                element(
                        "fes:TemporalOperator",
                        null,
                        attributes(new String[] {"name", "TOverlaps"}));
                element("fes:TemporalOperator", null, attributes(new String[] {"name", "Meets"}));
                element(
                        "fes:TemporalOperator",
                        null,
                        attributes(new String[] {"name", "OverlappedBy"}));
                element("fes:TemporalOperator", null, attributes(new String[] {"name", "MetBy"}));
                element("fes:TemporalOperator", null, attributes(new String[] {"name", "EndedBy"}));
                end("fes:TemporalOperators");
                end("fes:Temporal_Capabilities");

                List<Schema> typeMappingProfiles =
                        org.geotools.gml3.v3_2.GML.getInstance().getAllTypeMappingProfiles();

                start("fes:Functions");

                for (FunctionName fn : getAvailableFunctionNames()) {
                    start("fes:Function", attributes(new String[] {"name", fn.getName()}));

                    // figure out return type
                    Name returnType = lookupTypeName(typeMappingProfiles, fn.getReturn());
                    String prefix = getNamespaceSupport().getPrefix(returnType.getNamespaceURI());
                    if (prefix != null) {
                        element("fes:Returns", prefix + ":" + returnType.getLocalPart());
                    } else {
                        LOGGER.warning(
                                String.format(
                                        "Unable to map function return type to QName for "
                                                + "function %s. No namespace mapping for %s.",
                                        fn.getName(), returnType.getNamespaceURI()));
                    }

                    if (!fn.getArgumentNames().isEmpty()) {
                        start("fes:Arguments");
                        for (Parameter<?> arg : fn.getArguments()) {
                            start("fes:Argument", attributes(new String[] {"name", arg.getName()}));

                            Name argType = lookupTypeName(typeMappingProfiles, arg);
                            prefix = getNamespaceSupport().getPrefix(argType.getNamespaceURI());
                            if (prefix != null) {
                                element("fes:Type", prefix + ":" + argType.getLocalPart());
                            } else {
                                LOGGER.warning(
                                        String.format(
                                                "Unable to map function argument type to QName for "
                                                        + "function %s. No namespace mapping for %s.",
                                                arg.getName(), argType.getNamespaceURI()));
                            }
                            end("fes:Argument");
                        }
                        end("fes:Arguments");
                    }
                    end("fes:Function");
                }

                end("fes:Functions");

                // extended operators
                // TODO: eventually use all extended operator vactories... but for now just use
                // the WFS one because it gives some custom api for namespace stuff...

                //               WFSExtendedOperatorFactory wfsExtOpsFactory = new
                // WFSExtendedOperatorFactory();
                //               if (!wfsExtOpsFactory.getFunctionNames().isEmpty()) {
                //                   start("fes:ExtendedCapabilities");
                //
                //                   //declare the necessary namespaces
                //                   NamespaceSupport extOpNamespaces =
                // wfsExtOpsFactory.getNamespaces();
                //                   Enumeration prefixes = extOpNamespaces.getDeclaredPrefixes();
                //
                //                   List<String> xmlns = new ArrayList();
                //                   while(prefixes.hasMoreElements()) {
                //                       String prefix = (String) prefixes.nextElement();
                //                       if ("".equals(prefix) || "xml".equals(prefix)) {
                //                           continue;
                //                       }
                //                       xmlns.add("xmlns:" + prefix);
                //                       xmlns.add(extOpNamespaces.getURI(prefix));
                //                   }
                //
                //                   start("fes:AdditionalOperators", attributes(xmlns.toArray(new
                // String[xmlns.size()])));
                //                   for (Name extOp : wfsExtOpsFactory.getOperatorNames()) {
                //                       String prefix =
                // extOpNamespaces.getPrefix(extOp.getNamespaceURI());
                //                       String qName = prefix != null ? prefix + ":" +
                // extOp.getLocalPart() :
                //                           extOp.getLocalPart();
                //
                //                       element("fes:Operator", null, attributes(new
                // String[]{"name", qName}));
                //                   }
                //                   end("fes:AdditionalOperators");
                //                   end("fes:ExtendedCapabilities");
                //               }
                end("fes:Filter_Capabilities");
            }

            protected void operation(OperationMetadata operation) {
                start("ows:Operation", attributes(new String[] {"name", operation.getName()}));

                String serviceURL = buildURL(request.getBaseUrl(), "wfs", null, URLType.SERVICE);

                // dcp
                delegate.dcp(serviceURL, operation.isGet(), operation.isPost());

                // parameters
                for (DomainType parameter : operation.getParameters()) {
                    domainType("ows:Parameter", parameter);
                }
                // constraints
                for (DomainType constraint : operation.getConstraints()) {
                    domainType("ows:Constraint", constraint);
                }
                end("ows:Operation");
            }

            protected void domainType(String elementName, DomainType domainType) {
                start(elementName, attributes(new String[] {"name", domainType.getName()}));
                if (domainType.isNoValues()) {
                    element("ows:NoValues", null);
                }
                if (domainType.getDefaultValue() != null) {
                    element("ows:DefaultValue", domainType.getDefaultValue());
                }
                if (!domainType.getAllowedValues().isEmpty()) {
                    start("ows:AllowedValues");
                    for (String v : domainType.getAllowedValues()) {
                        element("ows:Value", v);
                    }
                    end("ows:AllowedValues");
                }
                end(elementName);
            }
        }

        protected Name lookupTypeName(List<Schema> profiles, Parameter arg) {
            // hack, look up for geometry mae
            if ("geometry".equals(arg.getName())) {
                return new NameImpl(org.geotools.gml3.v3_2.GML.AbstractGeometryType);
            }

            // default
            Class clazz = arg.getType();
            if (clazz == null || clazz == Object.class) {
                return new NameImpl(XS.STRING);
            }

            // TODO: this is stolen from FeaturTypeSchemaBuilder, factor out into utility class
            for (Schema profile : profiles) {
                for (Map.Entry<Name, AttributeType> e : profile.entrySet()) {
                    AttributeType at = e.getValue();
                    if (at.getBinding() != null && at.getBinding().equals(clazz)) {
                        return at.getName();
                    }
                }

                for (AttributeType at : profile.values()) {
                    if (clazz.isAssignableFrom(at.getBinding())) {
                        return at.getName();
                    }
                }
            }

            return new NameImpl(XS.STRING);
        }
    }
}
