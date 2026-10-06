/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.web;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.ogcapi.v1.styles.StylesConformance;
import org.geoserver.ogcapi.v1.styles.StylesServiceInfo;
import org.geoserver.web.ogcapi.ConformanceTable;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.services.BaseServiceAdminPage;

/** Configuration page for the OGC API - Styles service. */
public class StylesAdminPage extends BaseServiceAdminPage<StylesServiceInfo> {

    public StylesAdminPage() {
        this(new PageParameters());
    }

    public StylesAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public StylesAdminPage(StylesServiceInfo service) {
        super(service);
    }

    @Override
    protected Class<StylesServiceInfo> getServiceClass() {
        return StylesServiceInfo.class;
    }

    @Override
    protected String getServiceName() {
        return "Styles";
    }

    @Override
    protected String getServiceType() {
        return "Styles";
    }

    @Override
    protected AdminPagePanel buildPanel(String id, IModel<StylesServiceInfo> info, Form form) {
        return new StylesAdminPanel(id, info);
    }

    private static class StylesAdminPanel extends AdminPagePanel {

        StylesAdminPanel(String id, IModel<StylesServiceInfo> info) {
            super(id, info);
            // the service is reloaded per request, so the configuration is resolved on each access
            IModel<ConformanceInfo<?>> conformance = IModel.of(() -> (StylesConformance) info.getObject()
                    .getMetadata()
                    .computeIfAbsent(StylesConformance.METADATA_KEY, k -> new StylesConformance()));
            add(new ConformanceTable("stylesConformance", conformance, this));
        }
    }
}
