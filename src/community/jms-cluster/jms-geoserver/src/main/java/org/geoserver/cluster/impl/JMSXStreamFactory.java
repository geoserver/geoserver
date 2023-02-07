/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cluster.impl;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.SecurityMapper;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.TypePermission;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;

public class JMSXStreamFactory {

    private final XStream xs;

    public JMSXStreamFactory(XStreamPersisterFactory factory, GeoServer gs)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException,
                    NoSuchFieldException {
        XStreamPersister persister = factory.createXMLPersister();
        // add XStream configuration from the service loaders
        Method method =
                XStreamServiceLoader.class.getDeclaredMethod(
                        "initXStreamPersister", XStreamPersister.class, GeoServer.class);
        method.setAccessible(true);
        for (XStreamServiceLoader loader :
                GeoServerExtensions.extensions(XStreamServiceLoader.class)) {
            method.invoke(loader, persister, gs);
        }
        // copy over the security configuration using reflection, there are no openings to get it,
        // but we need that and not the converter configuration, aliases and the like, as
        // the event handlers should get full objects for references, not just ids
        XStream persisterXStream = persister.getXStream();
        this.xs = new XStream();
        Field securityMapperField = XStream.class.getDeclaredField("securityMapper");
        securityMapperField.setAccessible(true);
        SecurityMapper smPersister = (SecurityMapper) securityMapperField.get(persisterXStream);
        Field permissionsField = SecurityMapper.class.getDeclaredField("permissions");
        permissionsField.setAccessible(true);
        List<TypePermission> permissions = (List<TypePermission>) permissionsField.get(smPersister);
        permissions.stream()
                .filter(p -> !(p instanceof NoTypePermission))
                .forEach(p -> xs.addPermission(p));

        // Now add bits that are unique to the cluster module
        // add events from this module
        this.xs.allowTypesByWildcard(new String[] {"org.geoserver.cluster.**"});
        // add internal catalog events
        this.xs.allowTypeHierarchy(CatalogEvent.class);
        // add these converters to avoid the need to handle a large number of allows just
        // for the tree of objects that make up a CRS
        this.xs.registerConverter(new XStreamPersister.CRSConverter());
    }

    public XStream createXStream() {
        return xs;
    }
}
