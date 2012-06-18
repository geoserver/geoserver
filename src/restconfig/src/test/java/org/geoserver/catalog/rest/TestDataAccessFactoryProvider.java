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

    public List<DataAccessFactory> getDataStoreFactories() {
        return Collections.unmodifiableList(this.factories);
    }
}
