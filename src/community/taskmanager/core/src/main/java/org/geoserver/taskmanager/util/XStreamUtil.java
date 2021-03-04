/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentCollectionConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentMapConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentSortedMapConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernatePersistentSortedSetConverter;
import com.thoughtworks.xstream.hibernate.converter.HibernateProxyConverter;
import com.thoughtworks.xstream.hibernate.mapper.HibernateMapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.taskmanager.data.impl.AttributeImpl;
import org.geoserver.taskmanager.data.impl.BatchElementImpl;
import org.geoserver.taskmanager.data.impl.BatchImpl;
import org.geoserver.taskmanager.data.impl.ConfigurationImpl;
import org.geoserver.taskmanager.data.impl.ParameterImpl;
import org.geoserver.taskmanager.data.impl.TaskImpl;
import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.collection.internal.PersistentSortedMap;
import org.hibernate.collection.internal.PersistentSortedSet;
import org.hibernate.collection.spi.PersistentCollection;

public class XStreamUtil {

    private XStreamUtil() {}

    public static XStream xs() {
        final SecureXStream xs =
                new SecureXStream(new PureJavaReflectionProvider()) {
                    @Override
                    protected MapperWrapper wrapMapper(final MapperWrapper next) {
                        return new HibernateMapper(next);
                    }
                };
        xs.allowTypes(
                new Class[] {
                    ConfigurationImpl.class,
                    BatchImpl.class,
                    TaskImpl.class,
                    AttributeImpl.class,
                    BatchElementImpl.class,
                    ParameterImpl.class,
                    PersistentCollection.class,
                    PersistentMap.class,
                    PersistentSortedMap.class,
                    PersistentSortedSet.class
                });

        xs.autodetectAnnotations(true);
        xs.registerConverter(new HibernateProxyConverter());
        xs.registerConverter(new HibernatePersistentCollectionConverter(xs.getMapper()));
        xs.registerConverter(new HibernatePersistentMapConverter(xs.getMapper()));
        xs.registerConverter(new HibernatePersistentSortedMapConverter(xs.getMapper()));
        xs.registerConverter(new HibernatePersistentSortedSetConverter(xs.getMapper()));
        return xs;
    }
}
