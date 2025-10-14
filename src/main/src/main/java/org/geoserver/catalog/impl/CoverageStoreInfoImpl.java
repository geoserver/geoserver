/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.util.ProgressListener;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.util.factory.Hints;

/** Default implementation of {@link CoverageStoreInfo}. */
public class CoverageStoreInfoImpl extends StoreInfoImpl implements CoverageStoreInfo {

    protected String url;

    protected AbstractGridFormat format;

    protected CoverageStoreInfoImpl() {}

    public CoverageStoreInfoImpl(Catalog catalog) {
        super(catalog);
    }

    public CoverageStoreInfoImpl(Catalog catalog, String id) {
        super(catalog, id);
    }

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public void setURL(String url) {
        this.url = url;
    }

    @Override
    public AbstractGridFormat getFormat() {
        return catalog.getResourcePool().getGridCoverageFormat(this);
    }

    @Override
    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public GridCoverageReader getGridCoverageReader(ProgressListener listener, Hints hints) throws IOException {
        return catalog.getResourcePool().getGridCoverageReader(this, null, hints);
    }
}
