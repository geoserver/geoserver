/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.data.layer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import net.opengis.wfs20.ParameterExpressionType;
import net.opengis.wfs20.StoredQueryDescriptionType;
import net.opengis.wfs20.StoredQueryListItemType;
import net.opengis.wfs20.TitleType;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geotools.data.DataAccess;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMapping;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingBlockValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingDefaultValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingExpressionValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.StoredQueryConfiguration;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.logging.Logging;

/**
 * Page that controls the configuration for a single feature type based on a cascaded WFS Stored
 * Query.
 */
public abstract class CascadedWFSStoredQueryAbstractPage extends GeoServerSecuredPage {

    /** serialVersionUID */
    private static final long serialVersionUID = 3805287974674434336L;

    static final Logger LOGGER = Logging.getLogger(CascadedWFSStoredQueryAbstractPage.class);

    public static final String DATASTORE = "storeName";

    public static final String WORKSPACE = "wsName";

    String storeId;

    GeoServerTablePanel<StoredQueryParameterAttribute> parameters;
    StoredQueryParameterAttributeProvider parameterProvider;

    public CascadedWFSStoredQueryAbstractPage(PageParameters params) throws IOException {
        this(params.get(WORKSPACE).toOptionalString(), params.get(DATASTORE).toString(), null);
    }

    public CascadedWFSStoredQueryAbstractPage(
            String workspaceName, String storeName, String typeName) throws IOException {
        storeId =
                getCatalog().getStoreByName(workspaceName, storeName, DataStoreInfo.class).getId();

        Form<CascadedWFSStoredQueryAbstractPage> form =
                new Form<>("form", new CompoundPropertyModel<>(this));

        form.add(getStoredQueryNameComponent());

        parameterProvider = new StoredQueryParameterAttributeProvider();

        parameters =
                new GeoServerTablePanel<StoredQueryParameterAttribute>(
                        "parameters", parameterProvider) {
                    /** serialVersionUID */
                    private static final long serialVersionUID = 8282438267732625198L;

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            final IModel<StoredQueryParameterAttribute> itemModel,
                            Property<StoredQueryParameterAttribute> property) {
                        if (property == ATTR_MAPTYPE) {
                            Fragment f =
                                    new Fragment(
                                            id,
                                            "parameterMappingType",
                                            CascadedWFSStoredQueryAbstractPage.this);

                            ArrayList<ParameterMappingType> choices =
                                    new ArrayList<ParameterMappingType>();
                            for (ParameterMappingType pmt : ParameterMappingType.values()) {
                                choices.add(pmt);
                            }
                            DropDownChoice<ParameterMappingType> choice =
                                    new DropDownChoice<>(
                                            "dropdown",
                                            new PropertyModel<>(itemModel, "mappingType"),
                                            choices,
                                            new ParameterMappingTypeRenderer());

                            f.add(choice);

                            return f;

                        } else if (property == ATTR_VALUE) {
                            Fragment f =
                                    new Fragment(
                                            id,
                                            "parameterMappingValue",
                                            CascadedWFSStoredQueryAbstractPage.this);

                            TextField<String> textField =
                                    new TextField<>(
                                            "text", new PropertyModel<>(itemModel, "value"));
                            f.add(textField);

                            return f;
                        }
                        return null;
                    }
                };
        // just a plain table, no filters, no paging
        parameters.setFilterVisible(false);
        parameters.setSortable(false);
        parameters.setPageable(false);
        parameters.setOutputMarkupId(true);
        form.add(parameters);

        add(form);

