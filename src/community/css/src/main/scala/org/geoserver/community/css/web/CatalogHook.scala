package org.geoserver.community.css.web

import org.vfny.geoserver.global.GeoserverDataDirectory
import java.io.ByteArrayInputStream

/**
 * A terrible hack that does some setup related to the demo page, if necessary.
 *
 * @author David Winslow <cdwinslow@gmail.com>
 */
class CatalogHook(catalog: org.geoserver.catalog.Catalog)
extends CssDemoConstants {
  if (catalog.getStyleByName("cssdemo") == null) {
    val style = catalog.getFactory.createStyle()
    style.setName("cssdemo")
    style.setFilename("cssdemo.sld")
    catalog.add(style)

    val file = GeoserverDataDirectory.findStyleFile(style.getFilename)
    if (file == null || !file.exists) {
      catalog.getResourcePool.writeStyle(
        style,
        new ByteArrayInputStream(
          cssText2sldText(defaultStyle).right.get.getBytes
        )
      )
    }
  }
}
