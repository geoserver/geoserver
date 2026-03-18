/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

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
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.services.DisabledVersionsPanel;
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
    protected AdminPagePanel buildPanel(String id, IModel<WCSInfo> info, Form form) {
        return new WCSAdminPanel(id, info);
    }

    @Override
    protected String getServiceName() {
        return "WCS";
    }

    @Override
    protected String getServiceType() {
        return "WCS";
    }

    private class WCSAdminPanel extends AdminPagePanel {

        private static final boolean isCssEmpty = IsWicketCssFileEmpty(WCSAdminPage.WCSAdminPanel.class);

        @Override
        public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
            super.renderHead(response);
            // if the panel-specific CSS file contains actual css then have the browser load the css
            if (!isCssEmpty) {
                response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                        new org.apache.wicket.request.resource.PackageResourceReference(
                                getClass(), getClass().getSimpleName() + ".css")));
            }
        }

        public WCSAdminPanel(String id, IModel info) {
            super(id, info);

            // service control
            add(new DisabledVersionsPanel(
                    "disabledVersions", new PropertyModel<>(info, "disabledVersions"), getServiceType()));

            // overview policy
            add(new DropDownChoice<>(
                    "overviewPolicy", Arrays.asList(OverviewPolicy.values()), new OverviewPolicyRenderer()));
            add(new CheckBox("subsamplingEnabled"));

            // limited srs list
            TextArea<List<String>> srsList =
                    new SRSListTextArea("srs", LiveCollectionModel.list(new PropertyModel<>(info, "sRS")));
            add(srsList);

            // resource limits
            TextField<Integer> maxInputMemory = new TextField<>("maxInputMemory");
            maxInputMemory.add(RangeValidator.minimum(0l));
            add(maxInputMemory);
            TextField<Integer> maxOutputMemory = new TextField<>("maxOutputMemory");
            maxOutputMemory.add(RangeValidator.minimum(0l));
            add(maxOutputMemory);
            TextField<Integer> defaultDeflateCompressionLevel = new TextField<>("defaultDeflateCompressionLevel");
            defaultDeflateCompressionLevel.add(RangeValidator.range(1, 9));
            add(defaultDeflateCompressionLevel);

            // max dimension values
            TextField<Integer> maxRequestedDimensionValues = new TextField<>("maxRequestedDimensionValues");
            maxRequestedDimensionValues.add(RangeValidator.minimum(0));
            add(maxRequestedDimensionValues);

            // lat-lon VS lon-lat
            add(new CheckBox("latLon"));
            add(new LocalesDropdown("defaultLocale", new PropertyModel<>(info, "defaultLocale")));
        }
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
