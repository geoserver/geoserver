/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.web.services.BaseServiceAdminPage;

public class OSEOAdminPage extends BaseServiceAdminPage<OSEOInfo> {

    private static final long serialVersionUID = 3056925400600634877L;

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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void build(final IModel info, Form form) {
        OSEOInfo model = (OSEOInfo) info.getObject();
    }

    protected String getServiceName() {
        return "OSEO";
    }
}
