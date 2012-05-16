/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wfs.GetCapabilitiesType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.FactoryRegistry;
import org.geotools.filter.FunctionExpression;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.v1_0.OGC;
import org.geotools.gml3.GML;
import org.geotools.xlink.XLINK;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Function;
import org.vfny.geoserver.global.FeatureTypeInfoTitleComparator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import com.vividsolutions.jts.geom.Envelope;


/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the job
 * of encoding a WFS 1.0 Capabilities document.
 *
 * @author Gabriel Roldan, Axios Engineering
 * @author Chris Holmes
 * @author Justin Deoliveira
 *
 * @version $Id$
 */
public abstract class CapabilitiesTransformer extends TransformerBase {
    /** logger */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(CapabilitiesTransformer.class.getPackage()
                                                                                       .getName());

    /** identifer of a http get + post request */
    private static final String HTTP_GET = "Get";
    private static final String HTTP_POST = "Post";

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

    /** catalog */
    protected Catalog catalog;
    
    /**
     * Creates a new CapabilitiesTransformer object.
     */
    public CapabilitiesTransformer(WFSInfo wfs, Catalog catalog) {
        super();
        setNamespaceDeclarationEnabled(false);

        this.wfs = wfs;
        this.catalog = catalog;
    }
    
    
    /**
     * It turns out that the he WFS 1.0 and 1.1 specifications don't actually support an updatesequence-based
     * getcapabilities operation.  There's no mention of an updatesequence request parameter in the getcapabilities
     * operation, and there's no normative behaviour description for what the updatesequence parameter in the
     * capabilities document should *do*.
     * 
     * So this behaviour is not used right now, at all (as of Jan 2007)
     * @param request
     * @throws ServiceException
     */
    public void verifyUpdateSequence(GetCapabilitiesType request) throws ServiceException {
    	long reqUS = -1;
        if (request.getUpdateSequence() != null) {
	        try {
	        	reqUS = Long.parseLong(request.getUpdateSequence());
	        } catch (NumberFormatException nfe) {
	        	throw new ServiceException("GeoServer only accepts numbers in the updateSequence parameter");
	        }
        }
        long geoUS = wfs.getGeoServer().getGlobal().getUpdateSequence();
    	if (reqUS > geoUS) {
    		throw new ServiceException("Client supplied an updateSequence that is greater than the current sever updateSequence","InvalidUpdateSequence");
    	}
    	if (reqUS == geoUS) {
    		throw new ServiceException("WFS capabilities document is current (updateSequence = " + geoUS + ")","CurrentUpdateSequence");
    	}
    }
    
