/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;

public class GeopkgVectorPPIO extends GeopkgPPIO {

    /** Constructor. */
    protected GeopkgVectorPPIO() {
        super(SimpleFeatureCollection.class, SimpleFeatureCollection.class);
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        SimpleFeatureCollection fc = (SimpleFeatureCollection) value;
        File file = File.createTempFile("geopkg", ".tmp.gpkg");
        try {
            // write the geopackage
            try (GeoPackage geopkg = getGeoPackage(file)) {

                FeatureEntry e = new FeatureEntry();
                Object ri = fc.getSchema().getUserData().get(ResourceInfo.class);
                if (ri instanceof FeatureTypeInfo) {
                    FeatureTypeInfo meta = (FeatureTypeInfo) ri;
                    // initialize entry metadata
                    e.setIdentifier(meta.getTitle());
                    e.setDescription(abstractOrDescription(meta));
                }

                geopkg.add(e, fc);
                geopkg.createSpatialIndex(e);
            }

            // copy over to the output
            try (InputStream is = new FileInputStream(file)) {
                IOUtils.copy(is, os);
            }
        } finally {
            file.delete();
        }
    }
}
