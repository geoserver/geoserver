/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wps.ProcessEvent;
import org.geoserver.wps.ProcessListenerAdapter;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.executor.ExecutionStatus;
import org.junit.Assert;

public class RasterCleanerProcessListener extends ProcessListenerAdapter {

    private final RasterCleaner cleaner;
    Map<String, Integer> cleanerStatus = new HashMap<>();

    public RasterCleanerProcessListener(RasterCleaner cleaner) {
        this.cleaner = cleaner;
    }

    @Override
    public void succeeded(ProcessEvent event) throws WPSException {
        Integer count = cleanerStatus.get(event.getStatus().getExecutionId());
        if (count != null && count != 0) {
            Assert.fail("RasterCleaner did not clean up all images");
        }
    }

    @Override
    public void progress(ProcessEvent event) throws WPSException {
        ExecutionStatus status = event.getStatus();
        String processName = status.getProcessName().getLocalPart();
        if (("DownloadMap".equals(processName) || "DownloadAnimation".equals(processName))) {
            cleanerStatus.put(
                    status.getExecutionId(),
                    collectionSize(cleaner.getImages()) + collectionSize(cleaner.getCoverages()));
        }
    }

    private Integer collectionSize(List<? extends Object> images) {
        return Optional.ofNullable(images).map(Collection::size).orElse(0);
    }
}
