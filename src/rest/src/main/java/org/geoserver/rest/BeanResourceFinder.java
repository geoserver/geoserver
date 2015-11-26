/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
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
 *   <bean id="myResource" class="org.acme.MyResource" scope="prototype" />
 *   
 *   <bean id="myBeanResourceFinder" class="org.geoserver.rest.BeanResourceFinder">
 *     <constructor-arg ref="myResource"/>
 *   </bean>
 * </pre>
 * </p>
 * <p>The creation scope of the bean must be "prototype", at each lookup the resource
 * will be provided the request and response via its {@link Resource#init(org.restlet.Context, Request, Response)}
 * method, making it hold those values in the object fields: a singleton won't thus
 * be thread safe
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
        checkNotSingleton();
    }

    public void setApplicationContext(ApplicationContext context){
        applicationContext = context;
        checkNotSingleton();
    }        

    public void setBeanToFind(String name){
        this.beanName = name;
        checkNotSingleton();
    }

    public String getBeanToFind(){
        return beanName;
    }
    
    /**
     * GEOS-4179, REST resources based on BeanResourceFinder are not thread safe unless 
     * declared with prototype scope
     */
    void checkNotSingleton() {
        if(applicationContext != null && beanName != null && !applicationContext.isPrototype(beanName)) {
            throw new RuntimeException("Resource bean " + beanName + " has not been give prototype scope, making the associated restlet not thrad safe! "
                    + "Go back in the app context and give it prototype scope (singleton=\"false\" for DTD based context, scope=\"prototype\" for XSD based ones");
        }
    }

    public Resource findTarget(Request request, Response response){
        Resource res = (Resource) applicationContext.getBean(getBeanToFind());
        res.init(getContext(), request, response);
        return res;
    }
}
