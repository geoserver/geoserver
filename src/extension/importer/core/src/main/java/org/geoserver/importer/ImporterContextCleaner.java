/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.Date;
import java.util.TimerTask;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

public class ImporterContextCleaner extends TimerTask {

    static final Logger LOGGER = Logging.getLogger(ImporterContextCleaner.class);

    Importer importer;

    public ImporterContextCleaner(Importer importer) {
        this.importer = importer;
    }

    @Override
    public void run() {
        ImportStore store = importer.getStore();
        ImporterInfo config = importer.getConfiguration();

        // nothing to do in case the expiry has been cancelled
        double expiryMinutes = config.getContextExpiration();
        if (expiryMinutes <= 0) return;
        double expiryMillis = expiryMinutes * 60 * 1000;
        long now = System.currentTimeMillis();

        LOGGER.fine(
                () ->
                        "Cleaning up import contexts not updated in the last "
                                + expiryMinutes
                                + " minutes");

        store.query(
                context -> {
                    // skip RUNNING contexts
                    if (context.getState() == ImportContext.State.RUNNING) return;

                    // check if they have expired
                    Date updated = context.getUpdated();
                    if (updated != null) {
                        long diffMilliseconds = now - updated.getTime();
                        if (diffMilliseconds > expiryMillis) {
                            LOGGER.fine(() -> "Cleaning up import context " + context.getId());
                            store.remove(context);
                        }
                    }
                });
    }
}
