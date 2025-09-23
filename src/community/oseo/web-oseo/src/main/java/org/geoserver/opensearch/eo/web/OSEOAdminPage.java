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
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.ProductClass;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
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
    @SuppressWarnings({"unchecked", "serial"})
    protected void build(final IModel info, Form form) {
        this.model = info;
        OSEOInfo oseo = (OSEOInfo) info.getObject();

        TextField<String> attribution = new TextField<>("attribution");
        form.add(attribution);

        TextArea<List<String>> globalQueryables =
                new TextArea<List<String>>(
                        "globalQueryables", LiveCollectionModel.list(new PropertyModel(info, "globalQueryables"))) {
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        if (type.isAssignableFrom(ArrayList.class)) return (IConverter<C>) new QueryablesConverter();
                        return super.getConverter(type);
                    }
                };
        globalQueryables.setType(List.class);
        form.add(globalQueryables);

        this.backend = null;
        if (oseo.getOpenSearchAccessStoreId() != null) {
            this.backend = getCatalog().getDataStore(oseo.getOpenSearchAccessStoreId());
        }
        DropDownChoice<DataStoreInfo> openSearchAccessReference = new DropDownChoice<>(
                "openSearchAccessId",
                new PropertyModel<DataStoreInfo>(this, "backend"),
                new OpenSearchAccessListModel(),
                new StoreListChoiceRenderer());
        form.add(openSearchAccessReference);
        final TextField<Integer> aggregatesCacheTTL = new TextField<>("aggregatesCacheTTL", Integer.class);
        aggregatesCacheTTL.add(RangeValidator.minimum(0));
        aggregatesCacheTTL.setRequired(true);
        form.add(aggregatesCacheTTL);
        List<String> units =
                Arrays.asList(new String[] {TimeUnit.HOURS.name(), TimeUnit.MINUTES.name(), TimeUnit.SECONDS.name()});
        DropDownChoice<String> aggregatesCacheTTLUnit = new DropDownChoice<String>("aggregatesCacheTTLUnit", units);
        aggregatesCacheTTLUnit.setRequired(true);
        form.add(aggregatesCacheTTLUnit);
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
                if (recordsPerPage != null && maximum != null && records > maximum) {
                    form.error(new ParamResourceModel("recordsGreaterThanMaximum", form, records, maximum));
                }

                // doing the validation here, as just making the text fields as
                // required makes one lose edits, and error messages do not show up
                productClasses.processInputs();
                List<ProductClass> productClasses = oseo.getProductClasses();
                for (ProductClass pc : productClasses) {
                    if (Strings.isEmpty(pc.getName())
                            || Strings.isEmpty(pc.getPrefix())
                            || Strings.isEmpty(pc.getNamespace())) {
                        form.error(new ParamResourceModel("paramClassNotEmpty", form).getString());
                        break;
                    }
                }
            }

            @Override
            public FormComponent<?>[] getDependentFormComponents() {
                return new FormComponent<?>[] {recordsPerPage, maximumRecordsPerPage};
            }
        });

        productClasses =
                new GeoServerTablePanel<ProductClass>("productClasses", new ProductClassesProvider(info), true) {

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
                                f = new Fragment(id, "longtext", OSEOAdminPage.this);
                            } else {
                                f = new Fragment(id, "text", OSEOAdminPage.this);
                            }
                            TextField<?> text = new TextField("text", property.getModel(itemModel));
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
        form.add(productClasses);
        form.add(new HelpLink("productClassesHelp", this).setDialog(dialog));

        form.add(addLink());
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
        Fragment f = new Fragment(id, "imageLink", OSEOAdminPage.this);
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
        Image image =
                new Image("image", new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/delete.png"));
        link.add(image);
        return f;
    }

    @Override
    protected String getServiceName() {
        return "OSEO";
    }

    @Override
    protected void handleSubmit(OSEOInfo info) {
        if (backend != null) {
            info.setOpenSearchAccessStoreId(backend.getId());
        } else {
            info.setOpenSearchAccessStoreId(null);
        }
        super.handleSubmit(info);
    }
}
