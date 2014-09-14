/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.HashMap;
import java.util.Map;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class FooMapResource extends MapResource {

    Map posted;
    Map puted;
    
    public FooMapResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    public Map getMap() throws RestletException {
        HashMap map = new HashMap();
        map.put( "prop1", "one");
        map.put( "prop2", 2 );
        map.put( "prop3", 3.0 );
        
        return map;
    }
    
    @Override
    protected void postMap(Map map) {
        posted = map;
    }
    
    @Override
    protected void putMap(Map map) {
        puted = map;
    }
}
