/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs;

import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import net.opengis.tjs10.GetCapabilitiesType;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.ServiceException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.v1_0.OGC;
import org.geotools.ows.v1_1.OWS;
import org.geotools.tjs.TJS;
import org.geotools.xlink.XLINK;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.filter.capability.FunctionName;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import java.util.*;
import java.util.logging.Logger;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the job
 * of encoding a TJS 1.0 Capabilities document.
 *
 * @author Jos'e Luis Capote
 */
public abstract class CapabilitiesTransformer extends TransformerBase {

    /**
     * logger
     */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(CapabilitiesTransformer.class.getPackage().getName());
    /**
     * identifer of a http get + post request
     */
    private static final String HTTP_GET = "Get";
    private static final String HTTP_POST = "Post";
    /**
     * wfs namespace
     */
    protected static final String TJS_PREFIX = "tjs";
    protected static final String TJS_URI = "http://www.opengis.net/tjs";
    /**
     * xml schema namespace + prefix
     */
    protected static final String XSI_PREFIX = "xsi";
    protected static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";
    /**
     * filter namesapce + prefix
     */
    protected static final String OGC_PREFIX = "ogc";
    protected static final String OGC_URI = OGC.NAMESPACE;
    /**
     * wfs service
     */
    protected TJSInfo tjs;
    /**
     * catalog
     */
    protected TJSCatalog catalog;

    /**
     * Creates a new CapabilitiesTransformer object.
     */
    public CapabilitiesTransformer(TJSInfo tjs, TJSCatalog catalog) {
        super();
        setNamespaceDeclarationEnabled(false);

        this.tjs = tjs;
        this.catalog = catalog;
    }

    /**
     * It turns out that the he TJS 1.0 specification don't actually support an updatesequence-based
     * getcapabilities operation.  There's no mention of an updatesequence request parameter in the getcapabilities
     * operation, and there's no normative behaviour description for what the updatesequence parameter in the
     * capabilities document should *do*.
     * <p/>
     * So this behaviour is not used right now, at all (as of oct 2012)
     *
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
        GeoServer geoServer = tjs.getGeoServer();
        if (geoServer == null) {
            return;
        }
        long geoUS = geoServer.getGlobal().getUpdateSequence();
        if (reqUS > geoUS) {
            throw new ServiceException("Client supplied an updateSequence that is greater than the current sever updateSequence", "InvalidUpdateSequence");
        }
        if (reqUS == geoUS) {
            throw new ServiceException("WFS capabilities document is current (updateSequence = " + geoUS + ")", "CurrentUpdateSequence");
        }
    }

    Set<FunctionName> getAvailableFunctionNames() {
        //Sort them up for easier visual inspection
        SortedSet sortedFunctions = new TreeSet(new Comparator() {

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

    /**
     * Transformer for wfs 1.0 capabilities document.
     *
     * @author Justin Deoliveira, The Open Planning Project
     */
    public static class TJS1_0 extends CapabilitiesTransformer {

