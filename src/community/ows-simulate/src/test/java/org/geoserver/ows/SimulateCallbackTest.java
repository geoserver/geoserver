/*
 * (c) 2017 Open Source Geospatial Foundation - all rights reserved
 */
package org.geoserver.ows;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.UUID;
import net.sf.json.JSONObject;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.Version;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

public class SimulateCallbackTest {

    @Test
    public void test() {
        Foo foo = new Foo();
        Service srv = new Service("test", null, new Version("1.0.0"), ImmutableList.of("foo"));
        Operation op = new Operation("foo", srv, null, new Object[] {foo});

        JSONObject obj =
                JSONObject.fromObject(new SimulateCallback().toJSON(op, Collections.emptyMap()));
        System.out.println(obj.toString(2));
    }

    @Test
    public void testFilterAsCQL() throws Exception {
        Foo foo = new Foo();
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        foo.filter =
                ff.equals(
                        ff.function("in", ff.literal("prop1"), ff.literal(1), ff.literal(2)),
                        ff.literal(true));

        Service srv = new Service("test", null, new Version("1.0.0"), ImmutableList.of("foo"));
        Operation op = new Operation("foo", srv, null, new Object[] {foo});
        JSONObject obj =
                JSONObject.fromObject(new SimulateCallback().toJSON(op, Collections.emptyMap()));

        obj.getJSONObject("operation").getJSONObject("request").getString("filter");
    }

    public static class Foo {
        public String name = UUID.randomUUID().toString();
        public long time = System.currentTimeMillis();
        public Filter filter;

        public String getName() {
            return name;
        }

        public long getTime() {
            return time;
        }

        public Filter getFilter() {
            return filter;
        }
    }
}
