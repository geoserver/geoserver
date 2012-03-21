/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import org.geoserver.ows.Dispatcher;

/**
 * A simple bean acting as the response for the {@link GwcServiceProxy#dispatch} operation, in order
 * to make a call to a GWC service fit into the GeoServer {@link Dispatcher} framework.
 * <p>
 * See the package documentation for more insights on how these all fit together.
 * 
 */
public class GwcOperationProxy {

    private String contentType;

    private byte[] responseContent;

    public GwcOperationProxy(final String contentType, final byte[] responseContent)
            throws Exception {
        this.contentType = contentType;
        this.responseContent = responseContent;

    }

    public String getMimeType() {
        return contentType;
    }

    public byte[] getContents() {
        return responseContent;
    }
}
