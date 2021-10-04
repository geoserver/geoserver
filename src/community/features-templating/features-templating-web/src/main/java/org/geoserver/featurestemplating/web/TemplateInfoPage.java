/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

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
import org.geoserver.featurestemplating.configuration.FileTemplateDAOListener;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.TemplateService;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class TemplateInfoPage extends GeoServerSecuredPage {

    private GeoServerTablePanel<TemplateInfo> tablePanel;

    private AjaxLink<Object> remove;

    public TemplateInfoPage() {
        add(
                new AjaxLink<Object>("addNew") {

                    private static final long serialVersionUID = -4136656891019857299L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setResponsePage(
                                new TemplateConfigurationPage(
                                        new Model<>(new TemplateInfo()), true));
                    }
                });

        add(remove = newRemoveLink());

        tablePanel =
                new GeoServerTablePanel<TemplateInfo>(
                        "tablePanel", new TemplateInfoProvider(), true) {
                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<TemplateInfo> itemModel,
                            GeoServerDataProvider.Property<TemplateInfo> property) {
                        if (property.equals(TemplateInfoProvider.NAME)) {
                            return new SimpleAjaxLink<TemplateInfo>(
                                    id, itemModel, TemplateInfoProvider.NAME.getModel(itemModel)) {

                                @Override
                                protected void onClick(AjaxRequestTarget target) {
                                    setResponsePage(
                                            new TemplateConfigurationPage(getModel(), false));
                                }
                            };
                        } else if (property.equals(TemplateInfoProvider.EXTENSION))
                            return new Label(
                                    id, TemplateInfoProvider.EXTENSION.getModel(itemModel));
                        else if (property.equals(TemplateInfoProvider.WORKSPACE))
                            return new Label(
                                    id, TemplateInfoProvider.WORKSPACE.getModel(itemModel));
                        else if (property.equals(TemplateInfoProvider.FEATURE_TYPE_INFO)) {
                            return new Label(
                                    id, TemplateInfoProvider.FEATURE_TYPE_INFO.getModel(itemModel));
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
        TemplateInfoDAO.get().addTemplateListener(new FileTemplateDAOListener());
    }

    private AjaxLink<Object> newRemoveLink() {
        return new AjaxLink<Object>("removeSelected") {
            private static final long serialVersionUID = 2421854498051377608L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                TemplateService service = new TemplateService();
                List<TemplateInfo> templates = tablePanel.getSelection();
                templates.forEach(ti -> service.delete(ti));
                tablePanel.modelChanged();
                target.add(tablePanel);
                target.add(TemplateInfoPage.this);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                AjaxCallListener ajaxCall =
                        new AjaxCallListener() {

                            @Override
                            public CharSequence getPrecondition(Component component) {
                                CharSequence message =
                                        new ParamResourceModel(
                                                        "confirmRemove", TemplateInfoPage.this)
                                                .getString();
                                message = JavaScriptUtils.escapeQuotes(message);
                                return "return confirm('" + message + "');";
                            }
                        };
                attributes.getAjaxCallListeners().add(ajaxCall);
            }
        };
    }
}
