/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.rest;

import org.restlet.Restlet;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.springframework.context.ApplicationContext;

/**
 * A place holder restlet which delegates to another restlet which is 
 * defined in a spring context.
 * 
 * @author David Winslow, OpenGEO
 *
 */
public class BeanDelegatingRestlet extends Restlet{
    /**
     * name of the bean to lookup.
     */
    String beanName;
    /**
     * the application context.
     */
    ApplicationContext context;


    public BeanDelegatingRestlet(ApplicationContext context, String beanName){
       this.context = context;
       this.beanName = beanName;
    }

    public void handle(Request req, Response res){
        Restlet restlet = (Restlet)getBean();
        restlet.handle(req, res);
    }
    
    public Restlet getBean() {
        return (Restlet)context.getBean(beanName);
    }
}
