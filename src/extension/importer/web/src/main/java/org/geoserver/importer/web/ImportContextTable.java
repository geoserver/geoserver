/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.web;

import java.util.Date;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.importer.ImportContext;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.ocpsoft.pretty.time.PrettyTime;

public class ImportContextTable extends GeoServerTablePanel<ImportContext> {

    static PrettyTime PRETTY_TIME = new PrettyTime();

    public ImportContextTable(String id, ImportContextProvider dataProvider) {
        super(id, dataProvider);
    }

    public ImportContextTable(String id, ImportContextProvider dataProvider, boolean selectable) {
        super(id, dataProvider, selectable);
    }

    @Override
    protected Component getComponentForProperty(String id, IModel itemModel, Property property) {
        if (ImportContextProvider.ID == property) {
            PageParameters pp = new PageParameters();
            pp.add("id", property.getModel(itemModel).getObject());
            return new SimpleBookmarkableLink(
                    id, ImportPage.class, property.getModel(itemModel), pp);
        } else if (ImportContextProvider.CREATED == property
                || ImportContextProvider.UPDATED == property) {
            Date date = (Date) property.getModel(itemModel).getObject();
            String pretty = PRETTY_TIME.format(date);
            return new Label(id, pretty);
        }

        return new Label(id, property.getModel(itemModel));
    }
}
