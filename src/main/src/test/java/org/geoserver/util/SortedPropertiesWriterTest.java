/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.geoserver.util.SortedPropertiesWriter;

public class SortedPropertiesWriterTest {

    @Test
    public void testStoreSortedWritesAlphabeticalOrderWithUnicodeAndTimestamp() throws IOException {
        Properties props = new Properties();
        props.setProperty("zeta", "last");
        props.setProperty("alpha", "first");
        props.setProperty("müller", "unicode"); // triggers unicode escape
        props.setProperty("beta", "second");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SortedPropertiesWriter.storeSorted(props, out, "Test Header");

        String content = out.toString(StandardCharsets.UTF_8);

        // 1. Header present
        assertTrue(content.contains("Test Header"));

        // 2. Timestamp matches Java properties format, e.g. "#Mon Aug 26 10:16:17 SAST 2024"
        Pattern timestampPattern = Pattern.compile(
                "^#(Mon|Tue|Wed|Thu|Fri|Sat|Sun) [A-Z][a-z]{2} \\d{2} \\d{2}:\\d{2}:\\d{2} \\S+ \\d{4}$",
                Pattern.MULTILINE);
        Matcher matcher = timestampPattern.matcher(content);
        assertTrue("Timestamp line not found or does not match Java properties format", matcher.find());

        // 3. Keys are alphabetical (alpha, beta, müller, zeta)
        int alphaIdx = content.indexOf("alpha=");
        int betaIdx = content.indexOf("beta=");
        int muellerIdx = content.indexOf("m\\u00fcller=");
        int zetaIdx = content.indexOf("zeta=");

        assertTrue(alphaIdx < betaIdx);
        assertTrue(betaIdx < muellerIdx);
        assertTrue(muellerIdx < zetaIdx);

        // 4. Unicode is escaped
        assertTrue(content.contains("m\\u00fcller=unicode"));

        // 5. All values present
        assertTrue(content.contains("first"));
        assertTrue(content.contains("second"));
        assertTrue(content.contains("last"));
        assertTrue(content.contains("unicode"));
    }
}
