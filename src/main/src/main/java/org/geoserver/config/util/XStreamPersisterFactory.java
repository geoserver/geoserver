/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import org.geoserver.platform.GeoServerExtensions;

import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * Factory for {@link XStreamPersister} instances.
 * <p>
 * This class is a singleton registered in a spring context. Any code that needs 
 * to create an XStreamPersister instance should do one of the following: 
 * <ol>
 *   <li>Use dependency injection via spring. Example:
 *     <pre>
 *       <bean id="myBean" class="com.xyz.MyBean">
 *         <constructor-arg ref="xstreamPersisterFactory"/>
 *       </bean>
 *     </pre>
 *   <li>Lookup via {@link GeoServerExtensions#bean(Class)}:
 *     <pre>
 *       XStreamPersisterFactory xpf = GeoServerExtension.bean(XStreamPeristerFactory.class);
 *     </pre>
 * </ol>
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class XStreamPersisterFactory {

    /**
     * Creates an instance configured to persist XML. 
     */
    public XStreamPersister createXMLPersister() {
        return new XStreamPersister();
    }

    /**
     * Creates an instance configured to persist JSON.
     */
    public XStreamPersister createJSONPersister() {
        return new XStreamPersister(new JettisonMappedXmlDriver());
    }
}
