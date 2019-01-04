/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.wmts;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.web.services.BaseServiceAdminPage;

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
    protected void build(IModel info, Form form) {}
}
