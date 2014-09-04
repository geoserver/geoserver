/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.restlet.data.MediaType;

/**
 * A format that automatically converts a map into an XML document and vice versa.
 * <p>
 * The resulting XML document contains elements whose names match the keys of the map.
 * The contents of each element are mapped from the map values as follows: 
 * <ul>
 *   <li> A text value for regular "primitive" objects</li>
 *   <li> A sub document structure for maps </li>
 *   <li> A collection of &lt;entry> elements for a collection </li>
 * <ul>
 * </p>
 * <p>
 * The original map can be reconstructed from the XML document. However due to the use
 * of the element name "entry" to indicate a collection, it is not recommended that any 
 * keys of the map use this string.
 * </p>
 * 
 * @author David Winslow <dwinslow@openplans.org>
 */
public class MapXMLFormat extends StreamDataFormat {
    /** the name of the root element of the XML document */
    String myRootName;

    /**
     * Creates a new format that names the root element of the resulting document "root".
     */
    public MapXMLFormat(){
        this("root");
    }
    
    /**
     * Creates a new format specifying the name of the root element of the resulting XML document.
     */
    public MapXMLFormat(String s){
        super( MediaType.APPLICATION_XML );
        myRootName = s;
    }

    @Override
    protected void write(Object object, OutputStream out) throws IOException {
        Element root = new Element(myRootName);
        final Document doc = new Document(root);
        insert(root, object);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(doc, out);
    }
    
    /**
     * Generate the JDOM element needed to represent an object and insert it into the parent element given.
     * @todo This method is recursive and could cause stack overflow errors for large input maps.
     *
     * @param elem the parent Element into which to insert the created JDOM element
     * @param o the Object to be converted
     */
    private final void insert(Element elem, Object o){
        if (o instanceof Map){
            Iterator it = ((Map)o).entrySet().iterator();
            while (it.hasNext()){
                Map.Entry entry = (Map.Entry)it.next();
                Element newElem = new Element(entry.getKey().toString());
                insert(newElem, entry.getValue());
                elem.addContent(newElem);
            }
        } else if (o instanceof Collection) {
            Iterator it = ((Collection)o).iterator();
            while (it.hasNext()){
                Element newElem = new Element("entry");
                Object entry = it.next();
                insert(newElem, entry);
                elem.addContent(newElem); 
            }
        } else {
            elem.addContent(o == null ? "" : o.toString());
        }        
    }

    @Override
    protected Object read(InputStream in) throws IOException {
        Object result = null;
        SAXBuilder builder = new SAXBuilder();
        Document doc;
        try {
            doc = builder.build(in);
        } 
        catch (JDOMException e) {
            throw (IOException) new IOException("Error building document").initCause( e );
        }
        
        Element elem = doc.getRootElement();
        result = convert(elem);
        return result;
    }
    
    /**
     * Interpret XML and convert it back to a Java collection.
     *
     * @param elem a JDOM element
     * @return the Object produced by interpreting the XML
     */
    private Object convert(Element elem){
        List children = elem.getChildren();
        if (children.size() == 0){
            if (elem.getContent().size() == 0){
                return null;
            } else {
                return elem.getText();
            }
        } else if (children.get(0) instanceof Element){
            Element child = (Element)children.get(0);
            if (child.getName().equals("entry")){
                List l = new ArrayList();
                Iterator it = elem.getChildren("entry").iterator();
                while(it.hasNext()){
                    Element curr = (Element)it.next();
                    l.add(convert(curr));
                }
                return l;
            } else {
                Map m = new HashMap();
                Iterator it = children.iterator();
                while (it.hasNext()){
                    Element curr = (Element)it.next();
                    m.put(curr.getName(), convert(curr));
                }
                return m;
            }
        }
        throw new RuntimeException("Unable to parse XML");
    }
}
