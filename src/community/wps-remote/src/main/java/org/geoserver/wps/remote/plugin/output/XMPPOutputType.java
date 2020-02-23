/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin.output;

import org.geoserver.wps.remote.plugin.XMPPClient;

/**
 * Output Type interface
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public interface XMPPOutputType {

    /** */
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
            throws Exception;

    /** */
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
            throws Exception;
}
