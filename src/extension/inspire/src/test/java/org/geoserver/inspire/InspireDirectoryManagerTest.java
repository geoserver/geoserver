package org.geoserver.inspire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class InspireDirectoryManagerTest extends GeoServerSystemTestSupport {

    @Test
    public void testLanguagesMappingsOverride() throws IOException {
        InspireDirectoryManager inspireDir = InspireDirectoryManager.get();
        assertTrue(inspireDir.getLanguagesMappings().keySet().size() > 1);
        File file =
                getDataDirectory()
                        .get(
                                InspireDirectoryManager.INSPIRE_DIR,
                                InspireDirectoryManager.LANGUAGES_MAPPINGS)
                        .file();
        file.createNewFile();
        getResourceLoader()
                .copyFromClassPath("available_languages_override.properties", file, getClass());

        Properties properties = inspireDir.getLanguagesMappings();
        assertEquals(1, properties.keySet().size());
        assertEquals(
                "bg", properties.getProperty(properties.keySet().iterator().next().toString()));
    }
}
