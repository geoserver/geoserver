/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.GeoServerExtensions;

/** Base interface for TemplateInfo Data Access. */
public interface TemplateInfoDAO {

    static TemplateInfoDAOImpl get() {
        return GeoServerExtensions.bean(TemplateInfoDAOImpl.class);
    }

    public static final String TEMPLATE_DIR = "features-templating";

    /** @return all the saved template info. */
    public List<TemplateInfo> findAll();

    /**
     * Find all the template info that can be used for the FeatureTypeInfo. It means that all the
     * templates that are in the global directory plus all the templates in the workspace directory
     * to which the FeatureTypeInfo belongs plus all the templates in the FeatureTypeInfo directory
     * will be returned.
     *
     * @param featureTypeInfo
     * @return
     */
    public List<TemplateInfo> findByFeatureTypeInfo(FeatureTypeInfo featureTypeInfo);

    /**
     * @param id the identifier of the template info to retrieve.
     * @return the TemplateInfo object.
     */
    public TemplateInfo findById(String id);

    /**
     * Save or update the template info.
     *
     * @param templateData the template to save or update.
     * @return the template save or updated.
     */
    public TemplateInfo saveOrUpdate(TemplateInfo templateData);

    /**
     * Delete all the template info in the list.
     *
     * @param templateInfos list of template info to delete.
     */
    public void delete(List<TemplateInfo> templateInfos);

    /** @param templateData the template info to delete. */
    public void delete(TemplateInfo templateData);

    /** Deletes all the template info. */
    public void deleteAll();

    /**
     * Add a listener.
     *
     * @param listener the listener to add.
     */
    public void addTemplateListener(TemplateDAOListener listener);

    /**
     * Find a TemplateInfo from full name. By full name is meant the name of the template file
     * preceded by the workspace name and the feature type name if defined for the template. Format
     * for a full name is like the following: templateName or workspaceName:templateName or
     * workspaceName:featureTypeName:templateName
     *
     * @param fullName the full name of the template.
     * @return the corresponding TemplateInfo.
     */
    public TemplateInfo findByFullName(String fullName);
}
