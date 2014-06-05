package org.geoserver.web.data.layer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.opengis.wfs20.ParameterExpressionType;
import net.opengis.wfs20.StoredQueryDescriptionType;
import net.opengis.wfs20.StoredQueryListItemType;
import net.opengis.wfs20.TitleType;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.feature.retype.RetypingDataStore;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geotools.data.DataAccess;
import org.geotools.data.wfs.impl.WFSContentDataStore;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMapping;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingBlockValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingDefaultValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.ParameterMappingExpressionValue;
import org.geotools.data.wfs.internal.v2_0.storedquery.StoredQueryConfiguration;
import org.geotools.jdbc.VirtualTable;
import org.geotools.util.logging.Logging;



public abstract class CascadedWFSStoredQueryAbstractPage extends GeoServerSecuredPage {

    static final Logger LOGGER = Logging.getLogger(CascadedWFSStoredQueryAbstractPage.class);
    
    public static final String DATASTORE = "storeName";

    public static final String WORKSPACE = "wsName";

    DropDownChoice storedQueriesDropDown;
    StoredQueryParameterAttributeProvider parameterProvider;
    GeoServerTablePanel<StoredQueryParameterAttribute> parameters;
    
    String storeId;

    public CascadedWFSStoredQueryAbstractPage(PageParameters params) throws IOException {
        this(params.getString(WORKSPACE), params.getString(DATASTORE), null, null);
    }
    
    public CascadedWFSStoredQueryAbstractPage(String workspaceName, String storeName, String typeName, VirtualTable virtualTable)
            throws IOException {
        storeId = getCatalog().getStoreByName(workspaceName, storeName, DataStoreInfo.class)
                .getId();
        Form form = new Form("form", new CompoundPropertyModel(this));
        
        storedQueriesDropDown = storedQueriesDropDown();
        form.add(storedQueriesDropDown);

        parameterProvider = new StoredQueryParameterAttributeProvider();
        
        parameters = new GeoServerTablePanel<StoredQueryParameterAttribute>("parameters", parameterProvider) {
            @Override
            protected Component getComponentForProperty(String id,
                    final IModel itemModel,
                    Property<StoredQueryParameterAttribute> property) {
                if (property == ATTR_MAPTYPE) {
                    Fragment f = new Fragment(id, "parameterMappingType", CascadedWFSStoredQueryAbstractPage.this);
                    
                    ArrayList<ParameterMappingType> choices = new ArrayList<ParameterMappingType>();
                    for (ParameterMappingType pmt : ParameterMappingType.values()) {
                        choices.add(pmt);
                    }
                    DropDownChoice<ParameterMappingType> choice = 
                            new DropDownChoice<ParameterMappingType>("dropdown", 
                                    new PropertyModel(itemModel, "mappingType"),
                                    choices,
                                    new ParameterMappingTypeRenderer());
                    
                    f.add(choice);
                    
                    return f;
                    
                } else
                if (property == ATTR_VALUE) {
                    Fragment f = new Fragment(id, "parameterMappingValue", CascadedWFSStoredQueryAbstractPage.this);
                    
                    TextField textField = new TextField("text", new PropertyModel(itemModel, "value"));
                    f.add(textField);
                    
                    return f;
                }
                return null;
            }
            
        };
        // just a plain table, no filters, no paging, 
        parameters.setFilterVisible(false);
        parameters.setSortable(false);
        parameters.setPageable(false);
        parameters.setOutputMarkupId(true);
        form.add(parameters);
        
        add(form);

        // save and cancel at the bottom of the page
        form.add(new SubmitLink("save") {
            @Override
            public void onSubmit() {
                StoredQuery selection = (StoredQuery)storedQueriesDropDown.getDefaultModelObject();
                StoredQueryConfiguration config = createStoredQueryConfiguration(parameterProvider.items);
                onSave(selection.storedQueryId, config);
            }
        });
        form.add(new Link("cancel") {

            @Override
            public void onClick() {
                onCancel();
            }
        });
    }
    
    static StoredQueryConfiguration createStoredQueryConfiguration(
            List<StoredQueryParameterAttribute> items) {
        StoredQueryConfiguration ret = new StoredQueryConfiguration();
        
        for (StoredQueryParameterAttribute i : items) {
            ParameterMapping mapping;
            switch (i.getMappingType()) {
            case BLOCKED:
                mapping = new ParameterMappingBlockValue();
                break;
            case DEFAULT:
                mapping = new ParameterMappingDefaultValue(false, i.getValue());
                break;
            case STATIC:
                mapping = new ParameterMappingDefaultValue(true, i.getValue());
                break;
            case EXPRESSION_CQL:
                mapping = new ParameterMappingExpressionValue("CQL", i.getValue());
                break;
            case NONE:
            default:
                mapping = null;
            }
            if (mapping != null) {
                mapping.setParameterName(i.getParameterName());
                ret.getStoredQueryParameterMappings().add(mapping);
            }
        }
        
        return ret;
    }

