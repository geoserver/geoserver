package org.geoserver.bxml;

import java.io.IOException;

import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

public interface FeatureTypeProvider {

    @SuppressWarnings("rawtypes")
    public abstract FeatureSource resolveFeatureSource(final Name typeName) throws IOException;

    @SuppressWarnings("rawtypes")
    public abstract FeatureStore resolveFeatureStore(final Name typeName) throws IOException;

    public abstract FeatureType resolveFeatureType(final Name typeName) throws IOException;
}