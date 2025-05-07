/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

public class FileSchemaDAOListener implements SchemaDAOListener {

    private static final Logger LOGGER = Logging.getLogger(FileSchemaDAOListener.class);

    @Override
    public void handleDeleteEvent(SchemaInfoEvent deleteEvent) {
        try {
            SchemaFileManager.get().delete(deleteEvent.getSource());
        } catch (Exception e) {
            LOGGER.warning("Exception while deleting template file in a TemplateInfo delete event scope. Execption is: "
                    + e.getMessage());
        }
    }

    @Override
    public void handleUpdateEvent(SchemaInfoEvent updateEvent) {}
}
