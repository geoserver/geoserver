/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geoserver.rest.security.xml.AuthFilter;
import org.geoserver.rest.security.xml.JaxbUser;
import org.geoserver.security.config.AnonymousAuthenticationFilterConfig;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.config.ExceptionTranslationFilterConfig;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig;
import org.geoserver.security.config.LogoutFilterConfig;
import org.geoserver.security.config.RememberMeAuthenticationFilterConfig;
import org.geoserver.security.config.RoleFilterConfig;
import org.geoserver.security.config.SSLFilterConfig;
import org.geoserver.security.config.SecurityContextPersistenceFilterConfig;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.config.SecurityInterceptorFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerX509CertificateAuthenticationFilter;
import org.geotools.util.logging.Logging;


/**
 * XStreamPersisterInitializer implementation for gs-rest-config
 *
 * @author ImranR
 */
public class RestConfigXStreamPersister implements XStreamPersisterInitializer {

    /** logging instance */
    static Logger LOGGER = Logging.getLogger(RestConfigXStreamPersister.class);

    @Override
    public void init(XStreamPersister persister) {
        persister.getXStream().allowTypesByWildcard(new String[] {"org.geoserver.rest.security.xml.*"});
        persister.getXStream().alias("user", JaxbUser.class);

        persister.getXStream().alias("authFilter", AuthFilter.class);
        persister.getXStream().alias("config", SecurityFilterConfig.class);
        persister.getXStream().allowTypeHierarchy(SecurityFilterConfig.class);

        persister.getXStream().registerConverter(
                new SecurityFilterConfigConvertor(
                        persister.getXStream().getMapper(),
                        persister.getXStream().getReflectionProvider()
                ));
    }


    public static class SecurityFilterConfigConvertor extends ReflectionConverter {

        public SecurityFilterConfigConvertor(Mapper mapper, ReflectionProvider reflectionProvider) {
            super(mapper, reflectionProvider);
        }

        @Override
        public Object doUnmarshal(Object result, HierarchicalStreamReader reader, UnmarshallingContext context) {
            String className = reader.getAttribute("className");
            if (className == null) {
                return super.doUnmarshal(result, reader, context);
            }

            try {
                Class<?> concreteClass = Class.forName(className);
                Object instance = concreteClass.getDeclaredConstructor().newInstance();

                return super.doUnmarshal(instance, reader, context); // Pass the concrete instance
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException("Error loading class: " + className, e);
            }
        }

        @Override
        public void marshal(Object original, HierarchicalStreamWriter writer, MarshallingContext context) {
            writer.addAttribute("className", original.getClass().getCanonicalName());
            super.marshal(original, writer, context);
        }

        @Override
        public boolean canConvert(Class type) {
            return SecurityFilterConfig.class.isAssignableFrom(type);
        }
    }

}
