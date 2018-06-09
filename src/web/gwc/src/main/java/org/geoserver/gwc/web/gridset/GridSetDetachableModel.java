/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.gwc.GWC;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;

/** @see GWC#getGridSetBroker() */
class GridSetDetachableModel extends LoadableDetachableModel<GridSet> {
    private static final long serialVersionUID = 7948457592325861589L;

    private final String name;

    public GridSetDetachableModel(String name) {
        this.name = name;
    }

    @Override
    protected GridSet load() {
        GridSetBroker gridSetBroker = GWC.get().getGridSetBroker();
        GridSet gridSet = gridSetBroker.get(name);
        return gridSet;
    }
}
