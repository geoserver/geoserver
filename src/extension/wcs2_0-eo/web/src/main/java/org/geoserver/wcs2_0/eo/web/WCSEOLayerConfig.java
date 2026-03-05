/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.web;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.IOException;
import java.io.Serial;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wcs2_0.eo.WCSEOMetadata;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;

/** A configuration panel for CoverageInfo properties that related to WCS publication */
public class WCSEOLayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(WCSEOLayerConfig.class);

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
    private static final long serialVersionUID = 5069332181659419455L;

    public WCSEOLayerConfig(String id, IModel<LayerInfo> model) {
        super(id, model);

        // this panel is visible only if the reader is a structured grid coverage one
        setVisible(isStructuredCoverage(model));

        // add the checkbox to enable exposing a layer as a dataset
        MapModel<Boolean> datasetModel =
                new MapModel<>(new PropertyModel<>(model, "resource.metadata"), WCSEOMetadata.DATASET.key);
        CheckBox dataset = new CheckBox("dataset", datasetModel);
        add(dataset);
    }

    private boolean isStructuredCoverage(IModel<LayerInfo> model) {
        try {
            CoverageInfo ci = (CoverageInfo) model.getObject().getResource();
            boolean result = ci.getGridCoverageReader(null, null) instanceof StructuredGridCoverage2DReader;
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Faied to load reader to determine if it's WCS EO Dataset worthy", e);
        }
    }
}
