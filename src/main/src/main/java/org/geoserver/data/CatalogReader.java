/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.util.ReaderUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads the GeoServer catalog.xml file.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * File catalog = new File( ".../catalog.xml" );
 * CatalogReader reader = new CatalogReader();
 * reader.read( catalog );
 * List dataStores = reader.dataStores();
 * LIst nameSpaces = reader.nameSpaces();
 *
 * }</pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class CatalogReader {
    /** Root catalog element. */
    Element catalog;

    /**
     * Parses the catalog.xml file into a DOM.
     *
     * <p>This method *must* be called before any other methods.
     *
     * @param file The catalog.xml file.
     * @throws IOException In event of a parser error.
     */
    public void read(File file) throws IOException {

        try (FileReader reader = new FileReader(file)) {
            catalog = ReaderUtils.parse(reader);
        }
    }

    /**
     * Reads "datastore" elements from the catalog.xml file.
     *
     * <p>For each datastore element read, a map of the connection parameters is created.
     *
     * @return A list of Map objects containg the datastore connection parameters.
     * @throws Exception If error processing "datastores" element.
     */
    public List<Map<String, String>> dataStores() throws Exception {
        Element dataStoresElement = ReaderUtils.getChildElement(catalog, "datastores", true);

        NodeList dataStoreElements = dataStoresElement.getElementsByTagName("datastore");
        List<Map<String, String>> dataStores = new ArrayList<>();

        for (int i = 0; i < dataStoreElements.getLength(); i++) {
            Element dataStoreElement = (Element) dataStoreElements.item(i);

            try {
                Map<String, String> params = dataStoreParams(dataStoreElement);
                dataStores.add(params);
            } catch (Exception e) {
                // TODO: log this
                continue;
            }
        }

        return dataStores;
    }

    /**
     * Reads "namespace" elements from the catalog.xml file.
     *
     * <p>For each namespace element read, an entry of &lt;prefix,urilt;prefix,uriprefix,urigt;&gt; is created in a map.
     * The default uri is located under the empty string key.
     *
     * @return A map containing &lt;prefix,urilt;prefix,uriprefix,urigt;&gt; tuples.
     * @throws Exception If error processing "namespaces" element.
     */
    public Map<String, String> namespaces() throws Exception {
        Element namespacesElement = ReaderUtils.getChildElement(catalog, "namespaces", true);

        NodeList namespaceElements = namespacesElement.getElementsByTagName("namespace");
        Map<String, String> namespaces = new HashMap<>();

        for (int i = 0; i < namespaceElements.getLength(); i++) {
            Element namespaceElement = (Element) namespaceElements.item(i);

            try {
                Map.Entry<String, String> tuple = namespaceTuple(namespaceElement);
                namespaces.put(tuple.getKey(), tuple.getValue());

                // check for default
                if ("true".equals(namespaceElement.getAttribute("default"))) {
                    namespaces.put("", tuple.getValue());
                }
            } catch (Exception e) {
                // TODO: log this
                continue;
            }
        }

        return namespaces;
    }

    /**
     * Convenience method for reading connection parameters from a datastore element.
     *
     * @param dataStoreElement The "datastore" element.
     * @return The map of connection paramters.
     * @throws Exception If problem parsing any parameters.
     */
    protected Map<String, String> dataStoreParams(Element dataStoreElement) throws Exception {
        Element paramsElement = ReaderUtils.getChildElement(dataStoreElement, "connectionParameters", true);
        NodeList paramList = paramsElement.getElementsByTagName("parameter");

        Map<String, String> params = new HashMap<>();

        for (int i = 0; i < paramList.getLength(); i++) {
            Element paramElement = (Element) paramList.item(i);
            String key = ReaderUtils.getAttribute(paramElement, "name", true);
            String value = ReaderUtils.getAttribute(paramElement, "value", true);

            params.put(key, value);
        }

        return params;
    }

    /**
     * Convenience method for reading namespace prefix and uri from a namespace element.
     *
     * @param namespaceElement The "namespace" element.
     * @return A &lt;prefix,urilt;prefix,uriprefix,urigt;&gt; tuple.
     * @throws Exception If problem parsing any parameters.
     */
    protected Map.Entry<String, String> namespaceTuple(Element namespaceElement) throws Exception {
        final String pre = namespaceElement.getAttribute("prefix");
        final String uri = namespaceElement.getAttribute("uri");

        return new AbstractMap.SimpleEntry<>(pre, uri);
    }
}
