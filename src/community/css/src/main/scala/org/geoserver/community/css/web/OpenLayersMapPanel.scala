package org.geoserver.community.css.web

import org.apache.wicket.markup.html.IHeaderContributor
import org.apache.wicket.markup.html.IHeaderResponse
import org.apache.wicket.markup.html.panel.Panel

import org.geoserver.catalog.{ResourceInfo, StyleInfo}

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
    response.renderString("""
      <style type="text/css">
        div#%1$s {
          border: 2px groove black;
          height: 300px;
        }

        div#%1$s div.olMap {
          height: 100%%;
        }

        div#%1$s div.olControlScale {
          background: white;
          border-color: black;
          border-style: solid;
          border-width: 2px 0px 0px 2px;
          bottom: 0px;
          padding: 3px;
          right: 0px;
        }
      </style>
    """.format(getMarkupId()))

    response.renderJavascriptReference(
      "../openlayers/OpenLayers.js"
    )

    val olLoader = new java.util.Formatter(new java.util.Locale("zxx"))

    olLoader.format("""
      OpenLayers.DOTS_PER_INCH= 25.4 / 0.28;

      var cfg = {
        maxExtent: new OpenLayers.Bounds(%1$f, %2$f, %3$f, %4$f),
        maxResolution: %9$f,
        controls: [
          new OpenLayers.Control.PanZoomBar(),
          new OpenLayers.Control.Scale(),
          new OpenLayers.Control.Navigation()
        ]
      };

      var map = new OpenLayers.Map("%5$s", cfg);
      map.addLayer(new OpenLayers.Layer.WMS("GeoServer WMS", "../wms",
          {
            layers: "%6$s",
            styles: "%7$s",
            format: "image/png",
            random: %8$d
          }, {
            singleTile: true
          }
        )
      );

      map.zoomToMaxExtent();
      window.olMaps = window.olMaps || {};
      window.olMaps["%5$s"] = map;
    """,
        bbox.getMinX(): java.lang.Double, 
        bbox.getMinY(): java.lang.Double, 
        bbox.getMaxX(): java.lang.Double,
        bbox.getMaxY(): java.lang.Double,
        getMarkupId(),
        resource.getPrefixedName(),
        style.getName(),
        rand.nextInt(): java.lang.Integer,
        bbox.getSpan(0).max(bbox.getSpan(1)) / 256.0: java.lang.Double
    )

    response.renderOnLoadJavascript(olLoader.toString())
  }

  /*
   * Create the JavaScript snippet to execute when the map tiles should be
   * updated.
   */
  def getUpdateCommand(): String = {
  """
    var map = window.olMaps["%s"];
    for (var i = 0; i < map.layers.length; i++) {
      var layer = map.layers[i];
      if (layer.mergeNewParams) layer.mergeNewParams({random: %d});
    }
  """.format(getMarkupId(), rand.nextInt())
  }
}
