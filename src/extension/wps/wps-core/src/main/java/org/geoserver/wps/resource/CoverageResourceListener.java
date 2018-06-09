/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wps.ProcessEvent;
import org.geoserver.wps.ProcessListenerAdapter;
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
        final Set<String> inputsChecked = new HashSet<>();

        final Set<String> outputsChecked = new HashSet<>();
    }

    WPSResourceManager resourceManager;
    CoverageCleanerCallback cleaner;

    Map<String, ResourceStatus> resourceStates = new ConcurrentHashMap<>();

    public CoverageResourceListener(
            WPSResourceManager resourceManager, CoverageCleanerCallback cleaner) {
        this.resourceManager = resourceManager;
        this.cleaner = cleaner;
    }

    @Override
    public void progress(ProcessEvent event) throws WPSException {
        checkInputOutput(event);
    }

    private void checkInputOutput(ProcessEvent event) {
        Map<String, Object> inputs = event.getInputs();
        Map<String, Object> outputs = event.getOutputs();
        if (((inputs == null) || inputs.isEmpty()) && ((outputs == null) || outputs.isEmpty())) {
            return;
        }
        // check if we have the status
        String executionId = event.getStatus().getExecutionId();
        ResourceStatus status = resourceStates.get(executionId);
        if (status == null) {
            status = new ResourceStatus();
            resourceStates.put(executionId, status);
        }

        // check if the available inputs have already been checked
        Set<String> inputsChecked = status.inputsChecked;
        if ((inputs != null) && (inputsChecked.size() < inputs.size())) {
            for (Entry<String, Object> entry : inputs.entrySet()) {
                Object input = entry.getValue();
                if ((input != null)
                        && inputsChecked.add(entry.getKey())
                        && (input instanceof GridCoverage)) {
                    resourceManager.addResource(new GridCoverageResource((GridCoverage) input));
                }
            }
        }

        // check if the available outputs have already been checked
        Set<String> outputsChecked = status.outputsChecked;
        if ((outputs != null) && (outputsChecked.size() < outputs.size())) {
            for (Entry<String, Object> entry : outputs.entrySet()) {
                Object output = entry.getValue();
                if ((output != null)
                        && outputsChecked.add(entry.getKey())
                        && (output instanceof GridCoverage)) {
                    resourceManager.addResource(new GridCoverageResource((GridCoverage) output));
                }
            }
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
