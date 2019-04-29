/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.gwc.GWC;
import org.geowebcache.grid.GridSet;

public class GridSetEditPage extends AbstractGridSetPage {

    private static final long serialVersionUID = 1748616637023642755L;

    private String originalName;

    public GridSetEditPage(PageParameters parameters) {
        super(parameters);

        GridSetInfo info = form.getModelObject();
        originalName = info.getName();

        if (info.isInternal()) {
            form.info(new ResourceModel("GridSetEditPage.internalGridSetMessage").getObject());
            name.getFormComponent().setEnabled(false);
            description.setEnabled(false);
            crs.setEnabled(false);
            tileWidth.getFormComponent().setEnabled(false);
            tileHeight.getFormComponent().setEnabled(false);
            bounds.setEnabled(false);
            computeBoundsLink.setEnabled(false);
            tileMatrixSetEditor.setEnabled(false);
            saveLink.setVisible(false);
            addLevelLink.setVisible(false);
        }
    }

    @Override
    protected void onSave(AjaxRequestTarget target, Form<?> form) {
        GridSetInfo info = (GridSetInfo) form.getModelObject();
        GridSet newGridset = null;
        try {
            newGridset = toGridSet(target, form, info);
        } catch (Exception e) {
            form.error(e.getMessage());
            target.add(form);
            return;
        }

        try {
            // TODO: warn and eliminate caches
            GWC gwc = GWC.get();
            gwc.modifyGridSet(originalName, newGridset);
            doReturn(GridSetsPage.class);
        } catch (Exception e) {
            e.printStackTrace();
            form.error("Error saving gridset: " + e.getMessage());
            target.add(form);
        }
    }
}
