package org.geoserver.importer.transform;

import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;

/**
 * Vector transform that is performed before input occurs.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public interface PreVectorTransform extends VectorTransform {

    void apply(ImportTask task, ImportData data) throws Exception;
}
