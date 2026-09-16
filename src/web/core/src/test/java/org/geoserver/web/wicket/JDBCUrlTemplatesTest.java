/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertFalse;

import java.util.List;
import org.junit.Test;

public class JDBCUrlTemplatesTest {

    @Test
    public void testAlwaysOffersValidTemplates() {
        List<String> templates = JDBCUrlTemplates.forRegisteredDrivers();
        assertFalse("fallback must keep the field useful when no driver matches", templates.isEmpty());
        assertThat(templates, everyItem(startsWith("jdbc:")));
    }

    @Test
    public void testAlwaysPresentDriversSuggested() {
        // postgresql and hsqldb drivers are always on the GeoServer classpath
        List<String> templates = JDBCUrlTemplates.forRegisteredDrivers();
        assertThat(templates, hasItem("jdbc:postgresql://{host}:5432/{database}"));
        assertThat(templates, hasItem("jdbc:hsqldb:hsql://{host}:9001/{database}"));
    }
}
