/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.web;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IModelComparator;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.inspire.UniqueResourceIdentifier;
import org.geoserver.inspire.UniqueResourceIdentifiers;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.URIValidator;
import org.geoserver.wfs.WFSInfo;

/**
 * Shows and allows editing of the {@link UniqueResourceIdentifiers} attached to a {@link WFSInfo}
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class UniqueResourceIdentifiersEditor extends FormComponentPanel<UniqueResourceIdentifiers> {

    private GeoServerTablePanel<UniqueResourceIdentifier> identifiers;
    private AjaxButton button;

    /** @param identifiersModel Must return a {@link ResourceInfo} */
    public UniqueResourceIdentifiersEditor(
            String id, final IModel<UniqueResourceIdentifiers> identifiersModel) {
        super(id, identifiersModel);

        if (identifiersModel.getObject() == null) {
            identifiersModel.setObject(new UniqueResourceIdentifiers());
        }

        // container for ajax updates
        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        // the link list
        identifiers =
                new GeoServerTablePanel<UniqueResourceIdentifier>(
                        "identifiers",
                        new UniqueResourceIdentifiersProvider(identifiersModel),
                        false) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<UniqueResourceIdentifier> itemModel,
                            Property<UniqueResourceIdentifier> property) {
                        String name = property.getName();
                        if ("code".equals(name)) {
                            Fragment codeFragment =
                                    new Fragment(
                                            id,
                                            "txtFragment",
                                            UniqueResourceIdentifiersEditor.this);
                            FormComponentFeedbackBorder codeBorder =
                                    new FormComponentFeedbackBorder("border");
                            codeFragment.add(codeBorder);
                            TextField<String> code =
                                    new TextField<String>(
                                            "txt", new PropertyModel<String>(itemModel, "code"));
                            code.setLabel(
                                    new ParamResourceModel(
                                            "th.code", UniqueResourceIdentifiersEditor.this));
                            code.setRequired(true);
                            codeBorder.add(code);
                            return codeFragment;
                        } else if ("namespace".equals(name)) {
                            Fragment nsFragment =
                                    new Fragment(
                                            id,
                                            "txtFragment",
                                            UniqueResourceIdentifiersEditor.this);
                            FormComponentFeedbackBorder namespaceBorder =
                                    new FormComponentFeedbackBorder("border");
                            nsFragment.add(namespaceBorder);
                            TextField<String> namespace =
                                    new TextField<String>(
                                            "txt",
                                            new PropertyModel<String>(itemModel, "namespace"));
                            namespace.setLabel(
                                    new ParamResourceModel(
                                            "th.namespace", UniqueResourceIdentifiersEditor.this));
                            namespace.add(new URIValidator());
                            namespaceBorder.add(namespace);
                            return nsFragment;
                        } else if ("metadataURL".equals(name)) {
                            Fragment urlFragment =
                                    new Fragment(
                                            id,
                                            "txtFragment",
                                            UniqueResourceIdentifiersEditor.this);
                            FormComponentFeedbackBorder namespaceBorder =
                                    new FormComponentFeedbackBorder("border");
                            urlFragment.add(namespaceBorder);
                            TextField<String> url =
                                    new TextField<String>(
                                            "txt",
                                            new PropertyModel<String>(itemModel, "metadataURL"));
                            url.add(new URIValidator());
                            namespaceBorder.add(url);
                            return urlFragment;
                        } else if ("remove".equals(name)) {
                            Fragment removeFragment =
                                    new Fragment(
                                            id,
                                            "removeFragment",
                                            UniqueResourceIdentifiersEditor.this);
                            GeoServerAjaxFormLink removeLink =
                                    new GeoServerAjaxFormLink("remove") {

                                        @Override
                                        protected void onClick(
                                                AjaxRequestTarget target, Form form) {
                                            UniqueResourceIdentifiers identifiers =
                                                    identifiersModel.getObject();
                                            UniqueResourceIdentifier sdi =
                                                    (UniqueResourceIdentifier)
                                                            itemModel.getObject();
                                            identifiers.remove(sdi);
                                            target.add(container);
                                        }
                                    };
                            removeFragment.add(removeLink);
                            return removeFragment;
                        }
                        return null;
                    }
                };
        identifiers.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
        identifiers.setPageable(false);
        identifiers.setSortable(false);
        identifiers.setFilterable(false);
        container.add(identifiers);

        // add new link button
        button =
                new AjaxButton("addIdentifier") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        UniqueResourceIdentifiers identifiers = identifiersModel.getObject();
                        identifiers.add(new UniqueResourceIdentifier());

                        target.add(container);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        // the form validator triggered, but we don't want the msg to display
                        Session.get()
                                .getFeedbackMessages()
                                .clear(); // formally cleanupFeedbackMessages()
                        Session.get().dirty();
                        onSubmit(target, form);
                    }
                };
        add(button);

        // grab a seat... the way I'm adding this validator in onBeforeRender will be hard
        // to stomach... however, could not find other way to add a validation to an editabl table,
        // grrr
        add(
                new IValidator<UniqueResourceIdentifiers>() {

                    @Override
                    public void validate(IValidatable<UniqueResourceIdentifiers> validatable) {
                        UniqueResourceIdentifiers identifiers = identifiersModel.getObject();
                        if (identifiers.size() == 0) {
                            ValidationError error = new ValidationError();
                            String message =
                                    new ParamResourceModel(
                                                    "noSpatialDatasetIdentifiers",
                                                    UniqueResourceIdentifiersEditor.this)
                                            .getString();
                            error.setMessage(message);
                            validatable.error(error);
                        }
                    }
                });
    }

    @Override
    public void convertInput() {
        UniqueResourceIdentifiersProvider provider =
                (UniqueResourceIdentifiersProvider) identifiers.getDataProvider();
        UniqueResourceIdentifiers ids = provider.model.getObject();
        setConvertedInput(ids);
    }

    @Override
    public IModelComparator getModelComparator() {
        // if we don't use this one, the call to setObject won't be made, and the metadata
        // map won't be updated
        return new IModelComparator() {

            @Override
            public boolean compare(Component component, Object newObject) {
                return false;
            }
        };
    }
}
