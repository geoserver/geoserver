/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import java.io.IOException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class DataPanel extends Panel {
    public DataPanel(String id, IModel<CssDemoPage> model, FeatureTypeInfo layerInfo)
        throws IOException
    {
        super(id, model);
        FeatureCollection<SimpleFeatureType, SimpleFeature> features = (
            (FeatureSource<SimpleFeatureType, SimpleFeature>)
            layerInfo.getFeatureSource(null, null)
            ).getFeatures();
        SummaryProvider summaries = new SummaryProvider(features);

        add(new Label("summary-message",
            "For reference, here is a listing of the attributes in this data set."
        )); // TODO: I18N
        add(new SummaryTable("summary", summaries));
    }
}
