/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
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
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.FeatureTypeUtils;
import org.geoserver.wfs.WFSConstants;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geoserver.wfs.response.ComplexFeatureAwareFormat;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;
import org.w3c.dom.Document;

public abstract class BaseGML3OutputFormat extends WFSGetFeatureOutputFormat implements ComplexFeatureAwareFormat {

    /** Enables the optimized encoders */
    public static final boolean OPTIMIZED_ENCODING =
            Boolean.parseBoolean(System.getProperty("GML_OPTIMIZED_ENCODING", "true"));

    GeoServer geoServer;
    Catalog catalog;
    protected static DOMSource xslt;

    static {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        Document xsdDocument = null;
        try {
            xsdDocument = docFactory
                    .newDocumentBuilder()
                    .parse(BaseGML3OutputFormat.class.getResourceAsStream("/ChangeNumberOfFeature.xslt"));
            xslt = new DOMSource(xsdDocument);
        } catch (Exception e) {
            xslt = null;
            LOGGER.log(Level.INFO, e.getMessage(), e);
        }
    }

    public BaseGML3OutputFormat(Set<String> outputFormats, GeoServer geoServer) {
        super(geoServer, outputFormats);

        this.geoServer = geoServer;
        this.catalog = geoServer.getCatalog();
    }

    @Override
    public String getMimeType(Object value, Operation operation) {
        return "text/xml; subtype=gml/3.1.1";
    }

    @Override
    public String getCapabilitiesElementName() {
        return "GML3";
    }

