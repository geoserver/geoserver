/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.util;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ows.util.XmlCharsetDetector;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Reads the GeoServer catalog.xml file.
 *
 * <p>Usage:
 *
 * <pre>
 *         <code>
 *                 File catalog = new File( ".../catalog.xml" );
 *                 LegacygCatalogReader reader = new LegacygCatalogReader();
 *                 reader.read( catalog );
 *                 List dataStores = reader.dataStores();
 *                 List nameSpaces = reader.nameSpaces();
 *         </code>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class LegacyCatalogReader {

    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog");

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
    public void read(Resource file) throws IOException {
        Reader reader = XmlCharsetDetector.getCharsetAwareReader(file.in());

        try {
            catalog = ReaderUtils.parse(reader);
        } finally {
            reader.close();
        }
    }

    /**
     * Reads "datastore" elements from the catalog.xml file.
     *
     * <p>For each datastore element read, a map is returned which contains the following key /
     * values:
     *
     * <ul>
     *   <li>"id": data store id (String)
     *   <li>"namespace": namespace prefix of datastore (String) *
     *   <li>"enabled": wether the format is enabled or not (Boolean) *
     *   <li>"connectionParams": data store connection parameters (Map)
     * </ul>
     *
     * * indicates that the parameter is optional and may be <code>null</code>
     *
     * @return A list of Map objects containing datastore information.
     * @throws Exception If error processing "datastores" element.
     */
    public Map<String, Map<String, Object>> dataStores() throws Exception {
        Element dataStoresElement = ReaderUtils.getChildElement(catalog, "datastores", true);

        NodeList dataStoreElements = dataStoresElement.getElementsByTagName("datastore");
        Map dataStores = new LinkedHashMap();

        for (int i = 0; i < dataStoreElements.getLength(); i++) {
            Element dataStoreElement = (Element) dataStoreElements.item(i);

            Map dataStore = new HashMap();

            String id = ReaderUtils.getAttribute(dataStoreElement, "id", true);
            dataStore.put("id", id);
            dataStore.put(
                    "namespace", ReaderUtils.getAttribute(dataStoreElement, "namespace", false));
            dataStore.put(
                    "enabled",
                    Boolean.valueOf(
                            ReaderUtils.getBooleanAttribute(
                                    dataStoreElement, "enabled", false, true)));
            try {
                Map params = dataStoreParams(dataStoreElement);
                dataStore.put("connectionParams", params);

            } catch (Exception e) {
                LOGGER.warning("Error reading data store paramaters: " + e.getMessage());
                LOGGER.log(Level.INFO, "", e);
                continue;
            }

            dataStores.put(id, dataStore);
        }

        return dataStores;
    }

    /**
     * Reads "format" elements from the catalog.xml file.
     *
     * <p>For each format element read, a map is returned which contains the following key / values:
     *
     * <ul>
     *   <li>"id": format id (String)
     *   <li>"type": type of the format (String)
     *   <li>"namespace": namespace prefix of format (String) *
     *   <li>"enabled": wether the format is enabled or not (Boolean) *
     *   <li>"url": url of the format (String) *
     *   <li>"title": title of the format (String) *
     *   <li>"description": description of the format (String) *
     * </ul>
     *
     * * indicates that the parameter is optional and may be <code>null</code>
     *
     * @return A list of Map objects containg the format information.
     * @throws Exception If error processing "datastores" element.
     */
    public List<Map<String, Object>> formats() throws Exception {
        Element formatsElement = ReaderUtils.getChildElement(catalog, "formats", true);

        NodeList formatElements = formatsElement.getElementsByTagName("format");
        ArrayList formats = new ArrayList();

        for (int i = 0; i < formatElements.getLength(); i++) {
            Element formatElement = (Element) formatElements.item(i);

            Map format = new HashMap();

            format.put("id", ReaderUtils.getAttribute(formatElement, "id", true));
            format.put("namespace", ReaderUtils.getAttribute(formatElement, "namespace", false));
            format.put(
                    "enabled",
                    Boolean.valueOf(
                            ReaderUtils.getBooleanAttribute(
                                    formatElement, "enabled", false, true)));

            format.put("type", ReaderUtils.getChildText(formatElement, "type", true));
            format.put("url", ReaderUtils.getChildText(formatElement, "url", false));
            format.put("title", ReaderUtils.getChildText(formatElement, "title", false));
            format.put(
                    "description", ReaderUtils.getChildText(formatElement, "description", false));

            formats.add(format);
        }

        return formats;
    }

    /**
     * Reads "namespace" elements from the catalog.xml file.
     *
     * <p>For each namespace element read, an entry of <prefix,uri> is created in a map. The default
     * uri is located under the empty string key.
     *
     * @return A map containing <prefix,uri> tuples.
     * @throws Exception If error processing "namespaces" element.
     */
    public Map<String, String> namespaces() throws Exception {
        return readNamespaces(false);
    }

    /**
     * Reads "namespace" elements from the catalog.xml file that correspond to isolated workspaces.
     *
     * <p>For each namespace element read, an entry of <prefix,uri> is created in a map. The default
     * uri is located under the empty string key.
     *
     * @return A map containing <prefix,uri> tuples.
     * @throws Exception If error processing "namespaces" element.
     */
    public Map<String, String> isolatedNamespaces() throws Exception {
        return readNamespaces(true);
    }

    /**
     * Helper method that retrieves namespaces from the catalog.xml file. If readIsolated parameter
     * is TRUE isolated workspace will be read, otherwise only non isolated workspaces will be read.
     */
    private Map<String, String> readNamespaces(boolean readIsolated) throws Exception {
        // get the namespaces XML root element
        Element namespacesElement = ReaderUtils.getChildElement(catalog, "namespaces", true);
        if (namespacesElement == null) {
            // no namespaces available, unlikely but possible
            LOGGER.log(Level.INFO, "No namespaces available.");
            return Collections.emptyMap();
        }
        // get all namespaces XML nodes
        NodeList namespaceElements = namespacesElement.getElementsByTagName("namespace");
        // parse each namespace XML node and store it
        Map<String, String> namespaces = new HashMap<>();
        for (int i = 0; i < namespaceElements.getLength(); i++) {
            Element namespaceElement = (Element) namespaceElements.item(i);
            try {
                // get namespace information from the XML node
                String prefix = namespaceElement.getAttribute("prefix");
                String uri = namespaceElement.getAttribute("uri");
                boolean isDefault =
                        namespaceElement.getAttribute("default").equalsIgnoreCase("true");
                boolean isIsolated =
                        namespaceElement.getAttribute("isolated").equalsIgnoreCase("true");
                // let's see if we need to return this namespace
                if ((!readIsolated && isIsolated) || (readIsolated && !isIsolated)) {
                    // not interest in this namespace, move to the next one
                    continue;
                }
                namespaces.put(prefix, uri);
                // let's see if this is the default namespace
                if (isDefault) {
                    namespaces.put("", uri);
                }
            } catch (Exception exception) {
                // something bad happen when parsing the current element, log it and move to the
                // next one
                LOGGER.log(Level.WARNING, "Error parsing namespace XML element.", exception);
            }
        }
        return namespaces;
    }

    /**
     * Reads "style" elements from the catalog.xml file.
     *
     * <p>For each style element read, an entry of <id,filename> is created in a map.
     *
     * @return A map containing style <id,filename> tuples.
     * @throws Exception If error processing "styles" element.
     */
    public Map<String, String> styles() throws Exception {
        Element stylesElement = ReaderUtils.getChildElement(catalog, "styles", true);

        NodeList styleElements = stylesElement.getElementsByTagName("style");
        Map styles = new HashMap();

        for (int i = 0; i < styleElements.getLength(); i++) {
            Element styleElement = (Element) styleElements.item(i);
            styles.put(styleElement.getAttribute("id"), styleElement.getAttribute("filename"));
        }

        return styles;
    }

    /**
     * Convenience method for reading connection parameters from a datastore element.
     *
     * @param dataStoreElement The "datastore" element.
     * @return The map of connection paramters.
     * @throws Exception If problem parsing any parameters.
     */
    protected Map dataStoreParams(Element dataStoreElement) throws Exception {
        Element paramsElement =
                ReaderUtils.getChildElement(dataStoreElement, "connectionParams", true);
        NodeList paramList = paramsElement.getElementsByTagName("parameter");

        Map params = new HashMap();

        for (int i = 0; i < paramList.getLength(); i++) {
            Element paramElement = (Element) paramList.item(i);
            String key = ReaderUtils.getAttribute(paramElement, "name", true);
            String value = ReaderUtils.getAttribute(paramElement, "value", false);

            params.put(key, value);
        }

        return params;
    }

    /**
     * Convenience method for reading namespace prefix and uri from a namespace element.
     *
     * @param namespaceElement The "namespace" element.
     * @return A <prefix,uri> tuple.
     * @throws Exception If problem parsing any parameters.
     */
    protected Map.Entry namespaceTuple(Element namespaceElement) throws Exception {
        final String pre = namespaceElement.getAttribute("prefix");
        final String uri = namespaceElement.getAttribute("uri");

        return new Map.Entry() {
            public Object getKey() {
                return pre;
            }

            public Object getValue() {
                return uri;
            }

            public Object setValue(Object value) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
