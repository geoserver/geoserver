/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * Response to an operation, which serializes the result of the operation to an output stream.
 *
 * <p>A response must specify the following information:
 *
 * <ul>
 *   <li>The type of object it is capable of serializing, the class is bound to. See {@link
 *       #getBinding()}.
 *   <li>The mime-type of the resulting response. See {@link #getMimeType(Object, Operation)}.
 * </ul>
 *
 * <p>Optionally, a response may declare a well-known name for it. This well known name corresponds
 * to the "outputFormat" parameter which is supported on many types of OWS request.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class Response {

    public static final String DISPOSITION_INLINE = "inline";
    public static final String DISPOSITION_ATTACH = "attachment";

    /** Class of object to serialize */
    final Class<?> binding;

    /** The well known "outputFormat" of the response */
    final Set<String> outputFormats;

    /**
     * Constructor which specified the class this response is bound to.
     *
     * @param binding The class of object the response serializes.
     */
    public Response(Class<?> binding) {
        this(binding, (Set<String>) null);
    }

    /**
     * Constructor which specified the class this response is bound to, and a common name for the
     * type of response.
     *
     * @param binding The class of object the response serializes
     * @param outputFormat A common name for the response.
     */
    public Response(Class<?> binding, String outputFormat) {
        this(binding, outputFormat == null ? null : Collections.singleton(outputFormat));
    }

    /**
     * Constructor which specified the class this response is bound to, and a set of common names
     * for the type of response.
     *
     * @param binding The class of object the response serializes
     * @param outputFormats A set of common names for the response.
     */
    public Response(Class<?> binding, Set<String> outputFormats) {
        if (binding == null) {
            throw new NullPointerException("binding may not be null");
        }

        if (outputFormats == null) {
            outputFormats = Collections.emptySet();
        }

        this.binding = binding;
        this.outputFormats = outputFormats;
    }

    /** @return The type of object the response can handle. */
    public final Class<?> getBinding() {
        return binding;
    }

    /** @return Set of common or well-known name for the response, may be empty. */
    public final Set<String> getOutputFormats() {
        return outputFormats;
    }

    /**
     * Determines if the response can handle the operation being performed.
     *
     * <p>This method is called before {@link #write(Object, OutputStream, Operation)}.
     *
     * <p>Subclasses should override this method to perform additional checks against the operation
     * being performed. Example might be checking the version of the service.
     *
     * @param operation The operation being performed.
     * @return <code>true</code> if the response can handle the operation, otherwise <code>false
     *     </code>
     */
    public boolean canHandle(Operation operation) {
        return true;
    }

    /**
     * Returns the mime type to be uses when writing the response.
     *
     * @param value The value to serialize
     * @param operation The operation being performed.
     * @return The mime type of the response, must not be <code>null</code>
     */
    public abstract String getMimeType(Object value, Operation operation) throws ServiceException;

    /**
     * Returns a 2xn array of Strings, each of which is an HTTP header pair to be set on the HTTP
     * Response. Can return null if there are no headers to be set on the response.
     *
     * @param value The value to serialize
     * @param operation The operation being performed.
     * @return 2xn string array containing string-pairs of HTTP headers/values
     */
    public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
        // default implementation returns null = no headers to set
        return null;
    }

    /**
     * Serializes <code>value</code> to <code>output</code>.
     *
     * <p>The <code>operation</code> bean is provided for context.
     *
     * @param value The value to serialize.
     * @param output The output stream.
     * @param operation The operation which resulted in <code>value</code>
     * @throws IOException Any I/O errors that occur
     * @throws ServiceException Any service errors that occur
     */
    public abstract void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException;

    /**
     * Get the preferred Content-Disposition header for this response. The default is inline.
     * Subclasses can prefer attachment.
     *
     * @param value The value that will be serialized
     * @param operation The operation which resulted in <code>value</code>
     * @return inline or attachment
     */
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_INLINE;
    }

    /**
     * Get a name for a Content-Disposition attachment filename. The mimetype should match the file
     * extension. The default implementation will use the mimetype and operation id to attempt to
     * build a name.
     *
     * @param value The value that will be serialized
     * @param operation The operation being performed
     * @return null or a filename such as result.txt or map.tiff
     */
    public String getAttachmentFileName(Object value, Operation operation) {
        String mimeType = getMimeType(value, operation);
        String opName = operation.getId();
        String name = null;
        if (mimeType != null) {
            name = "geoserver";
            if (opName != null) {
                name = name + "-" + opName;
            }
            String[] typeParts = mimeType.split(";");
            name = name + "." + typeParts[0].split("/")[0];
        }
        return name;
    }

    /**
     * Returns the charset for this response, the Dispatcher will set it in the ServletResponse. The
     * default implementation returns <code>null</code>, in this case no encoding should be set.
     * Subclasses returning text documents (CSV,HTML,JSON) should override taking into account
     * SettingsInfo.getCharset() as well as the specific encoding requirements of the returned
     * format.
     */
    public String getCharset(Operation operation) {
        return null;
    }
}
