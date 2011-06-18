/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfsv.VersionedFeatureCollectionType;
import net.opengis.wfsv.WfsvFactory;

import org.geoserver.catalog.Catalog;
import org.geoserver.wfs.GetFeature;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.VersioningFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * An extension of {@link GetFeature} returning collection of versioned features
 * @author Administrator
 *
 */
public class VersionedGetFeature extends GetFeature {

    public VersionedGetFeature(WFSInfo wfs, Catalog catalog) {
        super(wfs, catalog);
    }
    
    protected FeatureCollection<? extends FeatureType, ? extends Feature> getFeatures(
            GetFeatureType request, FeatureSource<? extends FeatureType, ? extends Feature> source, Query gtQuery) throws IOException {
        if(!(source instanceof VersioningFeatureSource))
            throw new WFSException(source.getSchema().getName().getLocalPart() + " is not versioned, cannot " +
            		"execute a GetVersionedFeature on it");
        return ((VersioningFeatureSource) source).getVersionedFeatures(gtQuery);
    }
    
    /**
     * Allows subclasses to alter the result generation
     * @param count
     * @param results
     * @param lockId
     * @return
     */
    protected FeatureCollectionType buildResults(int count, List results,
            String lockId) {
        VersionedFeatureCollectionType result = WfsvFactory.eINSTANCE.createVersionedFeatureCollectionType();
        result.setNumberOfFeatures(BigInteger.valueOf(count));
        result.setTimeStamp(Calendar.getInstance());
        result.setLockId(lockId);
        result.getFeature().addAll(results);
        result.setVersion("xxx");
        return result;
    }

}
