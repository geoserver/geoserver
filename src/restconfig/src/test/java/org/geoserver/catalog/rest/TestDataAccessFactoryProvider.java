/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geoserver.data.DataAccessFactoryProducer;
import org.geotools.data.DataAccessFactory;

public class TestDataAccessFactoryProvider implements DataAccessFactoryProducer {

    private List<? extends DataAccessFactory> factories = new ArrayList<DataAccessFactory>();

    public void setDataStoreFactories(List<? extends DataAccessFactory> factories) {
        this.factories = factories;
    }

    @Override
    public List<DataAccessFactory> getDataStoreFactories() {
        return Collections.unmodifiableList(this.factories);
    }
}
