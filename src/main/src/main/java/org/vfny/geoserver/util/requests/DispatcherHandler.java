/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util.requests;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Uses SAX to extact a GetFeature query from and incoming GetFeature request XML stream.
 *
 * <p>Note that this Handler extension ignores Filters completely and must be chained as a parent to
 * the PredicateFilter method in order to recognize them. If it is not chained, it will still
 * generate valid queries, but with no filtering whatsoever.
 *
 * @author Chris Holmes, TOPP
 * @version $Id$
 */
public class DispatcherHandler extends XMLFilterImpl implements ContentHandler {

    /** Stores the internal request type as string */
    private String request = null;

    /** Stores hte internal service type as string */
    private String service = null;

    /** Flags whether or not type has been set */
    private boolean gotType = false;

    /** @return the service type. */
    public String getService() {
        return service;
    }

    /** @return The request type. */
    public String getRequest() {
        return request;
    }

    // JD: kill these methods
    /**
     * Gets the request type. See Dispatcher for the available types.
     *
     * @return an int of the request type.
     */

    //    public int getRequestType() {
    //        return requestType;
    //    }

    /**
     * Gets the service type, for now either WMS or WFS types of Dispatcher.
     *
     * @return an int of the service type.
     */

    //    public int getServiceType() {
    //        return serviceType;
    //    }

    /**
     * Notes the start of the element and checks for request type.
     *
     * @param namespaceURI URI for namespace appended to element.
     * @param localName Local name of element.
     * @param rawName Raw name of element.
     * @param atts Element attributes.
     */
    public void startElement(String namespaceURI, String localName, String rawName, Attributes atts)
            throws SAXException {
        if (gotType) {
            return;
        }

        this.request = localName;

        // JD: kill this
        //            if (localName.equals("GetCapabilities")) {
        //                this.requestType = Dispatcher.GET_CAPABILITIES_REQUEST;
        //            } else if (localName.equals("DescribeFeatureType")) {
        //                this.requestType = Dispatcher.DESCRIBE_FEATURE_TYPE_REQUEST;
        //            } else if (localName.equals("GetFeature")) {
        //                this.requestType = Dispatcher.GET_FEATURE_REQUEST;
        //            } else if (localName.equals("Transaction")) {
        //                this.requestType = Dispatcher.TRANSACTION_REQUEST;
        //            } else if (localName.equals("GetFeatureWithLock")) {
        //                this.requestType = Dispatcher.GET_FEATURE_LOCK_REQUEST;
        //            } else if (localName.equals("LockFeature")) {
        //                this.requestType = Dispatcher.LOCK_REQUEST;
        //            } else if (localName.equals("GetMap")) {
        //                this.requestType = Dispatcher.GET_MAP_REQUEST;
        //            } else if (localName.equals("GetFeatureInfo")) {
        //                this.requestType = Dispatcher.GET_FEATURE_INFO_REQUEST;
        //            } else {
        //                this.requestType = Dispatcher.UNKNOWN;
        //            }
        for (int i = 0, n = atts.getLength(); i < n; i++) {
            if (atts.getLocalName(i).equals("service")) {
                this.service = atts.getValue(i);

                // JD: kill this
                //                if (service.equals("WFS")) {
                //                    this.serviceType = Dispatcher.WFS_SERVICE;
                //                } else if (service.equals("WMS")) {
                //                    this.serviceType = Dispatcher.WMS_SERVICE;
                //                }
                //            } else {
                //                this.serviceType = Dispatcher.UNKNOWN;
            }
        }

        gotType = true;
    }
}
