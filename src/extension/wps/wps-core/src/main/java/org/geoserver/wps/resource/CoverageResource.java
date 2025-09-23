/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.awt.image.RenderedImage;
import org.eclipse.imagen.PlanarImage;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.util.ImageUtilities;

public class CoverageResource implements WPSResource {

    GridCoverage coverage;

    public CoverageResource(GridCoverage coverage) {
        this.coverage = coverage;
    }

    @Override
    public void delete() throws Exception {
        if (coverage instanceof GridCoverage2D gc) {
            final RenderedImage image = gc.getRenderedImage();
            if (image instanceof PlanarImage planarImage) {
                ImageUtilities.disposePlanarImageChain(planarImage);
            }
            gc.dispose(true);
        }
    }

    @Override
    public String getName() {
        return coverage.toString();
    }
}
