/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.List;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.util.decorate.AbstractDecorator;

/**
 * Delegates all methods to the provided delegate. Suclasses will override methods in order to
 * perform their decoration work
 *
 * @author Andrea Aime - TOPP
 * @param <T>
 * @param <F>
 */
public abstract class DecoratingDataAccess<T extends FeatureType, F extends Feature>
        extends AbstractDecorator<DataAccess<T, F>> implements DataAccess<T, F> {

    public DecoratingDataAccess(DataAccess<T, F> delegate) {
        super(delegate);
    }

    @Override
    public void createSchema(T featureType) throws IOException {
        delegate.createSchema(featureType);
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public FeatureSource<T, F> getFeatureSource(Name typeName) throws IOException {
        return delegate.getFeatureSource(typeName);
    }

    @Override
    public ServiceInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public List<Name> getNames() throws IOException {
        return delegate.getNames();
    }

    @Override
    public T getSchema(Name name) throws IOException {
        return delegate.getSchema(name);
    }

    @Override
    public void updateSchema(Name typeName, T featureType) throws IOException {
        delegate.updateSchema(typeName, featureType);
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        delegate.removeSchema(typeName);
    }
}
