/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;

/** Helper class that provides methods to manage the template file. */
public class TemplateFileManager extends FileManagerBase<TemplateInfo> {

    public TemplateFileManager(Catalog catalog, GeoServerDataDirectory dd) {
        super(catalog, dd);
    }

    /** @return the singleton instance of this class. */
    public static TemplateFileManager get() {
        return GeoServerExtensions.bean(TemplateFileManager.class);
    }

    @Override
    protected String getFeatureType(TemplateInfo info) {
        return info.getFeatureType();
    }

    @Override
    protected String getWorkspace(TemplateInfo info) {
        return info.getWorkspace();
    }

    @Override
    protected String getName(TemplateInfo info) {
        return info.getTemplateName();
    }

    @Override
    protected String getExtension(TemplateInfo info) {
        return info.getExtension();
    }

    @Override
    protected String getDir() {
        return TemplateInfoDAOImpl.TEMPLATE_DIR;
    }

    @Override
    protected String getFileType() {
        return "template";
    }

    /**
     * Return a {@link Resource} from a template info.
     *
     * @param templateInfo the template info for which we want to retrieve the corresponding resource.
     * @return the resource that corresponds to the template info.
     */
    public Resource getTemplateResource(TemplateInfo templateInfo) {
        return getResource(templateInfo);
    }

    /**
     * Delete the template file associated to the template info passed as an argument.
     *
     * @param templateInfo the templateInfo for which we want to delete the corresponding template file.
     * @return true if the delete process was successful false otherwise.
     */
    public boolean delete(TemplateInfo templateInfo) {
        return super.delete(templateInfo);
    }

    /**
     * Return the directory where the template file is as a File object.
     *
     * @param templateInfo the template info to which the desired template file is associated.
     * @return the directoryu where the template file associated to the templateInfo is placed.
     */
    public File getTemplateLocation(TemplateInfo templateInfo) {
        return getLocation(templateInfo);
    }

    /**
     * Save a template in string form to the directory defined for the template info object.
     *
     * @param templateInfo the template info object.
     * @param rawTemplate the template content to save to a file.
     */
    public void saveTemplateFile(TemplateInfo templateInfo, String rawTemplate) {
        saveFile(templateInfo, rawTemplate);
    }
}
