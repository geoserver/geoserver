/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.easymock.EasyMock.*;

import org.easymock.EasyMock;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Restlet;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class RESTDispatcherCallbackTest extends GeoServerSystemTestSupport {

    DispatcherCallback callback;
    
    @Before
    public void prepareCallback() throws Exception {
        callback = EasyMock.createMock(DispatcherCallback.class);
        applicationContext.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
            
            public Object postProcessBeforeInitialization(Object bean, String beanName)
                    throws BeansException {
                if ("testCallback".equals(beanName)) {
                    return callback;
                }
                return bean;
            }
            
            public Object postProcessAfterInitialization(Object bean, String beanName)
                    throws BeansException {
                return bean;
            }
        });
        applicationContext.getBeanFactory().destroySingletons();
        applicationContext.getBeanFactory().preInstantiateSingletons();
        
    }
    
    @Test
    public void testCallback() throws Exception {
        callback.init((Request)anyObject(), (Response)anyObject());
        expectLastCall();
        callback.dispatched((Request)anyObject(), (Response)anyObject(), (Restlet)anyObject());
        expectLastCall();
        callback.finished((Request)anyObject(), (Response)anyObject());
        expectLastCall();
        replay(callback);
        
        getAsServletResponse("/rest/index.html");
        verify(callback);
    }
    
    @Test
    public void testCallbackException() throws Exception {
        
        callback.init((Request)anyObject(), (Response)anyObject());
        expectLastCall();
        callback.dispatched((Request)anyObject(), (Response)anyObject(), (Restlet)anyObject());
        expectLastCall();
        callback.exception((Request)anyObject(), (Response)anyObject(), (Exception)anyObject());
        expectLastCall();
        callback.finished((Request)anyObject(), (Response)anyObject());
        expectLastCall();
        replay(callback);
        
        getAsServletResponse("/rest/exception?code=400&message=error");
        verify(callback);
    }

    static class TestCallback implements DispatcherCallback {

        public void init(Request request, Response response) {}

        public void dispatched(Request request, Response response, Restlet restlet) {}

        public void exception(Request request, Response response, Exception error) {}

        public void finished(Request request, Response response) {}

    }
}
