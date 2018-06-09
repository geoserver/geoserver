/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
