/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin.output;

import org.geoserver.wps.remote.plugin.XMPPClient;

/**
 * Output Type Producer Base Class.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class XMPPOutputDefaultProducer {

    /** */
    private final XMPPOutputVisitor visitor;

    /** */
    public static final XMPPOutputType[] outputProducers = {
        new XMPPTextualOutput(), new XMPPRawDataOutput()
    };

    /** */
    public XMPPOutputDefaultProducer() {
        this.visitor = new XMPPOutputDefaultVisitor();
    }

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
            throws Exception {

        Object wpsOutputValue = null;

        for (XMPPOutputType outputProducer : outputProducers) {
            wpsOutputValue =
                    outputProducer.accept(
                            this.visitor,
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
            if (wpsOutputValue != null) {
                return wpsOutputValue;
            }
        }

        return null;
    }
}
