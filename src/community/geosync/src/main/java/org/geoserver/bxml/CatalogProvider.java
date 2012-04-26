package org.geoserver.bxml;

import java.io.IOException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

public class CatalogProvider implements FeatureTypeProvider {

    private final Catalog catalog;

    public CatalogProvider(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public FeatureSource resolveFeatureSource(Name typeName) throws IOException {
        FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(typeName);
        if (typeInfo == null) {
            throw new IOException(typeName + " does not exist");
        }
        FeatureSource source = typeInfo.getFeatureSource(null, null);
        return source;
    }

    @Override
    public FeatureStore resolveFeatureStore(Name typeName) throws IOException {
        FeatureSource source = resolveFeatureSource(typeName);
        if (!(source instanceof FeatureStore)) {
            throw new IOException("Type is read only: " + typeName);
        }
        return (FeatureStore) source;
    }

    @Override
    public FeatureType resolveFeatureType(Name typeName) throws IOException {
        FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(typeName);
        if (typeInfo == null) {
            throw new IOException(typeName + " does not exist");
        }
        return typeInfo.getFeatureType();
    }

}
