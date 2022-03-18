/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jettison.mapped.Configuration;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Factory for {@link XStreamPersister} instances.
 *
 * <p>This class is a singleton registered in a spring context. Any code that needs to create an
 * XStreamPersister instance should do one of the following:
 *
 * <ol>
 *   <li>Use dependency injection via spring. Example:
 *       <pre>
 *       <bean id="myBean" class="com.xyz.MyBean">
 *         <constructor-arg ref="xstreamPersisterFactory"/>
 *       </bean>
 *     </pre>
 *   <li>Lookup via {@link GeoServerExtensions#bean(Class)}:
 *       <pre>
 *       XStreamPersisterFactory xpf = GeoServerExtension.bean(XStreamPeristerFactory.class);
 *     </pre>
 * </ol>
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class XStreamPersisterFactory implements ApplicationContextAware {

    /** Cache of initializers from application context */
    private List<XStreamPersisterInitializer> initializers;

    /**
     * Application context provided during initialization.
     *
     * <p>This may be null during initial startup, or during tests, when {@link
     * #setApplicationContext(ApplicationContext)} has not been called.
     */
    private ApplicationContext applicationContext;

    /** Creates an instance configured to persist XML. */
    public XStreamPersister createXMLPersister() {
        return buildPersister(null);
    }

    /**
     * Creates an instance configured to persist JSON.
     *
     * <p>Preserves legacy Jettison 1.0.1 behavior when encoding single-element collections as a
     * JSON object instead of a single-element JSON array.
     *
     * <p>Use {@link #createJSONPersister(true)} to force JSON encoding to always use JSON arrays
     * regardless of collection size.
     */
    public XStreamPersister createJSONPersister() {
        // preserve legacy single-element-array-as-object serialization
        boolean alwaysSerializeCollectionsAsArray = false;
        return createJSONPersister(alwaysSerializeCollectionsAsArray);
    }

    /**
     * @param alwaysSerializeCollectionsAsArray whether to encode single element collections as JSON
     *     arrays ({@code true}), which is the default value in Jettison 1.4+, or preserve legacy
     *     (as of Jettison 1.0.1) single-element-array-as-object serialization ({@code false}).
     */
    public XStreamPersister createJSONPersister(boolean alwaysSerializeCollectionsAsArray) {
        // needed for Jettison 1.4.1
        Configuration configuration = new Configuration();
        configuration.setRootElementArrayWrapper(false);
        JettisonMappedXmlDriver driver =
                new JettisonMappedXmlDriver(configuration, alwaysSerializeCollectionsAsArray);
        return buildPersister(driver);
    }

    /** Builds a persister and runs the initializers against it */
    private XStreamPersister buildPersister(HierarchicalStreamDriver driver) {
        XStreamPersister persister = new XStreamPersister(driver);
        // give the initializers a chance to register their own converters, aliases and so on
        for (XStreamPersisterInitializer initializer : getInitializers()) {
            initializer.init(persister);
        }
        return persister;
    }

    private List<XStreamPersisterInitializer> getInitializers() {
        if (initializers == null || initializers.isEmpty()) {
            // the factory is created also programmatically, and without
            if (applicationContext == null) {
                initializers =
                        new ArrayList<>(
                                GeoServerExtensions.extensions(XStreamPersisterInitializer.class));
            } else {
                initializers =
                        new ArrayList<>(
                                GeoServerExtensions.extensions(
                                        XStreamPersisterInitializer.class, applicationContext));
            }
        }
        return initializers;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        initializers =
                new ArrayList<>(
                        GeoServerExtensions.extensions(
                                XStreamPersisterInitializer.class, applicationContext));
    }

    /**
     * Programmatically adds a {@link XStreamPersisterInitializer} to the factory (initializers are
     * also automatically looked up from the Spring context, use this method only if you cannot
     * Declare your initializer as a spring bean)
     */
    public void addInitializer(XStreamPersisterInitializer initializer) {
        getInitializers().add(initializer);
    }

    /**
     * Removes an initializer
     *
     * @return True if the initializer was found and removed, false otherwise
     */
    public boolean removeInitializer(XStreamPersisterInitializer initializer) {
        return getInitializers().remove(initializer);
    }
}
