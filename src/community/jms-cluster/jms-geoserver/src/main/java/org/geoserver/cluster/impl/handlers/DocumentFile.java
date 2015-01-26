/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jdom.JDOMException;

/**
 * 
 * Class used to handle a text file
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class DocumentFile {

    private final File path;

    private final String body;

    /**
     * @return the path
     */
    public final File getPath() {
        return path;
    }

    /**
     * @return the body containing the parsed file
     */
    public final String getBody() {
        return body;
    }

    /**
     * Constructor
     * 
     * @param path the path referring to this file
     * @param document the string containing the body of the file (should be a valid JDOM document)
     * @throws JDOMException
     * @throws IOException
     */
    public DocumentFile(File path, final String document) throws JDOMException, IOException {
        if (!path.exists()) {
            throw new IllegalArgumentException("Unable to locate the file path: \'" + path + "\'");
        }
        this.path = path;
        this.body = document;
    }

    public DocumentFile(File path) throws JDOMException, IOException {
        if (!path.exists()) {
            throw new IllegalArgumentException("Unable to locate the file path: \'" + path + "\'");
        }
        this.path = path;
        this.body = FileUtils.readFileToString(path);
    }

    /**
     * write the body to the passed file argument
     * 
     * @param file
     * @throws JDOMException
     * @throws IOException
     */
    public void writeTo(File file) throws JDOMException, IOException {
        FileUtils.writeStringToFile(file, body);
    }
}
