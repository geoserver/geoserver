package org.geoserver.wms.map;

import junit.framework.Test;
import org.geoserver.wms.WMS;

public class GetMapOwsXmlReaderTest extends GetMapXmlReaderTestSupport {

  /** This is a READ ONLY TEST so we can use one time setup */
  public static Test suite() {
    return new OneTimeTestSetup(new GetMapOwsXmlReaderTest());
  }

  public void testResolveStylesForLayerGroup() throws Exception {
    testResolveStylesForLayerGroup("WMSGetMapOwsXmlLayerGroupNonDefaultStyle.xml");
  }

  public void testLayerFeatureConstraintFilterParsing() throws Exception {
    testLayerFeatureConstraintFilterParsing("WMSGetMapOwsXmlLayerFeatureConstraintFilter.xml");
  }

  public void testAllowDynamicStyles() throws Exception {
    testAllowDynamicStyles("WMSGetMapOwsXmlLayerGroupNonDefaultStyle.xml");
  }

  @Override
  protected AbstractGetMapXmlReader createReader(WMS wms) {
    return new GetMapOwsXmlReader(wms);
  }
}