/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import org.geoserver.security.Response;
import org.geoserver.security.SecureCatalogImpl;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 * Makes sure all the non writable attributes have null value
 *
 * @author Andrea Aime - GeoSolutions
 */
class CheckAttributesFeatureCollection extends DecoratingSimpleFeatureCollection {

    Set<String> writableAttributes;

    Response response;

    protected CheckAttributesFeatureCollection(
            SimpleFeatureCollection delegate,
            Collection<String> writableAttributes,
            Response response) {
        super(delegate);
        this.writableAttributes = new HashSet<String>(writableAttributes);
        this.response = response;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new CheckAttributesFeatureIterator(delegate.features(), writableAttributes);
    }

    public class CheckAttributesFeatureIterator implements SimpleFeatureIterator {

        SimpleFeatureIterator delegate;

        public CheckAttributesFeatureIterator(
                SimpleFeatureIterator delegate, Set<String> writableAttributes) {
            this.delegate = delegate;
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            final SimpleFeature next = delegate.next();

            // check all write protected attributes are null
            final SimpleFeatureType featureType = next.getFeatureType();
            for (AttributeDescriptor att : featureType.getAttributeDescriptors()) {
                String name = att.getLocalName();
                if (!writableAttributes.contains(name)) {
                    Object value = next.getAttribute(name);
                    if (value != null) {
                        String typeName = getSchema().getName().getLocalPart();
                        if (response == Response.CHALLENGE) {
                            throw SecureCatalogImpl.unauthorizedAccess(typeName);
                        } else {
                            throw new UnsupportedOperationException(
                                    "Trying to write on the write protected attribute " + name);
                        }
                    }
                }
            }

            return next;
        }
    }
}
