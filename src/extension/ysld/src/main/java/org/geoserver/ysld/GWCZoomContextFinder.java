/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 - 2016 Boundless Spatial Inc.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ysld;

import java.util.Set;
import javax.annotation.Nullable;
import org.geotools.ysld.parse.MedialZoomContext;
import org.geotools.ysld.parse.ZoomContext;
import org.geotools.ysld.parse.ZoomContextFinder;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;

/**
 * ZoomContextFinder that wraps the GWC Gridset Broker.
 *
 * @author Kevin Smith, Boundless
 */
public class GWCZoomContextFinder implements ZoomContextFinder {

    GridSetBroker broker;

    public GWCZoomContextFinder(GridSetBroker broker) {
        super();
        this.broker = broker;
    }

    @Override
    public @Nullable ZoomContext get(String name) {
        GridSet set = broker.get(name);
        if (set != null) {
            return new GWCZoomContext(set);
        } else {
            return null;
        }
    }

    @Override
    public Set<String> getNames() {
        return broker.getNames();
    }

    @Override
    public Set<String> getCanonicalNames() {
        return broker.getNames();
    }

    class GWCZoomContext extends MedialZoomContext {
        final GridSet gridset;

        public GWCZoomContext(GridSet gridset) {
            super();
            assert gridset != null;
            this.gridset = gridset;
        }

        @Override
        public double getScaleDenominator(int level) {
            if (level < 0) return Double.POSITIVE_INFINITY;
            if (level >= gridset.getNumLevels()) return 0;

            return gridset.getGrid(level).getScaleDenominator();
        }

        @Override
        protected double getMedialScale(int level) {
            if (level >= gridset.getNumLevels() - 1) {
                return 0;
            }
            return getScaleDenominator(level) / 1.005d; // Mimic TileFuser

            // return Math.sqrt(getScaleDenominator(level)*getScaleDenominator(level+1)); //
            // Geometric, like other implementations
        }

        @Override
        public boolean isInRange(int level) {
            return level >= 0 && level < gridset.getNumLevels();
        }
    }
}
