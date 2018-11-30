/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.layer.TileLayer;

/**
 * Page to configure a BlobStore
 *
 * @author Niels Charlier
 */
public class BlobStorePage extends GeoServerSecuredPage {

    private static final long serialVersionUID = -59024268194792891L;

    @SuppressWarnings("rawtypes")
    private DropDownChoice<BlobStoreType> typeOfBlobStore;

    private WebMarkupContainer blobConfigContainer;

    private Form<BlobStoreInfo> blobStoreForm;

    private TextField<String> tfId;

    private CheckBox cbDefault, cbEnabled;

    private GeoServerDialog dialog;

    public BlobStorePage() {
        this(null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public BlobStorePage(final BlobStoreInfo originalStore) {

        final List<String> assignedLayers = new ArrayList<String>();

        add(dialog = new GeoServerDialog("confirmDisableDialog"));
        dialog.setTitle(new ParamResourceModel("confirmDisableDialog.title", getPage()));
        dialog.setInitialHeight(200);

        typeOfBlobStore =
                new DropDownChoice<BlobStoreType>(
                        "typeOfBlobStore", new Model<BlobStoreType>(), BlobStoreTypes.getAll());
        typeOfBlobStore.setOutputMarkupId(true);
        typeOfBlobStore.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = 359589121400814043L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        blobStoreForm.setVisible(typeOfBlobStore.getModelObject() != null);
                        if (typeOfBlobStore.getModelObject() != null) {
                            blobStoreForm
                                    .getModel()
                                    .setObject(typeOfBlobStore.getModelObject().newConfigObject());
                            blobStoreForm.addOrReplace(
                                    typeOfBlobStore
                                            .getModelObject()
                                            .createPanel(
                                                    "blobSpecificPanel", blobStoreForm.getModel()));
                        }

                        target.add(blobConfigContainer);
                    }
                });
        typeOfBlobStore.add(
                new AttributeModifier("title", new ResourceModel("typeOfBlobStore.title")));

        Form<BlobStoreType<?>> selector = new Form<BlobStoreType<?>>("selector");
        selector.add(typeOfBlobStore);
        add(selector);

        blobConfigContainer = new WebMarkupContainer("blobConfigContainer");
        blobConfigContainer.setOutputMarkupId(true);
        add(blobConfigContainer);

        blobStoreForm =
                new Form<BlobStoreInfo>(
                        "blobStoreForm",
                        new CompoundPropertyModel<BlobStoreInfo>(
                                originalStore == null
                                        ? null
                                        : (BlobStoreInfo) originalStore.clone()));
        blobConfigContainer.add(blobStoreForm);
        blobStoreForm.setVisible(originalStore != null);

        blobStoreForm.add((tfId = new TextField<String>("name")).setRequired(true));
        tfId.add(new AttributeModifier("title", new ResourceModel("name.title")));
        blobStoreForm.add(cbEnabled = new CheckBox("enabled"));
        cbEnabled.add(new AttributeModifier("title", new ResourceModel("enabled.title")));
        blobStoreForm.add(cbDefault = new CheckBox("default"));
        cbDefault.add(new AttributeModifier("title", new ResourceModel("default.title")));

        if (originalStore != null) {
            typeOfBlobStore
                    .getModel()
                    .setObject(BlobStoreTypes.getFromClass(originalStore.getClass()));
            blobStoreForm.addOrReplace(
                    typeOfBlobStore
                            .getModelObject()
                            .createPanel("blobSpecificPanel", blobStoreForm.getModel()));
            typeOfBlobStore.setEnabled(false);

            for (TileLayer layer : GWC.get().getTileLayers()) {
                if (originalStore.getName().equals(layer.getBlobStoreId())) {
                    assignedLayers.add(layer.getName());
                }
            }
        }

        blobStoreForm.add(
                new AbstractFormValidator() {
                    private static final long serialVersionUID = 5240602030478856537L;

                    @Override
                    public FormComponent<?>[] getDependentFormComponents() {
                        return new FormComponent<?>[] {cbDefault, cbEnabled};
                    }

                    @Override
                    public void validate(Form<?> form) {
                        BlobStoreInfo blobStore = (BlobStoreInfo) form.getModelObject();
                        if (blobStore.isDefault() && !cbDefault.getConvertedInput()) {
                            form.error(
                                    new ParamResourceModel("defaultError", getPage()).getString());
                        } else if (cbDefault.getConvertedInput()
                                && !cbEnabled.getConvertedInput()) {
                            form.error(
                                    new ParamResourceModel("enabledError", getPage()).getString());
                        }
                    }
                });

