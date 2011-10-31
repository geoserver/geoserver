package org.geoserver.community.css.web

import java.io.Serializable

import scala.collection.JavaConverters._

import org.apache.wicket.Component
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.model.Model
import org.apache.wicket.model.IModel

import org.geoserver.web.wicket.GeoServerDataProvider.Property
import org.geoserver.web.wicket.GeoServerTablePanel

/**
 * A specialized GeoServerTablePanel that shows maximum and minimum data for a
 * particular featuretype.  It is intended to provide some extra context during
 * styling, etc.
 *
 * @author David Winslow <cdwinslow@gmail.com>
 */
class SummaryTable(id: String, summary: SummaryProvider)
extends GeoServerTablePanel[Summary](id, summary) {
  override def getComponentForProperty(
    id: String,
    value: IModel[_],
    property: Property[Summary]
  ): Component =
    new Label(id, new Model(property.getPropertyValue(value.getObject.asInstanceOf[Summary]).toString))
}
