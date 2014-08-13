/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.web;

import java.io.IOException;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.wcs2_0.eo.WCSEOMetadata;
import org.geoserver.web.publish.LayerConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.util.MetadataMapModel;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;

/**
 * A configuration panel for CoverageInfo properties that related to WCS publication
 */
@SuppressWarnings("serial")
public class WCSEOLayerConfig extends LayerConfigurationPanel {

    public WCSEOLayerConfig(String id, IModel<LayerInfo> model) {
        super(id, model);

        // this panel is visible only if the reader is a structured grid coverage one
        setVisible(isStructuredCoverage(model));

        // add the checkbox to enable exposing a layer as a dataset
        MapModel datasetModel = new MapModel(new PropertyModel<MetadataMap>(model,
                "resource.metadata"), WCSEOMetadata.DATASET.key);
        CheckBox dataset = new CheckBox("dataset", datasetModel);
        add(dataset);
    }

    private boolean isStructuredCoverage(IModel<LayerInfo> model) {
        try {
            CoverageInfo ci = (CoverageInfo) model.getObject().getResource();
            boolean result = ci.getGridCoverageReader(null, null) instanceof StructuredGridCoverage2DReader;
            return result;
        } catch(IOException e) {
            throw new RuntimeException("Faied to load reader to determine if it's WCS EO Dataset worthy", e);
        }
    }

}
