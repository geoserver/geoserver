/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.diskquota;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.geoserver.gwc.ConfigurableQuotaStoreProvider;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Warns the administrator that the
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DiskQuotaWarningPanel extends Panel {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(DiskQuotaWarningPanel.class);

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

    @Serial
    private static final long serialVersionUID = -343944585740739250L;

    public DiskQuotaWarningPanel(String id) {
        super(id);

        Exception exception = getException();
        Label label = new Label("diskQuotaError", new Model<>());
        if (exception != null) {
            ParamResourceModel rm = new ParamResourceModel("GWC.diskQuotaLoadFailed", null, exception.getMessage());
            label.setDefaultModelObject(rm.getString());
        } else {
            label.setVisible(false);
        }
        add(label);
    }

    static Exception getException() {
        ConfigurableQuotaStoreProvider provider =
                GeoServerApplication.get().getBeanOfType(ConfigurableQuotaStoreProvider.class);
        Exception exception = provider.getException();
        return exception;
    }
}
