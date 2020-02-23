/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * ReaderUtils purpose.
 *
 * <p>This class is intended to be used as a library of XML relevant operation for the GeoTools
 * XMLConfigDigester class.
 *
 * <p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @version $Id$
 */
public class ReaderUtils {
    /** Used internally to create log information to detect errors. */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.global");

    /**
     * ReaderUtils constructor.
     *
     * <p>Static class, this should never be called.
     */
    private ReaderUtils() {}

    /**
     * Parses the specified reader into a DOM tree.
     *
     * @param xml Reader representing xml stream to parse.
     * @return the root element of resulting DOM tree
     * @throws RuntimeException If reader failed to parse properly.
     */
    public static Element parse(Reader xml) {
        InputSource in = new InputSource(xml);
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

        dfactory.setNamespaceAware(false);
        dfactory.setValidating(false);
        dfactory.setIgnoringComments(true);
        dfactory.setCoalescing(true);
        dfactory.setIgnoringElementContentWhitespace(true);

        Document doc;

        try {
            doc = dfactory.newDocumentBuilder().parse(in);
        } catch (Exception e) {
            String msg = "Error reading : " + xml;
            throw new RuntimeException(msg, e);
        }

        return doc.getDocumentElement();
    }

    /**
     * Checks to ensure the file is valid.
     *
     * <p>Returns the file passed in to allow this to wrap file creations.
     *
     * @param file A file Handle to test.
     * @param isDir true when the File passed in is expected to be a directory, false when the
     *     handle is expected to be a file.
     * @return the File handle passed in
     * @throws FileNotFoundException When the file does not exist or is not the type specified.
     */
    public static File checkFile(File file, boolean isDir) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException(
                    (isDir ? "Folder" : "File") + " does not exist: " + file);
        }

        if (isDir && !file.isDirectory()) {
            throw new FileNotFoundException("File exists but is not a directory:" + file);
        }

        if (!isDir && !file.isFile()) {
            // may it be some sort of OS special file (e.g. /dev/tty1)
            throw new FileNotFoundException("File exists but is not a regular file:" + file);
        }

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer(new StringBuffer("File is valid: ").append(file).toString());
        }

        return file;
    }

    /**
     * getChildElements purpose.
     *
     * <p>Used to help with XML manipulations. Returns *all* child elements of the specified name.
     *
     * @param root The root element to look for children in.
     * @param name The name of the child element to look for.
     * @return The child element found, null if not found.
     * @see #getChildElement(Element,String,boolean)
     */
    public static Element[] getChildElements(Element root, String name) {
        try {
            return getChildElements(root, name, false);
        } catch (Exception e) {
            // will never be here.
            return null;
        }
    }

    /**
     * getChildElements purpose.
     *
     * <p>Used to help with XML manipulations. Returns *all* child elements of the specified name.
     * An exception occurs when the node is required and not found.
     *
     * @param root The root element to look for children in.
     * @param name The name of the child element to look for.
     * @param mandatory true when an exception should be thrown if the child element does not exist.
     * @return The child element found, null if not found.
     * @throws Exception When a child element is required and not found.
     */
    public static Element[] getChildElements(Element root, String name, boolean mandatory)
            throws Exception {
        final List<Element> elements = new ArrayList<Element>();
        Node child = root.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (name.equals(child.getNodeName())) {
                    elements.add((Element) child);
                }
            }

            child = child.getNextSibling();
        }

        if (mandatory && (elements.isEmpty())) {
            throw new Exception(
                    root.getNodeName() + " does not contains a child element named " + name);
        }

        return (Element[]) elements.toArray(new Element[0]);
    }

    /**
     * getChildElement purpose.
     *
     * <p>Used to help with XML manipulations. Returns the first child element of the specified
     * name. An exception occurs when the node is required and not found.
     *
     * @param root The root element to look for children in.
     * @param name The name of the child element to look for.
     * @param mandatory true when an exception should be thrown if the child element does not exist.
     * @return The child element found, null if not found.
     * @throws Exception When a child element is required and not found.
     */
    public static Element getChildElement(Element root, String name, boolean mandatory)
            throws Exception {
        Node child = root.getFirstChild();

        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (name.equals(child.getNodeName())) {
                    return (Element) child;
                }
            }

            child = child.getNextSibling();
        }

        if (mandatory && (child == null)) {
            throw new Exception(
                    root.getNodeName() + " does not contains a child element named " + name);
        }

        return null;
    }

    /**
     * getChildElement purpose.
     *
     * <p>Used to help with XML manipulations. Returns the first child element of the specified
     * name.
     *
     * @param root The root element to look for children in.
     * @param name The name of the child element to look for.
     * @return The child element found, null if not found.
     * @see #getChildElement(Element,String,boolean)
     */
    public static Element getChildElement(Element root, String name) {
        try {
            return getChildElement(root, name, false);
        } catch (Exception e) {
            // will never be here.
            return null;
        }
    }

    /**
     * getIntAttribute purpose.
     *
     * <p>Used to help with XML manipulations. Returns the first child integer attribute of the
     * specified name. An exception occurs when the node is required and not found.
     *
     * @param elem The root element to look for children in.
     * @param attName The name of the attribute to look for.
     * @param mandatory true when an exception should be thrown if the attribute element does not
     *     exist.
     * @param defaultValue a default value to return incase the attribute was not found. mutually
     *     exclusive with the Exception thrown.
     * @return The int value if the attribute was found, the default otherwise.
     * @throws Exception When a attribute element is required and not found.
     */
    public static int getIntAttribute(
            Element elem, String attName, boolean mandatory, int defaultValue) throws Exception {
        String attValue = getAttribute(elem, attName, mandatory);

        if (!mandatory && (attValue == null)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(attValue);
        } catch (Exception ex) {
            if (mandatory) {
                throw new Exception(
                        attName
                                + " attribute of element "
                                + elem.getNodeName()
                                + " must be an integer, but it's '"
                                + attValue
                                + "'");
            } else {
                return defaultValue;
            }
        }
    }

    /**
     * getIntAttribute purpose.
     *
     * <p>Used to help with XML manipulations. Returns the first child integer attribute of the
     * specified name. An exception occurs when the node is required and not found.
     *
     * @param elem The root element to look for children in.
     * @param attName The name of the attribute to look for.
     * @param mandatory true when an exception should be thrown if the attribute element does not
     *     exist.
     * @return The value if the attribute was found, the null otherwise.
     * @throws Exception When a child attribute is required and not found.
     */
    public static String getAttribute(Element elem, String attName, boolean mandatory)
            throws Exception {
        if (elem == null) {
            if (mandatory) {
                throw new NullPointerException();
            }

            return "";
        }

        Attr att = elem.getAttributeNode(attName);

        String value = null;

        if (att != null) {
            value = att.getValue();
        }

        if (mandatory) {
            if (att == null) {
                throw new Exception(
                        "element "
                                + elem.getNodeName()
                                + " does not contains an attribute named "
                                + attName);
            } else if ("".equals(value)) {
                throw new Exception(
                        "attribute " + attName + "in element " + elem.getNodeName() + " is empty");
            }
        }

        return value;
    }

    /**
     * getBooleanAttribute purpose.
     *
     * <p>Used to help with XML manipulations. Returns the first child integer attribute of the
     * specified name. An exception occurs when the node is required and not found.
     *
     * @param elem The root element to look for children in.
     * @param attName The name of the attribute to look for.
     * @param mandatory true when an exception should be thrown if the attribute element does not
     *     exist.
     * @param defaultValue what to return for a non-mandatory that is not found.
     * @return The value if the attribute was found, the false otherwise.
     * @throws Exception When a child attribute is required and not found.
     */
    public static boolean getBooleanAttribute(
            Element elem, String attName, boolean mandatory, boolean defaultValue)
            throws Exception {
        String value = getAttribute(elem, attName, mandatory);

        if ((value == null) || ("".equals(value))) {
            return defaultValue;
        }

        return Boolean.valueOf(value).booleanValue();
    }

    /**
     * getChildText purpose.
     *
     * <p>Used to help with XML manipulations. Returns the first child text value of the specified
     * element name.
     *
     * @param root The root element to look for children in.
     * @param childName The name of the attribute to look for.
     * @return The value if the child was found, the null otherwise.
     */
    public static String getChildText(Element root, String childName) {
        try {
            return getChildText(root, childName, false);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * getChildText purpose.
     *
     * <p>Used to help with XML manipulations. Returns the first child text value of the specified
     * element name. An exception occurs when the node is required and not found.
     *
     * @param root The root element to look for children in.
     * @param childName The name of the attribute to look for.
     * @param mandatory true when an exception should be thrown if the text does not exist.
     * @return The value if the child was found, the null otherwise.
     * @throws Exception When a child attribute is required and not found.
     */
    public static String getChildText(Element root, String childName, boolean mandatory)
            throws Exception {
        Element elem = getChildElement(root, childName, mandatory);

        if (elem != null) {
            return getElementText(elem, mandatory);
        } else {
            if (mandatory) {
                String msg = "Mandatory child " + childName + "not found in " + " element: " + root;

                throw new Exception(msg);
            }

            return null;
        }
    }

    /**
     * getChildText purpose.
     *
     * <p>Used to help with XML manipulations. Returns the text value of the specified element name.
     *
     * @param elem The root element to look for children in.
     * @return The value if the text was found, the null otherwise.
     */
    public static String getElementText(Element elem) {
        try {
            return getElementText(elem, false);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * getChildText purpose.
     *
     * <p>Used to help with XML manipulations. Returns the text value of the specified element name.
     * An exception occurs when the node is required and not found.
     *
     * @param elem The root element to look for children in.
     * @param mandatory true when an exception should be thrown if the text does not exist.
     * @return The value if the text was found, the null otherwise.
     * @throws Exception When text is required and not found.
     */
    public static String getElementText(Element elem, boolean mandatory) throws Exception {
        String value = null;

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer(new StringBuffer("getting element text for ").append(elem).toString());
        }

        if (elem != null) {
            Node child;

            NodeList childs = elem.getChildNodes();

            int nChilds = childs.getLength();

            for (int i = 0; i < nChilds; i++) {
                child = childs.item(i);

                if (child.getNodeType() == Node.TEXT_NODE) {
                    value = child.getNodeValue();

                    if (mandatory && "".equals(value.trim())) {
                        throw new Exception(elem.getNodeName() + " text is empty");
                    }

                    break;
                }
            }

            if (mandatory && (value == null)) {
                throw new Exception(elem.getNodeName() + " element does not contains text");
            }
        } else {
            throw new Exception("Argument element can't be null");
        }

        return unescape(value);
    }

    /**
     * getKeyWords purpose.
     *
     * <p>Used to help with XML manipulations. Returns a list of keywords that were found.
     *
     * @param keywordsElem The root element to look for children in.
     * @return The list of keywords that were found.
     */
    public static List getKeyWords(Element keywordsElem) {
        if (keywordsElem == null) {
            return Collections.EMPTY_LIST;
        }
        NodeList klist = keywordsElem.getElementsByTagName("keyword");
        int kCount = klist.getLength();
        List keywords = new ArrayList(kCount);
        String kword;
        Element kelem;

        for (int i = 0; i < kCount; i++) {
            kelem = (Element) klist.item(i);
            kword = getElementText(kelem);

            if (kword != null) {
                keywords.add(kword);
            }
        }

        Object[] s = (Object[]) keywords.toArray();

        if (s == null) {
            return new ArrayList();
        }

        ArrayList ss = new ArrayList(s.length);

        for (int i = 0; i < s.length; i++) ss.add(s[i]);

        return ss;
    }

    /**
     * getFirstChildElement purpose.
     *
     * <p>Used to help with XML manipulations. Returns the element which represents the first child.
     *
     * @param root The root element to look for children in.
     * @return The element if a child was found, the null otherwise.
     */
    public static Element getFirstChildElement(Element root) {
        Node child = root.getFirstChild();

        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) child;
            }

            child = child.getNextSibling();
        }

        return null;
    }

    /**
     * getDoubleAttribute purpose.
     *
     * <p>Used to help with XML manipulations. Returns the first child integer attribute of the
     * specified name. An exception occurs when the node is required and not found.
     *
     * @param elem The root element to look for children in.
     * @param attName The name of the attribute to look for.
     * @param mandatory true when an exception should be thrown if the attribute element does not
     *     exist.
     * @return The double value if the attribute was found, the NaN otherwise.
     * @throws Exception When a attribute element is required and not found.
     */
    public static double getDoubleAttribute(Element elem, String attName, boolean mandatory)
            throws Exception {
        String value = getAttribute(elem, attName, mandatory);

        if ((value == null) || ("".equals(value))) {
            return 0.0;
        }

        double d = Double.NaN;

        if (value != null) {
            try {
                d = Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                throw new ConfigurationException(
                        "Illegal attribute value for "
                                + attName
                                + " in element "
                                + elem.getNodeName()
                                + ". Expected double, but was "
                                + value);
            }
        }

        return d;
    }

    /**
     * Validates an xml document against a specified schema.
     *
     * @param xml The document.
     * @param errorHandler The validation error handler.
     * @param targetNamespace The target namespace of the schema, may be <code>null</code>
     * @param schemaLocation The location of the schema to validate against, may be <code>null
     *     </code>
     * @throws RuntimeException If reader failed to parse properly.
     */
    public static void validate(
            Document xml,
            DefaultHandler errorHandler,
            String targetNamespace,
            String schemaLocation) {
        try {
            Transformer tx = TransformerFactory.newInstance().newTransformer();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            tx.transform(new DOMSource(xml), new StreamResult(output));

            InputStreamReader reader =
                    new InputStreamReader(new ByteArrayInputStream(output.toByteArray()));
            validate(reader, errorHandler, targetNamespace, schemaLocation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates an xml document against a specified schema.
     *
     * @param xml Reader representing xml stream to parse.
     * @param errorHandler The validation error handler.
     * @param targetNamespace The target namespace of the schema, may be <code>null</code>
     * @param schemaLocation The location of the schema to validate against, may be <code>null
     *     </code>
     * @throws RuntimeException If reader failed to parse properly.
     */
    public static void validate(
            Reader xml,
            DefaultHandler errorHandler,
            String targetNamespace,
            String schemaLocation) {
        InputSource in = new InputSource(xml);

        try {

            // TODO: pretty sure this doesn't actually do validation
            // ahhh... xml in java....
            SAXParserFactory sf = SAXParserFactory.newInstance();
            sf.setNamespaceAware(true);
            sf.setValidating(true);
            SAXParser parser = sf.newSAXParser();
            parser.setProperty(
                    "http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                    "http://www.w3.org/2001/XMLSchema");

            //            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            //               parser.setProperty("http://xml.org/sax/features/validation",
            // Boolean.TRUE);
            //
            //                parser.setProperty("http://apache.org/xml/features/validation/schema",
            //                    Boolean.TRUE);
            //
            // parser.setProperty("http://apache.org/xml/features/validation/schema-full-checking",
            //                    Boolean.TRUE);

            if (schemaLocation != null) {
                parser.setProperty(
                        "http://java.sun.com/xml/jaxp/properties/schemaSource", schemaLocation);
                //                        if ( targetNamespace != null ) {
                //
                // parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",
                //                                    targetNamespace + " " + schemaLocation );
                //                        }
            }

            parser.parse(in, errorHandler);

        } catch (Exception e) {
            String msg = "Error reading : " + xml;
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Unescapes the provided text with XML entities, see
     * (http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Character_entities_in_XML)
     */
    private static String unescape(String text) {
        String s = text;
        if (s != null && s.matches(".*&(.*);.*")) {
            s = s.replaceAll("&quot;", "\"");
            s = s.replaceAll("&amp;", "&");
            s = s.replaceAll("&apos;", "'");
            s = s.replaceAll("&lt;", "<");
            s = s.replaceAll("&gt;", ">");
        }
        return s;
    }

    public static List<String> stringToList(String keywords, String delimiter) {
        ////
        //
        // In the following cases we return an empty string:
        // - empty or null keyword
        // - empty or null delimiter
        // -delimiter not found at all
        //
        /////
        if (keywords == null
                || keywords.length() == 0
                || delimiter == null
                || delimiter.length() == 0
                || keywords.indexOf(delimiter) < 0) return Collections.emptyList();

        ////
        //
        // We know that the delimiter is used at least once, let's spli this string and create the
        // corresponding list.
        //
        /////
        final List<String> elements = new ArrayList<String>();
        int index = -1;
        while ((index = keywords.indexOf(delimiter)) >= 0) {
            if (index > 0) elements.add(keywords.substring(0, index));
            keywords = keywords.substring(index);
        }
        if (keywords.length() > 0) elements.add(keywords);
        return elements;
    }
}
