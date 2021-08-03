/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import java.util.List;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.featurestemplating.expressions.MimeTypeFunction;

public class TemplateModelsValidator {

    public void validate(TemplateInfo info) throws TemplateConfigurationException {
        TemplateInfo ti = TemplateInfoDAO.get().findByFullName(info.getFullName());
        if (ti != null && !ti.getIdentifier().equals(info.getIdentifier())) {
            TemplateConfigurationException e = new TemplateConfigurationException();
            e.setId(TemplateConfigurationException.DUPLICATE_TEMPLATE_NAME);
            throw e;
        }
    }

    public void validate(TemplatePreviewPanel.PreviewInfoModel info)
            throws TemplateConfigurationException {
        SupportedFormat outputFormat = info.getOutputFormat();
        TemplateConfigurationException e = null;
        if (outputFormat == null) {
            e = new TemplateConfigurationException();
            e.setId(TemplateConfigurationException.MISSING_PREVIEW_OUTPUT_FORMAT);
        } else if (info.getFeatureType() == null) {
            e = new TemplateConfigurationException();
            e.setId(TemplateConfigurationException.MISSING_PREVIEW_FEATURE_TYPE);
        } else if (info.getWs() == null) {
            e = new TemplateConfigurationException();
            e.setId(TemplateConfigurationException.MISSING_PREVIEW_WORKSPACE);
        }
        if (e != null) throw e;
    }

    public void validate(TemplateRule rule) throws TemplateConfigurationException {
        String templateName = rule.getTemplateName();
        SupportedFormat outputFormat = rule.getOutputFormat();
        String cqlFilter = rule.getCqlFilter();
        TemplateConfigurationException e = null;
        boolean nameNullOrVoid = templateName == null || templateName.trim().equals("");
        if (outputFormat == null) {
            if (cqlFilter == null || !cqlFilter.contains(MimeTypeFunction.NAME.getName())) {
                e = new TemplateConfigurationException();
                e.setId(TemplateConfigurationException.MISSING_RULE_OUTPUT_FORMAT);
            }
        } else if (nameNullOrVoid) {
            e = new TemplateConfigurationException();
            e.setId(TemplateConfigurationException.MISSING_RULE_TEMPLATE_NAME);
        } else {
            TemplateInfo info = TemplateInfoDAO.get().findById(rule.getTemplateIdentifier());
            String extension = info.getExtension();
            List<SupportedFormat> formats = SupportedFormat.getByExtension(extension);
            if (!formats.contains(rule.getOutputFormat())) {
                e = new TemplateConfigurationException();
                e.setId(TemplateConfigurationException.INCOMPATIBLE_OUTPUT_FORMAT);
            }
        }
        if (e != null) throw e;
    }
}
