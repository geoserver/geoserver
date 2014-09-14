/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class FooReflectiveResource extends ReflectiveResource {

    public Foo posted;
    public Foo puted;
    
    public FooReflectiveResource(Context context, Request request, Response response) {
        super(context, request, response);
    }

    @Override
    protected Object handleObjectGet() {
        return new Foo( "one", 2, 3.0 );
    }

    @Override
    protected String handleObjectPost(Object object) {
        posted = (Foo) object;
        return posted.prop1;
    }

    @Override
    protected void handleObjectPut(Object object) {
        puted = (Foo) object;
    }

}
