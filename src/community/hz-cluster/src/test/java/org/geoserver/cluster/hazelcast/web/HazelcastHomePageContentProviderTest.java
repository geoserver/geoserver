package org.geoserver.cluster.hazelcast.web;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.apache.wicket.serialize.ISerializer;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class HazelcastHomePageContentProviderTest extends GeoServerWicketTestSupport {

    @Test
    public void testSerializable() throws IOException {
        ISerializer serializer =
                getGeoServerApplication().getFrameworkSettings().getSerializer();
        byte[] data = serializer.serialize(new HazelcastHomePageContentProvider());
        Object deserialized = serializer.deserialize(data);
        assertTrue(deserialized instanceof HazelcastHomePageContentProvider);
    }
}
