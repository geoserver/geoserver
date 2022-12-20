package org.geoserver.taskmanager.schedule.impl;

import java.util.logging.Logger;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

@Service
public class BatchJobServiceStarter implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = Logging.getLogger(BatchJobServiceStarter.class);

    @Autowired private BatchJobService batchJobService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // call only once at start-up, so not for child contexts.
        if (event.getApplicationContext().getParent() == null) {
            if (batchJobService.isInit()) {
                batchJobService.reloadFromData();
                batchJobService.closeInactiveBatchruns();
            } else {
                LOGGER.info("Skipping initialization as specified in configuration.");
            }

            batchJobService.startup();
        }
    }
}
