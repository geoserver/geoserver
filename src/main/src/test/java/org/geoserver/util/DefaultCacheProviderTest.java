/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.util;

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;

import java.util.logging.Level;
import org.easymock.EasyMock;
import org.geoserver.platform.GeoServerExtensionsHelper.ExtensionsHelperRule;
import org.geotools.util.logging.Logging;
import org.junit.Rule;
import org.junit.Test;

public class DefaultCacheProviderTest {

    @Rule public ExtensionsHelperRule extensions = new ExtensionsHelperRule();

    @Rule
    public LoggerRule logging =
            new LoggerRule(Logging.getLogger(DefaultCacheProvider.class), Level.WARNING);

    @Test
    public void testDefault() {
        CacheProvider provider = DefaultCacheProvider.findProvider();

        assertThat(provider, instanceOf(DefaultCacheProvider.class));
    }

    private CacheProvider addMockProvider(String name) {
        CacheProvider provider = EasyMock.createMock(name, CacheProvider.class);
        extensions.singleton(name, provider, CacheProvider.class);
        return provider;
    }

    @Test
    public void testFindInContext() {
        CacheProvider testCacheProvider1 = addMockProvider("testCacheProvider1");

        replay(testCacheProvider1);

        CacheProvider provider = DefaultCacheProvider.findProvider();

        assertThat(provider, sameInstance(testCacheProvider1));

        verify(testCacheProvider1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindTwoInContext() {
        CacheProvider testCacheProvider1 = addMockProvider("testCacheProvider1");
        CacheProvider testCacheProvider2 = addMockProvider("testCacheProvider2");

        replay(testCacheProvider1, testCacheProvider2);

        CacheProvider provider = DefaultCacheProvider.findProvider();

        assertThat(
                provider,
                anyOf(sameInstance(testCacheProvider1), sameInstance(testCacheProvider2)));

        String providerName = "testCacheProvider2";
        if (provider == testCacheProvider1) {
            providerName = "testCacheProvider1";
        }
        logging.assertLogged(
                allOf(
                        hasProperty("level", is(Level.WARNING)),
                        hasProperty(
                                "parameters",
                                arrayContainingInAnyOrder(
                                        // Name of the provider being used
                                        equalTo(providerName),
                                        // Available providers
                                        anyOf(
                                                equalTo("testCacheProvider1, testCacheProvider2"),
                                                equalTo("testCacheProvider2, testCacheProvider1")),
                                        // The system property to override
                                        equalTo("GEOSERVER_DEFAULT_CACHE_PROVIDER")))));
        verify(testCacheProvider1, testCacheProvider2);
    }

    @Test
    public void testResolveWithProperty() {
        CacheProvider testCacheProvider1 = addMockProvider("testCacheProvider1");
        CacheProvider testCacheProvider2 = addMockProvider("testCacheProvider2");

        // Test that the bean specified in the property is used

        extensions.property(DefaultCacheProvider.BEAN_NAME_PROPERTY, "testCacheProvider1");

        replay(testCacheProvider1, testCacheProvider2);

        CacheProvider provider = DefaultCacheProvider.findProvider();

        assertThat(provider, sameInstance(testCacheProvider1));

        verify(testCacheProvider1, testCacheProvider2);

        // Retry with the property changed to ensure we weren't jsut lucky before
        reset(testCacheProvider1, testCacheProvider2);

        extensions.property(DefaultCacheProvider.BEAN_NAME_PROPERTY, "testCacheProvider2");

        replay(testCacheProvider1, testCacheProvider2);

        provider = DefaultCacheProvider.findProvider();

        assertThat(provider, sameInstance(testCacheProvider2));

        verify(testCacheProvider1, testCacheProvider2);
    }

    @Test
    public void testPropertyPriority() {
        CacheProvider testCacheProvider3 = addMockProvider("testCacheProvider3");
        CacheProvider testCacheProvider2 = addMockProvider("testCacheProvider2");

        // Test that the bean specified in the property is used

        extensions.property(
                DefaultCacheProvider.BEAN_NAME_PROPERTY,
                "testCacheProvider1,testCacheProvider2,testCacheProvider3");

        replay(testCacheProvider3, testCacheProvider2);

        CacheProvider provider = DefaultCacheProvider.findProvider();

        assertThat(provider, sameInstance(testCacheProvider2));

        verify(testCacheProvider3, testCacheProvider2);
    }
}
