/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.data;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.DataStoreInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.data.TJSDataAccessFactory;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.ParamInfo;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.data.store.panel.PasswordParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.FileExistsValidator;
import org.geotools.data.DataAccessFactory.Param;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class DefaultDataStoreEditPanel extends Panel {

    protected final Form storeEditForm;

    private static final long serialVersionUID = -1969433619372747193L;

    protected TJSCatalog getTJSCatalog() {
        return TJSExtension.getTJSCatalog();
    }

    public DefaultDataStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId);
        this.storeEditForm = storeEditForm;

        final IModel model = storeEditForm.getModel();
        DataStoreInfo info = (DataStoreInfo) model.getObject();
        final boolean isNew = null == info.getId();
        if (isNew && info instanceof DataStoreInfo) {
            applyDataStoreParamsDefaults(info);
        }

        final TJSCatalog catalog = getTJSCatalog();
//        final ResourcePool resourcePool = catalog.getResourcePool();
        TJSDataAccessFactory dsFactory;
        try {
            dsFactory = catalog.getDataStoreFactory(info.getType());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final Map<String, ParamInfo> paramsMetadata = new LinkedHashMap<String, ParamInfo>();

        {
            final Param[] dsParams = dsFactory.getParametersInfo();
            for (Param p : dsParams) {
                ParamInfo paramInfo = new ParamInfo(p);
                paramsMetadata.put(p.key, paramInfo);

                if (isNew) {
                    // set default value
                    applyParamDefault(paramInfo, info);
                }
            }
        }

        final List<String> keys = new ArrayList<String>(paramsMetadata.keySet());
        final IModel paramsModel = new PropertyModel(model, "connectionParameters");

        ListView paramsList = new ListView("parameters", keys) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                String paramName = item.getDefaultModelObjectAsString();
                ParamInfo paramMetadata = paramsMetadata.get(paramName);

                Component inputComponent;
                inputComponent = getInputComponent("parameterPanel", paramsModel, paramMetadata);

                String description = paramMetadata.getTitle();
                if (description != null) {
                    inputComponent.add(new SimpleAttributeModifier("title", description));
                }
                item.add(inputComponent);
            }
        };
        // needed for form components not to loose state
        paramsList.setReuseItems(true);

        add(paramsList);

    }

    /**
     * Creates a form input component for the given datastore param based on its type and metadata
     * properties.
     *
     * @param paramMetadata
     * @return
     */
    private Panel getInputComponent(final String componentId, final IModel paramsModel,
                                    final ParamInfo paramMetadata) {

        final String paramName = paramMetadata.getName();
        final String paramLabel = paramMetadata.getName();
        final boolean required = paramMetadata.isRequired();
        final Class<?> binding = paramMetadata.getBinding();
        final List<Serializable> options = paramMetadata.getOptions();

        Panel parameterPanel;
        if ("dbtype".equals(paramName) || "filetype".equals(paramName)) {
            // skip the two well known discriminators
            IModel model = new MapModel(paramsModel, paramName);
            TextParamPanel tp = new TextParamPanel(componentId,
                                                          model, new ResourceModel(paramLabel, paramLabel), required);
            tp.setVisible(false);
            parameterPanel = tp;
        } else if (options != null && options.size() > 0) {

            IModel<Serializable> valueModel = new MapModel(paramsModel, paramName);
            IModel<String> labelModel = new ResourceModel(paramLabel, paramLabel);
            parameterPanel = new DropDownChoiceParamPanel(componentId, valueModel, labelModel, options,
                                                                 required);

        } else if (Boolean.class == binding) {
            // TODO Add prefix for better i18n?
            parameterPanel = new CheckBoxParamPanel(componentId, new MapModel(paramsModel,
                                                                                     paramName), new ResourceModel(paramLabel, paramLabel));

        } else if (String.class == binding && paramMetadata.isPassword()) {
            parameterPanel = new PasswordParamPanel(componentId, new MapModel(paramsModel,
                                                                                     paramName), new ResourceModel(paramLabel, paramLabel), required);
        } else {
            IModel model;
            if ("url".equalsIgnoreCase(paramName)) {
                model = new URLModel(paramsModel, paramName);
            } else {
                model = new MapModel(paramsModel, paramName);
            }

            TextParamPanel tp = new TextParamPanel(componentId,
                                                          model, new ResourceModel(paramLabel, paramLabel), required);
            // if it can be a reference to the local filesystem make sure it's valid
            if (paramName.equalsIgnoreCase("url")) {
                tp.getFormComponent().add(new FileExistsValidator());
            }
            // make sure the proper value is returned, but don't set it for strings otherwise
            // we incur in a wicket bug (the empty string is not converter back to a null)
            // GR: it doesn't work for File neither.
            // AA: better not mess with files, the converters turn data dir relative to
            // absolute and bye bye data dir portability
            if (binding != null && !String.class.equals(binding) && !File.class.equals(binding)
                        && !URL.class.equals(binding)) {
                tp.getFormComponent().setType(binding);
            }
            parameterPanel = tp;
        }
        return parameterPanel;
    }

    /**
     * Makes sure the file path for shapefiles do start with file:// otherwise
     * stuff like /home/user/file.shp won't be recognized as valid...
     *
     * @author aaime
     */
    private final class URLModel extends MapModel {
        private URLModel(IModel model, String expression) {
            super(model, expression);
        }

        @Override
        public void setObject(Object object) {
            String file = (String) object;
            if (!file.startsWith("file://") && !file.startsWith("file:") &&
                        !file.startsWith("http://"))
                file = "file://" + file;
            super.setObject(file);
        }
    }

    /**
     * Initializes all store parameters to their default value
     *
     * @param info
     */
    protected void applyDataStoreParamsDefaults(DataStoreInfo info) {
        // grab the factory
        final DataStoreInfo dsInfo = (DataStoreInfo) info;
        TJSDataAccessFactory dsFactory;
        try {
            dsFactory = getTJSCatalog().getDataStoreFactory(dsInfo.getType());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final Param[] dsParams = dsFactory.getParametersInfo();
        for (Param p : dsParams) {
            ParamInfo paramInfo = new ParamInfo(p);

            // set default value
            applyParamDefault(paramInfo, info);
        }
    }

    protected void applyParamDefault(ParamInfo paramInfo, DataStoreInfo info) {
        Serializable defValue;
        if (URL.class == paramInfo.getBinding()) {
            defValue = "file:data/example.extension";
        } else {
            defValue = paramInfo.getValue();
        }

        info.getConnectionParameters().put(paramInfo.getName(), defValue);
    }


}
