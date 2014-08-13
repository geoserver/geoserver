/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs;

import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.data.xml.ClassToXSDMapper;
import net.opengis.tjs10.DescribeDatasetsType;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FunctionFactory;
import org.geotools.filter.v1_0.OGC;
import org.geotools.geometry.jts.ReferencedEnvelope;
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

/**
 * Based on the <code>org.geotools.xml.transform</code> framework, does the job
 * of encoding a WFS 1.0 Capabilities document.
 *
 * @author Gabriel Roldan, Axios Engineering
 * @author Chris Holmes
 * @author Justin Deoliveira
 * @version $Id: CapabilitiesTransformer.java 16404 2011-10-06 18:36:00Z jdeolive $
 */
public abstract class DescribeDatasetsTransformer extends TransformerBase {

    /**
     * logger
     */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(DescribeDatasetsTransformer.class.getPackage().getName());
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
    public DescribeDatasetsTransformer(TJSInfo tjs, TJSCatalog catalog) {
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
    public static class TJS1_0 extends DescribeDatasetsTransformer {

        public TJS1_0(TJSInfo tjs, TJSCatalog catalog) {
            super(tjs, catalog);
        }

        public Translator createTranslator(ContentHandler handler) {
            return new DescribeDatasetTranslator(handler);
        }

        class DescribeDatasetTranslator extends TranslatorSupport {

            //            private static final String GML_3_1_1_FORMAT = "text/xml; subtype=gml/3.1.1";
            DescribeDatasetsType request;

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

            public DescribeDatasetTranslator(ContentHandler handler) {
                super(handler, null, null);
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
                request = (DescribeDatasetsType) object;

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

                start(TJS.DatasetDescriptions.getLocalPart(), attributes);


                for (FrameworkInfo framework : catalog.getFrameworks()) {
                    if (framework.getUri().equals(request.getFrameworkURI())) {
                        handleFramework(framework);
                    }
                }

                end(TJS.DatasetDescriptions.getLocalPart());
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

            /*
            *    <DatasetURI>http://sis.agr.gc.ca/cansis/nsdb/ecostrat/zone/v1/names</DatasetURI>
              <Organization>Agriculture and Agri-Food Canada</Organization>
              <Title>Names</Title>
              <Abstract></Abstract>
              <ReferenceDate>1999-09-16</ReferenceDate>
              <Version>1</Version>
              <Documentation></Documentation>
              <DescribeDataRequest xlink:href="http://sis.agr.gc.ca/pls/meta/tjs_1x0_describedata?Service=TJS&amp;Version=1.0&amp;Request=DescribeData&amp;FrameworkURI=http://sis.agr.gc.ca/cansis/nsdb/ecostrat/zone/v1&amp;DatasetURI=http://sis.agr.gc.ca/cansis/nsdb/ecostrat/zone/v1/names&amp;AcceptLanguages=en"/>
            */
            void handleDataset(FrameworkInfo framework, DatasetInfo dataset) {
                start(TJS.Dataset.getLocalPart());
                element(TJS.DatasetURI.getLocalPart(), dataset.getDatasetUri());
                element(TJS.Title.getLocalPart(), dataset.getName());
                if (dataset.getReferenceDate() != null) {
                    element(TJS.ReferenceDate.getLocalPart(), dataset.getReferenceDate().toString());
                }
                element(TJS.Version.getLocalPart(), String.valueOf(dataset.getVersion()));
                element(TJS.Documentation.getLocalPart(), dataset.getDocumentation());

                element(TJS.Organization.getLocalPart(), dataset.getOrganization());
                element(TJS.Abstract.getLocalPart(), dataset.getDescription());

                String url = getBaseURL() + "?request=DescribeData&" +
                                     "Service=TJS&Version=1.0.0&FrameworkURI=" + framework.getUri() +
                                     "&DatasetURI=" + dataset.getDatasetUri();
                AttributesImpl attributes = attributes(new String[]{"xlink:href", url});
                element(TJS.DescribeDataRequest.getLocalPart(), "", attributes);

                end(TJS.Dataset.getLocalPart());
            }

            void handleDatasets(FrameworkInfo framework) {
                List<DatasetInfo> datasets = catalog.getDatasetsByFramework(framework.getId());


                for (int i = 0; i < datasets.size(); i++) {
                    DatasetInfo datasetInfo = datasets.get(i);
                    if (request.getDatasetURI() != null) {
                        if (request.getDatasetURI().equals(datasetInfo.getDatasetUri())) {
                            handleDataset(framework, datasetInfo);
                        }
                    } else {
                        handleDataset(framework, datasetInfo);
                    }
                }
            }

            void handleFramework(FrameworkInfo framework) {
                start(TJS.Framework.getLocalPart());
                element(TJS.FrameworkURI.getLocalPart(), framework.getUri());
                element(TJS.Organization.getLocalPart(), framework.getOrganization());
                element(TJS.Title.getLocalPart(), framework.getName());
                element(TJS.Abstract.getLocalPart(), framework.getDescription());
                if (framework.getRefererenceDate() != null) {
                    element(TJS.ReferenceDate.getLocalPart(), framework.getRefererenceDate().toString());
                }
                element(TJS.Version.getLocalPart(), String.valueOf(framework.getVersion()));
                element(TJS.Documentation.getLocalPart(), framework.getDocumentation());
                handleFrameworkKey(framework.getFrameworkKey());
                handleBoundingCoordinates(framework.getBoundingCoordinates());

                String url = getBaseURL() + "?request=DescribeDatasets&Service=TJS&Version=1.0.0&FrameworkURI=" + framework.getUri();
                AttributesImpl attributes = attributes(new String[]{"xlink:href", url});
                element(TJS.DescribeDatasetsRequest.getLocalPart(), "", attributes);

                handleDatasets(framework);
                end(TJS.Framework.getLocalPart());
            }

        }
    }
}
