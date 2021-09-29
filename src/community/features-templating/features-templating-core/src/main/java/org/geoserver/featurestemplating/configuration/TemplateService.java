/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

/**
 * This class provides the business logic to handle save, update and delete operation on a template.
 */
public class TemplateService {

    private TemplateLoader loader;
    private TemplateFileManager templateFileManager;
    private TemplateInfoDAO templateInfoDAO;

    public TemplateService() {
        this.loader = TemplateLoader.get();
        this.templateFileManager = TemplateFileManager.get();
        this.templateInfoDAO = TemplateInfoDAO.get();
    }

    /**
     * Save or update a Template. Clean the cache from the previous template if necessary.
     *
     * @param templateInfo the Template Info to save or update.
     * @param rawTemplate the raw Template to save or update.
     */
    public void saveOrUpdate(TemplateInfo templateInfo, String rawTemplate) {
        templateFileManager.saveTemplateFile(templateInfo, rawTemplate);
        TemplateInfo current = templateInfoDAO.findById(templateInfo.getIdentifier());
        if (current != null && !current.getFullName().equals(templateInfo.getFullName())) {
            if (templateFileManager.delete(current)) {
                loader.removeAllWithIdentifier(templateInfo.getIdentifier());
            }
        }
        templateInfoDAO.saveOrUpdate(templateInfo);
    }

    /**
     * Delete a Template and removes it from the cache.
     *
     * @param templateInfo the Template Info object to delete.
     */
    public void delete(TemplateInfo templateInfo) {
        templateFileManager.delete(templateInfo);
        loader.removeAllWithIdentifier(templateInfo.getIdentifier());
        templateInfoDAO.delete(templateInfo);
    }
}
