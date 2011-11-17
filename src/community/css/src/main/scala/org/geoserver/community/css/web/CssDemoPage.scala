package org.geoserver.community.css.web

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, File, FileWriter }

import scala.collection.JavaConverters._
import scala.io.Source

import org.geoserver.catalog.{ Catalog, FeatureTypeInfo, LayerInfo, ResourceInfo, StyleInfo }
import org.geoserver.config.GeoServerDataDirectory
import org.geoserver.web.GeoServerSecuredPage
import org.geoserver.web.wicket.{ GeoServerDataProvider, GeoServerTablePanel, ParamResourceModel }

import org.geotools.data.FeatureSource

import org.opengis.feature.simple.{ SimpleFeature, SimpleFeatureType } 

import org.apache.wicket.{ Component, PageParameters, WicketRuntimeException }
import org.apache.wicket.ajax.AjaxRequestTarget
import org.apache.wicket.ajax.form.{ AjaxFormComponentUpdatingBehavior, AjaxFormValidatingBehavior }
import org.apache.wicket.ajax.markup.html.AjaxLink
import org.apache.wicket.ajax.markup.html.form.{ AjaxButton, AjaxCheckBox }
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tabs.{ AbstractTab, ITab, PanelCachingTab }
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.markup.html.form.{
  DropDownChoice, Form, IChoiceRenderer, SubmitLink, TextArea, TextField
}
import org.apache.wicket.markup.html.list.{ ListItem, ListView }
import org.apache.wicket.markup.html.link.Link
import org.apache.wicket.markup.html.panel.{ EmptyPanel, Fragment, Panel }
import org.apache.wicket.model.{ CompoundPropertyModel, IModel, Model, PropertyModel }
import org.apache.wicket.validation.{IValidator, IValidatable, ValidationError}
import org.apache.wicket.util.time.Duration

import org.geoscript.geocss._, CssParser._

class CssValidator extends IValidator[String] {
  override def validate(text: IValidatable[String]) = {
    text.getValue() match {
      case css: String =>
        parse(css) match {
          case ns: NoSuccess =>
            val errorMessage = 
              "Line %d, column %d: %s".format(
                ns.next.pos.line,
                ns.next.pos.column,
                ns.msg
              )
            text.error(new ValidationError().setMessage(errorMessage))
          case _ => ()
        }
      case _ => text.error(new ValidationError().setMessage("CSS text must not be empty"))
    }
  }
}

class CssSubmitButton(
  id: String,
  styleEditor: Form[_],
  page: CssDemoPage,
  cssSource: String,
  styleBody: String
) extends AjaxButton("submit", styleEditor) {
  override def onSubmit(target: AjaxRequestTarget, form: Form[_]) = {
    try {
      val file = page.findStyleFile(cssSource)

      try {
        val sld = page.cssText2sldText(styleBody)
        val writer = new FileWriter(file)
        writer.write(styleBody)
        writer.close()
        page.catalog.getResourcePool.writeStyle(
          page.getStyleInfo, new ByteArrayInputStream(sld.getBytes())
        )
      } catch {
        case ex => 
        // TODO: use custom exception here instead of catch-all
        // println(ex.getMessage())
      }
    } catch {
      case e => throw new WicketRuntimeException(e);
    }

    page.catalog.save(page.getStyleInfo)

    if (page.sldPreview != null) target.addComponent(page.sldPreview)
    if (page.map != null) target.appendJavascript(page.map.getUpdateCommand())
  }
}
