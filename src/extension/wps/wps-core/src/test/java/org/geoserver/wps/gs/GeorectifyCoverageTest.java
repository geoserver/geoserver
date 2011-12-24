package org.geoserver.wps.gs;

import org.geoserver.wps.WPSTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.RenderingTransformation;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;

public class GeorectifyCoverageTest extends WPSTestSupport {

    public void testIsRenderingProcess() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Function f = ff.function("gs:GeorectifyCoverage");
        assertTrue(f instanceof RenderingTransformation);
        assertNotNull(f);
    }
}