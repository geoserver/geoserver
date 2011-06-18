/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.HashMap;

import org.geoserver.ows.Response;
import org.geotools.map.MapLayer;

public abstract class WebMap {

    private String mimeType;

    private java.util.Map<String, String> responseHeaders;

    protected final WMSMapContext mapContext;

    /**
     * @param context
     *            the map context, can be {@code null} is there's _really_ no context around
     */
    public WebMap(final WMSMapContext context) {
        this.mapContext = context;
    }

    /**
     * Disposes any resource held by this Map.
     * <p>
     * This method is meant to be called right after the map is no longer needed. That generally
     * happens at the end of a {@link Response#write} operation, and is meant to free any resource
     * the map implementation might be holding, specially if it contains a refrerence to
     * {@link WMSMapContext}, in which case it's mandatory that the map context's
     * {@link WMSMapContext#dispose()} method is called.
     * </p>
     */
    public final void dispose() {
        if (mapContext != null) {
            mapContext.dispose();
        }
        disposeInternal();
    }

    /**
     * Hook for Map concrete subclasses to dispose any other resource than the {@link WMSMapContext}
     */
    protected void disposeInternal() {
        // default implementation does nothing
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public void setResponseHeader(final String name, final String value) {
        if (responseHeaders == null) {
            responseHeaders = new HashMap<String, String>();
        }
        responseHeaders.put(name, value);
    }

    public String[][] getResponseHeaders() {
        if (responseHeaders == null || responseHeaders.size() == 0) {
            return null;
        }
        String[][] headers = new String[responseHeaders.size()][2];
        int index = 0;
        for (java.util.Map.Entry<String, String> entry : responseHeaders.entrySet()) {
            headers[index][0] = entry.getKey();
            headers[index][1] = entry.getValue();
            index++;
        }
        return headers;
    }

    /**
     * Utility method to build a standard content disposition header.
     * <p>
     * It will concatenate the titles of the various layers in the map context, or generate
     * "geoserver" instead (in the event no layer title is set).
     * </p>
     * <p>
     * The file name will be followed by the extension provided, for example, to generate layer.pdf
     * extension will be ".pdf"
     * </p>
     */
    public void setContentDispositionHeader(final WMSMapContext mapContext, final String extension) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mapContext.getLayerCount(); i++) {
            MapLayer layer = mapContext.getLayer(i);
            String title = layer.getTitle();
            if (title != null && !title.equals("")) {
                sb.append(title).append("_");
            }
        }
        String value;
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
            value = "attachment; filename=" + sb.toString() + extension;
        } else {
            value = "attachment; filename=geoserver" + extension;
        }
        setResponseHeader("Content-Disposition", value);
    }
}
