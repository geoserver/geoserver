package org.geoserver.platform.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;

public class ResourcesTest extends FileWrapperResourceTheoryTest {

    @Test
    public void resourcesTest() throws IOException {
        Resource source = getResource();

        Resource directory = getDirectory();

        Resources.copy(source.file(), directory);

        Resource target = directory.get(source.name());

        assertTrue(Resources.exists(target));
        assertEquals(target.name(), source.name());
    }
}
