/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;

/**
 * Vector transform that is performed after an import has been completed.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public interface PostVectorTransform extends VectorTransform {

    void apply(ImportTask task, ImportData data) throws Exception;
}
