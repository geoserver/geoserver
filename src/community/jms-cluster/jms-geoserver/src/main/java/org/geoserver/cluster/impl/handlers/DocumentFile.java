/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.jdom.JDOMException;

/**
 * 
 * Class used to handle a text file
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 *
 */
public class DocumentFile {

    private final Resource path;

    private final String body;

    /**
     * @return the path
     */
    public final Resource getPath() {
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
    public DocumentFile(Resource path, final String document) throws JDOMException, IOException {
        if (!Resources.exists(path)) {
            throw new IllegalArgumentException("Unable to locate the file path: \'" + path + "\'");
        }
        this.path = path;
        this.body = document;
    }

    public DocumentFile(Resource path) throws JDOMException, IOException {
        if (!Resources.exists(path)) {
            throw new IllegalArgumentException("Unable to locate the file path: \'" + path + "\'");
        }
        this.path = path;
        InputStream in = path.in();
        try {
            this.body = IOUtils.toString(in);
        } finally {
            in.close();
        }
    }

    /**
     * write the body to the passed file argument
     * 
     * @param file
     * @throws JDOMException
     * @throws IOException
     */
    public void writeTo(Resource file) throws JDOMException, IOException {
        OutputStream out = path.out();
        try {
            IOUtils.write(body, out);
        } finally {
            out.close();
        }
    }
}
