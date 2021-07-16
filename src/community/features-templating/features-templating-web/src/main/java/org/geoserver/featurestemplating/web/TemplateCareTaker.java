/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.featurestemplating.configuration.TemplateFileManager;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDao;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 * This class provides functionality to undo operation performed on TemplateInfo, by storing {@link
 * TemplateInfoMemento} with previous state of a template. At the moment it is possible to store
 * only one memento object for TemplateInfo.
 */
class TemplateCareTaker {

    static final Logger LOGGER = Logging.getLogger(TemplateInfoMemento.class);

    private Map<String, TemplateInfoMemento> mementoMap;
    private TemplateFileManager fileManager;

    TemplateCareTaker() {
        this.mementoMap = new HashMap<>();
        this.fileManager = GeoServerExtensions.bean(TemplateFileManager.class);
    }

    /**
     * Delete the old template file if its new location is different from the one stored in the
     * memento object.
     *
     * @param info the TemplateInfo object with updated data.
     */
    void deleteOldTemplateFile(TemplateInfo info) {
        String identifier = info.getIdentifier();
        TemplateInfoMemento memento = mementoMap.get(identifier);
        if (memento == null) {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(
                        Level.WARNING,
                        "Cannot delete old template file, "
                                + "something went wrong when saving the previous info state");
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "Deleting template file for template with name "
                                + memento.getTemplateName());
            }
            if (!memento.lenientEquals(info)) {
                boolean result = fileManager.delete(memento);
                if (!result) {
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(
                                Level.WARNING,
                                "Cannot delete old template file, something went wrong during the delete process");
                }
            }
        }
    }

    /**
     * Restore the TemplateInfo to the state registered in the memento map. This method eventually
     * move the template file from the current position to the previous one if different.
     *
     * @param info the TemplateInfo to be restored.
     */
    void undo(TemplateInfo info, boolean deleteNew) {
        String identifier = info.getIdentifier();
        TemplateInfoMemento memento = mementoMap.get(identifier);
        if (memento == null) {
            if (!deleteNew) {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(
                            Level.WARNING,
                            "Cannot undo operation performed on template with name "
                                    + info.getTemplateName()
                                    + " something went wrong when saving the previous state");
            } else {
                fileManager.delete(info);
                TemplateInfoDao.get().delete(info);
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "Undoing modifications for templateInfo " + memento.getTemplateName());
            }
            TemplateInfo restored = new TemplateInfo(memento);
            TemplateInfoDao.get().saveOrUpdate(restored);
            fileManager.delete(info);
            fileManager.saveTemplateFile(memento, memento.getRawTemplate());
        }
    }

    /**
     * Add a memento object.
     *
     * @param templateInfo the TemplateInfo object to store in the memento map.
     * @param rawTemplate the template content as a string.
     */
    void addMemento(TemplateInfo templateInfo, String rawTemplate) {
        TemplateInfoMemento memento = new TemplateInfoMemento(templateInfo, rawTemplate);
        mementoMap.put(templateInfo.getIdentifier(), memento);
    }
}
