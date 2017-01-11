/* 
 * (c) 2017 Open Source Geospatial Foundation - all rights reserved
 */
package org.geoserver.ows;

import com.google.common.collect.ImmutableList;
import net.sf.json.JSONObject;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

public class SimulateCallbackTest {

  @Test
  public void test() {
    Foo foo = new Foo();
    Service srv = new Service("test", null, new Version("1.0.0"), ImmutableList.of("foo"));
    Operation op = new Operation("foo", srv, null, new Object[]{foo});

    JSONObject obj = JSONObject.fromObject(new SimulateCallback().toJSON(op, Collections.emptyMap()));
    System.out.println(obj.toString(2));
  }

  public static class Foo {
    public String name = UUID.randomUUID().toString();
    public long time = System.currentTimeMillis();

    public String getName() {
      return name;
    }

    public long getTime() {
      return time;
    }
  }


}
