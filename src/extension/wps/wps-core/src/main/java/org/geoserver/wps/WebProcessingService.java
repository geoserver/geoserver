/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.opengis.wps10.DescribeProcessType;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.GetCapabilitiesType;
import net.opengis.wps10.ProcessDescriptionsType;
import net.opengis.wps10.WPSCapabilitiesType;

/**
 * @author Lucas Reed, Refractions Research Inc
 * @author Justin Deoliveira, OpenGEO
 */
public interface WebProcessingService {
    /**
     * Generates a XML object for the return of the getCapabilities request
     *
     */
    WPSCapabilitiesType getCapabilities(GetCapabilitiesType request) throws WPSException;

    /**
     * Generates a XML object for  the return of the describeProcess request
     */
    ProcessDescriptionsType describeProcess(DescribeProcessType request) throws WPSException;

    /**
     * Executes a execute request and writes output to the Servlet response
     */
    ExecuteResponseType execute(ExecuteType reques) throws WPSException;

    /**
     * Executes a get schema request and writes the output to the Servlet response
     *
     * @param request
     * @param response
     * @throws WPSException
     */
    void getSchema(HttpServletRequest request, HttpServletResponse response) throws WPSException;
}
