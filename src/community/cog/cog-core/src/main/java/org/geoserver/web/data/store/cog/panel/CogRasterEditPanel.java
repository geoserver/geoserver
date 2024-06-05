/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.cog.panel;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.cog.CogSettings;
import org.geoserver.cog.CogSettingsStore;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.FileModel;
import org.geoserver.web.util.MetadataMapModel;
import org.geoserver.web.wicket.FileExistsValidator;
import org.geoserver.web.wicket.browser.ExtensionFileFilter;

/** A Raster Panel supporting COG settings. */
public class CogRasterEditPanel extends StoreEditPanel {

    private static final String[] EXTENSIONS = new String[] {".tiff", ".tif"};

    private boolean isCog;

    public CogRasterEditPanel(String componentId, Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);
        final CheckBox checkBox = new CheckBox("isCog", new PropertyModel<Boolean>(this, "isCog"));

        CogUrlParamPanel file =
                new CogUrlParamPanel(
                        "url",
                        new PropertyModel(model, "URL"),
                        new ResourceModel("url", "URL"),
                        new PropertyModel<Boolean>(this, "isCog"),
                        true);
        file.setOutputMarkupId(true);
        file.setFileFilter(new Model(new ExtensionFileFilter(EXTENSIONS)));
        file.getFormComponent().add(new CombinedCogFileExistsValidator(checkBox.getModel()));

        add(file);
        add(checkBox);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        final PropertyModel<MetadataMap> metadata =
                new PropertyModel<MetadataMap>(model, "metadata");

        // Check if already configured
        MetadataMap metadataObject = metadata.getObject();
        IModel<CogSettings> cogSettingsModel =
                new MetadataMapModel(metadata, CogSettings.COG_SETTINGS_KEY, CogSettings.class);
        if (metadataObject != null && metadataObject.containsKey(CogSettings.COG_SETTINGS_KEY)) {
            cogSettingsModel.setObject(
                    (CogSettings) metadataObject.get(CogSettings.COG_SETTINGS_KEY));
            isCog = true;
            checkBox.setModelObject(isCog);
        }

        CogSettingsStorePanel cogSettingsPanel =
                new CogSettingsStorePanel("cogSettings", cogSettingsModel, storeEditForm);

        cogSettingsPanel.setOutputMarkupId(true);
        cogSettingsPanel.setVisible(checkBox.getModelObject().booleanValue());
        container.add(cogSettingsPanel);
        checkBox.add(
                new OnChangeAjaxBehavior() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        boolean isCog = checkBox.getModelObject().booleanValue();
                        cogSettingsPanel.setVisible(isCog);
                        target.add(container);
                        if (isCog) {
                            GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
                            MetadataMap globalMap = gs.getGlobal().getSettings().getMetadata();
                            CogSettings defaultSettings =
                                    globalMap.get(CogSettings.COG_SETTINGS_KEY, CogSettings.class);
                            CogSettings cogSettings;
                            if (defaultSettings == null) {
                                cogSettings = new CogSettingsStore();
                            } else {
                                cogSettings = new CogSettingsStore(defaultSettings);
                            }
                            cogSettingsModel.setObject(cogSettings);
                        } else {
                            metadataObject.remove(CogSettings.COG_SETTINGS_KEY);
                        }
                    }
                });
    }

    /**
     * When the store is not configured as a COG, it will perform standard file validation as a
     * common raster store. When COG is selected, a cog:// prefix will be added to make sure that
     * only the COG enabled raster format will support that type of url.
     */
    static class CombinedCogFileExistsValidator implements IValidator<String> {

        private final IModel<Boolean> cog;

        // Inner FileExistsValidator being used when the input is not identified as cog
        FileExistsValidator fileExistsValidator = new FileExistsValidator();

        public CombinedCogFileExistsValidator(IModel<Boolean> cog) {
            this.cog = cog;
        }

        @Override
        public void validate(IValidatable<String> validatable) {

            String uriSpec = validatable.getValue();

            // Check if it's a cog path/isCog or a standard file
            try {
                URI uri = new URI(uriSpec);
                if (!cog.getObject()
                        && ((uri.getScheme() != null
                                && !CogSettings.COG_SCHEMA.equals(uri.getScheme())))) {
                    fileExistsValidator.validate(validatable);
                }
                // Avoid validation if it's a cog
            } catch (URISyntaxException e) {
                // may be a windows path, move on
            }
        }
    }

    public static class CogModel implements IModel<String> {

        FileModel fileModel;
        IModel<String> delegate;
        IModel<Boolean> cog;

        public CogModel(IModel<String> delegate, IModel<Boolean> cog) {
            this.delegate = delegate;
            this.cog = cog;
            this.fileModel =
                    new FileModel(
                            delegate,
                            GeoServerExtensions.bean(GeoServerResourceLoader.class)
                                    .getBaseDirectory());
        }

        @Override
        public String getObject() {
            Object obj = delegate.getObject();
            if (obj instanceof URL) {
                URL url = (URL) obj;
                return url.toExternalForm();
            }
            return (String) obj;
        }

        @Override
        public void setObject(String location) {
            if (location != null) {
                if (!cog.getObject()) {
                    if (!location.startsWith(CogSettings.COG_PREFIX)) {
                        fileModel.setObject(location);
                        location = fileModel.getObject();
                    }
                } else {
                    if (!location.startsWith(CogSettings.COG_PREFIX)) {
                        location = CogSettings.COG_URL_HEADER + location;
                    }
                }
            }
            delegate.setObject(location);
        }

        @Override
        public void detach() {}
    }
}
