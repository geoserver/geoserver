/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.ProductClass;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.GsIcon;
import org.geoserver.web.wicket.HelpLink;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.ParamResourceModel;

public class OSEOAdminPage extends BaseServiceAdminPage<OSEOInfo> {

    @Serial
    private static final long serialVersionUID = 3056925400600634877L;

    private static class QueryablesConverter implements IConverter<List<String>> {
        static final Pattern COMMA_SEPARATED = Pattern.compile("\\s*,\\s*", Pattern.MULTILINE);

        @Override
        public String convertToString(List<String> srsList, Locale locale) {
            if (srsList == null) return null;
            return srsList.stream().collect(Collectors.joining(", "));
        }

        @Override
        public List<String> convertToObject(String value, Locale locale) {
            if (value == null || value.trim().equals("")) return null;
            return new ArrayList<>(Arrays.asList(COMMA_SEPARATED.split(value)));
        }
    }

    DataStoreInfo backend;
    GeoServerTablePanel<ProductClass> productClasses;
    private IModel<OSEOInfo> model;

    public OSEOAdminPage() {
        super();
    }

    public OSEOAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public OSEOAdminPage(OSEOInfo service) {
        super(service);
    }

    @Override
    protected Class<OSEOInfo> getServiceClass() {
        return OSEOInfo.class;
    }

    @Override
    protected AdminPagePanel buildPanel(String id, IModel<OSEOInfo> info, Form form) {
        this.model = info;
        return new OSEOAdminPanel(id, info);
    }

    @SuppressWarnings({"unchecked", "serial"})
    private class OSEOAdminPanel extends AdminPagePanel {

        OSEOAdminPanel(String id, IModel<OSEOInfo> info) {
            super(id, info);
            OSEOInfo oseo = info.getObject();

            TextField<String> attribution = new TextField<>("attribution");
            add(attribution);

            TextArea<List<String>> globalQueryables =
                    new TextArea<>(
                            "globalQueryables",
                            LiveCollectionModel.list(new PropertyModel<>(info, "globalQueryables"))) {
                        @Override
                        public <C> IConverter<C> getConverter(Class<C> type) {
                            if (type.isAssignableFrom(ArrayList.class))
                                return (IConverter<C>) new QueryablesConverter();
                            return super.getConverter(type);
                        }
                    };
            globalQueryables.setType(List.class);
            add(globalQueryables);

            backend = null;
            if (oseo.getOpenSearchAccessStoreId() != null) {
                backend = getCatalog().getDataStore(oseo.getOpenSearchAccessStoreId());
            }
            DropDownChoice<DataStoreInfo> openSearchAccessReference = new DropDownChoice<>(
                    "openSearchAccessId",
                    new PropertyModel<>(OSEOAdminPage.this, "backend"),
                    new OpenSearchAccessListModel(),
                    new StoreListChoiceRenderer());
            add(openSearchAccessReference);
            final TextField<Integer> aggregatesCacheTTL = new TextField<>("aggregatesCacheTTL", Integer.class);
            aggregatesCacheTTL.add(RangeValidator.minimum(0));
            aggregatesCacheTTL.setRequired(true);
            add(aggregatesCacheTTL);
            List<String> units = Arrays.asList(TimeUnit.HOURS.name(), TimeUnit.MINUTES.name(), TimeUnit.SECONDS.name());
            DropDownChoice<String> aggregatesCacheTTLUnit = new DropDownChoice<>("aggregatesCacheTTLUnit", units);
            aggregatesCacheTTLUnit.setRequired(true);
            add(aggregatesCacheTTLUnit);
            final TextField<Integer> recordsPerPage = new TextField<>("recordsPerPage", Integer.class);
            recordsPerPage.add(RangeValidator.minimum(0));
            recordsPerPage.setRequired(true);
            add(recordsPerPage);
            final TextField<Integer> maximumRecordsPerPage = new TextField<>("maximumRecordsPerPage", Integer.class);
            maximumRecordsPerPage.add(RangeValidator.minimum(0));
            maximumRecordsPerPage.setRequired(true);
            add(maximumRecordsPerPage);

            productClasses = new GeoServerTablePanel<>("productClasses", new ProductClassesProvider(info), true) {

                @Override
                protected Component getComponentForProperty(
                        String id,
                        IModel<ProductClass> itemModel,
                        GeoServerDataProvider.Property<ProductClass> property) {
                    if (ProductClassesProvider.REMOVE.equals(property)) {
                        return removeLink(id, itemModel);
                    } else {
                        Fragment f;
                        if ("namespace".equals(property.getName())) {
                            f = new Fragment(id, "longtext", OSEOAdminPanel.this);
                        } else {
                            f = new Fragment(id, "text", OSEOAdminPanel.this);
                        }
                        TextField<?> text = new TextField<>("text", property.getModel(itemModel));
                        f.add(text);
                        return f;
                    }
                }
            };
            productClasses.setFilterVisible(false);
            productClasses.setSortable(false);
            productClasses.setPageable(false);
            productClasses.setOutputMarkupId(true);
            productClasses.setItemReuseStrategy(new DefaultItemReuseStrategy());
            productClasses.setFilterable(false);
            productClasses.setSelectable(false);
            add(productClasses);
            add(new HelpLink("productClassesHelp", this).setDialog(dialog));

            add(addLink());

            CheckBox skipNumberMatched =
                    new CheckBox("skipNumberMatched", new PropertyModel<>(info, "skipNumberMatched"));
            add(skipNumberMatched);
        }

