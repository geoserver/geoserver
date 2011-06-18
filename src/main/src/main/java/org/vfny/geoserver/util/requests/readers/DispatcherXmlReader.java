/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.util.requests.readers;

import java.io.IOException;
import java.io.Reader;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.xerces.parsers.SAXParser;
import org.geoserver.platform.ServiceException;
import org.vfny.geoserver.util.requests.DispatcherHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;


/**
 * This reads in a request and figures out what servlet to dispatch it to.
 * This does not extend XmlRequestReader, since it does not actually create
 * the Request, it is just used to pass on the request to a service that can
 * make the request. The Reader passed in should never be the one directly
 * from the HttpServletRequest, if that is used then the next xml reader to
 * get the request will not work.  A new  BufferedReader should be constructed
 * from the reader of HttpServletRequest, and its mark should be set and then
 * reset after this reader is done with it.  Nothing else seems to work, for
 * some reason.
 *
 * <p>
 * In an ideal, refactored world we would implement our handlers better, and
 * the xml reader could dynamically figure out which handler to pass it to.
 * But we have no time for that now, so we'll just live with this.
 * </p>
 *
 * @author Chris Holmes
 * @version $Id$
 *
 * @task REVISIT: This might be better implemented to extend XmlRequestReader,
 *       and to actually construct the requests with the DispatcherHandler.
 *       This way it could really handle WMS dispatching as well, since it
 *       could figure out the service as  well as the request.  The
 *       getRequestType would be complemented by getServiceType, and each
 *       would just extract the information from the Request that was made.
 *       But right now we don't have much in the way of
 */
public class DispatcherXmlReader {
    /** Class logger */
    private static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.requests.readers");

    /** Handler for request interpretation duties. */
    private DispatcherHandler currentRequest;

    public DispatcherXmlReader() {
    }

    /**
     * Constructor with raw request string.  Calls parent.
     *
     * @param reader A reader of the request from the http client.
     * @param req The actual request made.
     *
     * @throws ServiceException DOCUMENT ME!
     */
    public void read(Reader reader, HttpServletRequest req)
        throws ServiceException {
        //InputSource requestSource = new InputSource((Reader) tempReader);
        InputSource requestSource = new InputSource(reader);

        // instantiante parsers and content handlers
        XMLReader parser = new SAXParser();
        this.currentRequest = new DispatcherHandler();

        // read in XML file and parse to content handler
        try {
            parser.setContentHandler(currentRequest);
            parser.parse(requestSource);
        } catch (SAXException e) {
            //SAXException does not sets initCause(). Instead, it holds its own "exception" field.
            if(e.getException() != null && e.getCause() == null){
                e.initCause(e.getException());
            }
            throw new ServiceException(e, "XML request parsing error",
                DispatcherXmlReader.class.getName());
        } catch (IOException e) {
            throw new ServiceException(e, "XML request input error",
                DispatcherXmlReader.class.getName());
        }
    }

    /**
     * @return The service, WFS,WMS,WCS,etc...
     */
    public String getService() {
        return currentRequest.getService();
    }

    /**
     * @return The request, GetCapabilities,GetMap,etc...
     */
    public String getRequest() {
        LOGGER.info("getting request type from " + currentRequest);

        return currentRequest.getRequest();
    }

    //JD: kill these
    //    /**
    //     * Returns the guessed request type..
    //     *
    //     * @return Request type.
    //     */
    //    public int getRequestType() {
    //        
    //
    //        return currentRequest.getRequestType();
    //    }
    //
    //    public int getServiceType() {
    //        return currentRequest.getServiceType();
    //    }
}
