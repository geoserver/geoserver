/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.Markup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

/** UI test for Custom dimension editor for vector. */
public class VectorCustomDimensionEditorTest extends GeoServerWicketTestSupport {

    private static final String markup =
            "<form wicket:id=\"form\"><div wicket:id=\"dimEditor\" /></form>";

    @Test
    public void testEditor() {
        final Catalog catalog = getCatalog();
        final LayerInfo layerInfo = catalog.getLayerByName("RoadSegments");
        assertTrue(layerInfo.getResource() instanceof FeatureTypeInfo);
        final FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) layerInfo.getResource();
        final MetadataMap metadataMap = featureTypeInfo.getMetadata();
        final DimensionInfo dimension = new DimensionInfoImpl();
        dimension.setEnabled(true);
        dimension.setAttribute("name");
        dimension.setPresentation(DimensionPresentation.LIST);
        metadataMap.put("dim_name", dimension);
        catalog.save(featureTypeInfo);
        Pair<String, DimensionInfo> entry = Pair.of("dim_name", dimension);
        VectorCustomDimensionEntry dimEntry = new VectorCustomDimensionEntry(entry);
        VectorCustomDimensionEditor editor =
                new VectorCustomDimensionEditor(
                        "dimEditor", Model.of(dimEntry), featureTypeInfo, Serializable.class);
        Form form = new Form("form");
        form.add(editor);
        form = tester.startComponentInPage(form, Markup.of(markup));
        TextField<String> dimNameInput =
                (TextField<String>)
                        tester.getComponentFromLastRenderedPage("form:dimEditor:customDimName");
        assertEquals("name", dimNameInput.getModelObject());
    }
}
