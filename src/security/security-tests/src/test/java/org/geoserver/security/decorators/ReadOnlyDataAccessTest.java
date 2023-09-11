/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.geoserver.security.SecurityUtils;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.feature.NameImpl;
import org.junit.Before;
import org.junit.Test;

public class ReadOnlyDataAccessTest<T extends FeatureType, F extends Feature>
        extends SecureObjectsTest {

    private DataAccess<T, F> da;

    private NameImpl name;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        FeatureSource fs = createNiceMock(FeatureSource.class);
        replay(fs);
        FeatureType schema = createNiceMock(FeatureType.class);
        replay(schema);
        da = createNiceMock(DataAccess.class);
        name = new NameImpl("blah");
        expect(da.getFeatureSource(name)).andReturn(fs);
        replay(da);
    }

    @Test
    public void testDontChallenge() throws Exception {
        ReadOnlyDataAccess<T, F> ro = new ReadOnlyDataAccess<>(da, WrapperPolicy.hide(null));
        SecuredFeatureSource fs = (SecuredFeatureSource) ro.getFeatureSource(name);
        assertTrue(fs.policy.isHide());

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.createSchema(null);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ro.updateSchema(null, null);
            fail("Should have failed with an unsupported operation exception");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testChallenge() throws Exception {
        ReadOnlyDataAccess<T, F> ro =
                new ReadOnlyDataAccess<>(da, WrapperPolicy.readOnlyChallenge(null));
        SecuredFeatureSource fs = (SecuredFeatureSource) ro.getFeatureSource(name);
        assertTrue(fs.policy.isReadOnlyChallenge());

        // check the easy ones, those that are not implemented in a read only
        // collection
        try {
            ro.createSchema(null);
            fail("Should have failed with a security exception");
        } catch (Throwable e) {
            if (SecurityUtils.isSecurityException(e) == false)
                fail("Should have thrown a security exception...");
        }
        try {
            ro.updateSchema(null, null);
            fail("Should have failed with a security exception");
        } catch (Throwable e) {
            if (SecurityUtils.isSecurityException(e) == false)
                fail("Should have thrown a security exception...");
        }
    }
}
