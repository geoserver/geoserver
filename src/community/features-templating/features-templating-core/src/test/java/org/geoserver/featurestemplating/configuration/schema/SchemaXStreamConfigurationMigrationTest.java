package org.geoserver.featurestemplating.configuration.schema;

import static org.junit.Assert.assertSame;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.junit.Test;

public class SchemaXStreamConfigurationMigrationTest {

    @Test
    public void testPostProcessorSwallowsExceptions() {
        SchemaXStreamConfigurationMigration migration =
                new SchemaXStreamConfigurationMigration(new ExplodingResourceLoader());
        Object bean = new Object();
        Object result = migration.postProcessBeforeInitialization(bean, "configurationLock");
        assertSame(bean, result);
    }

    private static final class ExplodingResourceLoader extends GeoServerResourceLoader {
        @Override
        public Resource get(String path) {
            throw new RuntimeException("boom");
        }
    }
}
