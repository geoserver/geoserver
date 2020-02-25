/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A provider for wcs:Metadata contents as found in the DescribeCoverage response
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface WCS20CoverageMetadataProvider {

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
     * Encodes the extended coverage metadata
     *
     * @param tx the translator used to encode the extended capabilities to
     * @param context the encoding context, either a {@link GridCoverage2DReader} or a {@link
     *     GridCoverage2D} depending on what is available on the caller side
     */
    void encode(Translator tx, Object context) throws IOException;

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
