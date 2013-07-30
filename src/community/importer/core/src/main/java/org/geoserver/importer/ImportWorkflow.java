package org.geoserver.importer;

import java.util.ArrayList;
import java.util.List;

/**
 * Combination of {@link ImportStep}s that form a particular import workflow. 
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class ImportWorkflow {

    List<ImportStep> steps = new ArrayList<ImportStep>();
}
