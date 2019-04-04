/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers;

import com.thoughtworks.xstream.XStream;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.cluster.JMSEventHandlerSPI;

/**
 * Handler which is able to handle DocumentFile objects.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class DocumentFileHandlerSPI extends JMSEventHandlerSPI<String, DocumentFile> {

    final XStream xstream;

    public DocumentFileHandlerSPI(final int priority, final XStream xstream) {
        super(priority);
        this.xstream = xstream;
    }

    @Override
    public boolean canHandle(final Object jdom) {
        if (jdom instanceof DocumentFile) return true;
        else return false;
    }

    @Override
    public JMSEventHandler<String, DocumentFile> createHandler() {
        return new DocumentFileHandler(xstream, DocumentFileHandlerSPI.class);
    }
}
