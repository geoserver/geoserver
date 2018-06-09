/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.web;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.csw.CSWInfo;
import org.geoserver.csw.DirectDownloadSettings;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.util.MetadataMapModel;

public class CSWAdminPage extends BaseServiceAdminPage<CSWInfo> {

    private static final long serialVersionUID = 8779684527875704719L;

    public CSWAdminPage() {
        super();
    }

    public CSWAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public CSWAdminPage(CSWInfo service) {
        super(service);
    }

    protected Class<CSWInfo> getServiceClass() {
        return CSWInfo.class;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void build(final IModel info, Form form) {

        CSWInfo model = (CSWInfo) info.getObject();
        MetadataMap metadata = model.getMetadata();

        DirectDownloadSettings settings =
                DirectDownloadSettings.getSettingsFromMetadata(metadata, null);
        metadata = metadata == null ? new MetadataMap() : metadata;
        if (settings == null) {
            metadata.getMap()
                    .put(DirectDownloadSettings.DIRECTDOWNLOAD_KEY, new DirectDownloadSettings());
        }

        IModel<DirectDownloadSettings> directDownloadModel =
                new MetadataMapModel(
                        metadata,
                        DirectDownloadSettings.DIRECTDOWNLOAD_KEY,
                        DirectDownloadSettings.class);

        form.add(
                new CheckBox(
                        "directDownloadEnabled",
                        new PropertyModel(directDownloadModel, "directDownloadEnabled")));
        TextField maxDownloadSize =
                new TextField<Integer>(
                        "maxDownloadSize",
                        new PropertyModel(directDownloadModel, "maxDownloadSize"));
        maxDownloadSize.add(RangeValidator.minimum(0l));
        form.add(maxDownloadSize);
    }

    protected String getServiceName() {
        return "CSW";
    }
}
