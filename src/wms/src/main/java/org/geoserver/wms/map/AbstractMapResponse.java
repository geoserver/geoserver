/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WebMap;
import org.springframework.util.Assert;

/**
 * @author Simone Giannecchini, GeoSolutions
 * @author Gabriel Roldan
 */
public abstract class AbstractMapResponse extends Response {

    protected AbstractMapResponse(
            final Class<? extends WebMap> responseBinding, final String mime) {
        this(responseBinding, new String[] {mime});
    }

    @SuppressWarnings("unchecked")
    protected AbstractMapResponse(
            final Class<? extends WebMap> responseBinding, final String[] outputFormats) {
        this(
                responseBinding,
                outputFormats == null
                        ? Collections.EMPTY_SET
                        : new HashSet<String>(Arrays.asList(outputFormats)));
    }

    protected AbstractMapResponse(
            final Class<? extends WebMap> responseBinding, Set<String> outputFormats) {
        // Call Response superclass constructor with the kind of request we can handle
        // Make sure the output format comparison in canHandle is case insensitive
        super(responseBinding, caseInsensitiveOutputFormats(outputFormats));
    }

    private static Set<String> caseInsensitiveOutputFormats(Set<String> outputFormats) {
        if (outputFormats == null) {
            return Collections.emptySet();
        }
        Set<String> caseInsensitiveFormats = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveFormats.addAll(outputFormats);
        return caseInsensitiveFormats;
    }

    protected AbstractMapResponse() {
        this(null, (String[]) null);
    }

    /**
     * @return {@code ((WebMap)value).getMimeType()}
     * @see org.geoserver.ows.Response#getMimeType(java.lang.Object,
     *     org.geoserver.platform.Operation)
     */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        Assert.isInstanceOf(WebMap.class, value);
        return ((WebMap) value).getMimeType();
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        Assert.isInstanceOf(WebMap.class, value);
        // defer to WebMap - it has the extension and other information
        return ((WebMap) value).getAttachmentFileName();
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        Assert.isInstanceOf(WebMap.class, value);
        // defer to WebMap - it has the extension and other information
        return ((WebMap) value).getDisposition();
    }

    /**
     * Evaluates whether this response can handle the given operation by checking if the operation's
     * request is a {@link GetMapRequest} and the requested output format is contained in {@link
     * #getOutputFormatNames()}.
     *
     * <p>NOTE: requested MIME Types may come with parameters, like, for example: {@code
     * image/png;param1=value1}. This default canHandle implementation performs and exact match
     * check against the requested and supported format names. Subclasses may feel free to override
     * if needed.
     *
     * @see org.geoserver.ows.Response#canHandle(org.geoserver.platform.Operation)
     */
    @Override
    public boolean canHandle(final Operation operation) {
        GetMapRequest request;
        Object[] parameters = operation.getParameters();
        request = (GetMapRequest) OwsUtils.parameter(parameters, GetMapRequest.class);
        if (request == null) {
            return false;
        }
        Set<String> outputFormats = getOutputFormats();
        if (outputFormats.size() == 0) {
            // rely only on response binding
            return true;
        }
        String outputFormat = request.getFormat();
        boolean match = outputFormats.contains(outputFormat);
        return match;
    }

    /**
     * Returns a 2xn array of Strings, each of which is an HTTP header pair to be set on the HTTP
     * Response. Can return null if there are no headers to be set on the response.
     *
     * @param value must be a {@link WebMap}
     * @param operation The operation being performed.
     * @return {@link WebMap#getResponseHeaders()}: 2xn string array containing string-pairs of HTTP
     *     headers/values
     * @see Response#getHeaders(Object, Operation)
     * @see WebMap#getResponseHeaders()
     */
    @Override
    public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
        Assert.isInstanceOf(WebMap.class, value);
        WebMap map = (WebMap) value;
        String[][] responseHeaders = map.getResponseHeaders();
        return responseHeaders;
    }
}