        blobStoreForm.add(
                new AbstractFormValidator() {
                    private static final long serialVersionUID = 5240602030478856537L;

                    @Override
                    public FormComponent<?>[] getDependentFormComponents() {
                        return new FormComponent<?>[] {tfId};
                    }

                    @Override
                    public void validate(Form<?> form) {
                        for (BlobStoreInfo otherBlobStore : GWC.get().getBlobStores()) {
                            if (!otherBlobStore.equals(originalStore)
                                    && otherBlobStore.getName().equals(tfId.getConvertedInput())) {
                                form.error(
                                        new ParamResourceModel("duplicateIdError", getPage())
                                                .getString());
                            }
                        }
                    }
                });

        // build the submit/cancel
        blobStoreForm.add(
                new AjaxSubmitLink("save") {
                    private static final long serialVersionUID = 3735176778941168701L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {

                        final BlobStoreInfo blobStore = (BlobStoreInfo) getForm().getModelObject();

                        if (originalStore != null
                                && originalStore.isEnabled()
                                && !blobStore.isEnabled()
                                && assignedLayers.size() > 0) {
                            dialog.showOkCancel(
                                    target,
                                    new GeoServerDialog.DialogDelegate() {
                                        private static final long serialVersionUID =
                                                5257987095800108993L;

                                        private boolean success;

                                        private String error = null;

                                        @Override
                                        protected Component getContents(String id) {
                                            StringBuilder sb = new StringBuilder();
                                            sb.append(
                                                    new ParamResourceModel(
                                                                    "confirmDisableDialog.content",
                                                                    getPage())
                                                            .getString());
                                            for (String layer : assignedLayers) {
                                                sb.append("\n&nbsp;&nbsp;");
                                                sb.append(StringEscapeUtils.escapeHtml4(layer));
                                            }
                                            return new MultiLineLabel("userPanel", sb.toString())
                                                    .setEscapeModelStrings(false);
                                        }

                                        @Override
                                        protected boolean onSubmit(
                                                AjaxRequestTarget target, Component contents) {
                                            try {
                                                save(originalStore, blobStore, assignedLayers);
                                                success = true;
                                            } catch (ConfigurationException e) {
                                                error = e.getMessage();
                                            }
                                            return true;
                                        }

                                        @Override
                                        public void onClose(AjaxRequestTarget target) {
                                            if (success) {
                                                doReturn(BlobStoresPage.class);
                                            } else if (error != null) {
                                                error(error);
                                                addFeedbackPanels(target);
                                            }
                                        }
                                    });
                        } else {
                            try {
                                save(originalStore, blobStore, assignedLayers);
                                doReturn(BlobStoresPage.class);
                            } catch (ConfigurationException e) {
                                error(e.getMessage());
                                addFeedbackPanels(target);
                            }
                        }
                    }

                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        addFeedbackPanels(target);
                    }
                });
        blobStoreForm.add(new BookmarkablePageLink<BlobStoreInfo>("cancel", BlobStoresPage.class));
    }

    protected void save(
            BlobStoreInfo originalStore, BlobStoreInfo blobStore, List<String> assignedLayers)
            throws ConfigurationException {

        // remove default if necessary
        BlobStoreInfo defaultStore = null;
        if (blobStore.isDefault() && (originalStore == null || !originalStore.isDefault())) {
            defaultStore = GWC.get().getDefaultBlobStore();
            if (defaultStore != null) {
                defaultStore.setDefault(false);
                GWC.get().modifyBlobStore(defaultStore.getName(), defaultStore);
            }
        }

        // save
        try {
            if (originalStore == null) {
                GWC.get().addBlobStore(blobStore);
            } else {
                GWC.get().modifyBlobStore(originalStore.getName(), blobStore);
            }
        } catch (ConfigurationException e) {
            // reverse default
            if (defaultStore != null) {
                defaultStore.setDefault(true);
                GWC.get().modifyBlobStore(defaultStore.getName(), defaultStore);
            }
            throw e;
        }

        // update layers if necessary
        if (originalStore != null) {
            boolean updateId = !blobStore.getName().equals(originalStore.getName());
            boolean disable = originalStore.isEnabled() && !blobStore.isEnabled();

            if (updateId || disable) {
                for (String layerName : assignedLayers) {
                    TileLayer layer = GWC.get().getTileLayerByName(layerName);
                    if (updateId) {
                        layer.setBlobStoreId(blobStore.getName());
                    }
                    if (disable) {
                        layer.setEnabled(false);
                    }
                    GWC.get().save(layer);
                }
            }
        }
    }
}
