/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import java.io.IOException;
import org.geoserver.config.ServiceInfo;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Extension point that allows plugins to dynamically contribute extended properties to the OWS
 * capabilities documents. Each extension point will have an interface that is a subinterface of
 * this interface (like WFSExtendedCapabilitiesProvider)
 *
 * @author Jesse Eichar, Camptocamp
 */
public interface ExtendedCapabilitiesProvider<Info extends ServiceInfo, Request> {

    /**
     * Returns the locations of any references schema for the extended capabilities.
     *
     * <p>The returned String array must consist of namespace,location pairs in which the namespace
     * is the full namespace uri of the schema, and location is the url to where the schema
     * definition is located.
     *
     * <p>The location may be specified as a canonical external url. For example
     * <tt>http://schemas.opengis.net/foo/foo.xsd</tt>. Or if the schema is bundled within the
     * server the location can be a relative path such as <tt>foo/foo.xsd</tt>. In the latter case
     * the path will be appended to the base url from which the capabilities document is being
     * requested from.
     */
    String[] getSchemaLocations(String schemaBaseURL);

    /**
     * Registers the xmlns namespace prefix:uri mappings for any elements used by the extended
     * capabilities.
     */
    void registerNamespaces(NamespaceSupport namespaces);

    /**
     * Encodes the extended capabilities.
     *
     * @param tx the translator used to encode the extended capabilities to
     * @param wfs WFS service metadata
     * @param request the originating request, may be useful for the provider to decide whether or
     *     not, or how, to contribute to the capabilities document
     */
    void encode(Translator tx, Info wfs, Request request) throws IOException;

    /** Interface for clients to encode XML. */
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

        /** Ends an element creating a closing tag. */
        void end(String element);
    }
}
