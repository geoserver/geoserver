/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.form;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Tests specifically for the URIValidator's glob pattern detection. */
public class URIValidatorGlobPatternTest {

    /** Test to prove that the containsGlobPattern method correctly detects glob patterns in path strings. */
    @Test
    public void testContainsGlobPattern() {
        // This should detect the asterisk as a glob pattern
        assertTrue("Should detect * as a glob pattern", URIValidator.containsGlobPattern("/path/*.parquet"));

        // Test other glob patterns
        assertTrue("Should detect ** as a glob pattern", URIValidator.containsGlobPattern("/path/**/file.parquet"));

        assertTrue("Should detect ? as a glob pattern", URIValidator.containsGlobPattern("/path/file?.parquet"));

        assertTrue("Should detect [] as a glob pattern", URIValidator.containsGlobPattern("/path/file[123].parquet"));

        assertTrue("Should detect {} as a glob pattern", URIValidator.containsGlobPattern("/path/file{a,b}.parquet"));

        // Escaped characters should not be detected as glob patterns
        assertFalse(
                "Should not detect \\* as a glob pattern", URIValidator.containsGlobPattern("/path/file\\*.parquet"));

        // Path without glob pattern
        assertFalse(
                "Should not detect glob patterns in normal paths",
                URIValidator.containsGlobPattern("/normal/path/file.parquet"));

        // URL with query parameters - shouldn't detect ? as a glob pattern
        assertFalse(
                "Should not detect ? in URL query parameter as a glob pattern",
                URIValidator.containsGlobPattern("http://example.com/data%20with%20spaces.parquet?param=value%26more"));

        // URL with other special characters but no glob patterns
        assertFalse(
                "Should not detect special characters in encoded URLs as glob patterns",
                URIValidator.containsGlobPattern("http://example.com/path/to/data%20file.parquet"));

        // URL with actual glob pattern in the path
        assertTrue(
                "Should detect glob pattern in URL path",
                URIValidator.containsGlobPattern("http://example.com/path/*/data.parquet"));
    }
}
