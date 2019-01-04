/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.data.layer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wfs20.ParameterExpressionType;
import net.opengis.wfs20.StoredQueryListItemType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataAccess;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.internal.v2_0.storedquery.StoredQueryConfiguration;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;

public class CascadedWFSStoredQueryNewPage extends CascadedWFSStoredQueryAbstractPage {

    /** serialVersionUID */
    private static final long serialVersionUID = 5430480206314316146L;

    static final Logger LOGGER = Logging.getLogger(CascadedWFSStoredQueryNewPage.class);

    DropDownChoice<StoredQuery> storedQueriesDropDown;

    private String nativeName;

    public CascadedWFSStoredQueryNewPage(PageParameters params) throws IOException {
        super(params);
    }

    @Override
    protected Component getStoredQueryNameComponent() {
        Fragment f = new Fragment("storedQueryName", "newPage", this);
        storedQueriesDropDown = storedQueriesDropDown();
        f.add(storedQueriesDropDown);

        TextField<String> textField =
                new TextField<>("nativeName", new PropertyModel<>(this, "nativeName"));
        textField.setRequired(true);
        textField.add(new ViewNameValidator());

        f.add(textField);
        return f;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    public String getNativeName() {
        return nativeName;
    }

    @Override
    public void populateStoredQueryParameterAttribute(
            String storedQueryId, ParameterExpressionType pet, StoredQueryParameterAttribute attr) {
        // We're creating a new layer, all parameters are empty by default
        attr.setMappingType(ParameterMappingType.NONE);
        attr.setValue(null);
    }

    @Override
    protected void onSave() {
        // TODO: check stuff before saving
        StoredQuery selection = (StoredQuery) storedQueriesDropDown.getDefaultModelObject();
        StoredQueryConfiguration config =
                createStoredQueryConfiguration(
                        parameterProvider.getItems(), selection.storedQueryId);

        try {
            DataStoreInfo dsInfo = getCatalog().getStore(storeId, DataStoreInfo.class);
            WFSDataStore directDs = getContentDataStore();
            DataAccess<?, ?> da = dsInfo.getDataStore(null);

            Name typeName = directDs.addStoredQuery(getNativeName(), config.getStoredQueryId());

            CatalogBuilder builder = new CatalogBuilder(getCatalog());
            builder.setStore(dsInfo);
            FeatureTypeInfo fti = builder.buildFeatureType(da.getFeatureSource(typeName));

            fti.getMetadata().put(FeatureTypeInfo.STORED_QUERY_CONFIGURATION, config);
            LayerInfo layerInfo = builder.buildLayer(fti);
            setResponsePage(new ResourceConfigurationPage(layerInfo, true));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create feature type", e);
            error(new ParamResourceModel("creationFailure", this, e.getMessage()).getString());
        }
    }

    @Override
    protected void onCancel() {
        doReturn(LayerPage.class);
    }

    private DropDownChoice<StoredQuery> storedQueriesDropDown() {
        final DropDownChoice<StoredQuery> dropdown =
                new DropDownChoice<>(
                        "storedQueriesDropDown",
                        new Model<>(),
                        new StoredQueryListModel(),
                        new StoredQueryListRenderer());

        dropdown.setRequired(true);
        dropdown.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    /** serialVersionUID */
                    private static final long serialVersionUID = -7195159596309736905L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        StoredQuery selection = (StoredQuery) dropdown.getDefaultModelObject();
                        parameterProvider.refreshItems(selection.storedQueryId);
                        target.add(parameters);
                    }
                });

        return dropdown;
    }

    private class StoredQueryListModel extends LoadableDetachableModel<List<StoredQuery>> {
        /** serialVersionUID */
        private static final long serialVersionUID = 2434460260811775002L;

        @Override
        protected List<StoredQuery> load() {
            List<StoredQuery> ret = new ArrayList<StoredQuery>();

            for (StoredQueryListItemType sqlit : listStoredQueries()) {
                StoredQuery item = new StoredQuery();
                item.setStoredQueryId(sqlit.getId());
                item.setTitle(createStoredQueryTitle(sqlit));

                ret.add(item);
            }
            return ret;
        }
    }

    private class StoredQueryListRenderer extends ChoiceRenderer<StoredQuery> {
        /** serialVersionUID */
        private static final long serialVersionUID = 7539702994237874704L;

        @Override
        public Object getDisplayValue(StoredQuery object) {
            return object.getTitle();
        }

        @Override
        public String getIdValue(StoredQuery object, int index) {
            return object.getStoredQueryId();
        }
    }

    public static class StoredQuery implements Serializable {
        private static final long serialVersionUID = 1L;

        private String title;
        private String storedQueryId;

        public void setStoredQueryId(String storedQueryId) {
            this.storedQueryId = storedQueryId;
        }

        public String getStoredQueryId() {
            return storedQueryId;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    class ViewNameValidator implements IValidator<String> {
        /** serialVersionUID */
        private static final long serialVersionUID = 8023559657640603820L;

        @Override
        public void validate(IValidatable<String> validatable) {
            String csqName = (String) validatable.getValue();

            final DataStoreInfo store = getCatalog().getStore(storeId, DataStoreInfo.class);
            List<FeatureTypeInfo> ftis =
                    getCatalog().getResourcesByStore(store, FeatureTypeInfo.class);
            for (FeatureTypeInfo curr : ftis) {
                StoredQueryConfiguration config =
                        curr.getMetadata()
                                .get(
                                        FeatureTypeInfo.STORED_QUERY_CONFIGURATION,
                                        StoredQueryConfiguration.class);
                if (config != null) {
                    if (curr.getNativeName().equals(csqName)) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", csqName);
                        map.put("dataStore", store.getName());
                        IValidationError err =
                                new ValidationError("duplicateSqlViewName")
                                        .addKey("duplicateSqlViewName")
                                        .setVariables(map);
                        validatable.error(err);
                        return;
                    }
                }
            }
        }
    }
}
