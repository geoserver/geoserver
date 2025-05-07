package org.geoserver.featurestemplating.response;

import com.google.common.base.Charsets;
import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.schema.SchemaFileManager;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfo;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfoDAO;
import org.geoserver.featurestemplating.configuration.schema.SchemaRule;
import org.geoserver.featurestemplating.configuration.schema.SchemaRuleService;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.AbstractAppSchemaTestSupport;
import org.geoserver.test.FeatureChainingMockData;

/** Base class for tests that need to set up a schema override for a complex feature. */
public abstract class SchemaComplexTestSupport extends AbstractAppSchemaTestSupport {

    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new FeatureChainingMockData();
    }

    /**
     * Sets up a schema override for a complex feature type.
     *
     * @param cqlRuleCondition the CQL rule condition
     * @param profile the profile
     * @param outputFormat the output format
     * @param schemaFileName the schema file name
     * @param schemaName the schema name
     * @param schemaExtension the schema extension
     * @param workspace the workspace
     * @param ft the feature type info
     * @throws IOException if an error occurs while setting up the schema override
     */
    protected void setUpSchemaOverride(
            String cqlRuleCondition,
            String profile,
            SupportedFormat outputFormat,
            String schemaFileName,
            String schemaName,
            String schemaExtension,
            String workspace,
            FeatureTypeInfo ft)
            throws IOException {
        // setup the schema override
        String rawSchema =
                org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream(schemaFileName), Charsets.UTF_8);
        SchemaInfo info = new SchemaInfo();
        info.setExtension(schemaExtension);
        info.setSchemaName(schemaName);
        info.setWorkspace(workspace);
        info.setFeatureType(ft.getNativeName());
        SchemaInfoDAO.get().saveOrUpdate(info);
        SchemaFileManager.get().saveTemplateFile(info, rawSchema);
        // setup the rule
        SchemaRule rule = new SchemaRule();
        rule.setSchemaName(info.getFullName());
        rule.setCqlFilter(cqlRuleCondition);
        rule.setProfileFilter(profile);
        rule.setOutputFormat(outputFormat);
        rule.setSchemaIdentifier(info.getIdentifier());
        SchemaRuleService ruleService = new SchemaRuleService(ft);
        ruleService.saveRule(rule);
    }
}
