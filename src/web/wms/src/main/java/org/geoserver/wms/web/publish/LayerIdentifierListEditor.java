/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.wms.WMSInfo;
import org.springframework.util.Assert;

/**
 * Shows and allows editing of the list of {@link LayerIdentifierInfo} attached to a {@code WMSInfo}
 * , a {@code LayerInfo}, or a {@code LayerGroupInfo}.
 *
 * @author groldan
 * @see WMSInfo#getIdentifiers()
 * @see LayerInfo#getIdentifiers()
 * @see LayerGroupInfo#getIdentifiers()
 */
public class LayerIdentifierListEditor extends FormComponentPanel<List<LayerIdentifierInfo>> {

    private static final long serialVersionUID = 5098470663723800345L;

    private ListView<LayerIdentifierInfo> identifiers;

    private Label noMetadata;

    private WebMarkupContainer table;

    private List<AuthorityURLInfo> baseAuthorities;

    private final AuthorityURLListEditor availableAuthoritiesProvider;

    private class AuthListModel extends LoadableDetachableModel<List<String>> {
        private static final long serialVersionUID = 1L;

        @Override
        protected List<String> load() {
            List<AuthorityURLInfo> authorities = availableAuthoritiesProvider.getModelObject();
            List<String> names = new ArrayList<String>(authorities.size());
            for (AuthorityURLInfo auth : authorities) {
                names.add(auth.getName());
            }
            if (baseAuthorities != null) {
                for (AuthorityURLInfo baseAuth : baseAuthorities) {
                    names.add(baseAuth.getName());
                }
            }
            Collections.sort(names);
            return names;
        }
    }

    /**
     * @param list the model over the appropriate cataloginfo's list of {@link LayerIdentifierInfo}
     * @see WMSInfo#getIdentifiers()
     * @see LayerInfo#getIdentifiers()
     * @see LayerGroupInfo#getIdentifiers()
     */
    public LayerIdentifierListEditor(
            final String id,
            final IModel<List<LayerIdentifierInfo>> list,
            final AuthorityURLListEditor availableAuthoritiesProvider) {
        super(id, list);
        this.availableAuthoritiesProvider = availableAuthoritiesProvider;
        Assert.notNull(list.getObject(), "The list cannot be null");
        Assert.notNull(
                availableAuthoritiesProvider.getModelObject(),
                "The authority provider cannot be null");
        setOutputMarkupId(true);
        initUI();
    }

    /**
     * Sets a list of base authorities to populate the authority names drop down regardless of
     * whether they're defined on this specific layer or not (i.e., the root layer authorities when
     * editing the identifiers for a non root layer)
     */
    public void setBaseAuthorities(List<AuthorityURLInfo> baseAuthorities) {
        this.baseAuthorities = baseAuthorities;
    }

    private void initUI() {
        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // the link list
        table = new WebMarkupContainer("table");
        table.setOutputMarkupId(true);
        container.add(table);

        identifiers =
                new ListView<LayerIdentifierInfo>(
                        "identifiers", new ArrayList<LayerIdentifierInfo>(getModelObject())) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(final ListItem<LayerIdentifierInfo> item) {
                        // odd/even style
                        item.add(
                                AttributeModifier.replace(
                                        "class", item.getIndex() % 2 == 0 ? "even" : "odd"));

                        IModel<String> authModel =
                                new PropertyModel<String>(item.getModel(), "authority");

                        IModel<List<String>> authNamesModel = new AuthListModel();

                        // Authority name
                        DropDownChoice<String> authorities;
                        authorities =
                                new DropDownChoice<String>(
                                        "authority", authModel, authNamesModel.getObject());

                        authorities.setRequired(true);

                        FormComponentFeedbackBorder authFeedbak =
                                new FormComponentFeedbackBorder("authFeedbak");
                        authFeedbak.add(authorities);
                        item.add(authFeedbak);

                        // Identifier
                        TextField<String> identifier;
                        identifier =
                                new TextField<String>(
                                        "identifier",
                                        new PropertyModel<String>(item.getModel(), "identifier"));
                        identifier.setRequired(true);

                        FormComponentFeedbackBorder idFeedbak =
                                new FormComponentFeedbackBorder("idFeedbak");
                        idFeedbak.add(identifier);
                        item.add(idFeedbak);

                        // remove link
                        AjaxLink<Integer> link =
                                new AjaxLink<Integer>(
                                        "removeLink", new Model<Integer>(item.getIndex())) {

                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        List<LayerIdentifierInfo> list =
                                                new ArrayList<LayerIdentifierInfo>(
                                                        identifiers.getModelObject());
                                        int index = getModelObject();
                                        list.remove(index);
                                        identifiers.setModelObject(list);
                                        updateLinksVisibility();
                                        target.add(container);
                                    }
                                };
                        item.add(link);
                    }
                };
        // this is necessary to avoid loosing item contents on edit/validation checks
        identifiers.setOutputMarkupId(true);
        identifiers.setReuseItems(true);
        table.add(identifiers);

        // the no identifiers label
        noMetadata = new Label("noIdentifiers", new ResourceModel("noLayerIdentifiersSoFar"));
        container.add(noMetadata);
        updateLinksVisibility();

        // add new identifier button
        AjaxButton button =
                new AjaxButton("addIdentifier") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        List<LayerIdentifierInfo> list = identifiers.getModelObject();
                        LayerIdentifierInfo newIdentifier = new LayerIdentifier();
                        list.add(newIdentifier);
                        identifiers.setModelObject(list);
                        updateLinksVisibility();
                        target.add(LayerIdentifierListEditor.this);
                    }
                };
        add(button);
    }

    private void updateLinksVisibility() {
        List<LayerIdentifierInfo> list = identifiers.getModelObject();
        boolean anyLink = list.size() > 0;
        table.setVisible(anyLink);
        noMetadata.setVisible(!anyLink);
    }

    @Override
    public void convertInput() {
        List<LayerIdentifierInfo> info = identifiers.getModelObject();
        setConvertedInput(info);
    }

    /** */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
    }
}
