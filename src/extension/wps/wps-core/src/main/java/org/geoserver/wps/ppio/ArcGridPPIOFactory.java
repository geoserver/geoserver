/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.coverage.grid.GridCoverage2D;

public class ArcGridPPIOFactory {

    public static ArcGridPPIO create(WPSResourceManager resources) {
        try {
            // 1. Check if the dependency is actually there
            Class.forName("org.geotools.gce.arcgrid.ArcGridReader");
            // 2. Only if it is, instantiate the REAL PPIO
            return new ArcGridPPIO(resources);
        } catch (Throwable t) {
            return null;
        }
    }

    /** The real PPIO is hidden inside. Spring won't introspect this unless we successfully call 'new'. */
    public static class ArcGridPPIO extends CDataPPIO {
        private final WPSResourceManager resources;

        ArcGridPPIO(WPSResourceManager resources) {
            super(GridCoverage2D.class, GridCoverage2D.class, "application/arcgrid");
            this.resources = resources;
        }

        @Override
        public Object decode(InputStream input) throws Exception {
            return ArcGridDelegate.decode(input, resources);
        }

        @Override
        public Object decode(String arcgrid) throws Exception {
            // if the user forgot to add the final newline let's just add it
            if (!arcgrid.endsWith("\n")) {
                arcgrid += "\n";
            }
            return decode(new ByteArrayInputStream(arcgrid.getBytes()));
        }

        @Override
        public void encode(Object value, OutputStream os) throws Exception {
            ArcGridDelegate.encode(value, os);
        }

        @Override
        public String getFileExtension() {
            return "asc";
        }
    }
}
