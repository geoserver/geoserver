/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.geoserver.gwc.GWC;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.grid.GridSet;

/**
 * Panel listing the configured GridSet object on a table
 *
 * @author groldan
 * @see GridSetTableProvider
 */
public abstract class GridSetListTablePanel extends GeoServerTablePanel<GridSet> {

    private static final long serialVersionUID = 5957961031378924960L;

    public GridSetListTablePanel(
            final String id, final GridSetTableProvider provider, final boolean selectable) {
        super(id, provider, selectable);
    }

    @Override
    protected Component getComponentForProperty(
            final String id, final IModel<GridSet> itemModel, final Property<GridSet> property) {

        if (property == GridSetTableProvider.NAME) {
            GridSet gridSet = itemModel.getObject();
            return nameLink(id, gridSet);

        } else if (property == GridSetTableProvider.EPSG_CODE) {

            String epsgCode = (String) property.getModel(itemModel).getObject();
            return new Label(id, epsgCode);

        } else if (property == GridSetTableProvider.TILE_DIMENSION) {

            String tileDimension = (String) property.getModel(itemModel).getObject();
            return new Label(id, tileDimension);

        } else if (property == GridSetTableProvider.ZOOM_LEVELS) {

            Integer zoomLevels = (Integer) property.getModel(itemModel).getObject();
            return new Label(id, zoomLevels.toString());

        } else if (property == GridSetTableProvider.QUOTA_USED) {

            Quota usedQuota = (Quota) property.getModel(itemModel).getObject();
            String quotaStr = usedQuota == null ? "N/A" : usedQuota.toNiceString();
            return new Label(id, quotaStr);

        } else if (property == GridSetTableProvider.ACTION_LINK) {
            String gridSetName = (String) property.getModel(itemModel).getObject();

            Component actionLink = actionLink(id, gridSetName);

            return actionLink;
        }

        throw new IllegalArgumentException("Unknown property: " + property.getName());
    }

    protected abstract Component nameLink(final String id, final GridSet gridSet);

    protected abstract Component actionLink(final String id, String gridSetName);

    /**
     * Overrides to return a disabled and non selectable checkbox if the GridSet for the item is an
     * internally defined one
     *
     * @see org.geoserver.web.wicket.GeoServerTablePanel#selectOneCheckbox
     */
    @Override
    protected CheckBox selectOneCheckbox(Item<GridSet> item) {
        CheckBox cb = super.selectOneCheckbox(item);

        GridSet gs = (GridSet) item.getModelObject();

        String name = gs.getName();
        final boolean internal = GWC.get().isInternalGridSet(name);
        if (internal) {
            cb.setEnabled(false);
            cb.setModelObject(Boolean.FALSE);
        }
        return cb;
    }

    /**
     * Overrides to remove any internal gridset from the list
     *
     * @see org.geoserver.web.wicket.GeoServerTablePanel#getSelection()
     */
    @Override
    public List<GridSet> getSelection() {
        List<GridSet> selection = new ArrayList<GridSet>(super.getSelection());
        for (Iterator<GridSet> it = selection.iterator(); it.hasNext(); ) {
            GridSet g = it.next();
            if (GWC.get().isInternalGridSet(g.getName())) {
                it.remove();
            }
        }
        return selection;
    }
}
