/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.expression.PropertyName;

/**
 * Basic attribute shaver, works properly only against {@link CSWRecordDescriptor#RECORD}
 *
 * @author Andrea Aime - GeoSolutions
 */
class RetypingIterator implements Iterator<Feature>, Closeable {

    FeatureIterator<Feature> delegate;

    Set names;

    ComplexFeatureBuilder builder;

    public RetypingIterator(
            FeatureIterator<Feature> delegate, FeatureType schema, List<PropertyName> properties) {
        this.delegate = delegate;
        this.builder = new ComplexFeatureBuilder(schema);
        this.names = buildNames(properties);
    }

    private Set buildNames(List<PropertyName> properties) {
        Set result = new HashSet();
        for (PropertyName pn : properties) {
            String fullName = pn.getPropertyName();
            if (fullName.indexOf('@') != -1 || fullName.indexOf('/') != -1) {
                throw new IllegalArgumentException(
                        "Invalid property "
                                + fullName
                                + ", this code can only handle properties with the 'name' or 'prefix:name' structure");
            }

            // try to split in prefix and name if possible
            String name = fullName;
            String prefix = null;
            int idx = fullName.indexOf(':');
            if (idx > 0) {
                prefix = fullName.substring(0, idx);
                name = fullName.substring(idx + 1);
            }

            // build the Name according to what we found
            if (prefix != null && pn.getNamespaceContext() != null) {
                String ns = pn.getNamespaceContext().getURI(prefix);
                result.add(new NameImpl(ns, name));
            } else {
                result.add(name);
            }
        }

        return result;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public Feature next() {
        Feature original = delegate.next();
        // this does not really work...
        //        for (PropertyDescriptor pd : original.getType().getDescriptors()) {
        //            Collection<Property> properties = original.getProperties(pd.getName());
        //            if(properties != null) {
        //                for (Property p : properties) {
        //                    if (names.contains(p.getName()) ||
        // names.contains(p.getName().getLocalPart())) {
        //                        builder.append(pd.getName(), p);
        //                    }
        //                }
        //            }
        //        }

        for (Property p : original.getProperties()) {
            if (names.contains(p.getName()) || names.contains(p.getName().getLocalPart())) {
                // this makes the thing type specific, but at least it works for the record case
                // TODO: eventually figure out how to make this for the general case...
                if (p.getType().equals(CSWRecordDescriptor.SIMPLE_LITERAL)) {
                    builder.append(CSWRecordDescriptor.DC_ELEMENT_NAME, p);
                } else {
                    builder.append(p.getName(), p);
                }
            }
        }

        return builder.buildFeature(original.getIdentifier().getID());
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void close() {
        delegate.close();
    }
}
