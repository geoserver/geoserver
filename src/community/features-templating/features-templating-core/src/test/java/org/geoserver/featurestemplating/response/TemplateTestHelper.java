package org.geoserver.featurestemplating.response;

import com.google.common.base.Charsets;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.TemplateFileManager;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.featurestemplating.configuration.TemplateRuleService;

public class TemplateTestHelper {

    public void setUpTemplate(
            String cqlRuleCondition,
            SupportedFormat outputFormat,
            String templateFileName,
            String templateName,
            String templateExtension,
            String workspace,
            FeatureTypeInfo ft)
            throws IOException {
        setUpTemplate(
                cqlRuleCondition,
                null,
                outputFormat,
                templateFileName,
                templateName,
                templateExtension,
                workspace,
                ft);
    }

    public void setUpTemplate(
            String cqlRuleCondition,
            SupportedFormat outputFormat,
            InputStream template,
            String templateName,
            String templateExtension,
            String workspace,
            FeatureTypeInfo ft)
            throws IOException {
        setUpTemplate(
                cqlRuleCondition,
                null,
                outputFormat,
                template,
                templateName,
                templateExtension,
                workspace,
                ft);
    }

    public void setUpTemplate(
            String cqlRuleCondition,
            String profile,
            SupportedFormat outputFormat,
            String templateFileName,
            String templateName,
            String templateExtension,
            String workspace,
            FeatureTypeInfo ft)
            throws IOException {
        InputStream is = getClass().getResourceAsStream(templateFileName);
        setUpTemplate(
                cqlRuleCondition,
                profile,
                outputFormat,
                is,
                templateName,
                templateExtension,
                workspace,
                ft);
    }

    public void setUpTemplate(
            String cqlRuleCondition,
            String profile,
            SupportedFormat outputFormat,
            InputStream template,
            String templateName,
            String templateExtension,
            String workspace,
            FeatureTypeInfo ft)
            throws IOException {
        String rawTemplate = IOUtils.toString(template, Charsets.UTF_8);
        TemplateInfo info = new TemplateInfo();
        info.setExtension(templateExtension);
        info.setTemplateName(templateName);
        info.setWorkspace(workspace);
        info.setFeatureType(ft.getNativeName());
        TemplateInfoDAO.get().saveOrUpdate(info);
        TemplateFileManager.get().saveTemplateFile(info, rawTemplate);
        TemplateRule rule = new TemplateRule();
        rule.setTemplateName(info.getFullName());
        rule.setCqlFilter(cqlRuleCondition);
        rule.setProfileFilter(profile);
        rule.setOutputFormat(outputFormat);
        rule.setTemplateIdentifier(info.getIdentifier());
        TemplateRuleService ruleService = new TemplateRuleService(ft);
        ruleService.saveRule(rule);
    }
}
