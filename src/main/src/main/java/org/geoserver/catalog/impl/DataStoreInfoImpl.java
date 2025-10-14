/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.DataStoreInfo;
import org.geotools.api.data.DataAccess;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.util.ProgressListener;

/** Default implementation of {@link DataStoreInfo}. */
@SuppressWarnings("serial")
public class DataStoreInfoImpl extends StoreInfoImpl implements DataStoreInfo {

    protected DataStoreInfoImpl() {}

    public DataStoreInfoImpl(Catalog catalog) {
        super(catalog);
    }

    public DataStoreInfoImpl(Catalog catalog, String id) {
        super(catalog, id);
    }

    @Override
    public DataAccess<? extends FeatureType, ? extends Feature> getDataStore(ProgressListener listener)
            throws IOException {
        return catalog.getResourcePool().getDataStore(this);
    }

    @Override
    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    @SuppressWarnings("PMD.OverrideBothEqualsAndHashcode")
    public boolean equals(Object obj) {
        if (!(obj instanceof DataStoreInfo)) {
            return false;
        }

        return super.equals(obj);
    }
}
