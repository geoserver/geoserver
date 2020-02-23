/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.HashMap;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geotools.map.Layer;

public abstract class WebMap {

    private String mimeType;

    private java.util.Map<String, String> responseHeaders;

    protected final WMSMapContent mapContent;

    private String extension;

    private String disposition;

    /** @param context the map context, can be {@code null} is there's _really_ no context around */
    public WebMap(final WMSMapContent context) {
        this.mapContent = context;
    }

    /**
     * Disposes any resource held by this Map.
     *
     * <p>This method is meant to be called right after the map is no longer needed. That generally
     * happens at the end of a {@link Response#write} operation, and is meant to free any resource
     * the map implementation might be holding, specially if it contains a refrerence to {@link
     * WMSMapContent}, in which case it's mandatory that the map context's {@link
     * WMSMapContent#dispose()} method is called.
     */
    public final void dispose() {
        if (mapContent != null) {
            mapContent.dispose();
        }
        disposeInternal();
    }

    /**
     * Hook for Map concrete subclasses to dispose any other resource than the {@link WMSMapContent}
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
     *
     * <p>It will concatenate the titles of the various layers in the map context, or generate
     * "geoserver" instead (in the event no layer title is set).
     *
     * <p>The file name will be followed by the extension provided, for example, to generate
     * layer.pdf extension will be ".pdf"
     */
    public void setContentDispositionHeader(
            final WMSMapContent mapContent, final String extension) {
        setContentDispositionHeader(mapContent, extension, true);
    }

    /**
     * Utility method to build a standard content disposition header.
     *
     * <p>It will concatenate the titles of the various layers in the map context, or generate
     * "geoserver" instead (in the event no layer title is set).
     *
     * <p>The file name will be followed by the extension provided, for example, to generate
     * layer.pdf extension will be ".pdf"
     */
    public void setContentDispositionHeader(
            final WMSMapContent mapContent, final String extension, boolean attachment) {
        // ischneider - this is nasty, but backwards compatible
        this.extension = extension;
        this.disposition = attachment ? Response.DISPOSITION_ATTACH : Response.DISPOSITION_INLINE;
    }

    public String getDisposition() {
        return disposition;
    }

    public String getAttachmentFileName() {
        String filename = getSimpleAttachmentFileName();
        if (filename != null && extension != null) {
            return filename + extension;
        }
        return filename;
    }

    /** Returns the filename with no extension */
    public String getSimpleAttachmentFileName() {
        // see if we can get the original request, before the group expansion happened
        Request request = Dispatcher.REQUEST.get();
        String filename = null;
        if (request != null
                && request.getRawKvp() != null
                && request.getRawKvp().get("LAYERS") != null) {
            String layers = ((String) request.getRawKvp().get("LAYERS")).trim();
            if (layers.length() > 0) {
                filename = layers.replace(",", "_");
            }
        }
        if (filename == null && mapContent != null) {
            StringBuffer sb = new StringBuffer();
            for (Layer layer : mapContent.layers()) {
                String title = layer.getTitle();
                if (title != null && !title.equals("")) {
                    sb.append(title).append("_");
                }
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
                filename = sb.toString();
            }
        }
        if (filename != null) {
            filename = filename.replace(":", "-");
        }
        return filename;
    }
}