        // save and cancel at the bottom of the page
        form.add(
                new SubmitLink("save") {
                    /** serialVersionUID */
                    private static final long serialVersionUID = 2540349398885832870L;

                    @Override
                    public void onSubmit() {
                        onSave();
                    }
                });
        form.add(
                new Link<Void>("cancel") {

                    /** serialVersionUID */
                    private static final long serialVersionUID = 451678049485016709L;

                    @Override
                    public void onClick() {
                        onCancel();
                    }
                });
    }

    static StoredQueryConfiguration createStoredQueryConfiguration(
            List<StoredQueryParameterAttribute> items, String storedQueryId) {
        StoredQueryConfiguration ret = new StoredQueryConfiguration();
        ret.setStoredQueryId(storedQueryId);

        for (StoredQueryParameterAttribute i : items) {
            ParameterMapping mapping;
            String name = i.getParameterName();
            switch (i.getMappingType()) {
                case BLOCKED:
                    mapping = new ParameterMappingBlockValue(name);
                    break;
                case DEFAULT:
                    mapping = new ParameterMappingDefaultValue(name, false, i.getValue());
                    break;
                case STATIC:
                    mapping = new ParameterMappingDefaultValue(name, true, i.getValue());
                    break;
                case EXPRESSION_CQL:
                    mapping = new ParameterMappingExpressionValue(name, "CQL", i.getValue());
                    break;
                case NONE:
                default:
                    mapping = null;
            }
            if (mapping != null) {
                ret.getStoredQueryParameterMappings().add(mapping);
            }
        }
        return ret;
    }

    protected List<StoredQueryListItemType> listStoredQueries() {
        try {
            WFSDataStore contentStore = getContentDataStore();
            return contentStore.getStoredQueryListResponse().getStoredQuery();
        } catch (IOException ie) {
            throw new RuntimeException("Uanble to list stored queries", ie);
        }
    }

    protected WFSDataStore getContentDataStore() throws IOException {
        DataStoreInfo store = getCatalog().getStore(storeId, DataStoreInfo.class);
        DataAccess<?, ?> da = store.getDataStore(null);
        if (da instanceof Wrapper) {
            try {
                da = ((Wrapper) da).unwrap(DataAccess.class);
            } catch (IllegalArgumentException e) {
                throw new IOException(e);
            }
        }
        return (WFSDataStore) da;
    }

    public static String createStoredQueryTitle(StoredQueryListItemType object) {
        StringBuilder ret = new StringBuilder();

        TitleType title = null;
        for (TitleType t : object.getTitle()) {
            if (title == null) {
                title = t;
            } else if (title.getValue() == null || title.getValue().length() == 0) {
                title = t;
            }
        }

        if (title != null) {
            ret.append(title.getValue()).append(" (").append(object.getId()).append(")");
        } else {
            ret.append(object.getId());
        }

        return ret.toString();
    }

    /** Attribute model for how a single stored query parameter is supposed to be handled. */
    public class StoredQueryParameterAttribute implements Serializable {
        /** serialVersionUID */
        private static final long serialVersionUID = -9213562985695412130L;

        private String parameterName;
        private String title;
        private QName type;
        private ParameterMappingType mappingType;
        private String value;

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public void setType(QName type) {
            this.type = type;
        }

        public QName getType() {
            return type;
        }

        public void setMappingType(ParameterMappingType mappingType) {
            this.mappingType = mappingType;
        }

        public ParameterMappingType getMappingType() {
            return mappingType;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setParameterName(String parameterName) {
            this.parameterName = parameterName;
        }

        public String getParameterName() {
            return parameterName;
        }

        @Override
        public int hashCode() {
            final int prime = 37;
            int result = 1;
            result = prime * result + ((parameterName == null) ? 0 : parameterName.hashCode());
            result = prime * result + ((title == null) ? 0 : title.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result + ((mappingType == null) ? 0 : mappingType.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        private boolean nullSafeCompare(Object a, Object b) {
            if (a == null && b == null) {
                return true;
            }
            if (a == null && b != null) {
                return false;
            }
            return a.equals(b);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            StoredQueryParameterAttribute other = (StoredQueryParameterAttribute) obj;
            if (!nullSafeCompare(this.parameterName, other.parameterName)) {
                return false;
            }
            if (!nullSafeCompare(this.title, other.title)) {
                return false;
            }
            if (!nullSafeCompare(this.type, other.type)) {
                return false;
            }
            if (!nullSafeCompare(this.mappingType, other.mappingType)) {
                return false;
            }
            if (!nullSafeCompare(this.value, other.value)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder();

            ret.append(getParameterName())
                    .append(": ")
                    .append("title=")
                    .append(title)
                    .append(", ")
                    .append("type=")
                    .append(type)
                    .append(", ")
                    .append("mappingType=")
                    .append(mappingType)
                    .append(", ")
                    .append("value=")
                    .append(value);

            return ret.toString();
        }
    }

    public StoredQueryParameterAttribute createStoredQueryParameterAttribute(
            String storedQueryId, ParameterExpressionType pet) {
        StoredQueryParameterAttribute ret = new StoredQueryParameterAttribute();

        ret.setParameterName(pet.getName());
        ret.setType(pet.getType());
        StringBuilder title = new StringBuilder();
        for (TitleType t : pet.getTitle()) {
            if (t.getValue() != null && t.getValue().length() > 0) {
                title.append(t.getValue());
                break;
            }
        }
        ret.setTitle(title.toString());

        populateStoredQueryParameterAttribute(storedQueryId, pet, ret);
        return ret;
    }

    public abstract void populateStoredQueryParameterAttribute(
            String storedQueryId, ParameterExpressionType pet, StoredQueryParameterAttribute attr);

    public static Property<StoredQueryParameterAttribute> ATTR_NAME =
            new BeanProperty<StoredQueryParameterAttribute>("parameterName", "parameterName");
    public static Property<StoredQueryParameterAttribute> ATTR_TITLE =
            new BeanProperty<StoredQueryParameterAttribute>("title", "title");
    public static Property<StoredQueryParameterAttribute> ATTR_TYPE =
            new BeanProperty<StoredQueryParameterAttribute>("type", "type");
    public static Property<StoredQueryParameterAttribute> ATTR_MAPTYPE =
            new BeanProperty<StoredQueryParameterAttribute>("mappingType", "mappingType");
    public static Property<StoredQueryParameterAttribute> ATTR_VALUE =
            new BeanProperty<StoredQueryParameterAttribute>("value", "value");

    public enum ParameterMappingType {
        NONE,
        STATIC,
        DEFAULT,
        EXPRESSION_CQL,
        BLOCKED
    }

    public class ParameterMappingTypeRenderer extends ChoiceRenderer<ParameterMappingType> {
        /** serialVersionUID */
        private static final long serialVersionUID = 1875427995762137069L;

        @Override
        public Object getDisplayValue(ParameterMappingType object) {
            return new StringResourceModel(
                            "ParameterMappingType." + object.toString(),
                            CascadedWFSStoredQueryAbstractPage.this,
                            null)
                    .getString();
        }

        @Override
        public String getIdValue(ParameterMappingType object, int index) {
            return object.toString();
        }
    }

    public class StoredQueryParameterAttributeProvider
            extends GeoServerDataProvider<StoredQueryParameterAttribute> {
        /** serialVersionUID */
        private static final long serialVersionUID = 5295091510256421604L;

        private List<StoredQueryParameterAttribute> items =
                new ArrayList<StoredQueryParameterAttribute>();

        public void refreshItems(String storedQueryId) {
            items.clear();
            if (storedQueryId != null) {

                StoredQueryDescriptionType desc;
                try {
                    WFSDataStore contentStore = getContentDataStore();
                    desc = contentStore.getStoredQueryDescriptionType(storedQueryId);
                } catch (IOException ie) {
                    throw new RuntimeException("Unable to describe stored query", ie);
                }

                for (ParameterExpressionType pet : desc.getParameter()) {
                    items.add(createStoredQueryParameterAttribute(storedQueryId, pet));
                }
            }
        }

        @Override
        protected List<StoredQueryParameterAttribute> getItems() {
            return items;
        }

        @Override
        protected List<Property<StoredQueryParameterAttribute>> getProperties() {
            return Arrays.asList(ATTR_NAME, ATTR_TITLE, ATTR_TYPE, ATTR_MAPTYPE, ATTR_VALUE);
        }
    }

    protected abstract Component getStoredQueryNameComponent();

    protected abstract void onSave();

    protected abstract void onCancel();
}
