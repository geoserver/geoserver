/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.featurestemplating.expressions.MimeTypeFunction;

public class TemplateModelsValidator {

    public void validate(TemplateInfo info, String rawTemplate)
            throws TemplateConfigurationException {
        String templateName = info.getTemplateName();
        TemplateConfigurationException e = null;
        if (templateName == null || templateName.equals("")) {
            e = new TemplateConfigurationException();
            e.setId(TemplateConfigurationException.MISSING_TEMPLATE_NAME);
        } else if (info.getExtension() == null) {
            e = new TemplateConfigurationException();
            e.setId(TemplateConfigurationException.MISSING_FILE_EXTENSION);
        } else if (rawTemplate == null || rawTemplate.equals("")) {
            e = new TemplateConfigurationException();
            e.setId(TemplateConfigurationException.MISSING_TEMPLATE_CONTENT);
        }
        if (e != null) throw e;
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
        if (outputFormat == null) {
            if (cqlFilter == null || !cqlFilter.contains(MimeTypeFunction.NAME.getName())) {
                e = new TemplateConfigurationException();
                e.setId(TemplateConfigurationException.MISSING_RULE_OUTPUT_FORMAT);
            }
        } else if (templateName == null || templateName.trim().equals("")) {
            e = new TemplateConfigurationException();
            e.setId(TemplateConfigurationException.MISSING_RULE_TEMPLATE_NAME);
        }
        if (e != null) throw e;
    }
}
