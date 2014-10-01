/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.framework;

import gmx.iderc.geoserver.tjs.catalog.FrameworkInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.web.TJSBasePage;
import gmx.iderc.geoserver.tjs.web.TJSCatalogObjectDetachableModel;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.XMLNameValidator;
import org.geotools.util.logging.Logging;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * Allows editing a specific workspace
 */
@SuppressWarnings("serial")
public class FrameworkEditPage extends TJSBasePage {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web.data.workspace");

    IModel frameworkModel;

    FrameworkKeyPanel frameworkKeyPanel;

    /**
     * Uses a "name" parameter to locate the workspace
     *
     * @param parameters
     */
    public FrameworkEditPage(PageParameters parameters) {
        String frName = parameters.getString("name");
        FrameworkInfo fri = getTJSCatalog().getFrameworkByName(frName);

        if (fri == null) {
            error(new ParamResourceModel("FrameworkEditPage.notFound", this, frName).getString());
            setResponsePage(FrameworkPage.class);
            return;
        }

        init(fri);
    }

    public FrameworkEditPage(FrameworkInfo ws) {
        init(ws);
    }

    private void init(FrameworkInfo frameworkInfo) {

        frameworkModel = new TJSCatalogObjectDetachableModel(frameworkInfo);
//        final IModel model = new CompoundPropertyModel(frameworkInfo);


//        NamespaceInfo ns = getCatalog().getNamespaceByPrefix( ws.getName() );
//        nsModel = new NamespaceDetachableModel(ns);

        Form form = new Form("form", new CompoundPropertyModel(frameworkModel)) {
            protected void onSubmit() {
                try {
                    saveFramework();
                } catch (RuntimeException e) {
                    error(e.getMessage());
                }
            }
        };
        add(form);
        TextField name = new TextField("name", new PropertyModel(frameworkModel, "name"));
        name.setRequired(true);
        name.add(new XMLNameValidator());
        form.add(name);

        TextField description = new TextField("description", new PropertyModel(frameworkModel, "description"));
        description.setRequired(false);
        form.add(description);

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

        //stores
//        StorePanel storePanel = new StorePanel("storeTable", new StoreProvider(ws), false);
//        form.add(storePanel);

        SubmitLink submit = new SubmitLink("save");
        form.add(submit);
        form.setDefaultButton(submit);
        form.add(new BookmarkablePageLink("cancel", FrameworkPage.class));
    }

    private void saveFramework() {
        final TJSCatalog catalog = getTJSCatalog();
        FrameworkInfo frameworkInfo = (FrameworkInfo) frameworkModel.getObject();
        catalog.save(frameworkInfo);
        setResponsePage(FrameworkPage.class);
    }

}
