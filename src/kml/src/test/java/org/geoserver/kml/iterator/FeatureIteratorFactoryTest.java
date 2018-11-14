/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.iterator;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import java.util.Iterator;
import junit.framework.AssertionFailedError;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.junit.Test;

public class FeatureIteratorFactoryTest {

    @Test
    public void testNoScanOnEmptyStyle() {
        // a style with scale dependency
        StyleBuilder sb = new StyleBuilder();
        Style style = sb.createStyle(sb.createPolygonSymbolizer());
        style.featureTypeStyles().get(0).rules().get(0).setMaxScaleDenominator(1000);

        // the layer
        final DefaultFeatureCollection fc = new DefaultFeatureCollection();
        FeatureLayer layer = new FeatureLayer(fc, style);

        // a max context with a scale outside the rule range
        WMSMapContent mc = createNiceMock(WMSMapContent.class);
        expect(mc.getScaleDenominator()).andReturn(2000d).anyTimes();
        replay(mc);

        // and the context wiring everything toghether
        KmlEncodingContext context = createNiceMock(KmlEncodingContext.class);
        expect(context.openIterator(anyObject(SimpleFeatureCollection.class)))
                .andThrow(new AssertionFailedError("Should not have called openIterator"))
                .anyTimes();
        expect(context.getCurrentFeatureCollection()).andReturn(fc).anyTimes();
        expect(context.getMapContent()).andReturn(mc).anyTimes();
        replay(context);

        // check no features have been read
        FeatureIteratorFactory sf = new FeatureIteratorFactory(context, layer);
        Iterator<Feature> sequence = sf.newIterator();
        assertNull(sequence.next());
    }
}
