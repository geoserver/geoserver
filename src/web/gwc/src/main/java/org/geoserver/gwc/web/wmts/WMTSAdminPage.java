/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.wmts;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.services.DisabledVersionsPanel;

public class WMTSAdminPage extends BaseServiceAdminPage<WMTSInfo> {

    public WMTSAdminPage() {
        this(new PageParameters());
    }

    public WMTSAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public WMTSAdminPage(WMTSInfo service) {
        super(service);
    }

    @Override
    protected Class<WMTSInfo> getServiceClass() {
        return WMTSInfo.class;
    }

    @Override
    protected String getServiceName() {
        return "WMTS";
    }

    @Override
    protected String getServiceType() {
        return "WMTS";
    }

    @Override
    protected AdminPagePanel buildPanel(String id, IModel<WMTSInfo> info, Form form) {
        return new WMTSAdminPanel(id, info);
    }

    private class WMTSAdminPanel extends AdminPagePanel {

        private static final boolean isCssEmpty = IsWicketCssFileEmpty(WMTSAdminPage.WMTSAdminPanel.class);

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

        public WMTSAdminPanel(String id, IModel info) {
            super(id, info);

            // service control
            add(new DisabledVersionsPanel(
                    "disabledVersions", new PropertyModel<>(info, "disabledVersions"), getServiceType()));
        }
    }
}
