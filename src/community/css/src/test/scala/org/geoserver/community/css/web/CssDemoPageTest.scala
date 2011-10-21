package org.geoserver.community.css
package web

import org.geoserver.web.GeoServerWicketTestSupport

class CssDemoPageTest extends GeoServerWicketTestSupport {
  import GeoServerWicketTestSupport.tester

  def testTestSuite() {
    login()
    tester.startPage(classOf[CssDemoPage])
    tester.assertRenderedPage(classOf[CssDemoPage])
  }
}