    Set<FunctionName> getAvailableFunctionNames() {
        //Sort them up for easier visual inspection
        SortedSet sortedFunctions = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                String n1 = ((FunctionName) o1)
                    .getName();
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
    
    /**
     * Transformer for wfs 1.0 capabilities document.
     *
     * @author Justin Deoliveira, The Open Planning Project
     *
     */
    public static class WFS1_0 extends CapabilitiesTransformer {
        private final boolean skipMisconfigured;

        public WFS1_0(WFSInfo wfs, Catalog catalog) {
            super(wfs, catalog);
            this.skipMisconfigured = ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                    wfs.getGeoServer().getGlobal().getResourceErrorHandling());
        }

        public Translator createTranslator(ContentHandler handler) {
            return new CapabilitiesTranslator1_0(handler);
        }

        class CapabilitiesTranslator1_0 extends TranslatorSupport {
            GetCapabilitiesType request;
            
            public CapabilitiesTranslator1_0(ContentHandler handler) {
                super(handler, null, null);
            }

            public void encode(Object object) throws IllegalArgumentException {
                request = (GetCapabilitiesType)object;
                
                // Not used.  WFS 1.1 and 1.0 don't actually support updatesequence
                //verifyUpdateSequence(request);
                
                AttributesImpl attributes = new AttributesImpl();
                attributes.addAttribute("", "version", "version", "", "1.0.0");
                attributes.addAttribute("", "xmlns", "xmlns", "", WFS_URI);

                List<NamespaceInfo> namespaces = catalog.getNamespaces();

                for (NamespaceInfo namespace : namespaces ) {
                    String prefix = namespace.getPrefix();
                    String uri = namespace.getURI();

                    if ("xml".equals(prefix)) {
                        continue;
                    }

                    String prefixDef = "xmlns:" + prefix;
                    attributes.addAttribute("", prefixDef, prefixDef, "", uri);
                }

                //filter
                attributes.addAttribute("", "xmlns:" + OGC_PREFIX, "xmlns:" + OGC_PREFIX, "",
                    OGC_URI);

                //xml schema
                attributes.addAttribute("", "xmlns:" + XSI_PREFIX, "xmlns:" + XSI_PREFIX, "",
                    XSI_URI);

                String locationAtt = XSI_PREFIX + ":schemaLocation";
                String locationDef = WFS_URI + " " + (wfs.isCanonicalSchemaLocation()?org.geoserver.wfs.xml.v1_0_0.WFS.CANONICAL_SCHEMA_LOCATION_CAPABILITIES:
                    buildSchemaURL(request.getBaseUrl(), "wfs/1.0.0/WFS-capabilities.xsd"));
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
            *         <pre>
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
            *
            */
            private void handleService() {
                start("Service");
                element("Name", wfs.getName());
                element("Title", wfs.getTitle());
                element("Abstract", wfs.getAbstract());

                handleKeywords(wfs.getKeywords());

                element("OnlineResource", buildURL(request.getBaseUrl(), "wfs", null, URLType.SERVICE));
                element("Fees", wfs.getFees());
                element("AccessConstraints", wfs.getAccessConstraints());
                end("Service");
            }

            /**
             * Encodes the wfs:Keywords element.
             * <p>
             *
             *                 <pre>
             *         &lt;!-- Short words to help catalog searching.
             *                  Currently, no controlled vocabulary has
             *                 been defined. --&gt;
             *         &lt;xsd:element name="Keywords" type="xsd:string"/&gt;
             *                 </pre>
             *
             * </p>
             *
             */
            private void handleKeywords(String[] kwlist) {
                if (kwlist == null) {
                    handleKeywords((List) null);
                } else {
                    handleKeywords(Arrays.asList(kwlist));
                }
            }

            /**
             * Encodes the wfs:Keywords element.
             * <p>
             *
             *                 <pre>
             *         &lt;!-- Short words to help catalog searching.
             *                  Currently, no controlled vocabulary has
             *                 been defined. --&gt;
             *         &lt;xsd:element name="Keywords" type="xsd:string"/&gt;
             *                 </pre>
             *
             * </p>
             *
             */
            private void handleKeywords(List kwlist) {
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
             * <p>
             *
             *                 <pre>
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
             *
             * </p>
             */
            private void handleCapability() {
                start("Capability");
                start("Request");
                handleGetCapabilities();
                handleDescribeFT();
                handleGetFeature();

                
                if (wfs.getServiceLevel().contains( WFSInfo.ServiceLevel.TRANSACTIONAL ) ) {
                    handleTransaction();
                }

                if (wfs.getServiceLevel().contains( WFSInfo.ServiceLevel.COMPLETE ) ) {
                    handleLock();
                    handleFeatureWithLock();
                }

                end("Request");
                end("Capability");
            }

            /**
             * Encodes the wfs:GetCapabilities elemnt.
             * <p>
             *
             *                 <pre>
             * &lt;xsd:complexType name="GetCapabilitiesType"&gt;
             *        &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="DCPType" type="wfs:DCPTypeType" maxOccurs="unbounded"/&gt;
             *        &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             *                 </pre>
             *
             * </p>
             */
            private void handleGetCapabilities() {
                String capName = "GetCapabilities";
                start(capName);
                handleDcpType(capName, HTTP_GET);
                handleDcpType(capName, HTTP_POST);
                end(capName);
            }

            /**
             * Encodes the wfs:DescribeFeatureType element.
             * <p>
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
             * </p>
             */
            private void handleDescribeFT() {
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
             * &lt;xsd:complexType name="GetFeatureTypeType"&gt;
             *         &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="ResultFormat" type="wfs:ResultFormatType"/&gt;
             *                &lt;xsd:element name="DCPType" type="wfs:DCPTypeType" maxOccurs="unbounded"/&gt;
             *         &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             */
            private void handleGetFeature() {
                String capName = "GetFeature";
                start(capName);

                String resultFormat = "ResultFormat";
                start(resultFormat);

                //we accept numerous formats, but cite only allows you to have GML2
                if (wfs.isCiteCompliant()) {
                    element("GML2", null);
                } else {
                    //FULL MONTY
                    Collection featureProducers = GeoServerExtensions.extensions(WFSGetFeatureOutputFormat.class);

                    Map dupes = new HashMap();
                    for (Iterator i = featureProducers.iterator(); i.hasNext();) {
                        WFSGetFeatureOutputFormat format = (WFSGetFeatureOutputFormat) i.next();
                        if (!dupes.containsKey(format.getCapabilitiesElementName())) {
                            element(format.getCapabilitiesElementName(), null);
                            dupes.put(format.getCapabilitiesElementName(), new Object());
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
             * <p>
             * <pre>
             *  &lt;xsd:complexType name="TransactionType"&gt;
             *          &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="DCPType" type="wfs:DCPTypeType" maxOccurs="unbounded"/&gt;
             *          &lt;/xsd:sequence&gt;
             *  &lt;/xsd:complexType&gt;
                            * </pre>
                            * </p>
             */
            private void handleTransaction() {
                String capName = "Transaction";
                start(capName);
                handleDcpType(capName, HTTP_GET);
                handleDcpType(capName, HTTP_POST);
                end(capName);
            }

            /**
             * Encodes the wfs:LockFeature element.
             * <p>
             * <pre>
             *  &lt;xsd:complexType name="LockFeatureTypeType"&gt;
             *          &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="DCPType" type="wfs:DCPTypeType" maxOccurs="unbounded"/&gt;
             *         &lt;/xsd:sequence&gt;
             *        &lt;/xsd:complexType&gt;
             * </pre>
             * </p>
             */
            private void handleLock() {
                String capName = "LockFeature";
                start(capName);
                handleDcpType(capName, HTTP_GET);
                handleDcpType(capName, HTTP_POST);
                end(capName);
            }

            /**
             * Encodes the wfs:GetFeatureWithLock element.
             *
             * &lt;xsd:complexType name="GetFeatureTypeType"&gt;
             *         &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="ResultFormat" type="wfs:ResultFormatType"/&gt;
             *                &lt;xsd:element name="DCPType" type="wfs:DCPTypeType" maxOccurs="unbounded"/&gt;
             *         &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             */
            private void handleFeatureWithLock() {
                String capName = "GetFeatureWithLock";
                start(capName);
                start("ResultFormat");
                //TODO: output format extensions
                element("GML2", null);
                end("ResultFormat");
                handleDcpType(capName, HTTP_GET);
                handleDcpType(capName, HTTP_POST);
                end(capName);
            }

            /**
             * Encodes a <code>DCPType</code> element.
             *        <p>
             *        <pre>
             *  &lt;!-- Available Distributed Computing Platforms (DCPs) are
             *                listed here.  At present, only HTTP is defined. --&gt;
             *        &lt;xsd:complexType name="DCPTypeType"&gt;
             *                &lt;xsd:sequence&gt;
             *                        &lt;xsd:element name="HTTP" type="wfs:HTTPType"/&gt;
             *                &lt;/xsd:sequence&gt;
             *        &lt;/xsd:complexType&gt;
             *
             *        </pre>
             *        </p>
             * @param capabilityName the URL of the onlineresource for HTTP GET
             *        method requests
             * @param httpMethod the URL of the onlineresource for HTTP POST method
             *        requests
             */
            private void handleDcpType(String capabilityName, String httpMethod) {
                String onlineResource;
                if(HTTP_GET.equals(httpMethod)) {
                   onlineResource = buildURL(request.getBaseUrl(), "wfs", params("request", capabilityName), URLType.SERVICE);
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
             * <p>
             *         <pre>
             * &lt;xsd:complexType name="FeatureTypeListType"&gt;
             *        &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="Operations"
             *         type="wfs:OperationsType" minOccurs="0"/&gt;
             *                &lt;xsd:element name="FeatureType"
             *         type="wfs:FeatureTypeType" maxOccurs="unbounded"/&gt;
             *        &lt;/xsd:sequence&gt;
             * &lt;/xsd:complexType&gt;
             *         </pre>
             * </p>
             */
            private void handleFeatureTypes() {
                if (!wfs.isEnabled()) {
                    // should we return anything if we are disabled?
                }

                start("FeatureTypeList");
                start("Operations");

                if ((wfs.getServiceLevel().contains( WFSInfo.ServiceLevel.BASIC ) )) {
                    element("Query", null);
                }

                if ((wfs.getServiceLevel().getOps().contains( WFSInfo.Operation.TRANSACTION_INSERT))) {
                    element("Insert", null);
                }

                if ((wfs.getServiceLevel().getOps().contains( WFSInfo.Operation.TRANSACTION_UPDATE))) {
                    element("Update", null);
                }

                if ((wfs.getServiceLevel().getOps().contains( WFSInfo.Operation.TRANSACTION_DELETE))) {
                    element("Delete", null);
                }

                if ((wfs.getServiceLevel().getOps().contains( WFSInfo.Operation.LOCKFEATURE))) {
                    element("Lock", null);
                }

                end("Operations");

                List featureTypes = new ArrayList(catalog.getFeatureTypes());
                
                // filter out disabled feature types
                for (Iterator it = featureTypes.iterator(); it.hasNext();) {
                    FeatureTypeInfo ft = (FeatureTypeInfo) it.next();
                    if(!ft.enabled())
                        it.remove();
                }
                
                // filter the layers if a namespace filter has been set
                if(request.getNamespace() != null) {
                    String namespace = request.getNamespace();
                    for (Iterator it = featureTypes.iterator(); it.hasNext();) {
                        FeatureTypeInfo ft = (FeatureTypeInfo) it.next();
                        if(!namespace.equals(ft.getNamespace().getPrefix()))
                            it.remove();
                    }
                }
                
                Collections.sort(featureTypes, new FeatureTypeInfoTitleComparator());
                for (Iterator it = featureTypes.iterator(); it.hasNext();) {
                    FeatureTypeInfo ftype = (FeatureTypeInfo) it.next();
                    try {
                        mark();
                        handleFeatureType(ftype);
                        commit();
                    } catch (RuntimeException e) {
                        if (skipMisconfigured) {
                            reset();
                            LOGGER.log(Level.WARNING,
                                    "Couldn't encode WFS Capabilities entry for FeatureType: "
                                         + ftype.getPrefixedName(),
                                     e);
                        } else {
                            throw e;
                        }
                    }
                }

                end("FeatureTypeList");
            }

            /**
             * Default handle of a FeatureTypeInfo content that writes the
             * latLongBBox as well as the GlobalBasic's parameters
             *
             * <p>
             *         <pre>
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
             * </p>
             * @param ftype The FeatureType configuration to report capabilities
             *        on.
             *
             * @throws RuntimeException For any errors.
             */
            private void handleFeatureType(FeatureTypeInfo info) {
                Envelope bbox = null;
                bbox = info.getLatLonBoundingBox();

                start("FeatureType");
                element("Name", info.getPrefixedName());
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
             * <p>
             * <pre>
             * &lt;xsd:element name="Filter_Capabilities"&gt;
             *        &lt;xsd:complexType&gt;
             *          &lt;xsd:sequence&gt;
             *                &lt;xsd:element name="Spatial_Capabilities" type="ogc:Spatial_CapabilitiesType"/&gt;
             *                &lt;xsd:element name="Scalar_Capabilities" type="ogc:Scalar_CapabilitiesType"/&gt;
             *          &lt;/xsd:sequence&gt;
             *        &lt;/xsd:complexType&gt;
             *&lt;/xsd:element&gt;
             * </pre>
             * </p>
             */
            private void handleFilterCapabilities() {
                String ogc = "ogc:";

                //REVISIT: for now I"m just prepending ogc onto the name element.
                //Is the proper way to only do that for the qname?  I guess it
                //would only really matter if we're going to be producing capabilities
                //documents that aren't qualified, and I don't see any reason to
                //do that.
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

                handleFunctions(ogc); //djb: list functions

                end(ogc + "Arithmetic_Operators");
                end(ogc + "Scalar_Capabilities");
                end(ogc + "Filter_Capabilities");
            }

            /**
             * &lt;xsd:complexType name="FunctionsType"&gt;
             *         &lt;xsd:sequence&gt;
             *                 &lt;xsd:element name="Function_Names" type="ogc:Function_NamesType"/&gt;
             *         &lt;/xsd:sequence&gt;
             *         &lt;/xsd:complexType&gt;
             *
             */
            private void handleFunctions(String prefix) {
                FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
                
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

    /**
     * Transformer for wfs 1.1 capabilities document.
     *
     * @author Justin Deoliveira, The Open Planning Project
     *
     */
    public static class WFS1_1 extends CapabilitiesTransformer {
        private final boolean skipMisconfigured;
        
        public WFS1_1(WFSInfo wfs, Catalog catalog) {
            super(wfs, catalog);
            skipMisconfigured = ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                    wfs.getGeoServer().getGlobal().getResourceErrorHandling());
        }

        public Translator createTranslator(ContentHandler handler) {
            return new CapabilitiesTranslator1_1(handler);
        }

        class CapabilitiesTranslator1_1 extends TranslatorSupport {
            private static final String GML_3_1_1_FORMAT = "text/xml; subtype=gml/3.1.1";
            GetCapabilitiesType request;
            
            public CapabilitiesTranslator1_1(ContentHandler handler) {
                super(handler, null, null);
            }

            public void encode(Object object) throws IllegalArgumentException {
                request = (GetCapabilitiesType)object;
                
                verifyUpdateSequence(request);
                
                AttributesImpl attributes = attributes(new String[] {
                            "version", "1.1.0", "xmlns:xsi", XSI_URI, "xmlns", WFS_URI, "xmlns:wfs",
                            WFS_URI, "xmlns:ows", OWS.NAMESPACE, "xmlns:gml", GML.NAMESPACE,
                            "xmlns:ogc", OGC.NAMESPACE, "xmlns:xlink", XLINK.NAMESPACE,
                            "xsi:schemaLocation",
                            org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE + " " +
                            (wfs.isCanonicalSchemaLocation()?
                                    org.geoserver.wfs.xml.v1_1_0.WFS.CANONICAL_SCHEMA_LOCATION:
                                        (buildSchemaURL(request.getBaseUrl(), "wfs/1.1.0/wfs.xsd")))
                            });

                List<NamespaceInfo> namespaces = catalog.getNamespaces();
                for (NamespaceInfo namespace : namespaces) {
                    String prefix = namespace.getPrefix();
                    String uri = namespace.getURI();

                    //ignore xml prefix
                    if ("xml".equals(prefix)) {
                        continue;
                    }

                    String prefixDef = "xmlns:" + prefix;

                    attributes.addAttribute("", prefixDef, prefixDef, "", uri);
                }
                
                attributes.addAttribute("", "updateSequence", "updateSequence", "", 
                        wfs.getGeoServer().getGlobal().getUpdateSequence() + "");

                start("wfs:WFS_Capabilities", attributes);

                serviceIdentification();
                serviceProvider(wfs.getGeoServer());
                operationsMetadata();
                featureTypeList();
                //supportsGMLObjectTypeList();
                filterCapabilities();

                end("wfs:WFS_Capabilities");
            }

            /**
             * Encodes the ows:ServiceIdentification element.
             * <p>
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
             *&lt;/complexType&gt;
             * </pre>
             * </p>
             *
             */
            void serviceIdentification() {
                start("ows:ServiceIdentification");

                element("ows:Title", wfs.getTitle());
                element("ows:Abstract", wfs.getAbstract());

                keywords(wfs.getKeywords());

                element("ows:ServiceType", "WFS");
                element("ows:ServiceTypeVersion", "1.1.0");

                element("ows:Fees", wfs.getFees());
                element("ows:AccessConstraints", wfs.getAccessConstraints());

                end("ows:ServiceIdentification");
            }

            /**
                 * Encodes the ows:ServiceProvider element.
                 * <p>
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
                     *&lt;/complexType&gt;
                 * </pre>
                 * </p>
                 *
                 */
            void serviceProvider(GeoServer gs) {
                ContactInfo contact = gs.getGlobal().getContact();
                start("ows:ServiceProvider");

                element("ows:ProviderName", contact.getContactOrganization());
                start( "ows:ServiceContact");
                /*
                <sequence>
                <element ref="ows:IndividualName" minOccurs="0"/>
                <element ref="ows:OrganisationName" minOccurs="0"/>
                <element ref="ows:PositionName" minOccurs="0"/>
                <element ref="ows:ContactInfo" minOccurs="0"/>
                <element ref="ows:Role"/>
                </sequence>
                */
                element( "ows:IndividualName", contact.getContactPerson());
                element( "ows:PositionName", contact.getContactPosition() );

                start( "ows:ContactInfo" );
                /*
                  <sequence>
                        <element name="Phone" type="ows:TelephoneType" minOccurs="0">
                        <element name="Address" type="ows:AddressType" minOccurs="0">
                        <element name="OnlineResource" type="ows:OnlineResourceType" minOccurs="0">
                        <element name="HoursOfService" type="string" minOccurs="0">
                        <element name="ContactInstructions" type="string" minOccurs="0">
                 </sequence>
                 */
                start( "ows:Phone");
                element( "ows:Voice", contact.getContactVoice() );
                element( "ows:Facsimile", contact.getContactFacsimile() );
                end( "ows:Phone");
                
                start( "ows:Address");
                /*
                <element name="DeliveryPoint" type="string" minOccurs="0" maxOccurs="unbounded">
                <element name="City" type="string" minOccurs="0">
                <element name="AdministrativeArea" type="string" minOccurs="0">
                <element name="PostalCode" type="string" minOccurs="0">
                <element name="Country" type="string" minOccurs="0">
                <element name="ElectronicMailAddress" type="string" minOccurs="0" maxOccurs="unbounded">
                 */
                element( "ows:City", contact.getAddressCity() );
                element( "ows:AdministrativeArea", contact.getAddressState() );
                element( "ows:PostalCode", contact.getAddressPostalCode() );
                element( "ows:Country", contact.getAddressCountry() );
                end( "ows:Address" );
                
                end( "ows:ContactInfo" );
                
                end( "ows:ServiceContact");

                end("ows:ServiceProvider");
            }

            /**
                 * Encodes the ows:OperationsMetadata element.
                 * <p>
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
                 *&lt;/complexType&gt;
                 * </pre>
                 * </p>
                 *
                 */
            void operationsMetadata() {
                start("ows:OperationsMetadata");

                getCapabilities();
                describeFeatureType();
                getFeature();
                getGmlObject();
                
                if (wfs.getServiceLevel().contains( WFSInfo.ServiceLevel.COMPLETE )) {
                    lockFeature();
                    getFeatureWithLock();
                }

                if (wfs.getServiceLevel().contains( WFSInfo.ServiceLevel.TRANSACTIONAL) ) {
                    transaction();
                }

                end("ows:OperationsMetadata");
            }

            /**
                 * Encodes the GetCapabilities ows:Operation element.
                 *
                 */
            void getCapabilities() {
                Map.Entry[] parameters = new Map.Entry[] {
                        parameter("AcceptVersions", new String[] { "1.0.0", "1.1.0" }),
                        parameter("AcceptFormats", new String[] { "text/xml" })
                    //    				parameter( 
                    //    					"Sections", 
                    //    					new String[]{ 
                    //    						"ServiceIdentification", "ServiceProvider", "OperationsMetadata",
                    //    						"FeatureTypeList", "ServesGMLObjectTypeList", "SupportsGMLObjectTypeList", 
                    //    						"Filter_Capabilities"
                    //    					} 
                    //    				) 
                    };
                operation("GetCapabilities", parameters, true, true);
            }

            /**
                 * Encodes the DescribeFeatureType ows:Operation element.
                 */
            void describeFeatureType() {
                //TODO: process extension point
                Map.Entry[] parameters = new Map.Entry[] {
                        parameter("outputFormat", new String[] { GML_3_1_1_FORMAT })
                    };

                operation("DescribeFeatureType", parameters, true, true);
            }

            /**
                 * Encodes the GetFeature ows:Operation element.
                 */
            void getFeature() {
                String[] oflist = getoutputFormatNames();
                Map.Entry[] parameters = new Map.Entry[] {
                        parameter("resultType", new String[] { "results", "hits" }),
                        parameter("outputFormat", oflist)
                };
                    
                Map.Entry[] constraints = new Map.Entry[] {
                    parameter("LocalTraverseXLinkScope", new String[]{ "2" } )
                };
                
                operation("GetFeature", parameters, constraints, true, true);
            }

            private String[] getoutputFormatNames() {
                List<String> oflist = new ArrayList<String>();
                Collection featureProducers = GeoServerExtensions.extensions(WFSGetFeatureOutputFormat.class);
                for (Iterator i = featureProducers.iterator(); i.hasNext();) {
                    WFSGetFeatureOutputFormat format = (WFSGetFeatureOutputFormat) i.next();
                    for ( Iterator f = format.getOutputFormats().iterator(); f.hasNext(); ) {
                        oflist.add(f.next().toString());
                    }
                }
                Collections.sort(oflist);
                if(oflist.contains(GML_3_1_1_FORMAT)) {
                    oflist.remove(GML_3_1_1_FORMAT);
                    oflist.add(0, GML_3_1_1_FORMAT);
                }
                return (String[]) oflist.toArray(new String[oflist.size()]);
            }

            /**
                 * Encodes the GetFeatureWithLock ows:Operation element.
                 */
            void getFeatureWithLock() {
                String[] oflist = getoutputFormatNames();
                Map.Entry[] parameters = new Map.Entry[] {
                        parameter("resultType", new String[] { "results", "hits" }),
                        parameter("outputFormat", oflist)
                    };

                operation("GetFeatureWithLock", parameters, true, true);
            }

            /**
                 * Encodes the LockFeature ows:Operation element.
                 */
            void lockFeature() {
                Map.Entry[] parameters = new Map.Entry[] {
                        parameter("releaseAction", new String[] { "ALL", "SOME" })
                    };

                operation("LockFeature", parameters, true, true);
            }

            /**
                 * Encodes the Transaction ows:Operation element.
                 */
            void transaction() {
                Map.Entry[] parameters = new Map.Entry[] {
                        parameter("inputFormat", new String[] { GML_3_1_1_FORMAT }),
                        parameter("idgen",
                            new String[] { "GenerateNew", "UseExisting", "ReplaceDuplicate" }),
                        parameter("releaseAction", new String[] { "ALL", "SOME" })
                    };

                operation("Transaction", parameters, true, true);
            }

            /**
             * Encodes the GetGmlObject ows:Operation element.
             *
             */
            void getGmlObject() {
                Map.Entry[] parameters = new Map.Entry[] {        };
                operation("GetGmlObject", parameters, true, true);
            }
            
            /**
                 * Encdoes the wfs:FeatureTypeList element.
                 *<p>
                 *<pre>
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
                 *</pre>
                 *</p>
                 */
            void featureTypeList() {
                start("FeatureTypeList");

                start("Operations");

                if ((wfs.getServiceLevel().contains( WFSInfo.ServiceLevel.BASIC )) ) {
                    element("Operation", "Query");
                }

                if ((wfs.getServiceLevel().getOps().contains( WFSInfo.Operation.TRANSACTION_INSERT) ) ) {
                    element("Operation", "Insert");
                }

                if ((wfs.getServiceLevel().getOps().contains( WFSInfo.Operation.TRANSACTION_UPDATE) ) ) {
                    element("Operation", "Update");
                }

                if ((wfs.getServiceLevel().getOps().contains( WFSInfo.Operation.TRANSACTION_DELETE) ) ) {
                    element("Operation", "Delete");
                }

                if ((wfs.getServiceLevel().getOps().contains( WFSInfo.Operation.LOCKFEATURE) ) ) {
                    element("Operation", "Lock");
                }

                end("Operations");
                
                List featureTypes = new ArrayList(catalog.getFeatureTypes());
                
                // filter out disabled feature types
                for (Iterator it = featureTypes.iterator(); it.hasNext();) {
                    FeatureTypeInfo ft = (FeatureTypeInfo) it.next();
                    if(!ft.enabled())
                        it.remove();
                }
                
                // filter the layers if a namespace filter has been set
                if(request.getNamespace() != null) {
                    String namespace = request.getNamespace();
                    for (Iterator it = featureTypes.iterator(); it.hasNext();) {
                        FeatureTypeInfo ft = (FeatureTypeInfo) it.next();
                        if(!namespace.equals(ft.getNamespace().getPrefix()))
                            it.remove();
                    }
                }
                
                Collections.sort(featureTypes, new FeatureTypeInfoTitleComparator());
                for (Iterator i = featureTypes.iterator(); i.hasNext();) {
                    FeatureTypeInfo featureType = (FeatureTypeInfo) i.next();
                    if(featureType.enabled()) {
                        try {
                            mark();
                            featureType(featureType);
                            commit();
                        } catch (RuntimeException ex) {
                            if (skipMisconfigured) {
                                reset();
                                LOGGER.log(Level.WARNING,
                                        "Couldn't encode WFS capabilities entry for featuretype: "
                                            + featureType.getPrefixedName(),
                                        ex);
                            } else {
                                throw ex;
                            }
                        }
                    }
                }

                end("FeatureTypeList");
            }

            /**
                 * Encodes the wfs:FeatureType element.
                 * <p>
                 *         <pre>
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
                 * </p>
                 * @param featureType
                 */
            void featureType(FeatureTypeInfo featureType) {
                String prefix = featureType.getNamespace().getPrefix();
                String uri = featureType.getNamespace().getURI();

                start("FeatureType", attributes(new String[] { "xmlns:" + prefix, uri }));

                element("Name", featureType.getPrefixedName());
                element("Title", featureType.getTitle());
                element("Abstract", featureType.getAbstract());
                keywords(featureType.getKeywords());

                //default srs
                //element("DefaultSRS", "urn:x-ogc:def:crs:EPSG:6.11.2:" + featureType.getSRS());
                element("DefaultSRS", "urn:x-ogc:def:crs:" + featureType.getSRS());
                //TODO: other srs's

                Envelope bbox = null;
                bbox = featureType.getLatLonBoundingBox();

                start("ows:WGS84BoundingBox");

                element("ows:LowerCorner", bbox.getMinX() + " " + bbox.getMinY());
                element("ows:UpperCorner", bbox.getMaxX() + " " + bbox.getMaxY());

                end("ows:WGS84BoundingBox");

                end("FeatureType");
            }

            /**
                 * Encodes the wfs:SupportsGMLObjectTypeList element.
                 *        <p>
                 *        <pre>
                 *&lt;xsd:complexType name="GMLObjectTypeListType"&gt;
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
                 *        </p>
                 */
            void supportsGMLObjectTypeList() {
                element("SupportsGMLObjectTypeList", null);
            }

            /**
                 * Encodes the ogc:Filter_Capabilities element.
                 * <p>
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
                 * </p>
                 *
                 */
            void filterCapabilities() {
                start("ogc:Filter_Capabilities");

                start("ogc:Spatial_Capabilities");

                start("ogc:GeometryOperands");
                element("ogc:GeometryOperand", "gml:Envelope");
                element("ogc:GeometryOperand", "gml:Point");
                element("ogc:GeometryOperand", "gml:LineString");
                element("ogc:GeometryOperand", "gml:Polygon");
                end("ogc:GeometryOperands");

                start("ogc:SpatialOperators");
                element("ogc:SpatialOperator", null, attributes(new String[] { "name", "Disjoint" }));
                element("ogc:SpatialOperator", null, attributes(new String[] { "name", "Equals" }));
                element("ogc:SpatialOperator", null, attributes(new String[] { "name", "DWithin" }));
                element("ogc:SpatialOperator", null, attributes(new String[] { "name", "Beyond" }));
                element("ogc:SpatialOperator", null,
                    attributes(new String[] { "name", "Intersects" }));
                element("ogc:SpatialOperator", null, attributes(new String[] { "name", "Touches" }));
                element("ogc:SpatialOperator", null, attributes(new String[] { "name", "Crosses" }));
                element("ogc:SpatialOperator", null, attributes(new String[] { "name", "Contains" }));
                element("ogc:SpatialOperator", null, attributes(new String[] { "name", "Overlaps" }));
                element("ogc:SpatialOperator", null, attributes(new String[] { "name", "BBOX" }));
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

            void functions() {
                start("ogc:Functions");

                Set<FunctionName> functions = getAvailableFunctionNames();
                if (!functions.isEmpty()) {
                    start("ogc:FunctionNames");

                    for (FunctionName fe : functions) {
                        element("ogc:FunctionName", fe.getName(),
                            attributes(new String[] { "nArgs", "" + fe.getArgumentCount() }));
                    }

                    end("ogc:FunctionNames");
                }

                end("ogc:Functions");
            }

            /**
                 * Encodes the ows:Keywords element.
                 * <p>
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
                 * </p>
                 * @param keywords
                 */
            void keywords(KeywordInfo[] keywords) {
                if ((keywords == null) || (keywords.length == 0)) {
                    return;
                }

                start("ows:Keywords");

                for (int i = 0; i < keywords.length; i++) {
                    element("ows:Keyword", keywords[i].getValue());
                }

                end("ows:Keywords");
            }

            void keywords(List keywords) {
                if(keywords != null){
                    keywords((KeywordInfo[]) keywords.toArray(new KeywordInfo[keywords.size()]));
                }
            }

            /**
             * Encodes the ows:Operation element.
             * <p>
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
             * </p>
             *
             * @param name
             * @param parameters
             * @param get
             * @param post
             */
            void operation(String name, Map.Entry[] parameters, Map.Entry[] constraints, boolean get, boolean post) { 
                start("ows:Operation", attributes(new String[] { "name", name }));

                //dcp
                start("ows:DCP");
                start("ows:HTTP");
                
                String serviceURL = buildURL(request.getBaseUrl(), "wfs", null, URLType.SERVICE);
                if (get) {
                    element("ows:Get", null, attributes(new String[] { "xlink:href", serviceURL}));
                }

                if (post) {
                    element("ows:Post", null, attributes(new String[] { "xlink:href", serviceURL}));
                }

                end("ows:HTTP");
                end("ows:DCP");

                //parameters
                for (int i = 0; i < parameters.length; i++) {
                    String pname = (String) parameters[i].getKey();
                    String[] pvalues = (String[]) parameters[i].getValue();

                    start("ows:Parameter", attributes(new String[] { "name", pname }));

                    for (int j = 0; j < pvalues.length; j++) {
                        element("ows:Value", pvalues[j]);
                    }

                    end("ows:Parameter");
                }
                
                //constraints
                for ( int i = 0; constraints != null && i < constraints.length; i++ ) {
                    String cname = (String) constraints[i].getKey();
                    String[] cvalues = (String[]) constraints[i].getValue();
                    
                    start( "ows:Constraint", attributes(new String[] { "name", cname }));

                    for (int j = 0; j < cvalues.length; j++) {
                        element("ows:Value", cvalues[j]);
                    }

                    end( "ows:Constraint" );
                }
                
                end("ows:Operation");
            }
            
            /**
             * @see {@link #operation(String, java.util.Map.Entry[], java.util.Map.Entry[], boolean, boolean)}
             */
            void operation(String name, Map.Entry[] parameters, boolean get, boolean post) {
               operation(name,parameters,null,get,post);
            }

            AttributesImpl attributes(String[] nameValues) {
                AttributesImpl atts = new AttributesImpl();

                for (int i = 0; i < nameValues.length; i += 2) {
                    String name = nameValues[i];
                    String valu = nameValues[i + 1];

                    atts.addAttribute(null, null, name, null, valu);
                }

                return atts;
            }

            Map.Entry parameter(final String name, final String[] values) {
                return new Map.Entry() {
                        public Object getKey() {
                            return name;
                        }

                        public Object getValue() {
                            return values;
                        }

                        public Object setValue(Object value) {
                            return null;
                        }
                    };
            }
        }
    }
}
