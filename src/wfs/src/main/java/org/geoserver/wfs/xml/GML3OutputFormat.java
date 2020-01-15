/* (c) 2014 -2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.opengis.wfs.FeatureCollectionType;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.w3c.dom.Document;

public class GML3OutputFormat extends WFSGetFeatureOutputFormat {

    /** Enables the optimized encoders */
    public static final boolean OPTIMIZED_ENCODING =
            Boolean.parseBoolean(System.getProperty("GML_OPTIMIZED_ENCODING", "true"));

    GeoServer geoServer;
    Catalog catalog;
    WFSConfiguration configuration;
    protected static DOMSource xslt;

    static {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        Document xsdDocument = null;
        try {
            xsdDocument =
                    docFactory
                            .newDocumentBuilder()
                            .parse(
                                    GML3OutputFormat.class.getResourceAsStream(
                                            "/ChangeNumberOfFeature.xslt"));
            xslt = new DOMSource(xsdDocument);
        } catch (Exception e) {
            xslt = null;
            LOGGER.log(Level.INFO, e.getMessage(), e);
        }
    }

    public GML3OutputFormat(GeoServer geoServer, WFSConfiguration configuration) {
        this(
                new HashSet(Arrays.asList(new Object[] {"gml3", "text/xml; subtype=gml/3.1.1"})),
                geoServer,
                configuration);
    }

    public GML3OutputFormat(
            Set<String> outputFormats, GeoServer geoServer, WFSConfiguration configuration) {
        super(geoServer, outputFormats);

        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();

        this.configuration = configuration;
    }

    public String getMimeType(Object value, Operation operation) {
        return "text/xml; subtype=gml/3.1.1";
    }

    public String getCapabilitiesElementName() {
        return "GML3";
    }

    protected void write(
            FeatureCollectionResponse results, OutputStream output, Operation getFeature)
            throws ServiceException, IOException, UnsupportedEncodingException {
        List featureCollections = results.getFeature();

        int numDecimals = getNumDecimals(featureCollections, geoServer, catalog);
        boolean padWithZeros = getPadWithZeros(featureCollections, geoServer, catalog);
        boolean forcedDecimal = getForcedDecimal(featureCollections, geoServer, catalog);

        GetFeatureRequest request = GetFeatureRequest.adapt(getFeature.getParameters()[0]);

        // round up the info objects for each feature collection
        HashMap<String, Set<ResourceInfo>> ns2metas = new HashMap<String, Set<ResourceInfo>>();
        for (int fcIndex = 0; fcIndex < featureCollections.size(); fcIndex++) {
            if (request != null) {
                List<Query> queries = request.getQueries();
                Query queryType = queries.get(fcIndex);

                // may have multiple type names in each query, so add them all
                for (QName name : queryType.getTypeNames()) {
                    // get a feature type name from the query
                    Name featureTypeName =
                            new NameImpl(name.getNamespaceURI(), name.getLocalPart());
                    ResourceInfo meta =
                            catalog.getResourceByName(featureTypeName, ResourceInfo.class);

                    if (meta == null) {
                        throw new WFSException(
                                request,
                                "Could not find feature type "
                                        + featureTypeName
                                        + " in the GeoServer catalog");
                    }

                    // add it to the map
                    Set<ResourceInfo> metas = ns2metas.get(featureTypeName.getNamespaceURI());

                    if (metas == null) {
                        metas = new HashSet<ResourceInfo>();
                        ns2metas.put(featureTypeName.getNamespaceURI(), metas);
                    }
                    metas.add(meta);
                }
            } else {
                FeatureType featureType =
                        ((FeatureCollection) featureCollections.get(fcIndex)).getSchema();

                // load the metadata for the feature type
                String namespaceURI = featureType.getName().getNamespaceURI();
                FeatureTypeInfo meta = catalog.getFeatureTypeByName(featureType.getName());

                if (meta == null)
                    throw new WFSException(
                            request,
                            "Could not find feature type "
                                    + featureType.getName()
                                    + " in the GeoServer catalog");

                // add it to the map
                Set metas = ns2metas.get(namespaceURI);

                if (metas == null) {
                    metas = new HashSet();
                    ns2metas.put(namespaceURI, metas);
                }

                metas.add(meta);
            }
        }

        WFSInfo wfs = getInfo();

        // set feature bounding parameter
        // JD: this is quite bad as its not at all thread-safe, once we remove the configuration
        // as being a singleton on trunk/2.0.x this should not be an issue
        if (wfs.isFeatureBounding()) {
            configuration.getProperties().remove(GMLConfiguration.NO_FEATURE_BOUNDS);
        } else {
            configuration.getProperties().add(GMLConfiguration.NO_FEATURE_BOUNDS);
        }

        if (wfs.isCiteCompliant()) {
            // cite compliance forces us to forgo srsDimension attribute
            configuration.getProperties().add(GMLConfiguration.NO_SRS_DIMENSION);
        } else {
            configuration.getProperties().remove(GMLConfiguration.NO_SRS_DIMENSION);
        }

        if (OPTIMIZED_ENCODING) {
            configuration.getProperties().add(GMLConfiguration.OPTIMIZED_ENCODING);
        } else {
            configuration.getProperties().remove(GMLConfiguration.OPTIMIZED_ENCODING);
        }

        // set up the srsname syntax
        configuration.setSrsSyntax(
                wfs.getGML().get(WFSInfo.Version.V_11).getSrsNameStyle().toSrsSyntax());

        /*
         * Set property encoding featureMemeber as opposed to featureMembers
         *
         */
        if (wfs.isEncodeFeatureMember()) {
            configuration.getProperties().add(GMLConfiguration.ENCODE_FEATURE_MEMBER);
        } else {
            configuration.getProperties().remove(GMLConfiguration.ENCODE_FEATURE_MEMBER);
        }

        // declare wfs schema location
        Object gft = getFeature.getParameters()[0];

        Configuration configuration = customizeConfiguration(this.configuration, ns2metas, gft);
        boolean encodeMeasures = encodeMeasures(featureCollections, catalog);
        updateConfiguration(
                configuration, numDecimals, padWithZeros, forcedDecimal, encodeMeasures);
        Encoder encoder = createEncoder(configuration, ns2metas, gft);

        encoder.setEncoding(Charset.forName(geoServer.getSettings().getCharset()));
        Request dispatcherRequest = Dispatcher.REQUEST.get();
        if (dispatcherRequest != null) {
            encoder.setOmitXMLDeclaration(dispatcherRequest.isSOAP());
        }

        if (wfs.isCanonicalSchemaLocation()) {
            encoder.setSchemaLocation(getWfsNamespace(), getCanonicalWfsSchemaLocation());
        } else {
            encoder.setSchemaLocation(
                    getWfsNamespace(),
                    buildSchemaURL(request.getBaseURL(), getRelativeWfsSchemaLocation()));
        }

        // declare application schema namespaces

        Map<String, String> params =
                params(
                        "service",
                        "WFS",
                        "version",
                        request.getVersion(),
                        "request",
                        "DescribeFeatureType");
        for (Iterator i = ns2metas.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();

            String namespaceURI = (String) entry.getKey();
            Set metas = (Set) entry.getValue();

            StringBuffer typeNames = new StringBuffer();
            for (Iterator m = metas.iterator(); m.hasNext(); ) {
                ResourceInfo ri = (ResourceInfo) m.next();
                if (ri instanceof FeatureTypeInfo) {
                    final FeatureTypeInfo meta = (FeatureTypeInfo) ri;
                    FeatureType featureType = meta.getFeatureType();
                    Object userSchemaLocation = featureType.getUserData().get("schemaURI");
                    if (userSchemaLocation != null && userSchemaLocation instanceof Map) {
                        Map<String, String> schemaURIs = (Map<String, String>) userSchemaLocation;
                        for (String namespace : schemaURIs.keySet()) {
                            encoder.setSchemaLocation(namespace, schemaURIs.get(namespace));
                        }
                    } else {
                        typeNames.append(meta.prefixedName());
                        if (m.hasNext()) {
                            typeNames.append(",");
                        }
                    }
                } else {
                    encoder.getNamespaces()
                            .declarePrefix(ri.getStore().getWorkspace().getName(), namespaceURI);
                }
            }

            if (typeNames.length() > 0) {
                params.put("typeName", typeNames.toString());
                // set the made up schema location for types not provided by the user
                String schemaLocation =
                        buildURL(request.getBaseURL(), "wfs", params, URLType.SERVICE);
                LOGGER.finer(
                        "Unable to find user-defined schema location for: "
                                + namespaceURI
                                + ". Using a built schema location by default: "
                                + schemaLocation);
                encoder.setSchemaLocation(namespaceURI, schemaLocation);
            }
        }

        setAdditionalSchemaLocations(encoder, request, wfs);
        if (isComplexFeature(results)) {
            complexFeatureStreamIntercept(results, output, encoder);
        } else {
            encode(results, output, encoder);
        }
    }

    protected void updateConfiguration(
            Configuration configuration,
            int numDecimals,
            boolean padWithZeros,
            boolean forcedDecimal,
            boolean encodeMeasures) {
        // GML 3.1. configuration
        GMLConfiguration gml31 = configuration.getDependency(GMLConfiguration.class);
        if (gml31 != null) {
            gml31.setNumDecimals(numDecimals);
            gml31.setPadWithZeros(padWithZeros);
            gml31.setForceDecimalEncoding(forcedDecimal);
            gml31.setEncodeMeasures(encodeMeasures);
            return;
        }
        // GML 3.2 configuration
        org.geotools.gml3.v3_2.GMLConfiguration gml32 =
                configuration.getDependency(org.geotools.gml3.v3_2.GMLConfiguration.class);
        if (gml32 != null) {
            gml32.setNumDecimals(numDecimals);
            gml32.setPadWithZeros(padWithZeros);
            gml32.setForceDecimalEncoding(forcedDecimal);
            gml32.setEncodeMeasures(encodeMeasures);
        }
    }

    protected Configuration customizeConfiguration(
            Configuration configuration, Map<String, Set<ResourceInfo>> resources, Object request) {
        return configuration;
    }

    protected Encoder createEncoder(
            Configuration configuration, Map<String, Set<ResourceInfo>> resources, Object request) {
        // reuse the WFS configuration feature builder, otherwise build a new one
        FeatureTypeSchemaBuilder schemaBuilder;
        if (configuration instanceof WFSConfiguration) {
            schemaBuilder = ((WFSConfiguration) configuration).getSchemaBuilder();
        } else {
            schemaBuilder = new FeatureTypeSchemaBuilder.GML3(geoServer);
        }
        // create this request specific schema
        ApplicationSchemaXSD1 schema = new ApplicationSchemaXSD1(schemaBuilder);
        schema.setBaseURL(GetFeatureRequest.adapt(request).getBaseURL());
        schema.setResources(resources);
        if (schema.getFeatureTypes().isEmpty()) {
            // no feature types so let's use the base WFS schema
            XSDSchema result;
            try {
                result = configuration.getXSD().getSchema();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return new Encoder(configuration, result);
        }
        try {
            // let's just instantiate the encoder
            return new Encoder(configuration, schema.getSchema());
        } catch (IOException exception) {
            throw new RuntimeException(
                    "Error generating the XSD schema during the encoder instantiation.", exception);
        }
    }

    protected void setAdditionalSchemaLocations(
            Encoder encoder, GetFeatureRequest request, WFSInfo wfs) {
        // hook for subclasses
    }

    protected void encode(FeatureCollectionResponse results, OutputStream output, Encoder encoder)
            throws IOException {
        encoder.encode(
                results.unadapt(FeatureCollectionType.class),
                org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION,
                output);
    }

    protected DOMSource getXSLT() {
        return GML3OutputFormat.xslt;
    }

    private void complexFeatureStreamIntercept(
            FeatureCollectionResponse results, OutputStream output, Encoder encoder)
            throws IOException {
        if (this.getXSLT() == null) {
            throw new FileNotFoundException("Unable to locate xslt resource file");
        }

        // Create a temporary file for the xml dump. _dump is added to ensure the hash create is
        // more then 3 char.
        File featureOut = File.createTempFile(output.hashCode() + "_dump", ".xml");
        // create a buffered output stream to write the output from encode to disk first
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(featureOut));
        // create a buffered input stream to read the dumped xml file in
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(featureOut));
        try {
            // the output file has to be unique with each Class object to ensure concurrency
            encode(results, out, encoder);
            this.transform(in, this.getXSLT(), output);
        } catch (TransformerException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } finally {
            out.close();
            in.close();
            featureOut.delete();
        }
    }

    protected String getWfsNamespace() {
        return org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE;
    }

    protected String getCanonicalWfsSchemaLocation() {
        return WFS.CANONICAL_SCHEMA_LOCATION;
    }

    protected String getRelativeWfsSchemaLocation() {
        return "wfs/1.1.0/wfs.xsd";
    }

    public static boolean isComplexFeature(FeatureCollectionResponse results) {
        boolean hasComplex = false;
        for (int fcIndex = 0; fcIndex < results.getFeature().size(); fcIndex++) {
            if (!(results.getFeature().get(fcIndex).getSchema() instanceof SimpleFeatureTypeImpl)) {
                hasComplex = true;
                break;
            }
        }
        return hasComplex;
    }

    public void transform(InputStream in, DOMSource xslt, OutputStream out)
            throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer =
                (xslt == null ? factory.newTransformer() : factory.newTransformer(xslt));
        transformer.setErrorListener(new TransformerErrorListener());
        transformer.transform(new StreamSource(in), new StreamResult(out));
    }

    // If an application does not register its own custom ErrorListener, the default ErrorListener
    // is used which reports all warnings and errors to System.err and does not throw any Exceptions
    private class TransformerErrorListener implements ErrorListener {

        public void error(TransformerException exception) throws TransformerException {
            throw exception;
        }

        public void fatalError(TransformerException exception) throws TransformerException {
            throw exception;
        }

        public void warning(TransformerException exception) throws TransformerException {
            throw exception;
        }
    }
}
