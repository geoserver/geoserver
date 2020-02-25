/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin.output;

import org.geoserver.wps.remote.plugin.XMPPClient;

/**
 * Visitor Pattern interface for Output types
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public interface XMPPOutputVisitor {

    /** */
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
            throws Exception;

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
            throws Exception;
}
