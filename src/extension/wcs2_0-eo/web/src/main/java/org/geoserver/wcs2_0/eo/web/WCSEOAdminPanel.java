/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.web;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.wcs2_0.eo.WCSEOMetadata;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.util.MapModel;

public class WCSEOAdminPanel extends AdminPagePanel {
    private static final long serialVersionUID = 1302234327415740649L;

    public WCSEOAdminPanel(String id, IModel<?> model) {
        super(id, model);

        PropertyModel<MetadataMap> metadata = new PropertyModel<MetadataMap>(model, "metadata");

        CheckBox enabled =
                new CheckBox("enabled", new MapModel(metadata, WCSEOMetadata.ENABLED.key));
        add(enabled);

        TextField<Integer> defaultCount =
                new TextField<Integer>(
                        "defaultCount",
                        new MapModel(metadata, WCSEOMetadata.COUNT_DEFAULT.key),
                        Integer.class);
        defaultCount.add(RangeValidator.minimum(1));
        add(defaultCount);
    }
}
