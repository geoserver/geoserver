/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.wicket.ParamResourceModel;

public class OSEOAdminPage extends BaseServiceAdminPage<OSEOInfo> {

    private static final long serialVersionUID = 3056925400600634877L;

    DataStoreInfo backend;

    public OSEOAdminPage() {
        super();
    }

    public OSEOAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public OSEOAdminPage(OSEOInfo service) {
        super(service);
    }

    protected Class<OSEOInfo> getServiceClass() {
        return OSEOInfo.class;
    }

    @SuppressWarnings({ "rawtypes", "unchecked", "serial" })
    protected void build(final IModel info, Form form) {
        OSEOInfo model = (OSEOInfo) info.getObject();

        this.backend = null;
        if (model.getOpenSearchAccessStoreId() != null) {
            this.backend = getCatalog().getDataStore(model.getOpenSearchAccessStoreId());
        }
        DropDownChoice<DataStoreInfo> openSearchAccessReference = new DropDownChoice<>(
                "openSearchAccessId", new PropertyModel<DataStoreInfo>(this, "backend"),
                new OpenSearchAccessListModel(), new StoreListChoiceRenderer());
        form.add(openSearchAccessReference);
        
        final TextField<Integer> recordsPerPage = new TextField<>("recordsPerPage", Integer.class);
        recordsPerPage.add(RangeValidator.minimum(0));
        recordsPerPage.setRequired(true);
        form.add(recordsPerPage);
        final TextField<Integer> maximumRecordsPerPage = new TextField<>("maximumRecordsPerPage", Integer.class);
        maximumRecordsPerPage.add(RangeValidator.minimum(0));
        maximumRecordsPerPage.setRequired(true);
        form.add(maximumRecordsPerPage);
        // check that records is lower or equal than maximum
        form.add(new AbstractFormValidator() {
            
            @Override
            public void validate(Form<?> form) {
                Integer records = recordsPerPage.getConvertedInput();
                Integer maximum = maximumRecordsPerPage.getConvertedInput();
                if(recordsPerPage != null && maximum != null && records > maximum) {
                    form.error(new ParamResourceModel("recordsGreaterThanMaximum", form, records, maximum));
                }
            }
            
            @Override
            public FormComponent<?>[] getDependentFormComponents() {
                return new FormComponent<?>[] {recordsPerPage, maximumRecordsPerPage};
            }
        });
    }

    protected String getServiceName() {
        return "OSEO";
    }
    
    @Override
    protected void handleSubmit(OSEOInfo info) {
        if(backend != null) {
            info.setOpenSearchAccessStoreId(backend.getId());
        } else {
            info.setOpenSearchAccessStoreId(null);
        }
        super.handleSubmit(info);
    }
}
