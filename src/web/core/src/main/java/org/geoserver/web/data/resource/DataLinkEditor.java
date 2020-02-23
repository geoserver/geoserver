/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DataLinkInfoImpl;

/**
 * Shows and allows editing of the {@link DataLinkInfo} attached to a {@link ResourceInfo}
 *
 * @author Marcus Sen - British Geological Survey
 */
@SuppressWarnings("serial")
public class DataLinkEditor extends Panel {

    private ListView<DataLinkInfo> links;
    private Label noData;
    private WebMarkupContainer table;

    /** @param resourceModel Must return a {@link ResourceInfo} */
    public DataLinkEditor(String id, final IModel<ResourceInfo> resourceModel) {
        super(id, resourceModel);

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // the link list
        table = new WebMarkupContainer("table");
        table.setOutputMarkupId(true);
        container.add(table);
        links =
                new ListView<DataLinkInfo>(
                        "links",
                        new PropertyModel<List<DataLinkInfo>>(resourceModel, "dataLinks")) {

                    @Override
                    protected void populateItem(ListItem<DataLinkInfo> item) {

                        // odd/even style
                        item.add(
                                AttributeModifier.replace(
                                        "class", item.getIndex() % 2 == 0 ? "even" : "odd"));

                        // link info
                        FormComponentFeedbackBorder urlBorder =
                                new FormComponentFeedbackBorder("urlBorder");
                        item.add(urlBorder);
                        TextField<String> format =
                                new TextField<>(
                                        "format",
                                        new PropertyModel<String>(item.getModel(), "type"));
                        format.setRequired(true);
                        item.add(format);
                        TextField<String> url =
                                new TextField<>(
                                        "dataLinkURL",
                                        new PropertyModel<String>(item.getModel(), "content"));
                        url.add(new UrlValidator());
                        url.setRequired(true);
                        urlBorder.add(url);

                        // remove link
                        AjaxLink<DataLinkInfo> link =
                                new AjaxLink<DataLinkInfo>("removeLink", item.getModel()) {

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        ResourceInfo ri = (ResourceInfo) resourceModel.getObject();
                                        ri.getDataLinks().remove(getModelObject());
                                        updateLinksVisibility();
                                        target.add(container);
                                    }
                                };
                        item.add(link);
                    }
                };
        // this is necessary to avoid loosing item contents on edit/validation checks
        links.setReuseItems(true);
        table.add(links);

        // the no data links label
        noData = new Label("noLinks", new ResourceModel("noDataLinksSoFar"));
        container.add(noData);
        updateLinksVisibility();

        // add new link button
        AjaxButton button =
                new AjaxButton("addlink") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        ResourceInfo ri = (ResourceInfo) resourceModel.getObject();
                        DataLinkInfo link = ri.getCatalog().getFactory().createDataLink();
                        link.setType("text/plain");
                        ri.getDataLinks().add(link);
                        updateLinksVisibility();

                        target.add(container);
                    }
                };
        add(button);
    }

    private void updateLinksVisibility() {
        ResourceInfo ri = (ResourceInfo) getDefaultModelObject();
        boolean anyLink = ri.getDataLinks().size() > 0;
        table.setVisible(anyLink);
        noData.setVisible(!anyLink);
    }

    public class UrlValidator implements IValidator<String> {

        @Override
        public void validate(IValidatable<String> validatable) {
            String url = validatable.getValue();
            if (url != null) {
                try {
                    DataLinkInfoImpl.validate(url);
                } catch (IllegalArgumentException ex) {
                    IValidationError err =
                            new ValidationError("invalidDataLinkURL")
                                    .addKey("invalidDataLinkURL")
                                    .setVariable("url", url);
                    validatable.error(err);
                }
            }
        }
    }
}