    private DropDownChoice storedQueriesDropDown() {
        final DropDownChoice dropdown = new DropDownChoice("storedQueriesDropDown", new Model(),
                new StoredQueryListModel(), new StoredQueryListRenderer());
        
        dropdown.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                StoredQuery selection = (StoredQuery)dropdown.getDefaultModelObject();
                parameterProvider.refreshItems(selection);
                target.addComponent(parameters);
            }
        });
        
        return dropdown;
    }
    
    protected List<StoredQueryListItemType> listStoredQueries()  {
        try {
            WFSContentDataStore contentStore = getContentDataStore();
            return contentStore.getStoredQueryListResponse().getStoredQuery();
        } catch(IOException ie) {
            throw new RuntimeException("Uanble to list stored queries", ie);
        }
    }

    protected WFSContentDataStore getContentDataStore() throws IOException {
        DataStoreInfo store = getCatalog().getStore(storeId, DataStoreInfo.class);
        DataAccess da = store.getDataStore(null);
        
        // Hack. Andrea will refactor around this issue
        if (da instanceof RetypingDataStore) {
            da = ((RetypingDataStore)da).getWrapped();
        }
        WFSContentDataStore contentStore = (WFSContentDataStore)da;
        return contentStore;
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
    
    private class StoredQueryListModel extends LoadableDetachableModel<List<StoredQuery>> {
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
    
    private class StoredQueryListRenderer implements IChoiceRenderer<StoredQuery> {
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
    
    public static class StoredQueryParameterAttribute implements Serializable {
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
        
        public void setMappingType(ParameterMappingType mappingType)
        {
            this.mappingType = mappingType;
        }
        
        public ParameterMappingType getMappingType()
        {
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
        
        public static StoredQueryParameterAttribute create(ParameterExpressionType pet) {
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
            ret.setMappingType(ParameterMappingType.NONE);
            ret.setValue(null);
            return ret;
        }
        
        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder();
            
            ret.append(getParameterName()).append(": ")
                .append("title=").append(title).append(", ")
                .append("type=").append(type).append(", ")
                .append("mappingType=").append(mappingType).append(", ")
                .append("value=").append(value);
            
            return ret.toString();
        }
    }
    
    public static Property<StoredQueryParameterAttribute> ATTR_NAME  = new BeanProperty<StoredQueryParameterAttribute>("parameterName", "parameterName");
    public static Property<StoredQueryParameterAttribute> ATTR_TITLE = new BeanProperty<StoredQueryParameterAttribute>("title", "title");
    public static Property<StoredQueryParameterAttribute> ATTR_TYPE  = new BeanProperty<StoredQueryParameterAttribute>("type", "type");
    public static Property<StoredQueryParameterAttribute> ATTR_MAPTYPE = new BeanProperty<StoredQueryParameterAttribute>("mappingType", "mappingType");
    public static Property<StoredQueryParameterAttribute> ATTR_VALUE = new BeanProperty<StoredQueryParameterAttribute>("value", "value");
    
    public enum ParameterMappingType {
        NONE,
        STATIC,
        DEFAULT,
        EXPRESSION_CQL,
        BLOCKED
    }
    
    public class ParameterMappingTypeRenderer implements IChoiceRenderer<ParameterMappingType> {
        @Override
        public Object getDisplayValue(ParameterMappingType object) {
            return new StringResourceModel("ParameterMappingType."+object.toString(), CascadedWFSStoredQueryAbstractPage.this, null).getString();
        }
        
        @Override
        public String getIdValue(ParameterMappingType object, int index) {
            return object.toString();
        }
    }
    
    public class StoredQueryParameterAttributeProvider extends GeoServerDataProvider<StoredQueryParameterAttribute> {
        private List<StoredQueryParameterAttribute> items = new ArrayList<StoredQueryParameterAttribute>();
        
        public void refreshItems(StoredQuery selection) {
            items.clear();
            if (selection != null) {
                
                StoredQueryDescriptionType desc;
                try {
                    WFSContentDataStore contentStore = getContentDataStore();
                    desc = contentStore.getStoredQueryDescriptionType(selection.getStoredQueryId());
                } catch(IOException ie) {
                    throw new RuntimeException("Unable to describe stored query", ie);
                }
                
                for (ParameterExpressionType pet : desc.getParameter()) {
                    items.add(StoredQueryParameterAttribute.create(pet));
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
    
    protected abstract void onSave(String storedQueryId, StoredQueryConfiguration configuration);
    
    protected abstract void onCancel();
}
