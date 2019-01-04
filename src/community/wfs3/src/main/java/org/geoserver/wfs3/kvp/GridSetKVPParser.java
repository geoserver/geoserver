/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.kvp;

import java.util.Optional;
import org.geoserver.ows.KvpParser;
import org.geoserver.platform.ServiceException;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.grid.GridSet;

public class GridSetKVPParser extends KvpParser {

    DefaultGridsets gridSets;

    public GridSetKVPParser(String key, DefaultGridsets gridSets) {
        super(key, GridSet.class);
        this.gridSets = gridSets;
    }

    @Override
    public Object parse(String value) throws Exception {
        Optional<GridSet> gridSet = gridSets.getGridSet(value);
        if (!gridSet.isPresent()) {
            throw new ServiceException("Invalid gridset name " + value);
        }
        return gridSet.get();
    }
}
