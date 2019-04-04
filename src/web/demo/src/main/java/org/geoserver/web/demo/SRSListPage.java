/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.SRSListPanel;

/** Lists all the SRS available in GeoServer */
@SuppressWarnings("serial")
public class SRSListPage extends GeoServerBasePage {

    public SRSListPage() {
        add(srsListPanel());
    }

    SRSListPanel srsListPanel() {
        return new SRSListPanel("srsListPanel") {

            @Override
            protected void onCodeClicked(AjaxRequestTarget target, String epsgCode) {
                setResponsePage(
                        SRSDescriptionPage.class,
                        new PageParameters().add("code", "EPSG:" + epsgCode));
            }
        };
    }
}
