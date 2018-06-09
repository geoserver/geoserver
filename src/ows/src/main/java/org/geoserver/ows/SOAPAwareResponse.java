/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

/**
 * Interface to be implemented by responses that are aware that they are encoding as the content of
 * a SOAP repsonse.
 *
 * <p>Depending on the type of content it may be encoded differently as part of a SOAP request. An
 * example is xml schema in a DescribeFeatureType. The {@link #getBodyType()} method returns the
 * type of encoding.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface SOAPAwareResponse {

    /**
     * Returns the value of the attribute of the "type" attribute to be included on the "Body"
     * element of the SOAP response.
     */
    String getBodyType();
}
