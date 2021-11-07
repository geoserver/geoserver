/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.Locale;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class LocalizationsFinderTest {

    @Test
    public void testFinder() throws Exception {
        List<Locale> locales = LocalizationsFinder.getAvailableLocales();

        // testing just a few, we cannot predict which locales will come and go,
        // these are hopefully going to be stable enough
        assertThat(locales, CoreMatchers.hasItem(Locale.ENGLISH));
        assertThat(locales, CoreMatchers.hasItem(Locale.FRENCH));
        assertThat(locales, CoreMatchers.hasItem(Locale.GERMAN));
    }
}
