/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wms.eo.web;

import java.util.logging.Level;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.data.layergroup.LayerGroupPage;
import org.geoserver.web.data.store.StorePage;
import org.geoserver.wms.eo.EoCatalogBuilder;

/**
 * Wicket page to delete a WMS-EO layer group.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class WmsEoDeleteGroupPage extends EoPage {

    public WmsEoDeleteGroupPage() {
        IModel<WmsEoAddLayerModel> model = new Model<WmsEoAddLayerModel>(new WmsEoAddLayerModel());

        // build the form
        Form<WmsEoAddLayerModel> paramsForm = new Form<WmsEoAddLayerModel>("deleteGroupForm", model);
        add(paramsForm);

        LayerGroupPanel groupPanel = new LayerGroupPanel("groupPanel",
                new PropertyModel<LayerGroupInfo>(model, "group"), new ResourceModel("group",
                        "WMS-EO Group"), true, new LayerGroupInfoFilter() {
                    @Override
                    public boolean accept(LayerGroupInfo group) {
                        return LayerGroupInfo.Mode.EO.equals(group.getMode());
                    }
                });
        paramsForm.add(groupPanel);

        // cancel / submit buttons
        AjaxSubmitLink submitLink = deleteLink(paramsForm);
        paramsForm.add(new BookmarkablePageLink<StorePage>("cancel", WmsEoDeleteGroupPage.class));
        paramsForm.add(submitLink);
        paramsForm.setDefaultButton(submitLink);

        // feedback panel for error messages
        paramsForm.add(new FeedbackPanel("feedback"));
    }

    private AjaxSubmitLink deleteLink(Form<WmsEoAddLayerModel> paramsForm) {
        return new AjaxSubmitLink("delete", paramsForm) {
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> paramsForm) {
                super.onError(target, paramsForm);
                target.addComponent(paramsForm);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> paramsForm) {
                WmsEoAddLayerModel model = (WmsEoAddLayerModel) paramsForm.getModelObject();
                LayerGroupInfo group = model.getGroup();

                EoCatalogBuilder builder = new EoCatalogBuilder(getCatalog());
                try {
                    builder.delete(group);
                    setResponsePage(new LayerGroupPage());
                } catch (RuntimeException e) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.log(Level.INFO, e.getMessage(), e);
                    }

                    paramsForm.error(e.getMessage());
                    target.addComponent(paramsForm);
                } catch (Exception e) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.log(Level.INFO, e.getMessage(), e);
                    }

                    paramsForm.error(e.getMessage());
                    target.addComponent(paramsForm);
                }
            }
        };
    }
}