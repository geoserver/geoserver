package org.geoserver.community.css.web

import java.io.Serializable

import scala.collection.JavaConversions._
import scala.collection.mutable.LinkedHashMap

import org.apache.wicket.Component
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.model.Model
import org.apache.wicket.model.IModel

import com.vividsolutions.jts.geom.Geometry
import org.geotools.feature.FeatureCollection
import org.opengis.feature.simple.SimpleFeature
import org.opengis.feature.simple.SimpleFeatureType
import org.opengis.feature.`type`.AttributeDescriptor

import org.geoserver.web.wicket.GeoServerDataProvider
import org.geoserver.web.wicket.GeoServerDataProvider.AbstractProperty
import org.geoserver.web.wicket.GeoServerDataProvider.Property
import org.geoserver.web.wicket.GeoServerTablePanel


/**
 * The Summary class represents an entry in the SummaryTable; including a
 * propertyname, minimum, and maximum value.
 */
case class Summary(name: String, min: AnyRef, max: AnyRef) extends Serializable

object Summary {
  def summarize(data: FeatureCollection[SimpleFeatureType, SimpleFeature])
    : List[Summary] = {
    val (comparable, noncomparable) = 
      data.getSchema.getAttributeDescriptors.toList.partition { x => 
        classOf[Comparable[_]].isAssignableFrom(x.getType.getBinding) &&
        !classOf[Geometry].isAssignableFrom(x.getType.getBinding)
      }
    var maxima = new LinkedHashMap[AttributeDescriptor,Comparable[AnyRef]]
    var minima = new LinkedHashMap[AttributeDescriptor,Comparable[AnyRef]]
    var it = data.features
    try {
      while (it.hasNext) {
        val feature = it.next
        comparable.foreach { prop =>
          val v = 
            feature.getAttribute(prop.getName).asInstanceOf[Comparable[AnyRef]]
          if (v != null) {
            val min = minima.getOrElse(prop, v)
            val max = maxima.getOrElse(prop, v)
            minima.put(prop, if (v.compareTo(min) < 0) v else min)
            maxima.put(prop, if (v.compareTo(max) > 0) v else max)
          }
        }
      }
    } finally {
      it.close()
    }

    val comparableTable = comparable map { prop =>
      Summary(prop.getLocalName, minima(prop), maxima(prop))
    }

    val noncomparableTable = noncomparable map { prop =>
      Summary(prop.getLocalName, "[n/a]", "[n/a]")
    }

    comparableTable ++ noncomparableTable
  }
}

/**
 * A provider to make summaries available to the SummaryTable.
 */
class SummaryProvider(summaries: List[Summary])
extends GeoServerDataProvider[Summary] {

  def this(data: FeatureCollection[SimpleFeatureType, SimpleFeature])
    = this(Summary.summarize(data))

  // override def newModel(a: Serializable): IModel[Serializable] = new Model(a)

  override def getProperties(): java.util.List[Property[Summary]] = {
    val list = new java.util.ArrayList[Property[Summary]]
    list.add(new AbstractProperty[Summary]("Name"){
      override def getPropertyValue(sum: Summary) = sum.name
    })
    list.add(new AbstractProperty[Summary]("Minimum"){
      override def getPropertyValue(sum: Summary) = sum.min
    })
    list.add(new AbstractProperty[Summary]("Maximum"){
      override def getPropertyValue(sum: Summary) = sum.max
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
