/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.featurestemplating.writers;

import java.io.Closeable;
import java.io.IOException;

/** Base interface for all the writers */
public interface TemplateOutputWriter extends Closeable {

    /**
     * Write an element name. An element name may be the field name in a JSON or an element tag name
     * in case of a gml output.
     *
     * @param elementName an object representing the element name
     * @throws IOException
     */
    void writeElementName(Object elementName) throws IOException;

    /**
     * Write an element value. An element value may be i.e. a field value in a JSON or an element
     * content in a gml output.
     *
     * @param elementValue an object representing the element value
     * @throws IOException
     */
    void writeElementValue(Object elementValue) throws IOException;

    /**
     * Write a static content, no matter how nested.
     *
     * @param name the name of the element
     * @param staticContent an object representing some static content
     * @throws IOException
     */
    void writeStaticContent(String name, Object staticContent) throws IOException;

    /**
     * Write the start of an object
     *
     * @throws IOException
     */
    void startObject() throws IOException;

    /**
     * Write the end of an object
     *
     * @throws IOException
     */
    void endObject() throws IOException;

    /**
     * Write the start of an array
     *
     * @throws IOException
     */
    void startArray() throws IOException;

    /**
     * Write the end of an array
     *
     * @throws IOException
     */
    void endArray() throws IOException;

    void startTemplateOutput() throws IOException;

    void endTemplateOutput() throws IOException;

    @Override
    void close() throws IOException;
}