        public TJS1_0(TJSInfo tjs, TJSCatalog catalog) {
            super(tjs, catalog);
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

            protected String getBaseURL() {
                try {
                    Request owsRequest = ((ThreadLocal<Request>) Dispatcher.REQUEST).get();
                    if (owsRequest != null){
                        return owsRequest.getHttpRequest().getRequestURL().toString();
                    }else{
                        //ocurre cuando se realizan los test
                        return "http://localhost:8080/geoserver/";
                    }
                } catch (Exception ex) {
                    return null;
                }
            }

            public void encode(Object object) throws IllegalArgumentException {
                request = (GetCapabilitiesType) object;

                verifyUpdateSequence(request);

                AttributesImpl attributes = attributes(new String[]{
                                                                           "version", "1.0",
                                                                           "lang", "es",
                                                                           "service", "TJS",
                                                                           "capabilities", "http://sis.agr.gc.ca/pls/meta/tjs_1x0_getcapabilities",
                                                                           "xmlns:xsi", XSI_URI,
                                                                           "xmlns", TJS.NAMESPACE,
                                                                           "xmlns:ows", OWS.NAMESPACE, //"xmlns:gml", GML.NAMESPACE,
                                                                           "xmlns:ogc", OGC.NAMESPACE, "xmlns:xlink", XLINK.NAMESPACE,
                                                                           "xsi:schemaLocation", TJS.NAMESPACE + " "
                                                                                                         + "http://schemas.opengis.net/tjs/1.0/tjsDescribeDatasets_response.xsd"
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

                if (tjs.getGeoServer() != null) {
                    attributes.addAttribute("", "updateSequence", "updateSequence", "",
                                                   tjs.getGeoServer().getGlobal().getUpdateSequence() + "");
                }

                start(TJS.Capabilities.getLocalPart(), attributes);

                serviceIdentification();
                serviceProvider(tjs.getGeoServer());
                operationsMetadata();
//                featureTypeList();
                //supportsGMLObjectTypeList();
//                filterCapabilities();

                end(TJS.Capabilities.getLocalPart());
            }

            void serviceIdentification() {
                start("ows:ServiceIdentification");

                element("ows:Title", tjs.getTitle());
                element("ows:Abstract", tjs.getAbstract());

                keywords(tjs.getKeywords());

                element("ows:ServiceType", "TJS");
                element("ows:ServiceTypeVersion", "1.0.0");

                element("ows:Fees", tjs.getFees());
                element("ows:AccessConstraints", tjs.getAccessConstraints());

                end("ows:ServiceIdentification");
            }

            void serviceProvider(GeoServer gs) {
                start(OWS.ServiceProvider.getLocalPart());

                if (gs != null) {
                    ContactInfo contact = gs.getGlobal().getContact();
                    element("ows:ProviderName", contact.getContactOrganization());
                    start("ows:ServiceContact");
                    element("ows:IndividualName", contact.getContactPerson());
                    element("ows:PositionName", contact.getContactPosition());

                    start("ows:ContactInfo");
                    start("ows:Phone");
                    element("ows:Voice", contact.getContactVoice());
                    element("ows:Facsimile", contact.getContactFacsimile());
                    end("ows:Phone");

                    start("ows:Address");
                    element("ows:City", contact.getAddressCity());
                    element("ows:AdministrativeArea", contact.getAddressState());
                    element("ows:PostalCode", contact.getAddressPostalCode());
                    element("ows:Country", contact.getAddressCountry());
                    end("ows:Address");

                    end("ows:ContactInfo");

                    end("ows:ServiceContact");
                }
                end(OWS.ServiceProvider.getLocalPart());
            }

            void operationsMetadata() {
                start("ows:OperationsMetadata");

                getCapabilities();
                getDescribeFrameworks();
                getDescribeKey();
                getDescribeDatasets();
                getDescribeData();
                getGetData();


                //getGmlObject();

                if (tjs.getServiceLevel().contains(TJSInfo.ServiceLevel.BASIC)) {
//                    lockFeature();
//                    getFeatureWithLock();
                }

                if (tjs.getServiceLevel().contains(TJSInfo.ServiceLevel.COMPLETE)) {
//                    transaction();
                }

                end("ows:OperationsMetadata");
            }

            void getCapabilities() {
                Map.Entry[] parameters = new Map.Entry[]{
                                                                parameter("AcceptVersions", new String[]{"1.0.0"}),
                                                                parameter("AcceptFormats", new String[]{"text/xml"})
                };
                operation("GetCapabilities", parameters, true, true);
            }

            void getDescribeFrameworks() {
                Map.Entry[] parameters = new Map.Entry[]{};
                operation("DescribeFrameworks", parameters, true, true);
            }

            void getDescribeKey() {
                Map.Entry[] parameters = new Map.Entry[]{};
                operation("DescribeKey", parameters, true, true);
            }

            void getDescribeDatasets() {
                Map.Entry[] parameters = new Map.Entry[]{};
                operation("DescribeDatasets", parameters, true, true);
            }

            void getDescribeData() {
                Map.Entry[] parameters = new Map.Entry[]{};
                operation("DescribeData", parameters, true, true);
            }

            void getGetData() {
                Map.Entry[] parameters = new Map.Entry[]{};
                operation("GetData", parameters, true, true);
            }

            void getDescribeJoinAbilities() {
                Map.Entry[] parameters = new Map.Entry[]{};
                operation("DescribeJoinAbilities", parameters, true, true);
            }

            void getJoinData() {
                Map.Entry[] parameters = new Map.Entry[]{};
                operation("JoinData", parameters, true, true);
            }

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
                if (keywords != null) {
                    keywords((KeywordInfo[]) keywords.toArray(new KeywordInfo[keywords.size()]));
                }
            }

            void operation(String name, Map.Entry[] parameters, Map.Entry[] constraints, boolean get, boolean post) {
                start("ows:Operation", attributes(new String[]{"name", name}));

                //dcp
                start("ows:DCP");
                start("ows:HTTP");

                String serviceURL = buildURL(getBaseURL(), "tjs", null, URLType.SERVICE);
                if (get) {
                    element("ows:Get", null, attributes(new String[]{"xlink:href", serviceURL}));
                }

                if (post) {
                    element("ows:Post", null, attributes(new String[]{"xlink:href", serviceURL}));
                }

                end("ows:HTTP");
                end("ows:DCP");

                //parameters
                for (int i = 0; i < parameters.length; i++) {
                    String pname = (String) parameters[i].getKey();
                    String[] pvalues = (String[]) parameters[i].getValue();

                    start("ows:Parameter", attributes(new String[]{"name", pname}));

                    for (int j = 0; j < pvalues.length; j++) {
                        element("ows:Value", pvalues[j]);
                    }

                    end("ows:Parameter");
                }

                //constraints
                for (int i = 0; constraints != null && i < constraints.length; i++) {
                    String cname = (String) constraints[i].getKey();
                    String[] cvalues = (String[]) constraints[i].getValue();

                    start("ows:Constraint", attributes(new String[]{"name", cname}));

                    for (int j = 0; j < cvalues.length; j++) {
                        element("ows:Value", cvalues[j]);
                    }

                    end("ows:Constraint");
                }

                end("ows:Operation");
            }

            /**
             * @see {@link #operation(String, java.util.Map.Entry[], java.util.Map.Entry[], boolean, boolean)}
             */
            void operation(String name, Map.Entry[] parameters, boolean get, boolean post) {
                operation(name, parameters, null, get, post);
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
