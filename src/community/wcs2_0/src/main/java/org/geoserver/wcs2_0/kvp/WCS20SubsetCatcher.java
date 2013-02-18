/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wcs2_0.kvp;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import net.opengis.wcs20.DimensionSubsetType;
import net.opengis.wcs20.GetCoverageType;

import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.wcs2_0.WCS20Const;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;

/**
 * This {@link DispatcherCallback} is responsible for parsing the subset KVP for WCS 2.0.1 since
 * the current implementation of the KVP dispatcher is not able to do get multiple elements.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public final class WCS20SubsetCatcher extends AbstractDispatcherCallback implements DispatcherCallback {

    private final static Logger LOGGER= Logging.getLogger(WCS20SubsetCatcher.class);
    
    private final SubsetKvpParser subsetKVPParser;
       
    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        final String operationID = operation.getId();
        
        // we need to look for the GetCoverage operation of WCS 2.0.1
        if(operationID!=null&&operationID.equals("GetCoverage")){
            // ok is this WCS 2.0.1?
            final Service service= operation.getService();
            final Version version = service.getVersion();
            if(version!=null&&version.toString().equalsIgnoreCase(WCS20Const.V201)){
                if(LOGGER.isLoggable(Level.FINE)){
                    LOGGER.fine("MultipleKeyKVPCatcher in action");
                }
                
                // get the underlying object to manipulate
                Object[] params = operation.getParameters();
                if(params!=null){
                    // search for the GetCoverageType param
                    for(Object p:params){
                        if(p instanceof GetCoverageType){
                            
                            // get the HTTP KVP params and parse again the subset ones
                            HttpServletRequest httpRequest = request.getHttpRequest();
                            if(httpRequest!=null){
                                
                                // split the query string 
                                final String queryString = httpRequest.getQueryString();
                                if(queryString!=null&&queryString.length()>0){
                                    final String[] elements = queryString.split("&");
                                    if(elements!=null&&elements.length>0){
                                        // get the param
                                        GetCoverageType originalGetCoverageType=(GetCoverageType)p;
                                        if(originalGetCoverageType.getDimensionSubset().size()>0){
                                            originalGetCoverageType.getDimensionSubset().clear();
                                        }
                                        //look for subset elements
                                        for(String element:elements){
                                            // preliminar check
                                            if(element==null){
                                                continue;
                                            }
                                            // look for the KVPs we need
                                            final String kvp[]=element.split("=");
                                            if(kvp!=null&&kvp.length ==2){
                                                if(kvp[0]!=null&&kvp[0].equalsIgnoreCase("subset")&&kvp[1]!=null){
                                                    try {
                                                        final Object parsedElement=subsetKVPParser.parse(kvp[1]);
                                                        
                                                        // handle dimension subsets
                                                        if (parsedElement instanceof DimensionSubsetType) {
                                                            originalGetCoverageType.getDimensionSubset().add((DimensionSubsetType) parsedElement);
                                                        } else {
                                                            throw new RuntimeException("Unable to parse the subset element: "+kvp[1]);
                                                        }
                                                    } catch (Exception e) {
                                                        throw new RuntimeException("Unable to parse the subset element: "+kvp[1],e);//TODO this does not allow me to effectively launch exceptions
                                                    }
                                                    
                                                }
                                            }                                    
                                        }
                                    }
                                }
                            }
                            
                        }
                    }
                }
            }
            
        }
        
        // do nothing
        return super.operationDispatched(request, operation);
    }

    /**
     * Default constructor.
     */
    public WCS20SubsetCatcher(SubsetKvpParser subsetKVPParser) {
        this.subsetKVPParser = subsetKVPParser;
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("MultipleKeyKVPCatcher created!");
        }
    }

}
