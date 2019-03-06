/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.core;

import static org.geoserver.generatedgeometries.core.GeometryGenerationStrategy.getStrategyName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.junit.Test;

public class GeometryGenerationStrategyTest {

    @Test
    public void testThatStrategyNameUnavailableWhenFeatureInfoIsNull() {
        // then
        assertEquals(getStrategyName(null), Optional.empty());
    }

    @Test
    public void testThatStrategyNameUnavailableWhenNoMetadata() {
        // then
        assertEquals(getStrategyName(mock(FeatureTypeInfo.class)), Optional.empty());
    }

    @Test
    public void testThatStrategyNameAvailable() {
        // given
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = new MetadataMap();
        when(info.getMetadata()).thenReturn(metadata);
        String strategyName = "strategyName";
        metadata.put(GeometryGenerationStrategy.STRATEGY_METADATA_KEY, strategyName);

        // then
        Optional<String> strategyNameOptional = getStrategyName(info);
        assertTrue(strategyNameOptional.isPresent());
        assertEquals(strategyNameOptional.get(), strategyName);
    }
}
