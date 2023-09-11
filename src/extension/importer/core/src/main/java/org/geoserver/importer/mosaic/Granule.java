/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.mosaic;

import java.util.Date;
import org.geoserver.importer.SpatialFile;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;

/** A tile of a mosaic. */
public class Granule extends SpatialFile {

    Date timestamp;
    ReferencedEnvelope envelope;
    GridGeometry2D grid;

    public Granule(SpatialFile file) {
        super(file);
    }

    public ReferencedEnvelope getEnvelope() {
        return envelope;
    }

    public void setEnvelope(ReferencedEnvelope envelope) {
        this.envelope = envelope;
    }

    public GridGeometry2D getGrid() {
        return grid;
    }

    public void setGrid(GridGeometry2D grid) {
        this.grid = grid;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
