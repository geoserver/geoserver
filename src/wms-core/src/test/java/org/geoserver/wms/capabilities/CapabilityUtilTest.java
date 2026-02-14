/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.util.NumberRange;
import org.junit.Test;

public class CapabilityUtilTest extends WMSTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("multiNamedLayers", "MultiNamedLayers.sld", getClass(), catalog);
        testData.addStyle("regionated", "Regionated.sld", getClass(), catalog);
    }

    @Test
    public void testSearchMinMaxScaleDenominator() throws Exception {
        Catalog catalog = getCatalog();
        Set<StyleInfo> styles = Collections.singleton(catalog.getStyleByName("multiNamedLayers"));
        NumberRange<Double> denominatorsMultiNamed = CapabilityUtil.searchMinMaxScaleDenominator(styles);
        assertEquals(
                "There are two NamedLayer sections in this SLD, should grab both to get min denom",
                10000000,
                denominatorsMultiNamed.getMinValue(),
                0.0);
        assertEquals(
                "There are two NamedLayer sections in this SLD, should grab both to get max denom",
                30000000,
                denominatorsMultiNamed.getMaxValue(),
                0.0);
        Set<StyleInfo> stylesSingle = Collections.singleton(catalog.getStyleByName("regionated"));
        NumberRange<Double> denominatorsSingle = CapabilityUtil.searchMinMaxScaleDenominator(stylesSingle);
        assertEquals(
                "There is one UserStyle section in this SLD, should be able to get min denom",
                80000000,
                denominatorsSingle.getMinValue(),
                0.0);
        assertEquals(
                "There is one UserStyle section in this SLD, should be able to get max denom",
                640000000,
                denominatorsSingle.getMaxValue(),
                0.0);
    }
}
