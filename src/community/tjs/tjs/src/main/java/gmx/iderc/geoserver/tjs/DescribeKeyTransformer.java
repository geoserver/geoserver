/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs;

import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.data.xml.ClassToXSDMapper;
import net.opengis.tjs10.DescribeKeyType;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.v1_0.OGC;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.v1_1.OWS;
import org.geotools.tjs.TJS;
import org.geotools.xlink.XLINK;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.Feature;
import org.opengis.filter.capability.FunctionName;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the job
 * of encoding a WFS 1.0 Capabilities document.
 *
 * @author Gabriel Roldan, Axios Engineering
 * @author Chris Holmes
 * @author Justin Deoliveira
 * @version $Id: CapabilitiesTransformer.java 16404 2011-10-06 18:36:00Z jdeolive $
 */
public abstract class DescribeKeyTransformer extends TransformerBase {

    /**
     * logger
     */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(DescribeKeyTransformer.class.getPackage().getName());
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
    public DescribeKeyTransformer(TJSInfo tjs, TJSCatalog catalog) {
        super();
        setNamespaceDeclarationEnabled(false);

        this.tjs = tjs;
        this.catalog = catalog;
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
    public static class TJS1_0 extends DescribeKeyTransformer {

        public TJS1_0(TJSInfo tjs, TJSCatalog catalog) {
            super(tjs, catalog);
        }

        public Translator createTranslator(ContentHandler handler) {
            return new DescribeKeyTranslator(handler);
        }

        class DescribeKeyTranslator extends TranslatorSupport {

            //            private static final String GML_3_1_1_FORMAT = "text/xml; subtype=gml/3.1.1";
            DescribeKeyType request;

            public DescribeKeyTranslator(ContentHandler handler) {
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

            AttributesImpl attributes(String[] nameValues) {
                AttributesImpl atts = new AttributesImpl();

                for (int i = 0; i < nameValues.length; i += 2) {
                    String name = nameValues[i];
                    String valu = nameValues[i + 1];

                    atts.addAttribute(null, null, name, null, valu);
                }

                return atts;
            }

            public void encode(Object object) throws IllegalArgumentException {
                request = (DescribeKeyType) object;

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

                start(TJS.FrameworkKeyDescription.getLocalPart(), attributes);


                for (FrameworkInfo framework : catalog.getFrameworks()) {
                    if (framework.getUri().equals(request.getFrameworkURI())) {
                        handleFramework(framework);
                    }
                }

                end(TJS.FrameworkKeyDescription.getLocalPart());
            }

            void handleFrameworkKey(AttributeTypeInfo frameworkKey) {
                if (frameworkKey == null) {
                    return;
                }
                start(TJS.FrameworkKey.getLocalPart());
                //   <Column name="ecozone" type="http://www.w3.org/TR/xmlschema-2/#integer" length="2" decimals="0" />
                AttributesImpl attributes = attributes(new String[]{
                                                                           "name", frameworkKey.getName(),
                                                                           "type", ClassToXSDMapper.map(frameworkKey.getBinding()),
                                                                           "length", String.valueOf(frameworkKey.getLength()),
                                                                           "decimals", "0"});
                element("Column", "", attributes);
                end(TJS.FrameworkKey.getLocalPart());
            }

            void handleBoundingCoordinates(ReferencedEnvelope envelope) {
                if (envelope == null) {
                    return;
                }
                start(TJS.BoundingCoordinates.getLocalPart());
                element("North", String.valueOf(envelope.getMaxY()));
                element("South", String.valueOf(envelope.getMinY()));
                element("East", String.valueOf(envelope.getMaxX()));
                element("West", String.valueOf(envelope.getMinX()));
                end(TJS.BoundingCoordinates.getLocalPart());
            }

            void handleFramework(FrameworkInfo framework) {
                start(TJS.Framework.getLocalPart());
                element(TJS.FrameworkURI.getLocalPart(), framework.getUri());
                element(TJS.Organization.getLocalPart(), framework.getOrganization());
                element(TJS.Title.getLocalPart(), framework.getName());
                element(TJS.Abstract.getLocalPart(), framework.getDescription());
                if (framework.getRefererenceDate() != null) {
                    DateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:SS");
                    element(TJS.ReferenceDate.getLocalPart(), format.format(framework.getRefererenceDate()));
                }
                element(TJS.Version.getLocalPart(), String.valueOf(framework.getVersion()));
                element(TJS.Documentation.getLocalPart(), framework.getDocumentation());
                handleFrameworkKey(framework.getFrameworkKey());
                handleBoundingCoordinates(framework.getBoundingCoordinates());
                handleRowSet(framework);
                end(TJS.Framework.getLocalPart());
            }

            void handleRowSet(FrameworkInfo framework) {
                try {
                    if (framework.getFeatureType() == null) {
                        return;
                    }
                    if (framework.getFrameworkKey() == null) {
                        return;
                    }
                    boolean hasTitle = framework.getFrameworkKeyTitle() != null;
                    start(TJS.Rowset.getLocalPart());
                    Catalog geoCatalog = framework.getCatalog().getGeoserverCatalog();
                    DataStoreInfo dsi = framework.getFeatureType().getStore();
                    FeatureSource featureSource = geoCatalog.getResourcePool().getFeatureSource(framework.getFeatureType(), null);
                    String keyName = framework.getFrameworkKey().getName();
                    String titleName = "";
                    if (hasTitle) {
                        titleName = framework.getFrameworkKeyTitle().getName();
                    }
                    for (FeatureIterator fit = featureSource.getFeatures().features(); fit.hasNext(); ) {
                        Feature feature = fit.next();
                        start("Row");
                        element(TJS.K.getLocalPart(), feature.getProperty(keyName).getValue().toString());
                        if (hasTitle) {
                            element(TJS.Title.getLocalPart(), feature.getProperty(titleName).getValue().toString());
                        }
                        end("Row");
                    }
                    end(TJS.Rowset.getLocalPart());
                } catch (IOException ex) {
                    Logger.getLogger(DescribeKeyTransformer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
