/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

public class FileTemplateDAOListener implements TemplateDAOListener {

    private static final Logger LOGGER = Logging.getLogger(FileTemplateDAOListener.class);

    @Override
    public void handleDeleteEvent(TemplateInfoEvent deleteEvent) {
        try {
            TemplateFileManager.get().delete(deleteEvent.getSource());
        } catch (Exception e) {
            LOGGER.warning(
                    "Exception while deleting template file in a TemplateInfo delete event scope. Execption is: "
                            + e.getMessage());
        }
    }

    @Override
    public void handleUpdateEvent(TemplateInfoEvent updateEvent) {
        // do nothing
    }
}
