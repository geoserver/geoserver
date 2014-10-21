/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.opengis.ows10.ExceptionReportType;
import net.opengis.ows10.ExceptionType;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.gss.CentralRevisionsType.LayerRevision;
import org.geoserver.gss.xml.GSS;
import org.geoserver.gss.xml.GSSConfiguration;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A client that connects to a single Unit.
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class HTTPGSSClient implements GSSClient {
    
    static final Logger LOGGER = Logging.getLogger(HTTPGSSClient.class);

    HttpClient client;

    URL address;

    String username;

    String password;

    GSSConfiguration configuration;

    Catalog catalog;

    public HTTPGSSClient(HttpClient client, GSSConfiguration configuration, Catalog catalog,
            URL gssServiceURL, String username, String password) {
        if (gssServiceURL.getPath().endsWith("/")) {
            try {
                String external = gssServiceURL.toExternalForm();
                this.address = new URL(external.substring(0, external.length() - 1));
            } catch (MalformedURLException e) {
                throw new RuntimeException("Unexpected error normalizing GSS url" + e);
            }
        } else {
            this.address = gssServiceURL;
        }
        this.configuration = configuration;
        this.client = client;
        this.username = username;
        this.password = password;
        this.catalog = catalog;
    }

    public long getCentralRevision(QName layerName) throws IOException {
        // execute the GetCentralRevision call
        GetMethod method = new GetMethod(address
                + "?service=GSS&version=1.0.0&request=GetCentralRevision&typeName="
                + prefixedName(layerName));
        Object response = executeMethod(method);

        // interpret the parsed response
        if (response instanceof CentralRevisionsType) {
            CentralRevisionsType cr = (CentralRevisionsType) response;
            for (LayerRevision lr : cr.getLayerRevisions()) {
                if (layerName.equals(lr.getTypeName())) {
                    return lr.getCentralRevision();
                }
            }
            throw new IOException("Response to GetCentralRevision received, but it "
                    + "did not contain a central revision for " + layerName);
        } else {
            if (response == null) {
                throw new IOException("The response was parsed to a null object");
            }
            throw new IOException("The response was parsed to an unrecognized object type: "
                    + response.getClass());
        }
    }

    String prefixedName(QName layerName) {
        return layerName.getPrefix() + ":" + layerName.getLocalPart();
    }

    public GetDiffResponseType getDiff(GetDiffType getDiff) throws IOException {
        // prepare the encoder
        Encoder encoder = new Encoder(configuration, configuration.getXSD().getSchema());
        QName layerName = getDiff.getTypeName();
        encoder.getNamespaces().declarePrefix(layerName.getPrefix(), layerName.getNamespaceURI());
        encoder.setEncoding(Charset.forName("UTF-8"));

        // prepare POST request
        PostMethod method = new PostMethod(address.toExternalForm());
        method.setContentChunked(true);
        method.setRequestEntity(new XMLEntity(getDiff, GSS.GetDiff, encoder));

        // execute the request and interpret the response
        Object response = executeMethod(method);
        if (response instanceof GetDiffResponseType) {
            return (GetDiffResponseType) response;
        } else {
            if (response == null) {
                throw new IOException("The response was parsed to a null object");
            }
            throw new IOException("The response was parsed to an unrecognized object type: "
                    + response.getClass());
        }

    }

    public void postDiff(PostDiffType postDiff)
            throws IOException {
        // prepare the encoder
        Encoder encoder = buildEncoderForTransaction(postDiff.getTransaction());

        // prepare POST request
        PostMethod method = new PostMethod(address.toExternalForm());
        method.setContentChunked(true);
        method.setRequestEntity(new XMLEntity(postDiff, GSS.PostDiff, encoder));

        // execute the request and interpret the parsed response
        Object response = executeMethod(method);
        if (response instanceof PostDiffResponseType) {
            // that's fine, it's like a placeholder for now
        } else {
            if (response == null) {
                throw new IOException("The response was parsed to a null object");
            }
            throw new IOException("The response was parsed to an unrecognized object type: "
                    + response.getClass());
        }
    }

    /**
     * Executes the http method, checks the response status, parses the response and returns it.
     * Will throw an exception in case of communication errors, error codes, or service exceptions
     * 
     * @throws IOException
     */
    Object executeMethod(HttpMethod method) throws IOException {
        Object response;
        try {
            if(LOGGER.isLoggable(Level.FINE)) {
                if(method instanceof GetMethod) {
                    GetMethod gm = (GetMethod) method;
                    LOGGER.fine("Sending GET request:\n " + method.getURI());
                } else if(method instanceof PostMethod) {
                    PostMethod pm = (PostMethod) method;
                    XMLEntity entity = (XMLEntity) pm.getRequestEntity();
                    String request = entity.toString();
                    LOGGER.fine("Sending POST request:\n " + method.getURI() + "\n" 
                            + request);
                    // ugly, but Encoder cannot be reused, so we have to set a new entity
                    // that uses the already generated string
                    pm.setRequestEntity(new StringRequestEntity(request, "text/xml", "UTF-8"));
                } else {
                    LOGGER.fine("Sending unknown method type : " + method);
                }
            }
            
            // plain execution
            int statusCode = client.executeMethod(method);

            // check the HTTP status
            if (statusCode != HttpStatus.SC_OK) {
                throw new IOException("HTTP client returned with code " + statusCode);
            }

            // parse the response
            Parser parser = new Parser(configuration);
            parser.setStrict(true);
            parser.setFailOnValidationError(true);
            InputStream is;
            if(LOGGER.isLoggable(Level.FINE)) {
                String responseString = method.getResponseBodyAsString();
                LOGGER.log(Level.FINE, "Response from Unit:\n" + responseString);
                is = new ByteArrayInputStream(responseString.getBytes());
            } else {
                is = method.getResponseBodyAsStream();
            }
            response = parser.parse(is);
        } catch (Exception e) {
            throw (IOException) new IOException("Error occurred while executing "
                    + "a call to the Unit").initCause(e);
        } finally {
            method.releaseConnection();
        }

        // convert a service exception into an IOException if necessary
        if (response instanceof ExceptionReportType) {
            ExceptionReportType report = (ExceptionReportType) response;
            StringBuilder sb = new StringBuilder("The Unit service reported a failure: ");
            for (Iterator it = report.getException().iterator(); it.hasNext();) {
                ExceptionType et = (ExceptionType) it.next();
                for (Iterator eit = et.getExceptionText().iterator(); eit.hasNext();) {
                    String text = (String) eit.next();
                    sb.append(text);
                    if (eit.hasNext())
                        sb.append(". ");
                }

            }

            throw new IOException(sb.toString());
        }

        return response;
    }

    /**
     * Builds a XML encoder for the specified transaction. The code will declare all
     * prefix/namespace URI associations necessary for the elements in the transaction
     */
    Encoder buildEncoderForTransaction(TransactionType changes) throws IOException {
        Encoder encoder = new Encoder(configuration, configuration.getXSD().getSchema());

        // try to declare all namespace prefixes properly
        NamespaceSupport namespaces = encoder.getNamespaces();
        List<DeleteElementType> deletes = changes.getDelete();
        List<UpdateElementType> updates = changes.getUpdate();
        List<InsertElementType> inserts = changes.getInsert();
        for (DeleteElementType delete : deletes) {
            QName typeName = delete.getTypeName();
            namespaces.declarePrefix(typeName.getPrefix(), typeName.getNamespaceURI());
        }
        for (UpdateElementType update : updates) {
            QName typeName = update.getTypeName();
            namespaces.declarePrefix(typeName.getPrefix(), typeName.getNamespaceURI());
        }
        for (InsertElementType insert : inserts) {
            List<SimpleFeature> features = insert.getFeature();
            for (SimpleFeature feature : features) {
                Name typeName = feature.getType().getName();
                NamespaceInfo nsi = catalog.getNamespaceByURI(typeName.getNamespaceURI());
                if (nsi != null) {
                    namespaces.declarePrefix(nsi.getPrefix(), nsi.getURI());
                }
            }
        }

        encoder.setEncoding(Charset.forName("UTF-8"));
        return encoder;
    }

    /**
     * An XML entitity based
     * 
     * @author aaime
     * 
     */
    static class XMLEntity implements RequestEntity {

        Encoder encoder;

        Object object;

        QName element;

        public XMLEntity(Object object, QName element, Encoder encoder) {
            super();
            this.encoder = encoder;
            this.object = object;
            this.element = element;
        }

        public long getContentLength() {
            return -1;
        }

        public String getContentType() {
            return "text/xml";
        }

        public boolean isRepeatable() {
            return true;
        }

        public void writeRequest(OutputStream os) throws IOException {
            encoder.encode(object, element, os);
        }
        
        @Override
        public String toString() {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                encoder.setIndenting(true);
                encoder.encode(object, element, bos);
                return bos.toString();
            } catch(IOException e) {
                return "XMLEntity toString failed: " + e.getMessage();
            }
        }

    }
    
    
    
}
