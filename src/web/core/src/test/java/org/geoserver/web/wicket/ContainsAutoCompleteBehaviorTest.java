/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;

public class ContainsAutoCompleteBehaviorTest {

    private final ContainsAutoCompleteBehavior behavior =
            new ContainsAutoCompleteBehavior("jdbc:postgresql://host/db", "jdbc:hsqldb://host/db", "jdbc:oracle:thin");

    @Test
    public void testEmptyInputReturnsAll() {
        assertThat(choices(""), contains("jdbc:postgresql://host/db", "jdbc:hsqldb://host/db", "jdbc:oracle:thin"));
    }

    @Test
    public void testContainsCaseInsensitive() {
        assertThat(choices("ORACLE"), contains("jdbc:oracle:thin"));
        assertThat(choices("hsqldb"), contains("jdbc:hsqldb://host/db"));
    }

    @Test
    public void testMatchesSubstringNotJustPrefix() {
        // "host" appears mid-string in two URLs; prefix matching would miss them
        assertThat(choices("host"), contains("jdbc:postgresql://host/db", "jdbc:hsqldb://host/db"));
    }

    @Test
    public void testNoMatch() {
        assertThat(choices("mysql"), emptyIterable());
    }

    private List<String> choices(String input) {
        List<String> result = new ArrayList<>();
        Iterator<String> it = behavior.getChoices(input);
        it.forEachRemaining(result::add);
        return result;
    }
}
