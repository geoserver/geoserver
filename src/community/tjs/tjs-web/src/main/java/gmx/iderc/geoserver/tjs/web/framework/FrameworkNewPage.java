/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.framework;

import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.web.TJSBasePage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.web.wicket.XMLNameValidator;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Allows creation of a new framework
 */
@SuppressWarnings("serial")
public class FrameworkNewPage extends TJSBasePage {

    Form form;
    TextField nsUriTextField;
    boolean defaultWs;
    FrameworkKeyPanel frameworkKeyPanel;
    String featureTypeId;

    public FrameworkNewPage() {
        this(null);
    }

    public FrameworkNewPage(String featureTypeId) {

        this.featureTypeId = featureTypeId;

        final boolean thereAreNotVectorLayers = getCatalog().getFeatureTypes().isEmpty();

        if (thereAreNotVectorLayers) {
            super.error((String) new ResourceModel("FrameworkNewPage.noFeatureTypesErrorMessage")
                                         .getObject());
        }

        // the store selector, used when no store is initially known
        FrameworkInfo frameworkInfo = getTJSCatalog().getFactory().newFrameworkInfo();
        frameworkInfo.setEnabled(true);

        final IModel frameworkModel = new CompoundPropertyModel(frameworkInfo);
        form = new Form("form", frameworkModel) {
            @Override
            protected void onSubmit() {
                TJSCatalog catalog = getTJSCatalog();

                FrameworkInfo ws = (FrameworkInfo) form.getModelObject();

//                NamespaceInfo ns = catalog.getFactory().createNamespace();
//                ns.setPrefix ( ws.getName() );
//                ns.setURI(nsUriTextField.getDefaultModelObjectAsString());

//                catalog.add( ws );
                catalog.add(ws);
                catalog.save();
//                if(defaultWs)
//                    catalog.setDefaultWorkspace(ws);

                //TODO: set the response page to be the edit
                setResponsePage(FrameworkPage.class);
            }
        };
        add(form);

        TextField<String> nameTextField = new TextField<String>("name");
        nameTextField.setRequired(true);
        nameTextField.add(new XMLNameValidator());
        nameTextField.add(new StringValidator() {

            @Override
            protected void onValidate(IValidatable<String> validatable) {
//                if(CatalogImpl.DEFAULT.equals(validatable.getValue())) {
//                    error(validatable, "defaultWsError");
//                }
            }
        });
        form.add(nameTextField.setRequired(true));

        TextField<String> descriptionTextField = new TextField<String>("description");
        form.add(descriptionTextField.setRequired(false));

        TextField<String> organizationTextField = new TextField<String>("organization");
        form.add(organizationTextField.setRequired(false));

        DateTextField referenceDateTextField = new DateTextField("referenceDate", "dd-MM-yyyy");
        referenceDateTextField.setConvertEmptyInputStringToNull(true);
        form.add(referenceDateTextField.setRequired(false));

        TextField<String> versionTextField = new TextField<String>("version");
        form.add(versionTextField.setRequired(false));

        TextField<String> documentationTextField = new TextField<String>("documentation");
        documentationTextField.add(new StringValidator() {

            @Override
            protected void onValidate(IValidatable<String> paramIValidatable) {
                try {
                    URI uri = new URI(paramIValidatable.getValue());
                } catch (URISyntaxException ex) {
                    error(paramIValidatable, "badUriError");
                }
            }
        });
        form.add(documentationTextField.setRequired(false));

        frameworkKeyPanel = new FrameworkKeyPanel("frameworkKeyPanel", frameworkModel, true);
        form.add(frameworkKeyPanel);

//        nsUriTextField = new TextField( "uri", new Model() );
//        // maybe a bit too restrictive, but better than not validation at all
//        nsUriTextField.setRequired(true);
//        nsUriTextField.add(new URIValidator());
//        form.add( nsUriTextField );

        final IModel associatedWMSLabelModel = new ResourceModel("associatedWMS", "Associated WMS");
        Label label = new Label("associatedWMSLabel", associatedWMSLabelModel.getObject() + "*");
        form.add(label);

        IModel associatedWMSModel = new PropertyModel(frameworkModel, "associatedWMS");
        DropDownChoice associatedWMSChoice = new DropDownChoice("associatedWMSValue", associatedWMSModel,
                                                                       new AssociatedWMSsModel(), new AssociatedWMSChoiceRenderer());
        associatedWMSChoice.setRequired(true);
        // set the label to be the paramLabelModel otherwise a validation error would look like
        // "Parameter 'paramValue' is required"
        associatedWMSChoice.setLabel(associatedWMSLabelModel);
        associatedWMSChoice.setOutputMarkupId(true);

        associatedWMSChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            protected void onUpdate(AjaxRequestTarget target) {
                // Reset the phone model dropdown when the vendor changes
            }

        });
        form.add(associatedWMSChoice);

        CheckBox enabledChk = new CheckBox("enabled", new PropertyModel(frameworkModel, "enabled"));
        form.add(enabledChk);

        SubmitLink submitLink = new SubmitLink("submit", form);
        form.add(submitLink);
        form.setDefaultButton(submitLink);

        AjaxLink cancelLink = new AjaxLink("cancel") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(FrameworkPage.class);
            }
        };
        form.add(cancelLink);

    }


    static final class FeatureTypeListChoiceRenderer implements IChoiceRenderer {

        public Object getDisplayValue(Object featureType) {
            FeatureTypeInfo info = (FeatureTypeInfo) featureType;
            return new StringBuilder(info.getName());
        }

        public String getIdValue(Object featureType, int arg1) {
            return ((FeatureTypeInfo) featureType).getId();
        }

    }


}
