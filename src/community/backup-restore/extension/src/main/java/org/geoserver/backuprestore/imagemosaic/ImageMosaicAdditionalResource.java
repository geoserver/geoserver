/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.imagemosaic;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.Filter;

/** @author Alessio Fabiani, GeoSolutions */
public abstract class ImageMosaicAdditionalResource {

    public static final String COVERAGE_TYPE = "ImageMosaic";

    public static final String IMAGEMOSAIC_INDEXES_FOLDER = "imagemosaic_indexes";

    /*
     *
     */
    public static Map<String, Filter<Resource>> resources = new HashMap<String, Filter<Resource>>();

    /*
     *
     */
    static {
        resources.put(
                "properties",
                new Filter<Resource>() {

                    @Override
                    public boolean accept(Resource res) {
                        if (res.name().endsWith(".properties")) {
                            return true;
                        }
                        return false;
                    }
                });

        resources.put(
                "templates",
                new Filter<Resource>() {

                    @Override
                    public boolean accept(Resource res) {
                        if (res.name().endsWith(".template")) {
                            return true;
                        }
                        return false;
                    }
                });

        resources.put(
                "info",
                new Filter<Resource>() {

                    @Override
                    public boolean accept(Resource res) {
                        if (res.name().endsWith(".xml")) {
                            return true;
                        }
                        return false;
                    }
                });
    }
}
