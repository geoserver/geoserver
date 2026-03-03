/* (c) 2014 - 2019 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;

/**
 * Edits a {@link DimensionInfo} object for the specified resource
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DimensionEditor extends DimensionEditorBase<DimensionInfo> {

    private boolean isCssEmpty = org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty(getClass());

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        //if the panel-specific CSS file contains actual css then have the browser load the css 
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    public DimensionEditor(String id, IModel<DimensionInfo> model, ResourceInfo resource, Class<?> type) {
        super(id, model, resource, type);
    }

    public DimensionEditor(
            String id,
            IModel<DimensionInfo> model,
            ResourceInfo resource,
            Class<?> type,
            boolean editNearestMatch,
            boolean editRawNearestMatch) {
        super(id, model, resource, type, editNearestMatch, editRawNearestMatch);
    }

    @Override
    protected DimensionInfo infoOf() {
        return new DimensionInfoImpl();
    }

    @Override
    protected DimensionInfo infoOf(DimensionInfo info) {
        return new DimensionInfoImpl(info);
    }
}
