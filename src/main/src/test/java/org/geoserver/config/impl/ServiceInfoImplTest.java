/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import java.util.function.Consumer;
import org.geotools.util.GrowableInternationalString;
import org.junit.Test;

public class ServiceInfoImplTest {

    @Test
    public void testEqualityI18nTitle() {
        ServiceInfoImpl s1 = new ServiceInfoImpl();
        ServiceInfoImpl s2 = new ServiceInfoImpl();

        // initialize both with the same state, no title but i18n title available
        Consumer<ServiceInfoImpl> initer =
                s -> {
                    GrowableInternationalString title =
                            new GrowableInternationalString("default language");
                    title.add(Locale.ITALIAN, "lingua italiana");
                    s.setInternationalTitle(title);
                };
        initer.accept(s1);
        initer.accept(s2);

        assertEquals(s1, s2);
    }

    @Test
    public void testEqualityI18nAbstract() {
        ServiceInfoImpl s1 = new ServiceInfoImpl();
        ServiceInfoImpl s2 = new ServiceInfoImpl();

        // initialize both with the same state, no abstract but i18n abstract available
        Consumer<ServiceInfoImpl> initer =
                s -> {
                    GrowableInternationalString abs =
                            new GrowableInternationalString("default language");
                    abs.add(Locale.ITALIAN, "lingua italiana");
                    s.setInternationalAbstract(abs);
                };
        initer.accept(s1);
        initer.accept(s2);

        assertEquals(s1, s2);
    }
}
