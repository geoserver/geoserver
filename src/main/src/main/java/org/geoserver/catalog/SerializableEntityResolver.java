/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.util.EntityResolverProvider;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Simple wrapper to make entity resolvers serializable, on deserialization it will fetch the
 * resolver from the {@link EntityResolverProvider}
 */
class SerializableEntityResolver implements EntityResolver, Serializable {

    private static final long serialVersionUID = -447221633611119495L;
    transient EntityResolver delegate;

    public SerializableEntityResolver(EntityResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        if (delegate != null) {
            return delegate.resolveEntity(publicId, systemId);
        } else {
            return null;
        }
    }

    private Object readResolve() throws ObjectStreamException {
        EntityResolverProvider resolverProvider =
                GeoServerExtensions.bean(EntityResolverProvider.class);
        EntityResolver resolver = null;
        if (resolverProvider != null) {
            resolver = resolverProvider.getEntityResolver();
        }
        return new SerializableEntityResolver(resolver);
    }
}