    @Override
    protected void write(FeatureCollectionResponse results, OutputStream output, Operation getFeature)
            throws ServiceException, IOException, UnsupportedEncodingException {
        List featureCollections = results.getFeature();

        int numDecimals = getNumDecimals(featureCollections, geoServer, catalog);
        boolean padWithZeros = getPadWithZeros(featureCollections, geoServer, catalog);
        boolean forcedDecimal = getForcedDecimal(featureCollections, geoServer, catalog);

        GetFeatureRequest request = GetFeatureRequest.adapt(getFeature.getParameters()[0]);

        // round up the info objects for each feature collection
        HashMap<String, Set<ResourceInfo>> ns2metas = new HashMap<>();
        for (int fcIndex = 0; fcIndex < featureCollections.size(); fcIndex++) {
            if (request != null) {
                List<Query> queries = request.getQueries();
                Query queryType = queries.get(fcIndex);

                // may have multiple type names in each query, so add them all
                for (QName name : queryType.getTypeNames()) {
                    // get a feature type name from the query
                    Name featureTypeName = new NameImpl(name.getNamespaceURI(), name.getLocalPart());
                    ResourceInfo meta = catalog.getResourceByName(featureTypeName, ResourceInfo.class);

                    if (meta == null) {
                        throw new WFSException(
                                request,
                                "Could not find feature type " + featureTypeName + " in the GeoServer catalog");
                    }

                    // add it to the map
                    Set<ResourceInfo> metas = ns2metas.get(featureTypeName.getNamespaceURI());

                    if (metas == null) {
                        metas = new HashSet<>();
                        ns2metas.put(featureTypeName.getNamespaceURI(), metas);
                    }
                    metas.add(meta);
                }
            } else {
                FeatureType featureType = ((FeatureCollection) featureCollections.get(fcIndex)).getSchema();

                // load the metadata for the feature type
                String namespaceURI = featureType.getName().getNamespaceURI();
                FeatureTypeInfo meta = catalog.getFeatureTypeByName(featureType.getName());

                if (meta == null)
                    throw new WFSException(
                            request,
                            "Could not find feature type " + featureType.getName() + " in the GeoServer catalog");

                // add it to the map
                Set<ResourceInfo> metas = ns2metas.get(namespaceURI);

                if (metas == null) {
                    metas = new HashSet<>();
                    ns2metas.put(namespaceURI, metas);
                }

                metas.add(meta);
            }
        }

        WFSInfo wfs = getInfo();
        // declare wfs schema location
        Object gft = getFeature.getParameters()[0];
        Configuration config = createConfiguration(ns2metas, gft);

        // set feature bounding parameter
        // JD: this is quite bad as its not at all thread-safe, once we remove the configuration
        // as being a singleton on trunk/2.0.x this should not be an issue
        if (wfs.isFeatureBounding()) {
            config.getProperties().remove(GMLConfiguration.NO_FEATURE_BOUNDS);
        } else {
            config.getProperties().add(GMLConfiguration.NO_FEATURE_BOUNDS);
        }

        if (wfs.isCiteCompliant()) {
            // cite compliance forces us to forgo srsDimension attribute
            config.getProperties().add(GMLConfiguration.NO_SRS_DIMENSION);
        } else {
            config.getProperties().remove(GMLConfiguration.NO_SRS_DIMENSION);
        }

        if (OPTIMIZED_ENCODING) {
            config.getProperties().add(GMLConfiguration.OPTIMIZED_ENCODING);
        } else {
            config.getProperties().remove(GMLConfiguration.OPTIMIZED_ENCODING);
        }

        /*
         * Set property encoding featureMemeber as opposed to featureMembers
         *
         */
        if (wfs.isEncodeFeatureMember()) {
            config.getProperties().add(GMLConfiguration.ENCODE_FEATURE_MEMBER);
        } else {
            config.getProperties().remove(GMLConfiguration.ENCODE_FEATURE_MEMBER);
        }

        boolean encodeMeasures = encodeMeasures(featureCollections, catalog);
        updateConfiguration(config, numDecimals, padWithZeros, forcedDecimal, encodeMeasures);
        Encoder encoder = createEncoder(config, ns2metas, gft);

        encoder.setEncoding(Charset.forName(geoServer.getSettings().getCharset()));
        if (wfs.isVerbose() || geoServer.getSettings().isVerbose()) {
            encoder.setIndenting(true);
        } else {
            encoder.setIndenting(false);
        }
        Request dispatcherRequest = Dispatcher.REQUEST.get();
        if (dispatcherRequest != null) {
            encoder.setOmitXMLDeclaration(dispatcherRequest.isSOAP());
        }

        if (wfs.isCanonicalSchemaLocation()) {
            encoder.setSchemaLocation(getWfsNamespace(), getCanonicalWfsSchemaLocation());
        } else {
            encoder.setSchemaLocation(
                    getWfsNamespace(), buildSchemaURL(request.getBaseURL(), getRelativeWfsSchemaLocation()));
        }

        // declare application schema namespaces

        Map<String, String> params =
                params("service", "WFS", "version", request.getVersion(), "request", "DescribeFeatureType");
        for (Map.Entry<String, Set<ResourceInfo>> stringSetEntry : ns2metas.entrySet()) {
            Map.Entry entry = stringSetEntry;

            String namespaceURI = (String) entry.getKey();
            Set metas = (Set) entry.getValue();

            StringBuffer typeNames = new StringBuffer();
            for (Iterator m = metas.iterator(); m.hasNext(); ) {
                ResourceInfo ri = (ResourceInfo) m.next();
                if (ri instanceof FeatureTypeInfo meta) {
                    FeatureType featureType = meta.getFeatureType();
                    Object userSchemaLocation = featureType.getUserData().get("schemaURI");
                    if (userSchemaLocation != null && userSchemaLocation instanceof Map) {
                        @SuppressWarnings("unchecked")
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
                String schemaLocation = buildURL(request.getBaseURL(), "wfs", params, URLType.SERVICE);
                LOGGER.finer("Unable to find user-defined schema location for: "
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

    protected abstract void updateConfiguration(
            Configuration configuration,
            int numDecimals,
            boolean padWithZeros,
            boolean forcedDecimal,
            boolean encodeMeasures);

    protected abstract Configuration createConfiguration(Map<String, Set<ResourceInfo>> resources, Object request);

    protected abstract Encoder createEncoder(
            Configuration configuration, Map<String, Set<ResourceInfo>> resources, Object request);

    protected void setAdditionalSchemaLocations(Encoder encoder, GetFeatureRequest request, WFSInfo wfs) {
        // hook for subclasses
    }

    protected void encode(FeatureCollectionResponse results, OutputStream output, Encoder encoder) throws IOException {
        encoder.encode(results.unadapt(FeatureCollectionType.class), WFSConstants.FEATURECOLLECTION_1_1, output);
    }

    protected DOMSource getXSLT() {
        return BaseGML3OutputFormat.xslt;
    }

    private void complexFeatureStreamIntercept(FeatureCollectionResponse results, OutputStream output, Encoder encoder)
            throws IOException {
        if (this.getXSLT() == null) {
            throw new FileNotFoundException("Unable to locate xslt resource file");
        }

        // Create a temporary file for the xml dump. _dump is added to ensure the hash create is
        // more then 3 char.
        File featureOut = File.createTempFile(output.hashCode() + "_dump", ".xml");
        // create a buffered output stream to write the output from encode to disk first
        // create a buffered input stream to read the dumped xml file in
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(featureOut));
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(featureOut))) {
            // the output file has to be unique with each Class object to ensure concurrency
            encode(results, out, encoder);
            this.transform(in, this.getXSLT(), output);
        } catch (TransformerException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        } finally {
            featureOut.delete();
        }
    }

    protected String getWfsNamespace() {
        return WFSConstants.NAMESPACE_1_1_0;
    }

    protected String getCanonicalWfsSchemaLocation() {
        return WFSConstants.CANONICAL_SCHEMA_LOCATION_1_1;
    }

    protected String getRelativeWfsSchemaLocation() {
        return "wfs/1.1.0/wfs.xsd";
    }

    public static boolean isComplexFeature(FeatureCollectionResponse results) {
        return FeatureTypeUtils.isComplexFeature(results);
    }

    public void transform(InputStream in, DOMSource xslt, OutputStream out) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = (xslt == null ? factory.newTransformer() : factory.newTransformer(xslt));
        transformer.setErrorListener(new TransformerErrorListener());
        transformer.transform(new StreamSource(in), new StreamResult(out));
    }

    // If an application does not register its own custom ErrorListener, the default ErrorListener
    // is used which reports all warnings and errors to System.err and does not throw any Exceptions
    private static class TransformerErrorListener implements ErrorListener {

        @Override
        public void error(TransformerException exception) throws TransformerException {
            throw exception;
        }

        @Override
        public void fatalError(TransformerException exception) throws TransformerException {
            throw exception;
        }

        @Override
        public void warning(TransformerException exception) throws TransformerException {
            throw exception;
        }
    }

    @Override
    public boolean supportsComplexFeatures(Object value, Operation operation) {
        return true;
    }
}
