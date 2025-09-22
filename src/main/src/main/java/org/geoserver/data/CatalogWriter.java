/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Writes the GeoServer catalog.xml file.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * Map dataStores = ...
 * Map nameSpaces = ...
 *
 * CatalogWriter writer = new CatalogWriter();
 * writer.dataStores( dataStores );
 * writer.nameSpaces( nameSpaces );
 *
 * File catalog = new File( ".../catalog.xml" );
 * writer.write( catalog );
 *
 *
 * }</pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class CatalogWriter {
    /** The xml document */
    Document document;

    /** Root catalog element. */
    Element catalog;

    /** The coverage type key (aka format name) */
    public static final String COVERAGE_TYPE_KEY = "coverageType";

    /** The coverage url key (the actual coverage data location) */
    public static final String COVERAGE_URL_KEY = "coverageUrl";

    public CatalogWriter() {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(false);
            builderFactory.setValidating(false);

            document = builderFactory.newDocumentBuilder().newDocument();
            catalog = document.createElement("catalog");
            document.appendChild(catalog);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes "datastore" elements to the catalog.xml file.
     *
     * @param dataStores map of id to connection parameter map
     * @param namespaces map of id to namespace prefix map
     */
    public void dataStores(
            Map /* <String,Map> */ dataStores, Map /*<String,String>*/ namespaces, Set /*<String>*/ disabled) {
        Element dataStoresElement = document.createElement("datastores");
        catalog.appendChild(dataStoresElement);

        for (Object item : dataStores.entrySet()) {
            Map.Entry dataStore = (Map.Entry) item;
            String id = (String) dataStore.getKey();
            Map params = (Map) dataStore.getValue();

            Element dataStoreElement = document.createElement("datastore");
            dataStoresElement.appendChild(dataStoreElement);

            // set the datastore id
            dataStoreElement.setAttribute("id", id);
            dataStoreElement.setAttribute("enabled", Boolean.toString(!disabled.contains(id)));

            // set the namespace
            dataStoreElement.setAttribute("namespace", (String) namespaces.get(id));

            // encode hte ocnnection paramters
            Element connectionParamtersElement = document.createElement("connectionParams");
            dataStoreElement.appendChild(connectionParamtersElement);

            for (Object o : params.entrySet()) {
                Map.Entry param = (Map.Entry) o;
                String name = (String) param.getKey();
                Object value = param.getValue();

                // skip null values
                if (value == null) {
                    continue;
                }

                Element parameterElement = document.createElement("parameter");
                connectionParamtersElement.appendChild(parameterElement);

                parameterElement.setAttribute("name", name);
                parameterElement.setAttribute("value", value.toString());
            }
        }
    }

    /** Writers the "formats" element of the catalog.xml file */
    public void coverageStores(HashMap coverageStores, HashMap namespaces, Set disabled) {
        Element formatsElement = document.createElement("formats");
        catalog.appendChild(formatsElement);

        for (Object o : coverageStores.entrySet()) {
            Map.Entry dataStore = (Map.Entry) o;
            String id = (String) dataStore.getKey();
            Map params = (Map) dataStore.getValue();

            Element formatElement = document.createElement("format");
            formatsElement.appendChild(formatElement);

            // set the datastore id
            formatElement.setAttribute("id", id);
            formatElement.setAttribute("enabled", Boolean.toString(!disabled.contains(id)));

            // set the namespace
            formatElement.setAttribute("namespace", (String) namespaces.get(id));

            // encode type and url
            Element typeElement = document.createElement("type");
            formatElement.appendChild(typeElement);
            typeElement.setTextContent((String) params.get(COVERAGE_TYPE_KEY));
            Element urlElement = document.createElement("url");
            formatElement.appendChild(urlElement);
            urlElement.setTextContent((String) params.get(COVERAGE_URL_KEY));
        }
    }

    /**
     * Writes "namespace" elements to the catalog.xml file.
     *
     * @param namespaces map of &lt;prefix,urilt;prefix,uriprefix,urigt;&gt;, default uri is located under the empty
     *     string key.
     */
    public void namespaces(Map namespaces) {
        namespaces(namespaces, Collections.emptyList());
    }

    /**
     * Writes namespaces elements to the catalog.xml file.
     *
     * @param namespaces map containing namespaces prefix and URIs
     * @param isolatedNamespaces list containing the prefix of isolated namespaces
     */
    public void namespaces(Map namespaces, List<String> isolatedNamespaces) {
        Element namespacesElement = document.createElement("namespaces");
        catalog.appendChild(namespacesElement);

        for (Object o : namespaces.entrySet()) {
            Map.Entry namespace = (Map.Entry) o;
            String prefix = (String) namespace.getKey();
            String uri = (String) namespace.getValue();

            // dont write out default prefix
            if ("".equals(prefix)) {
                continue;
            }

            Element namespaceElement = document.createElement("namespace");
            namespacesElement.appendChild(namespaceElement);

            namespaceElement.setAttribute("uri", uri);
            namespaceElement.setAttribute("prefix", prefix);

            // check for default
            if (uri.equals(namespaces.get(""))) {
                namespaceElement.setAttribute("default", "true");
            }

            // check if is an isolated workspace
            if (isolatedNamespaces.contains(prefix)) {
                // mark this namespace as isolated
                namespaceElement.setAttribute("isolated", "true");
            }
        }
    }

    /**
     * Writes "style" elements to the catalog.xml file.
     *
     * @param styles map of &lt;id,filename&gt;
     */
    public void styles(Map styles) {
        Element stylesElement = document.createElement("styles");
        catalog.appendChild(stylesElement);

        for (Object o : styles.entrySet()) {
            Map.Entry style = (Map.Entry) o;
            String id = (String) style.getKey();
            String filename = (String) style.getValue();

            Element styleElement = document.createElement("style");
            stylesElement.appendChild(styleElement);

            styleElement.setAttribute("id", id);
            styleElement.setAttribute("filename", filename);
        }
    }

    /**
     * WRites the catalog.xml file.
     *
     * <p>This method *must* be called after any other methods.
     *
     * @param file The catalog.xml file.
     * @throws IOException In event of a writing error.
     */
    public void write(File file) throws IOException {
        try (FileOutputStream os = new FileOutputStream(file)) {
            Transformer tx = TransformerFactory.newInstance().newTransformer();
            tx.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(os);

            tx.transform(source, result);
        } catch (Exception e) {
            String msg = "Could not write catalog to " + file;
            throw new IOException(msg, e);
        }
    }
}
