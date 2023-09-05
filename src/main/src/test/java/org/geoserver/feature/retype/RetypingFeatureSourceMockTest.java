/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature.retype;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.junit.Test;

public class RetypingFeatureSourceMockTest {

    @Test
    public void testGetTypeNamesCalls() throws Exception {
        SimpleFeatureType orig = createNiceMock(SimpleFeatureType.class);
        expect(orig.getTypeName()).andReturn("orig").anyTimes();
        replay(orig);

        SimpleFeatureType wrapped = createNiceMock(SimpleFeatureType.class);
        expect(wrapped.getTypeName()).andReturn("wrapped").anyTimes();
        replay(wrapped);

        // ensure that getTypeNames() never called
        DataStore ds = createMock(DataStore.class);

        SimpleFeatureSource fs = createMock(SimpleFeatureSource.class);
        expect(fs.getSchema()).andReturn(orig).anyTimes();
        expect(fs.getDataStore()).andReturn(ds).anyTimes();
        expect(fs.getFeatures(new Query(orig.getTypeName(), Filter.INCLUDE)))
                .andReturn(null)
                .once();
        replay(fs);

        SimpleFeatureSource rts = RetypingFeatureSource.getRetypingSource(fs, wrapped);
        rts.getFeatures();
    }
}
