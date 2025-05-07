/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web.schema;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.core.util.string.JavaScriptUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.featurestemplating.configuration.schema.FileSchemaDAOListener;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfo;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfoDAO;
import org.geoserver.featurestemplating.configuration.schema.SchemaService;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class SchemaInfoPage extends GeoServerSecuredPage {

    private GeoServerTablePanel<SchemaInfo> tablePanel;

    private GeoServerTablePanel<SchemaInfo> schemaPanel;

    private GeoServerTablePanel<SchemaInfo> schemaTablePanel;

    private AjaxLink<Object> remove;

    public SchemaInfoPage() {
        addFeatureTeplateSection();
        addSchemaDefinitionSection();
    }

    private void addFeatureTeplateSection() {
        add(new AjaxLink<Object>("addNew") {

            private static final long serialVersionUID = -4136656891019857299L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new SchemaConfigurationPage(new Model<>(new SchemaInfo()), true));
            }
        });

        add(remove = newRemoveLink());

        tablePanel = new GeoServerTablePanel<SchemaInfo>("tablePanel", new SchemaInfoProvider(), true) {
            @Override
            protected Component getComponentForProperty(
                    String id, IModel<SchemaInfo> itemModel, GeoServerDataProvider.Property<SchemaInfo> property) {
                if (property.equals(SchemaInfoProvider.NAME)) {
                    return new SimpleAjaxLink<SchemaInfo>(id, itemModel, SchemaInfoProvider.NAME.getModel(itemModel)) {

                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            setResponsePage(new SchemaConfigurationPage(getModel(), false));
                        }
                    };
                } else if (property.equals(SchemaInfoProvider.EXTENSION))
                    return new Label(id, SchemaInfoProvider.EXTENSION.getModel(itemModel));
                else if (property.equals(SchemaInfoProvider.WORKSPACE))
                    return new Label(id, SchemaInfoProvider.WORKSPACE.getModel(itemModel));
                else if (property.equals(SchemaInfoProvider.FEATURE_TYPE_INFO)) {
                    return new Label(id, SchemaInfoProvider.FEATURE_TYPE_INFO.getModel(itemModel));
                }
                return null;
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                remove.setEnabled(tablePanel.getSelection().size() > 0);
                target.add(remove);
            }
        };
        tablePanel.setOutputMarkupId(true);
        tablePanel.setEnabled(true);
        add(tablePanel);
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);
        SchemaInfoDAO.get().addTemplateListener(new FileSchemaDAOListener());
    }

    private void addSchemaDefinitionSection() {
        add(new AjaxLink<Object>("addNewSchema") {

            private static final long serialVersionUID = -4136656891019857299L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new SchemaConfigurationPage(new Model<>(new SchemaInfo()), true));
            }
        });
    }

    private AjaxLink<Object> newRemoveSchemaLink() {
        return new AjaxLink<Object>("removeSelectedSchema") {
            private static final long serialVersionUID = 2421854498051377608L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                SchemaService service = new SchemaService();
                List<SchemaInfo> schemaInfos = schemaPanel.getSelection();
                schemaInfos.forEach(ti -> service.delete(ti));
                tablePanel.modelChanged();
                target.add(tablePanel);
                target.add(SchemaInfoPage.this);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                AjaxCallListener ajaxCall = new AjaxCallListener() {

                    @Override
                    public CharSequence getPrecondition(Component component) {
                        CharSequence message = new ParamResourceModel("confirmRemove", SchemaInfoPage.this).getString();
                        message = JavaScriptUtils.escapeQuotes(message);
                        return "return confirm('" + message + "');";
                    }
                };
                attributes.getAjaxCallListeners().add(ajaxCall);
            }
        };
    }

    private AjaxLink<Object> newRemoveLink() {
        return new AjaxLink<Object>("removeSelected") {
            private static final long serialVersionUID = 2421854498051377608L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                SchemaService service = new SchemaService();
                List<SchemaInfo> schemaInfos = tablePanel.getSelection();
                schemaInfos.forEach(si -> service.delete(si));
                tablePanel.modelChanged();
                target.add(tablePanel);
                target.add(SchemaInfoPage.this);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                AjaxCallListener ajaxCall = new AjaxCallListener() {

                    @Override
                    public CharSequence getPrecondition(Component component) {
                        CharSequence message = new ParamResourceModel("confirmRemove", SchemaInfoPage.this).getString();
                        message = JavaScriptUtils.escapeQuotes(message);
                        return "return confirm('" + message + "');";
                    }
                };
                attributes.getAjaxCallListeners().add(ajaxCall);
            }
        };
    }
}
