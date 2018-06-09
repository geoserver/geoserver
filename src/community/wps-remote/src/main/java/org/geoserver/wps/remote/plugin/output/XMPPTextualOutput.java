/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin.output;

import java.util.logging.Logger;
import org.geoserver.wps.remote.plugin.XMPPClient;
import org.geotools.util.logging.Logging;

/**
 * Actual implementation of a STRING Output Type
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class XMPPTextualOutput implements XMPPOutputType {

    /** The LOGGER */
    public static final Logger LOGGER =
            Logging.getLogger(XMPPTextualOutput.class.getPackage().getName());

    @Override
    public Object accept(
            XMPPOutputVisitor visitor,
            Object value,
            String type,
            String pID,
            String baseURL,
            XMPPClient xmppClient,
            boolean publish,
            String name,
            String title,
            String description,
            String defaultStyle,
            String targetWorkspace,
            String metadata)
            throws Exception {
        return visitor.visit(
                this,
                value,
                type,
                pID,
                baseURL,
                xmppClient,
                publish,
                name,
                title,
                description,
                defaultStyle,
                targetWorkspace,
                metadata);
    }

    @Override
    public Object produceOutput(
            Object value,
            String type,
            String pID,
            String baseURL,
            XMPPClient xmppClient,
            boolean publish,
            String name,
            String title,
            String description,
            String defaultStyle,
            String targetWorkspace,
            String metadata)
            throws Exception {
        // Do nothing. The output cannot published neither.
        LOGGER.fine("Do nothing. The output cannot published neither.");

        return value;
    }
}
