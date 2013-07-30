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
