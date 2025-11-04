/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import static org.geoserver.backuprestore.web.BackupRestoreExecutionsProvider.*;

import java.util.Date;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.backuprestore.AbstractExecutionAdapter;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.ocpsoft.pretty.time.PrettyTime;

/** @author Alessio Fabiani, GeoSolutions */
public class BackupRestoreExecutionsTable<T extends AbstractExecutionAdapter> extends GeoServerTablePanel<T> {

    static PrettyTime PRETTY_TIME = new PrettyTime();
    private Class<T> clazz;

    @SuppressWarnings("unchecked")
    public BackupRestoreExecutionsTable(String id, BackupRestoreExecutionsProvider dataProvider, Class<T> clazz) {
        super(id, (GeoServerDataProvider<T>) dataProvider);
        this.clazz = clazz;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public BackupRestoreExecutionsTable(
            String id, BackupRestoreExecutionsProvider dataProvider, boolean selectable, Class<T> clazz) {
        super(id, (GeoServerDataProvider<T>) dataProvider, selectable);
        this.clazz = clazz;
    }

    public Class<T> getType() {
        return this.clazz;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Component getComponentForProperty(String id, IModel itemModel, Property property) {
        if (ID == property) {
            PageParameters pp = new PageParameters();
            pp.add("id", property.getModel(itemModel).getObject());
            pp.add("clazz", getType().getSimpleName());

            return new SimpleBookmarkableLink(id, BackupRestorePage.class, property.getModel(itemModel), pp);
        } else if (STARTED == property) {
            Date date = (Date) property.getModel(itemModel).getObject();
            String pretty = PRETTY_TIME.format(date);
            return new Label(id, pretty);
        } else if (ARCHIVEFILE == property) {
            String pretty = ((Resource) property.getModel(itemModel).getObject()).name();
            return new Label(id, pretty);
        }

        return new Label(id, property.getModel(itemModel));
    }
}
