/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.gwc.GWC;
import org.geowebcache.grid.GridSet;

public class GridSetNewPage extends AbstractGridSetPage {

    private static final long serialVersionUID = -3748376561268773207L;

    public GridSetNewPage(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onSave(AjaxRequestTarget target, Form<?> form) {
        GridSetInfo info = (GridSetInfo) form.getModelObject();
        GridSet gridset = null;
        try {
            gridset = toGridSet(target, form, info);
        } catch (Exception e) {
            form.error(e.getMessage());
            target.add(form);
            return;
        }

        try {
            GWC gwc = GWC.get();
            gwc.addGridSet(gridset);
            doReturn(GridSetsPage.class);
        } catch (Exception e) {
            form.error(e.getMessage());
            target.add(form);
        }
    }
}
