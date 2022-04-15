/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web;

import java.util.Arrays;
import java.util.List;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.web.data.resource.LocalesDropdown;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.SRSListTextArea;
import org.geotools.coverage.grid.io.OverviewPolicy;

public class WCSAdminPage extends BaseServiceAdminPage<WCSInfo> {

    public WCSAdminPage() {
        super();
    }

    public WCSAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public WCSAdminPage(WCSInfo service) {
        super(service);
    }

    @Override
    protected Class<WCSInfo> getServiceClass() {
        return WCSInfo.class;
    }

    @Override
    protected void build(IModel info, Form form) {
        // overview policy
        form.add(
                new DropDownChoice<>(
                        "overviewPolicy",
                        Arrays.asList(OverviewPolicy.values()),
                        new OverviewPolicyRenderer()));
        form.add(new CheckBox("subsamplingEnabled"));

        // limited srs list
        TextArea<List<String>> srsList =
                new SRSListTextArea(
                        "srs", LiveCollectionModel.list(new PropertyModel<>(info, "sRS")));
        form.add(srsList);

        // resource limits
        TextField<Integer> maxInputMemory = new TextField<>("maxInputMemory");
        maxInputMemory.add(RangeValidator.minimum(0l));
        form.add(maxInputMemory);
        TextField<Integer> maxOutputMemory = new TextField<>("maxOutputMemory");
        maxOutputMemory.add(RangeValidator.minimum(0l));
        form.add(maxOutputMemory);
        TextField<Integer> defaultDeflateCompressionLevel =
                new TextField<>("defaultDeflateCompressionLevel");
        defaultDeflateCompressionLevel.add(RangeValidator.range(1, 9));
        form.add(defaultDeflateCompressionLevel);
        // max dimension values
        TextField<Integer> maxRequestedDimensionValues =
                new TextField<>("maxRequestedDimensionValues");
        maxRequestedDimensionValues.add(RangeValidator.minimum(0));
        form.add(maxRequestedDimensionValues);

        // lat-lon VS lon-lat
        form.add(new CheckBox("latLon"));
        form.add(new LocalesDropdown("defaultLocale", new PropertyModel<>(info, "defaultLocale")));
    }

    @Override
    protected String getServiceName() {
        return "WCS";
    }

    private class OverviewPolicyRenderer extends ChoiceRenderer<OverviewPolicy> {

        @Override
        public Object getDisplayValue(OverviewPolicy object) {
            return new StringResourceModel(object.name(), WCSAdminPage.this, null).getString();
        }

        @Override
        public String getIdValue(OverviewPolicy object, int index) {
            return object.name();
        }
    }

    @Override
    protected boolean supportInternationalContent() {
        return true;
    }
}
