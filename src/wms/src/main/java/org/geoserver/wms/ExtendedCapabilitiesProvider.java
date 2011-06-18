/* Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Extension point that allows plugins to dynamically contribute extended properties
 * to the WMS capabilities document.
 *  
 * @author Justin Deoliveira, OpenGeo
 *
 */
public interface ExtendedCapabilitiesProvider {

    /**
     * Returns the locations of any references schema for the extended capabilities.
     * <p>
     * The returned String array must consist of namespace,location pairs in which the namespace
     * is the full namespace uri of the schema, and location is the url to where the schema defintion
     * is located.
     * </p>
     * <p>
     * The location may be specified as a canonical external url. For example 
     * <tt>http://schemas.opengis.net/foo/foo.xsd</tt>. Or if the schema is bundled within the 
     * server the location can be a relative path such as <tt>foo/foo.xsd</tt>. In the latter
     * case the path will be appended to the base url from which the capabilities document is being
     * requested from.
     * </p>
     * @param schemaBaseURL 
     */
    String[] getSchemaLocations(String schemaBaseURL);

    /**
     * Returns the element names that are direct children of {@code VendorSpecificCapabilities}
     * contributed by this extended capabilities provider for WMS 1.1.1 DOCTYPE declaration.
     * <p>
     * This method returns only the element names that are direct children of
     * VendorSpecificCapabilities so that they can be aggregated in a single declaration like
     * {@code <!ELEMENT VendorSpecificCapabilities (ContributedElementOne*, ContributedElementTwo*) >}
     * . Implement {@link #getVendorSpecificCapabilitiesChildDecls()} to contribute the child
     * elements of these root ones.
     * </p>
     * 
     * @return the name of the elements to be declared as direct children of
     *         VendorSpecificCapabilities in a WMS 1.1.1 DOCTYPE internal DTD.
     */
    List<String> getVendorSpecificCapabilitiesRoots(GetCapabilitiesRequest request);
    
    /**
     * Returns the list of internal DTD element declarations contributed to WMS 1.1.1 DOCTYPE
     * GetCapabilities document.
     * <p>
     * Example DTD element declaration that could be a memeber of the returned list: "
     * {@code <!ELEMENT Resolutions (#PCDATA) >}"
     * </p>
     * 
     * @return the list of GetCapabilities internal DTD elements declarations, may be empty.
     */
    List<String> getVendorSpecificCapabilitiesChildDecls(GetCapabilitiesRequest request);
    
    /**
     * Registers the xmlns namespace prefix:uri mappings for any elements used by 
     * the extended capabilities. 
     */
    void registerNamespaces(NamespaceSupport namespaces);
    
    /**
     * Encodes the extended capabilities.
     * 
     * @param tx
     *            the translator used to encode the extended capabilities to
     * @param wms
     *            WMS service metadata
     * @param request
     *            the originating request, may be useful for the provider to decide whether or not,
     *            or how, to contribute to the capabilities document
     */
    void encode(Translator tx, WMSInfo wms, GetCapabilitiesRequest request) throws IOException;
    
    /**
     * Interface for clients to encode XML.
     */
    public interface Translator {
        
        /**
         * Starts an element creating the opening tag.
         * 
         * @param element The name of the element.
         */
        void start(String element);
        
        /**
         * Starts an element with attributes, creating the opening tag.
         * 
         * @param element The name of the element.
         * @param attributes The attributes of the element.
         */
        void start(String element, Attributes attributes);
        
        /**
         * Creates a text node within an element.
         * 
         * @param text The character text.
         */
        void chars(String text);
        
        /**
         * Ends an element creating a closing tag.
         * @param element
         */
        void end(String element);
    }

}
