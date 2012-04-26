package org.geoserver.geogit;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.VersioningPlugin;
import org.geoserver.data.versioning.decorator.VersioningAdapterFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GEOGITPlugin extends VersioningPlugin {

    @Override
    public SimpleFeatureSource wrap(SimpleFeatureSource featureSource,
            SimpleFeatureType featureType, FeatureTypeInfo info, CoordinateReferenceSystem crs) {

        return (SimpleFeatureSource) VersioningAdapterFactory.create(featureSource);
    }

}
