/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.adapters;

import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.ows.HttpServletRequestAware;
import org.vfny.geoserver.util.requests.readers.KvpRequestReader;

/**
 * Wraps an old style {@link KvpRequestReader} in a new {@link org.geoserver.ows.KvpRequestReader}.
 *
 * <p>This class needs to be defined in a spring context like:
 *
 * <pre>
 * <code>
 *   &lt;bean id="getMapKvpReader" class="org.geoserver.ows.adapters.KvpRequestReaderAdapter"&gt;
 *      &lt;!-- first argument is the request class --&gt;
 *      &lt;constructor-arg index="0" value="org.vfny.geoserver.wms.requests.GetMapRequest" /&gt;
 *
 *      &lt;!-- second argument is the old style kvp reader class --&gt;
 *      &lt;constructor-arg index="1" value="org.vfny.geoserver.wms.requests.GetMapKvpReader" /&gt;
 *
 *      &lt;!-- third argument is the old style service --&gt;
 *      &lt;constructor-arg index="2" ref="wmsService" /&gt;
 *   &lt;bean&gt;
 * </code>
 * </pre>
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class KvpRequestReaderAdapter extends org.geoserver.ows.KvpRequestReader
        implements HttpServletRequestAware {
    Class delegateClass;
    ServiceInfo service;
    HttpServletRequest request;

    public KvpRequestReaderAdapter(Class requestBean, Class delegateClass, ServiceInfo service) {
        super(requestBean);
        this.delegateClass = delegateClass;
        this.service = service;
    }

    public void setHttpRequest(HttpServletRequest request) {
        this.request = request;
    }

    public Object createRequest() throws Exception {
        // simulate the old kvp processin
        Map kvp = new HashMap();
        String paramName;
        String paramValue;

        for (Enumeration pnames = request.getParameterNames(); pnames.hasMoreElements(); ) {
            paramName = (String) pnames.nextElement();
            paramValue = request.getParameter(paramName);
            kvp.put(paramName.toUpperCase(), paramValue);
        }

        // look for a constructor, may have to walk up teh class hierachy
        Class clazz = GeoServerImpl.unwrap(service).getClass();
        Constructor constructor = null;

        while (clazz != null && constructor == null) {
            try {
                constructor = delegateClass.getConstructor(new Class[] {Map.class, clazz});
            } catch (NoSuchMethodException e) {
                Class[] classes = clazz.getInterfaces();
                for (Class c : classes) {
                    try {
                        constructor = delegateClass.getConstructor(new Class[] {Map.class, c});
                    } catch (NoSuchMethodException e2) {
                        // no harm done
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }

        if (constructor == null) {
            throw new IllegalStateException("No appropriate constructor");
        }

        // create an instance of the delegate
        KvpRequestReader delegate =
                (KvpRequestReader) constructor.newInstance(new Object[] {kvp, service});

        // create the request object
        return delegate.getRequest(request);
    }

    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // request object already initialized, just send it back
        return request;
    }
}
