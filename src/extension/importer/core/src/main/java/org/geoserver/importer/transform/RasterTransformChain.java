/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;

/**
 * @todo implement me
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class RasterTransformChain extends TransformChain<RasterTransform> {

    @Override
    public void pre(ImportTask task, ImportData data) throws Exception {
        if (transforms.size() > 0) {
            throw new RuntimeException("Not implemented");
        }
    }

    @Override
    public void post(ImportTask task, ImportData data) throws Exception {
    }
    
}
