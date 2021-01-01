package org.geoserver.wms.map;

import junit.framework.Test;
import org.geoserver.wms.WMS;

public class GetMapSldXmlReaderTest extends GetMapXmlReaderTestSupport {

  /** This is a READ ONLY TEST so we can use one time setup */
  public static Test suite() {
    return new OneTimeTestSetup(new GetMapSldXmlReaderTest());
  }

  public void testResolveStylesForLayerGroup() throws Exception {
    testResolveStylesForLayerGroup("WMSGetMapSldXmlLayerGroupNonDefaultStyle.xml");
  }

  public void testLayerFeatureConstraintFilterParsing() throws Exception {
    testLayerFeatureConstraintFilterParsing("WMSGetMapSldXmlLayerFeatureConstraintFilter.xml");
  }

  public void testAllowDynamicStyles() throws Exception {
    testAllowDynamicStyles("WMSGetMapSldXmlLayerGroupNonDefaultStyle.xml");
  }

  @Override
  protected AbstractGetMapXmlReader createReader(WMS wms) {
    return new GetMapSldXmlReader(wms);
  }
}
