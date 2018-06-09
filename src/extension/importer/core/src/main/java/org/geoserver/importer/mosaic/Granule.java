/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.mosaic;

import java.util.Date;
import org.geoserver.importer.SpatialFile;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;

/** A tile of a mosaic. */
public class Granule extends SpatialFile {

    Date timestamp;
    Envelope2D envelope;
    GridGeometry2D grid;

    public Granule(SpatialFile file) {
        super(file);
    }

    public Envelope2D getEnvelope() {
        return envelope;
    }

    public void setEnvelope(Envelope2D envelope) {
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
