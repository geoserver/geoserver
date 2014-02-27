/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.DataStoreInfo;
import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.util.ProgressListener;

/**
 * Default implementation of {@link DataStoreInfo}.
 * 
 */
@SuppressWarnings("serial")
public class DataStoreInfoImpl extends StoreInfoImpl implements DataStoreInfo {

    protected DataStoreInfoImpl() {
    }

    public DataStoreInfoImpl(Catalog catalog) {
        super(catalog);
    }

    public DataStoreInfoImpl(Catalog catalog,String id) {
        super(catalog,id);
    }

    public DataAccess<? extends FeatureType, ? extends Feature> getDataStore(
            ProgressListener listener) throws IOException {
        return catalog.getResourcePool().getDataStore(this);
    }

    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!( obj instanceof DataStoreInfo ) ) {
            return false;
        }
        
        return super.equals( obj );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }
}
