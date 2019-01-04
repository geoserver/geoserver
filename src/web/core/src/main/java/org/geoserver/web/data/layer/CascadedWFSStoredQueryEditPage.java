/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import net.opengis.wfs20.ParameterExpressionType;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Fragment;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMapping;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingBlockValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingDefaultValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingExpressionValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.StoredQueryConfiguration;

public class CascadedWFSStoredQueryEditPage extends CascadedWFSStoredQueryAbstractPage {

    /** serialVersionUID */
    private static final long serialVersionUID = 7254877970765799559L;

    private ResourceConfigurationPage previousPage;

    private FeatureTypeInfo editableType;

    private StoredQueryConfiguration configuration;
    private String storedQueryId;

    public CascadedWFSStoredQueryEditPage(
            FeatureTypeInfo type, ResourceConfigurationPage previousPage) throws IOException {
        super(type.getStore().getWorkspace().getName(), type.getStore().getName(), type.getName());

        this.editableType = type;

        this.configuration =
                (StoredQueryConfiguration)
                        type.getMetadata().get(FeatureTypeInfo.STORED_QUERY_CONFIGURATION);

        this.storedQueryId = this.configuration.getStoredQueryId();

        this.previousPage = previousPage;

        parameterProvider.refreshItems(configuration.getStoredQueryId());
    }

    @Override
    public void populateStoredQueryParameterAttribute(
            String storedQueryId, ParameterExpressionType pet, StoredQueryParameterAttribute attr) {
        // Sanity check
        if (!storedQueryId.equals(configuration.getStoredQueryId())) {
            throw new RuntimeException(
                    "Programming error! Stored query ids do not match: '"
                            + storedQueryId
                            + "' vs '"
                            + configuration.getStoredQueryId()
                            + "'");
        }

        ParameterMapping mapping = null;
        if (configuration.getStoredQueryParameterMappings() != null) {
            for (ParameterMapping pm : configuration.getStoredQueryParameterMappings()) {
                if (pm.getParameterName().equals(pet.getName())) {
                    mapping = pm;
                    break;
                }
            }
        }

        if (mapping == null) {
            // No current mapping, return no mapping
            attr.setMappingType(ParameterMappingType.NONE);
            attr.setValue(null);
        } else {
            // Convert mapping to UI model
            ParameterMappingType type = ParameterMappingType.NONE;
            String value = null;

            if (mapping instanceof ParameterMappingBlockValue) {
                type = ParameterMappingType.BLOCKED;
            } else if (mapping instanceof ParameterMappingDefaultValue) {
                ParameterMappingDefaultValue pmdv = (ParameterMappingDefaultValue) mapping;
                if (pmdv.isForcible()) {
                    type = ParameterMappingType.STATIC;
                } else {
                    type = ParameterMappingType.DEFAULT;
                }
                value = pmdv.getDefaultValue();
            } else if (mapping instanceof ParameterMappingExpressionValue) {
                ParameterMappingExpressionValue pmev = (ParameterMappingExpressionValue) mapping;
                if (pmev.getExpressionLanguage().equals("CQL")) {
                    type = ParameterMappingType.EXPRESSION_CQL;
                    value = pmev.getExpression();
                }
            }

            attr.setMappingType(type);
            attr.setValue(value);
        }
    }

    @Override
    protected Component getStoredQueryNameComponent() {
        return new Fragment("storedQueryName", "editPage", this);
    }

    @Override
    protected void onSave() {
        StoredQueryConfiguration config =
                createStoredQueryConfiguration(parameterProvider.getItems(), storedQueryId);

        editableType.getMetadata().put(FeatureTypeInfo.STORED_QUERY_CONFIGURATION, config);

        setResponsePage(previousPage);
    }

    @Override
    protected void onCancel() {
        setResponsePage(previousPage);
    }
}
