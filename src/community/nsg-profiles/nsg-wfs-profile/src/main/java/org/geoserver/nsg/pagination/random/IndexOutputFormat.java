/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.nsg.pagination.random;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.response.v2_0.HitsOutputFormat;
import org.geotools.data.DataStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.wfs.v2_0.WFSConfiguration;
import org.geotools.xsd.Encoder;
import org.opengis.feature.simple.SimpleFeature;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This output format handles requests if the original requested result type was "index" See {@link
 * IndexResultTypeDispatcherCallback}
 *
 * @author sandr
 */
public class IndexOutputFormat extends HitsOutputFormat {

    private static final Logger LOGGER = Logging.getLogger(IndexOutputFormat.class);

    private String resultSetId;

    private Request request;

    private IndexConfigurationManager indexConfiguration;

    public IndexOutputFormat(GeoServer gs, IndexConfigurationManager indexConfiguration) {
        super(gs);
        this.indexConfiguration = indexConfiguration;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        // generate an UUID (resultSetID) for this request, GeoTools complained about the - in the
        // ID
        resultSetId = UUID.randomUUID().toString().replaceAll("-", "");
        // store request and associate it to UUID
        storeGetFeature(resultSetId, this.request);
        super.write(value, output, operation);
    }

    @Override
    protected void encode(FeatureCollectionResponse hits, OutputStream output, WFSInfo wfs)
            throws IOException {

        hits.setNumberOfFeatures(BigInteger.ZERO);
        // instantiate the XML encoder
        Encoder encoder = new Encoder(new WFSConfiguration());
        encoder.setEncoding(Charset.forName(wfs.getGeoServer().getSettings().getCharset()));
        encoder.setSchemaLocation(
                WFS.NAMESPACE, ResponseUtils.appendPath(wfs.getSchemaBaseURL(), "wfs/2.0/wfs.xsd"));
        Document document;
        try {
            // encode the HITS result using FeatureCollection as the root XML element
            document = encoder.encodeAsDOM(hits.getAdaptee(), WFS.FeatureCollection);
        } catch (Exception exception) {
            throw new RuntimeException("Error encoding INDEX result.", exception);
        }
        // add the resultSetID attribute to the result
        addResultSetIdElement(document, resultSetId);
        // write the XML document to response output stream
        writeDocument(document, output);
    }

    /**
     * Helper method that serialize GetFeature request, store it in the file system and associate it
     * with resultSetId
     */
    private void storeGetFeature(String resultSetId, Request request) throws RuntimeException {
        try {
            IndexConfigurationManager.READ_WRITE_LOCK.writeLock().lock();
            DataStore dataStore = this.indexConfiguration.getCurrentDataStore();
            // Create and store new feature
            SimpleFeatureStore featureStore =
                    (SimpleFeatureStore)
                            dataStore.getFeatureSource(IndexConfigurationManager.STORE_SCHEMA_NAME);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureStore.getSchema());
            Long now = System.currentTimeMillis();
            // Add ID field value (see IndexInitializer.STORE_SCHEMA)
            builder.add(resultSetId);
            // Add created field value (see IndexInitializer.STORE_SCHEMA)
            builder.add(now);
            // Add updated field value (see IndexInitializer.STORE_SCHEMA)
            builder.add(now);
            SimpleFeature feature = builder.buildFeature(null);
            SimpleFeatureCollection collection =
                    new ListFeatureCollection(featureStore.getSchema(), Arrays.asList(feature));
            featureStore.addFeatures(collection);
            // Create and store file
            Resource storageResource = this.indexConfiguration.getStorageResource();

            // Serialize KVP parameters and the POST content
            Map kvp = request.getKvp();
            Map rawKvp = request.getRawKvp();
            RequestData data = new RequestData();
            data.setKvp(kvp);
            data.setRawKvp(rawKvp);
            if (kvp.containsKey("POST_REQUEST")) {
                data.setPostRequest((String) kvp.get("POST_REQUEST"));
            }

            try (ObjectOutputStream oos =
                    new ObjectOutputStream(
                            new FileOutputStream(
                                    new File(storageResource.dir(), resultSetId + ".feature")))) {
                oos.writeObject(data);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Error storing feature.", exception);
        } finally {
            IndexConfigurationManager.READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    /**
     * Helper method that adds the resultSetID attribute to XML result. If no FeatureCollection
     * element can be found nothing will be done.
     */
    private static void addResultSetIdElement(Document document, String resultSetId) {
        // search FeatureCollection XML nodes
        NodeList nodes = document.getElementsByTagName("wfs:FeatureCollection");
        if (nodes.getLength() != 1) {
            // only one node should exists, let's log an warning an move on
            LOGGER.warning(
                    "No feature collection element could be found, resultSetID attribute will not be added.");
            return;
        }
        // get the FeatureCollection node
        Node node = nodes.item(0);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            // the found node is a XML element so let's add the resultSetID attribute
            Element element = (Element) node;
            element.setAttribute("resultSetID", resultSetId);
        } else {
            // unlikely but we got a XML node that is not a XML element
            LOGGER.warning(
                    "Feature collection node is not a XML element, resultSetID attribute will not be added.");
        }
    }

    /** Helper method that just writes a XML document to a given output stream. */
    private static void writeDocument(Document document, OutputStream output) {
        // instantiate a new XML transformer
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (Exception exception) {
            throw new RuntimeException("Error creating XML transformer.", exception);
        }
        // write the XML document to the provided output stream
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(output);
        try {
            transformer.transform(source, result);
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Error writing INDEX result to the output stream.", exception);
        }
    }
}
