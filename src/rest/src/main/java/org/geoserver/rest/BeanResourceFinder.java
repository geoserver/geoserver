/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A resource finder which simply returns a resource corresponding to a spring bean 
 * with a specific name.
 * <p>
 * The following shows an example of using this class in a spring context:
 * <pre>
 *   <bean id="myResource" class="org.acme.MyResource"/>
 *   
 *   <bean id="myBeanResourceFinder" class="org.geoserver.rest.BeanResourceFinder">
 *     <constructor-arg ref="myResource"/>
 *   </bean>
 * </pre>
 * </p>
 * 
 * @author David Winslow, OpenGEO
 * @author Justin Deoliveira, OpenGEO
 *
 */
public class BeanResourceFinder extends Finder implements ApplicationContextAware{
    /** the app context used to look up the bean*/
    ApplicationContext applicationContext;
    /** the name of the bean to lookup */
    String beanName;

    /**
     * Creates the new finder specifying the name of the bean to return.
     * 
     * @param beanName The name of a spring bean which is a subclass of {@link Resource}.
     */
    public BeanResourceFinder(String beanName) {
        this.beanName = beanName;
    }
    
    /**
     * Creates the new finder specifying the name of the bean to return, and the application
     * context with which to look-up the bean.
     * 
     * @param context The application context.
     * @param beanName The name of a spring bean which is a subclass of {@link Resource}.
     * 
     */
    public BeanResourceFinder(ApplicationContext context, String beanName){
        this.applicationContext = context;
        this.beanName = beanName;
    }

    public void setApplicationContext(ApplicationContext context){
        applicationContext = context;
    }        

    public void setBeanToFind(String name){
        this.beanName = name;
    }

    public String getBeanToFind(){
        return beanName;
    }

    public Resource findTarget(Request request, Response response){
        Resource res = (Resource) applicationContext.getBean(getBeanToFind());
        res.init(getContext(), request, response);
        return res;
    }
}