        private GeoServerAjaxFormLink addLink() {
            return new GeoServerAjaxFormLink("addClass") {
                @Override
                public void onClick(AjaxRequestTarget target, Form form) {
                    productClasses.processInputs();
                    OSEOInfo oseo = model.getObject();
                    oseo.getProductClasses().add(new ProductClass("", "", ""));
                    target.add(productClasses);
                }
            };
        }

        private Component removeLink(String id, IModel<ProductClass> itemModel) {
            Fragment f = new Fragment(id, "imageLink", this);
            final ProductClass entry = itemModel.getObject();
            GeoServerAjaxFormLink link = new GeoServerAjaxFormLink("link") {

                @Override
                protected void onClick(AjaxRequestTarget target, Form form) {
                    productClasses.processInputs();
                    OSEOInfo oseo = model.getObject();
                    oseo.getProductClasses().remove(entry);
                    target.add(productClasses);
                }
            };
            f.add(link);
            link.add(new GsIcon("image", "gs-icon-delete"));
            return f;
        }
    }

    @Override
    protected String getServiceName() {
        return "OSEO";
    }

    @Override
    protected String getServiceType() {
        return "OSEO";
    }

    @Override
    protected void handleSubmit(OSEOInfo info) {
        // validate on save/apply only: doing it in a form validator would also fire on tab switches,
        // which run full form processing, and wrongly reject a valid configuration
        validate(info);
        if (backend != null) {
            info.setOpenSearchAccessStoreId(backend.getId());
        } else {
            info.setOpenSearchAccessStoreId(null);
        }
        super.handleSubmit(info);
    }

    private void validate(OSEOInfo info) {
        Integer records = info.getRecordsPerPage();
        Integer maximum = info.getMaximumRecordsPerPage();
        if (records != null && maximum != null && records > maximum) {
            throw new IllegalArgumentException(
                    new ParamResourceModel("recordsGreaterThanMaximum", this, records, maximum).getString());
        }
        for (ProductClass pc : info.getProductClasses()) {
            if (Strings.isEmpty(pc.getName())
                    || Strings.isEmpty(pc.getPrefix())
                    || Strings.isEmpty(pc.getNamespace())) {
                throw new IllegalArgumentException(new ParamResourceModel("paramClassNotEmpty", this).getString());
            }
        }
    }
}
