package org.geoserver.community.css.web

import java.io.Serializable

import scala.collection.JavaConverters._
import scala.collection.mutable.LinkedHashMap

import org.apache.wicket.Component
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.model.Model
import org.apache.wicket.model.IModel

import com.vividsolutions.jts.geom.Geometry
import org.geotools.feature.FeatureCollection
import org.geotools.feature.FeatureIterator
import org.opengis.feature.simple.SimpleFeature
import org.opengis.feature.simple.SimpleFeatureType
import org.opengis.feature.`type`.AttributeDescriptor

import org.geoserver.web.wicket.GeoServerDataProvider
import org.geoserver.web.wicket.GeoServerDataProvider.AbstractProperty
import org.geoserver.web.wicket.GeoServerDataProvider.Property
import org.geoserver.web.wicket.GeoServerTablePanel

/**
 * A provider to make summaries available to the SummaryTable.
 */
class SummaryProvider(summaries: Seq[Summary])
extends GeoServerDataProvider[Summary] {

  def this(data: FeatureCollection[SimpleFeatureType, SimpleFeature])
    = this(Summary.summarize(data).asScala)

  // override def newModel(a: Serializable): IModel[Serializable] = new Model(a)

  override def getProperties(): java.util.List[Property[Summary]] = {
    val list = new java.util.ArrayList[Property[Summary]]
    list.add(new AbstractProperty[Summary]("Name"){
      override def getPropertyValue(sum: Summary) = sum.getName
    })
    list.add(new AbstractProperty[Summary]("Minimum"){
      override def getPropertyValue(sum: Summary) = sum.getMin
    })
    list.add(new AbstractProperty[Summary]("Maximum"){
      override def getPropertyValue(sum: Summary) = sum.getMax
    })
    return list
  }

  override def getItems(): java.util.List[Summary] = {
    val list = new java.util.ArrayList[Summary]
    summaries.foreach { (x: Summary) =>
      list.add(x)
    }
    return list
  }
}

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
    property: Property[Summary]): Component = {
    new Label(id, new Model(property.getPropertyValue(value.getObject.asInstanceOf[Summary]).toString))
  }
}
