/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.data;

import java.io.IOException;
import java.util.List;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.feature.NameImpl;
import org.geotools.filter.FilterCapabilities;

public abstract class ComplexContentDataAccess implements DataAccess<FeatureType, Feature> {

    @SuppressWarnings("deprecation")
    FilterCapabilities filterCapabilities;

    protected String namespaceURI;

    private List<Name> typeNames;

    public ComplexContentDataAccess() {
        filterCapabilities = createFilterCapabilities();
    }

    protected abstract FilterCapabilities createFilterCapabilities();

    @Override
    public ServiceInfo getInfo() {
        return null;
    }

    @Override
    public void createSchema(FeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Not a supported operation");
    }

    @Override
    public void updateSchema(Name typeName, FeatureType featureType) throws IOException {
        throw new UnsupportedOperationException("Not a supported operation");
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw new UnsupportedOperationException("Not a supported operation");
    }

    @Override
    public List<Name> getNames() throws IOException {
        if (typeNames == null) {
            typeNames = createTypeNames();
        }
        return typeNames;
    }

    @Override
    public FeatureType getSchema(Name name) throws IOException {
        return getFeatureSource(name).getSchema();
    }

    public FilterCapabilities getFilterCapabilities() {
        return filterCapabilities;
    }

    public void setNamespaceURI(String uri) {
        this.namespaceURI = uri;
    }

    protected abstract List<Name> createTypeNames();

    protected final Name name(String typeName) {
        return new NameImpl(namespaceURI, typeName);
    }
}
