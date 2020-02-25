/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin.output;

import org.geoserver.wps.remote.plugin.XMPPClient;

/**
 * Default Output Type visitor pattern implementation.
 *
 * <p>By default we can have RAW DATA or STRING Output Types.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class XMPPOutputDefaultVisitor implements XMPPOutputVisitor {

    @Override
    public Object visit(
            XMPPTextualOutput visitor,
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
        if (type.equals("textual")) {
            return visitor.produceOutput(
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

        return null;
    }

    @Override
    public Object visit(
            XMPPRawDataOutput visitor,
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
        if (value != null && value instanceof String && !((String) value).isEmpty()) {
            return visitor.produceOutput(
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

        return null;
    }
}
