package org.geoserver.community.css
package web

import collection.JavaConverters._

import org.apache.wicket.ajax.markup.html.AjaxLink
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel
import org.apache.wicket.markup.html.panel.EmptyPanel
import org.apache.wicket.markup.html.panel.Panel
import org.{ geoserver => gs }
import gs.web.GeoServerWicketTestSupport
import junit.framework.Assert._

class CssDemoPageTest extends GeoServerWicketTestSupport {
  import GeoServerWicketTestSupport.tester

  def testBasicLayout() {
    login()
    tester.startPage(classOf[CssDemoPage])
    tester.assertRenderedPage(classOf[CssDemoPage])
    tester.assertComponent("main-content:context", classOf[AjaxTabbedPanel])
    tester.assertComponent("main-content:context:panel", classOf[EmptyPanel])
    tester.assertComponent("main-content:change.style", classOf[AjaxLink[_]])
    tester.assertComponent("main-content:change.layer", classOf[AjaxLink[_]])
    tester.assertComponent("main-content:create.style", classOf[AjaxLink[_]])
    tester.assertComponent("main-content:associate.styles" , classOf[AjaxLink[_]])
    tester.assertComponent("main-content:style.editing" , classOf[StylePanel])
  }

  def testOpenLayersMapPanel() {
    login()
    tester.startPage(classOf[CssDemoPage])
    tester.clickLink("main-content:context:tabs-container:tabs:1:link")
    tester.assertComponent("main-content:context:panel", classOf[OpenLayersMapPanel])
  }

  // need to make sure there's a non-empty layer loaded before testing this
  // def testDataPanel() {
  //   login()
  //   tester.startPage(classOf[CssDemoPage])
  //   tester.clickLink("main-content:context:tabs-container:tabs:2:link")
  //   tester.assertComponent("main-content:context:panel", classOf[DataPanel])
  // }

  def testSLDPreviewPanel() {
    login()
    tester.startPage(classOf[CssDemoPage])
    tester.clickLink("main-content:context:tabs-container:tabs:3:link")
    tester.assertComponent("main-content:context:panel", classOf[SLDPreviewPanel])
  }

  def testStyleChooser() {
    login()
    tester.startPage(classOf[CssDemoPage])
    tester.clickLink("main-content:change.style")
    tester.assertComponent("main-content:popup:content:style.table",
      classOf[gs.web.wicket.GeoServerTablePanel[_]])
  }

  def testLayerChooser() {
    login()
    tester.startPage(classOf[CssDemoPage])
    tester.clickLink("main-content:change.layer")
    tester.assertComponent("main-content:popup:content:layer.table",
       classOf[gs.web.wicket.GeoServerTablePanel[_]])
  }
  
  def testDocsPanel() {
    login()
    tester.startPage(classOf[CssDemoPage])
    tester.clickLink("main-content:context:tabs-container:tabs:4:link")
    tester.assertComponent("main-content:context:panel", classOf[DocsPanel])
  }
}
