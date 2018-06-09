/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers;

import com.thoughtworks.xstream.XStream;
import java.io.OutputStream;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.platform.resource.Resources;

/**
 * XML file handler:<br>
 * This class can be used to handle small XML files using JDOM
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class DocumentFileHandler extends JMSEventHandler<String, DocumentFile> {
    public DocumentFileHandler(XStream xstream, Class clazz) {
        super(xstream, clazz);
    }

    @Override
    public boolean synchronize(DocumentFile event) throws Exception {
        try (OutputStream fout = Resources.fromPath(event.getResourcePath()).out()) {
            xstream.toXML(event.getBody(), fout);
            return true;
        } catch (IllegalStateException e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                LOGGER.severe(e.getLocalizedMessage());
            throw e;
        }
    }

    @Override
    public String serialize(DocumentFile o) throws Exception {
        return xstream.toXML(o);
    }

    @Override
    public DocumentFile deserialize(String o) throws Exception {
        return (DocumentFile) xstream.fromXML(o);
    }
}
