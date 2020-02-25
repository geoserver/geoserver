/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.wms.WMSInfo;
import org.springframework.util.Assert;

/**
 * Shows and allows editing of the {@link AuthorityURLInfo} attached to a {@link WMSInfo}, a {@link
 * LayerInfo}, or a {@link LayerGroupInfo}.
 *
 * @author groldan
 */
public class AuthorityURLListEditor extends FormComponentPanel<List<AuthorityURLInfo>> {

    private static final long serialVersionUID = 5098470663723800345L;

    private ListView<AuthorityURLInfo> authorityURLs;

    private Label noMetadata;

    private WebMarkupContainer table;

    /**
     * @see WMSInfo#getAuthorityURLs()
     * @see LayerInfo#getAuthorityURLs()
     * @see LayerGroupInfo#getAuthorityURLs()
     */
    public AuthorityURLListEditor(final String id, final IModel<List<AuthorityURLInfo>> list) {
        super(id, list);
        Assert.notNull(list.getObject(), "The list cannot be null");

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // the link list
        table = new WebMarkupContainer("table");
        table.setOutputMarkupId(true);
        container.add(table);
        authorityURLs =
                new ListView<AuthorityURLInfo>(
                        "authorities", new ArrayList<AuthorityURLInfo>(list.getObject())) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(final ListItem<AuthorityURLInfo> item) {
                        // odd/even style
                        item.add(
                                AttributeModifier.replace(
                                        "class", item.getIndex() % 2 == 0 ? "even" : "odd"));

                        // Authority name
                        TextField<String> authName;
                        authName =
                                new TextField<String>(
                                        "authName",
                                        new PropertyModel<String>(item.getModel(), "name"));
                        authName.setRequired(true);

                        FormComponentFeedbackBorder authNameBorder =
                                new FormComponentFeedbackBorder("authNameBorder");
                        item.add(authNameBorder);
                        authNameBorder.add(authName);

                        // Authority URL
                        TextField<String> authURL;
                        authURL =
                                new TextField<String>(
                                        "authorityURL",
                                        new PropertyModel<String>(item.getModel(), "href"));
                        authURL.setRequired(true);
                        authURL.add(new UrlValidator());
                        FormComponentFeedbackBorder urlBorder =
                                new FormComponentFeedbackBorder("urlBorder");
                        item.add(urlBorder);
                        urlBorder.add(authURL);

                        // remove link
                        AjaxLink<Integer> link =
                                new AjaxLink<Integer>(
                                        "removeLink", new Model<Integer>(item.getIndex())) {

                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        List<AuthorityURLInfo> list =
                                                new ArrayList<AuthorityURLInfo>(
                                                        authorityURLs.getModelObject());
                                        int index = getModelObject();
                                        list.remove(index);
                                        authorityURLs.setModelObject(list);
                                        updateLinksVisibility();
                                        target.add(container);
                                    }
                                };
                        item.add(link);
                    }
                };
        // this is necessary to avoid loosing item contents on edit/validation checks
        authorityURLs.setReuseItems(true);
        table.add(authorityURLs);

        // the no metadata links label
        noMetadata = new Label("noURLs", new ResourceModel("noAuthorityURLsSoFar"));
        container.add(noMetadata);
        updateLinksVisibility();

        // add new link button
        AjaxButton button =
                new AjaxButton("addURL") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        List<AuthorityURLInfo> list = authorityURLs.getModelObject();
                        AuthorityURLInfo authorityURL = new AuthorityURL();
                        list.add(authorityURL);
                        authorityURLs.setModelObject(list);
                        AuthorityURLListEditor.this.convertInput();
                        updateLinksVisibility();
                        target.add(container);
                    }
                };
        add(button);
    }

    private void updateLinksVisibility() {
        List<AuthorityURLInfo> list = authorityURLs.getModelObject();
        boolean anyLink = list.size() > 0;
        table.setVisible(anyLink);
        noMetadata.setVisible(!anyLink);
    }

    @Override
    public void convertInput() {
        List<AuthorityURLInfo> info = authorityURLs.getModelObject();
        if (info == null || info.size() == 0) {
            setConvertedInput(new ArrayList<AuthorityURLInfo>(2));
            return;
        }

        setConvertedInput(info);
    }

    /** */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
    }
}
