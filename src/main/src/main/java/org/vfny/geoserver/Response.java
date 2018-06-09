/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.ServiceException;

/**
 * The Response interface serves as a common denominator for all service operations that generates
 * content.
 *
 * <p>The work flow for this kind of objects is divided in two parts: the first is executing a
 * request and the second writing the result to an OuputStream.
 *
 * <ol>
 *   <li>Execute: execute(Request)
 *       <ul>
 *         <li>Executing the request means taking a Request object and, based on it's set of request
 *             parameters, do any heavy processing necessary to produce the response.
 *         <li>Once the execution has been made, the Response object should be ready to send the
 *             response content to an output stream with minimal risk of generating an exception.
 *         <li>Anyway, it is not required, even recomended, that the execution process generates the
 *             response content itself; just that it performs any query or processing that should
 *             generate a trapable error.
 *         <li>Execute may throw a ServiceException if they wish to supply a specific response in
 *             error. As an example the WFSTransaction process has a defined Transaction Document
 *             with provisions for reporting error information.
 *       </ul>
 *   <li>ContentType: getContentType()
 *       <ul>
 *         <li>Called to set the response type. Depending on the stratagy used by AbstractService
 *             the framework may be commited to returning this type.
 *       </ul>
 *   <li>Writing: writeTo(OutputStream)
 *       <ul>
 *         <li>Write the response to the provided output stream.
 *         <li>Any exceptions thrown by this writeTo method may never reach the end user in useable
 *             form. You should assume you are writing directly to the client.
 *       </ul>
 * </ol>
 *
 * <p><b>Note:</b> abort() will be called as part of error handling giving your response subclass a
 * chance to clean up any temporary resources it may have required in execute() for use in
 * writeTo().
 *
 * <p>This is specially usefull for streamed responses such as wfs GetFeature or WMS GetMap, where
 * the execution process can be used to parse parameters, execute queries upon the corresponding
 * data sources and leave things ready to generate a streamed response when the consumer calls
 * writeTo.
 *
 * <p>
 *
 * @author Gabriel Rold?n
 * @version $Id$
 * @deprecated implement {@link org.geoserver.ows.Response} instead
 */
public interface Response {
    /**
     * Excecutes a request. If this method finalizes without throwing an Exception, the Response
     * instance should be ready to write the response content through the writeTo method with the
     * minimal posible risk of failure other than not beeing able to write to the output stream due
     * to external reassons
     *
     * <p>We should clarify when a ServiceException is thrown? I would assume that a "failed"
     * request should still result in a Response that we could write out.
     *
     * @param request a Request object that implementations should cast to it's Request
     *     specialization, wich must contain the parsed and ready to use parameters sent by the
     *     calling client. In general, such a Request will be created by either a KVP or XML request
     *     reader; resulting in a Request object more usefull than a set of raw parameters, as can
     *     be the list of feature types requested as a set of FeatureTypeInfo objects rather than
     *     just a list of String type names
     * @throws ServiceException
     */
    public void execute(Request request) throws ServiceException;

    /**
     * MIME type of this Response - example <code>"text/xml"</code>.
     *
     * <p>thinked to be called after excecute(), this method must return the MIME type of the
     * response content that will be writen when writeTo were called
     *
     * <p>an implementation of this interface is required to throw an IllegalStateException if
     * execute has not been called yet, to indicate that an inconsistence in the work flow that may
     * result in an inconsistence between the response content and the content type declared for it,
     * if such an implementation can return different contents based on the request that has
     * originated it. i.e. a WMS GetMap response will return different content encodings based on
     * the FORMAT requested, so it would be impossible to it knowing the exact MIME response type if
     * it has not processed the request yet.
     *
     * <p>There is some MIME stuff in JDK for reference:
     *
     * <ul>
     *   <li>java.awt.datatransfer package
     *   <li>javax.mail.internet
     *   <li>and a few other places as well.
     * </ul>
     *
     * @return the MIME type of the generated or ready to generate response content
     * @throws IllegalStateException if this method is called and execute has not been called yet
     */
    public String getContentType(GeoServer gs) throws IllegalStateException;

    /**
     * Returns any special content encoding this response will encode its contents to, such as
     * "gzip" or "deflate"
     *
     * @return the content encoding writeTo will encode with, or null if none
     */
    public String getContentEncoding();

    /**
     * Returns any special content disposition this response will encode its contents to, such as
     * "filename" and "attachement"
     *
     * @return the content disposition writeTo will encode with, or null if none
     * @uml.property name="contentDisposition" multiplicity="(0 1)"
     */
    public String getContentDisposition();

    /**
     * Returns any extra headers that this Response might wish to have set in the HTTP response
     * object.
     *
     * <p>In particular, a WMS might wish to have some external caching information added to the
     * HTTP response, so that caches can hang onto this map for a while and ligten the load on
     * geoserver.
     */
    public HashMap<String, String> getResponseHeaders();

    /**
     * Writes this respone to the provided output stream.
     *
     * <p>To implememt streaming, execution is sometimes delayed until the write opperation (for
     * example of this see FeatureResponse). Hopefully this is okay? GR:the idea for minimize risk
     * error at writing time, is that execute performs any needed query/processing, leaving to this
     * method just the risk of encountering an uncaught or IO exception. i.e. FeatureResponse should
     * execute the queries inside the execute method, and have a set of FeatureReader's (or results)
     * ready to be streamed here. This approach fits well with the Chirs' idea of configuring
     * geoserver for speed or full conformance, wich ends in just writing directly to the http
     * response output stream or to a ByteArrayOutputStream JG: Consider using a Writer here? GR: I
     * don't think so, because not all responses will be char sequences, such as an image in a WMS
     * GetImage response.
     *
     * @param out
     * @throws ServiceException wrapping of any unchecked exception or other predictable exception
     *     except an IO error while writing to <code>out</code>
     * @throws IOException ONLY if an error occurs trying to write content to the passed
     *     OutputStream. By this way, we'll can control the very common situation of a
     *     java.net.SocketException product of the client closing the connection (like a user
     *     pressing it's refresh browser button many times)
     */
    public void writeTo(OutputStream out) throws ServiceException, IOException;

    /**
     * Called when things go horriably wrong.
     *
     * <p>Used try and restore application state when things go wrong. This is called by
     * AbstractAction to try and recover when sending out a ServiceException.
     *
     * <p>Allows a Response a chance to clean up after its self when AbstractionAction is error
     * handling.
     */
    public void abort(ServiceInfo gs);
}
