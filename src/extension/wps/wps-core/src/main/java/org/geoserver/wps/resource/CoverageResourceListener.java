/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wps.ProcessListenerAdapter;
import org.geoserver.wps.ProcessEvent;
import org.geoserver.wps.WPSException;
import org.opengis.coverage.grid.GridCoverage;

/**
 * Listens to progress events and makes sure the input/output coverages are marked as resources to
 * clean up (to release readers and thus file system locks and associated tiles in the tile cache)
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CoverageResourceListener extends ProcessListenerAdapter {

    /**
     * A simple check to assess whether the
     * 
     * @author Andrea Aime - GeoSolutions
     */
    private static class ResourceStatus {
        boolean inputChecked;

        boolean outputChecked;
    }

    WPSResourceManager resourceManager;
    CoverageCleanerCallback  cleaner;
    
    Map<String, ResourceStatus> resourceStates = new ConcurrentHashMap<>();

    public CoverageResourceListener(WPSResourceManager resourceManager, CoverageCleanerCallback cleaner) {
        this.resourceManager = resourceManager;
        this.cleaner = cleaner;
    }

    @Override
    public void progress(ProcessEvent event) throws WPSException {
        if (event.getInputs() == null) {
            return;
        }

        checkInputOutput(event);
    }

    private void checkInputOutput(ProcessEvent event) {
        String executionId = event.getStatus().getExecutionId();

        // check if we have the status, and if inputs have already been checked
        ResourceStatus status = resourceStates.get(executionId);
        if (status == null) {
            status = new ResourceStatus();
            resourceStates.put(executionId, status);
        }
        if (!status.inputChecked) {
            for (Object input : event.getInputs().values()) {
                if (input instanceof GridCoverage) {
                    resourceManager.addResource(new GridCoverageResource(((GridCoverage) input)));
                }
            }
            status.inputChecked = true;
        }

        // check if the outputs are available have already been checked
        if (!status.outputChecked && event.getOutputs() != null) {
            for (Object output : event.getOutputs().values()) {
                if (output instanceof GridCoverage) {
                    resourceManager.addResource(new GridCoverageResource(((GridCoverage) output)));
                }
            }
            status.outputChecked = true;
        }
    }

    @Override
    public void succeeded(ProcessEvent event) throws WPSException {
        cleanResourceStatus(event);
    }

    @Override
    public void dismissed(ProcessEvent event) throws WPSException {
        cleanResourceStatus(event);
    }

    @Override
    public void failed(ProcessEvent event) {
        cleanResourceStatus(event);
        cleaner.clean();
    }

    private void cleanResourceStatus(ProcessEvent event) {
        // just in case we have partially processed inputs and the process ended before
        // even starting, check the inputs and outputs
        checkInputOutput(event);
        // and now remove the ResourceStatus from the map, we're done
        resourceStates.remove(event.getStatus().getExecutionId());
    }
}
