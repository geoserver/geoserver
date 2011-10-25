package org.geoserver.community.css.web

import collection.JavaConverters._

import org.apache.wicket.markup.html.IHeaderContributor
import org.apache.wicket.markup.html.IHeaderResponse
import org.apache.wicket.markup.html.panel.Panel

import org.geoserver.catalog.{ResourceInfo, StyleInfo}

object OpenLayersMapPanel {
  val templates = {
    val cfg = (new freemarker.template.Configuration);
    cfg.setClassForTemplateLoading(classOf[OpenLayersMapPanel], "")
    cfg.setObjectWrapper(new freemarker.template.DefaultObjectWrapper)
    cfg
  }
}

/**
 * A Wicket widget that encapsulates an OpenLayers interactive map.
 *
 * @author David Winslow <cdwinslow@gmail.com>
 */
class OpenLayersMapPanel(id: String, resource: ResourceInfo, style: StyleInfo) extends Panel(id)
with IHeaderContributor {
  val rand = new java.util.Random
  val bbox = resource.getLatLonBoundingBox
  setOutputMarkupId(true)

  def renderHead(response: IHeaderResponse) {
    val cssContext = Map("id" -> getMarkupId()).asJava
    val cssTemplate = OpenLayersMapPanel.templates.getTemplate("ol-style.ftl")
    val css = new java.io.StringWriter()
    cssTemplate.process(cssContext, css)
    response.renderString(css.toString)
    response.renderJavascriptReference("../openlayers/OpenLayers.js")

    val scriptContext = Map(
      "minx" -> bbox.getMinX(),
      "miny" -> bbox.getMinY(),
      "maxx" -> bbox.getMaxX(),
      "maxy" -> bbox.getMaxY(),
      "id" -> getMarkupId(),
      "layer" -> resource.getPrefixedName(),
      "style" -> style.getName(),
      "cachebuster" -> rand.nextInt(),
      "resolution" -> bbox.getSpan(0).max(bbox.getSpan(1)) / 256.0
    ).asJava
    val scriptTemplate = OpenLayersMapPanel.templates.getTemplate("ol-load.ftl")
    val script = new java.io.StringWriter()
    scriptTemplate.process(scriptContext, script)
    response.renderOnLoadJavascript(script.toString)
  }

  /*
   * Create the JavaScript snippet to execute when the map tiles should be
   * updated.
   */
  def getUpdateCommand(): String = {
    val context = Map(
      "id" -> getMarkupId(),
      "cachebuster" -> rand.nextInt()
    ).asJava
    val template = OpenLayersMapPanel.templates.getTemplate("ol-update.ftl")
    val script = new java.io.StringWriter()
    template.process(context, script)
    script.toString
  }
}
