/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.geoserver.data.test.CiteTestData.STREAMS;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.RenderingVariables;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.NullFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xml.styling.SLDParser;
import org.junit.Test;
import org.opengis.filter.spatial.BBOX;

/**
 * This test class simply ensures that the vector to raster transform is given a BBOX within its
 * query filter
 *
 * @author Rich Fecher
 */
public class VectorToRasterTransformTest extends WMSTestSupport {
    @SuppressWarnings("unchecked")
    @Test
    public void testVectorToRasterTransformUsesBBox() throws IOException {
        final GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        map.setMapWidth(100);
        map.setMapHeight(100);
        map.setRequest(request);
        final ReferencedEnvelope bounds =
                new ReferencedEnvelope(0, 45, 0, 45, DefaultGeographicCRS.WGS84);
        map.getViewport().setBounds(bounds);

        final FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(STREAMS.getNamespaceURI(), STREAMS.getLocalPart());

        final SimpleFeatureSource featureSource =
                (SimpleFeatureSource) ftInfo.getFeatureSource(null, null);
        final MutableBoolean containsBBox = new MutableBoolean(false);
        // This source should make the renderer fail when asking for the features
        final DecoratingFeatureSource source =
                new DecoratingFeatureSource(featureSource) {
                    @Override
                    public SimpleFeatureCollection getFeatures(final Query query)
                            throws IOException {
                        query.getFilter()
                                .accept(
                                        new NullFilterVisitor() {

                                            @Override
                                            public Object visit(
                                                    final BBOX filter, final Object data) {
                                                containsBBox.setValue(true);
                                                return data;
                                            }
                                        },
                                        null);
                        return featureSource.getFeatures(query);
                    }
                };

        final Style style = parseStyle("HeatmapTransform.sld");
        map.addLayer(new FeatureLayer(source, style));
        request.setFormat("image/gif");

        RenderingVariables.setupEnvironmentVariables(map);
        final RenderedImageMap imageMap =
                new RenderedImageMapOutputFormat(getWMS()).produceMap(map);
        imageMap.dispose();
        assertTrue("The query filter should have a BBOX", containsBBox.booleanValue());
    }

    private Style parseStyle(final String styleName) throws IOException {
        final SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory());
        parser.setInput(RasterSymbolizerVisitorTest.class.getResource(styleName));
        final StyledLayerDescriptor sld = parser.parseSLD();
        final NamedLayer ul = (NamedLayer) sld.getStyledLayers()[0];
        return ul.getStyles()[0];
    }
}
