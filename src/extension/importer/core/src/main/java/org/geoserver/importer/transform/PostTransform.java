/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;

/**
 * Transform that is performed after an import has been completed.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface PostTransform extends ImportTransform {

    void apply(ImportTask task, ImportData data) throws Exception;
}
