/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockCreator;
import org.geoserver.data.test.MockTestData;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.GeoServerDigestPasswordEncoder;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geoserver.security.password.GeoServerPlainTextPasswordEncoder;

/**
 * Base test class for GeoServer mock tests that work from mocked up configuration.
 *
 * <h2>Test Setup Frequency</h2>
 *
 * <p>By default the setup cycle is executed once for extensions of this class. Subclasses that
 * require a different test setup frequency should annotate themselves with the appropriate {@link
 * TestSetup} annotation. For example to implement a repeated setup: <code><pre>
 *  {@literal @}TestSetup(run=TestSetupFrequency.REPEATED}
 *  public class MyTest extends GeoServerMockTestSupport {
 *
 *  }
 * </pre></code> *
 *
 * <h2>Mock Customization</h2>
 *
 * <p>Subclasses extending this base class may customize the mock setup by setting a custom {@link
 * MockCreator} object to {@link #setMockCreator(MockCreator)}. Tests that utilize the one time
 * setup (which is the default for this class) may call this method from the {@link
 * GeoServerBaseTestSupport#setUp(TestData)} hook. For test classes requiring per test case mock
 * customization this method should be called from the test method itself, but the test class must
 * declare a setup frequency of {@link TestSetupFrequency#REPEAT}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerMockTestSupport extends GeoServerBaseTestSupport<MockTestData> {

    @Override
    protected MockTestData createTestData() throws Exception {
        return new MockTestData();
    }

    public Catalog getCatalog() {
        return getTestData().getCatalog();
    }

    public GeoServerSecurityManager getSecurityManager() {
        return getTestData().getSecurityManager();
    }

    /** Accessor for plain text password encoder. */
    protected GeoServerPlainTextPasswordEncoder getPlainTextPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerPlainTextPasswordEncoder.class);
    }

    /** Accessor for digest password encoder. */
    protected GeoServerDigestPasswordEncoder getDigestPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerDigestPasswordEncoder.class);
    }

    /** Accessor for regular (weak encryption) pbe password encoder. */
    protected GeoServerPBEPasswordEncoder getPBEPasswordEncoder() {
        return getSecurityManager()
                .loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, false);
    }

    /** Accessor for strong encryption pbe password encoder. */
    protected GeoServerPBEPasswordEncoder getStrongPBEPasswordEncoder() {
        return getSecurityManager()
                .loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, true);
    }

    /** Forwards through to {@link MockTestData#setMockCreator(MockCreator)} */
    protected void setMockCreator(MockCreator mockCreator) {
        getTestData().setMockCreator(mockCreator);
    }
}
