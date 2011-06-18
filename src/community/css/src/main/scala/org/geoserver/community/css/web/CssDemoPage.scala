package org.geoserver.community.css.web

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileWriter

import scala.io.Source

import org.geoserver.catalog.{FeatureTypeInfo, ResourceInfo, StyleInfo}
import org.geoserver.config.GeoServerDataDirectory
import org.geoserver.web.GeoServerSecuredPage

import org.geotools.data.FeatureSource

import org.opengis.feature.simple.SimpleFeature
import org.opengis.feature.simple.SimpleFeatureType

import org.apache.wicket.PageParameters
import org.apache.wicket.WicketRuntimeException
import org.apache.wicket.ajax.AjaxRequestTarget
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior
import org.apache.wicket.ajax.form.AjaxFormValidatingBehavior
import org.apache.wicket.ajax.markup.html.AjaxLink
import org.apache.wicket.ajax.markup.html.form.AjaxButton
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab
import org.apache.wicket.extensions.markup.html.tabs.ITab
import org.apache.wicket.extensions.markup.html.tabs.PanelCachingTab
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.markup.html.form.DropDownChoice
import org.apache.wicket.markup.html.form.Form
import org.apache.wicket.markup.html.form.IChoiceRenderer
import org.apache.wicket.markup.html.form.SubmitLink
import org.apache.wicket.markup.html.form.TextArea
import org.apache.wicket.markup.html.form.TextField
import org.apache.wicket.markup.html.panel.{ EmptyPanel, Fragment, Panel }
import org.apache.wicket.model.CompoundPropertyModel
import org.apache.wicket.model.IModel
import org.apache.wicket.model.Model
import org.apache.wicket.model.PropertyModel
import org.apache.wicket.validation.{IValidator, IValidatable, ValidationError}
import org.apache.wicket.util.time.Duration

import org.geoscript.geocss._
import org.geoscript.geocss.CssParser._

trait CssDemoConstants {
  val styleName = "cssdemo"
  val defaultStyle = """ * {
  fill: lightgrey;
  stroke: black;
  mark: symbol(square);
}"""

  val Translator = new Translator()

  private def styleSheetXML(stylesheet: Seq[Rule]): String = {
    val style = Translator.css2sld(stylesheet)
    val sldBytes = new java.io.ByteArrayOutputStream
    val xform = new org.geotools.styling.SLDTransformer
    xform.setIndentation(2)
    xform.transform(style, sldBytes)
    sldBytes.toString
  }

  def cssText2sldText(css: String): Either[NoSuccess, String] = {
    parse(css) match {
      case Success(rules, in) => Right(styleSheetXML(rules))
      case ns: NoSuccess => Left(ns)
    }
  }

}

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

/**
 * A Wicket page using the GeoServer extension system.  It adds a simple form
 * and an OpenLayers map that can be used to try out CSS styling interactively
 * with the layers loaded in the GeoServer catalog.
 *
 * @author David Winslow <cdwinslow@gmail.com>
 */
class CssDemoPage(params: PageParameters) extends GeoServerSecuredPage
with CssDemoConstants
{
  val datadir = new GeoServerDataDirectory(getCatalog().getResourceLoader())
  val styledir = datadir.findStyleDir()
  override val Translator =
    new Translator(Option(styledir).map { _.toURI.toURL })

  def findStyleFile(f: String): java.io.File = new java.io.File(styledir, f)

  class UpdatingTextArea(id: String, m: IModel[String]) extends TextArea(id, m) {
    add(new AjaxFormComponentUpdatingBehavior("onblur") {
      override def onUpdate(target: AjaxRequestTarget) = {
        target.addComponent(getFeedbackPanel())
      }
    })
  }

  def this() = this(new PageParameters)
  def catalog = getCatalog

  def sldText = {
    val filename = styleInfo.getFilename()
    val file = Some(findStyleFile(filename)) filter (null !=)
    file map { file => Source.fromFile(file).mkString }
  }

  class StylePanel(id: String, model: IModel[CssDemoPage], map: OpenLayersMapPanel) extends Panel(id, model) {
    var styleBody = {
      val file = findStyleFile(cssSource)
      if (file != null && file.exists) {
        Source.fromFile(file).mkString
      } else {
        """
No CSS file was found for this style.  Please
make sure this is the style you intended to
edit, since saving the CSS will destroy the
existing SLD.
        """.trim
      }
    }

    val sldModel: IModel[String] = 
      new org.apache.wicket.model.AbstractReadOnlyModel[String] {
        override def getObject() = sldText getOrElse ("""
No SLD file found for this style.  One will be generated automatically if you
submit a CSS file.
        """.trim)
      }

    val sldPreview = new Label("sld-preview", sldModel)

    sldPreview.setOutputMarkupId(true)
    add(sldPreview)

    val styleEditor = new Form("style-editor")
    styleEditor.add(new Label("label", "The stylesheet for this map..."))
    val textArea =
      new UpdatingTextArea("editor", new PropertyModel(this, "styleBody"))
    textArea.add(new CssValidator)
    styleEditor.add(textArea)
    styleEditor.add(new AjaxButton("submit", styleEditor) {
      override def onSubmit(target: AjaxRequestTarget, form: Form[_]) = {
        try {
          val file = findStyleFile(cssSource)

          cssText2sldText(styleBody) match {
            case Left(noSuccess) => println(noSuccess.toString)
            case Right(sld) => {
              val writer = new FileWriter(file)
              writer.write(styleBody)
              writer.close()
              getCatalog.getResourcePool.writeStyle(
                styleInfo,
                new ByteArrayInputStream(sld.getBytes)
              )
            }
          }
        } catch {
          case e => throw new WicketRuntimeException(e);
        }

        getCatalog.save(styleInfo)

        target.addComponent(sldPreview)
        target.appendJavascript(map.getUpdateCommand())
      }
    })


    AjaxFormValidatingBehavior.addToAllFormComponents(styleEditor, "onkeyup", Duration.ONE_SECOND)
    add(styleEditor)
  }

  class DataPanel(id: String, model: IModel[CssDemoPage]) extends Panel(id, model) {
    add(new Label(
      "summary-message",
      "For reference, here is a listing of the attributes in this data set."
    ))

    val states =
      new SummaryProvider(
        layerInfo.getFeatureSource(null,null)
        .asInstanceOf[FeatureSource[SimpleFeatureType, SimpleFeature]]
        .getFeatures
      )
    add(new SummaryTable("summary", states))
  }

  class CreateLinkPanel(id: String) extends Panel(id)
  class NamePanel(id: String) extends Panel(id)

  var layerInfo = {
    def res(a: String, b: String) =
      catalog.getResourceByName(a, b, classOf[FeatureTypeInfo])

    if (params.containsKey("layer")) {
      val name = params.getString("layer").split(":")
      res(name(0), name(1))
    } else {
      val states =
        catalog.getResourceByName("topp", "states", classOf[FeatureTypeInfo])

      if (states != null) {
        states
      } else {
        val ftypes = catalog.getResources(classOf[FeatureTypeInfo])
        if (ftypes.size > 0) {
          ftypes.get(0)
        } else {
          null
        }
      }
    }
  }

  val styleInfo =
    if ((params containsKey "style") && catalog.getStyleByName(params.getString("style")) != null) {
      catalog.getStyleByName(params.getString("style"))
    } else {
      catalog.getLayers(layerInfo).get(0).getDefaultStyle()
    }

  def cssSource = styleInfo.getFilename.replaceAll("\\.sld$","") + ".css"

  def createCssTemplate(name: String) {
    if (catalog.getStyleByName(name) == null) {
      val style = catalog.getFactory().createStyle()
      style.setName(name)
      style.setFilename(name + ".sld")
      catalog.add(style)

      val sld = findStyleFile(style.getFilename())
      if (sld == null || !sld.exists) {
        catalog.getResourcePool().writeStyle(
          style,
          new ByteArrayInputStream(
            cssText2sldText(defaultStyle).right.get.getBytes()
          )
        )
      }

      val css = findStyleFile(name + ".css")
      if (!css.exists) {
        val writer = new FileWriter(css)
        writer.write(defaultStyle)
        writer.close()
      }
    }
  }

  if (layerInfo != null && styleInfo != null) {
    val mainContent = new Fragment("main-content", "normal", this) {
      val layerSelectionForm = new Form("layer-selection")
      val layerResources = catalog.getResources(classOf[FeatureTypeInfo])
      java.util.Collections.sort(
        layerResources,
        new java.util.Comparator[FeatureTypeInfo] {
          override def compare(a: FeatureTypeInfo, b: FeatureTypeInfo): Int = 
             a.getName().compareTo(b.getName())
        }
      )

      layerSelectionForm.add(
        new DropDownChoice(
          "layername",
          new PropertyModel[ResourceInfo](CssDemoPage.this, "layerInfo"),
          layerResources,
          new IChoiceRenderer[ResourceInfo] {
            override def getDisplayValue(resource: ResourceInfo) = {
              val layers = getCatalog.getLayers(resource)
              if (layers != null && layers.size > 0) {
                "%s [%s]".format(layers.get(0).getName, resource.getPrefixedName)
              } else {
                resource.getPrefixedName
              }
            }

            override def getIdValue(choice: ResourceInfo, index: Int) = choice.getId
          }
        )
      )

      val styleResources = new java.util.ArrayList[StyleInfo]
      styleResources.addAll(catalog.getStyles())
      java.util.Collections.sort(
        styleResources,
        new java.util.Comparator[StyleInfo] {
          override def compare(a: StyleInfo, b: StyleInfo): Int = 
            a.getName().compareTo(b.getName())
        }
      )

      layerSelectionForm.add(
        new DropDownChoice(
          "stylename",
          new PropertyModel[StyleInfo](CssDemoPage.this, "styleInfo"),
          styleResources,
          new IChoiceRenderer[StyleInfo] {
            override def getDisplayValue(choice: StyleInfo) = choice.getName()

            override def getIdValue(choice: StyleInfo, index: Int) = choice.getId
          }
        )
      )

      layerSelectionForm.add(new SubmitLink("submit", layerSelectionForm) {
          override def onSubmit() {
            val params = new org.apache.wicket.PageParameters
            params.put("layer", layerInfo.getPrefixedName())
            params.put("style", styleInfo.getName())
            setResponsePage(classOf[CssDemoPage], params)
          }
        }
      )
      
      val createPanel: Panel = new CreateLinkPanel("create") {
        setOutputMarkupId(true)

        add(new AjaxLink("create-style", new Model("Create")) {
          override def onClick(target: AjaxRequestTarget) {
            createPanel.replaceWith(namePanel)
            target.addComponent(namePanel)
          }
        })
      }

      val namePanel: Panel = new NamePanel("create") {
        setOutputMarkupId(true)
        var stylename = new Model("New style name")

        add(new Form("create-style") {
          add(new TextField("new-style-name", stylename))

          add(new SubmitLink("new-style-submit", this))

          add(new AjaxLink("new-style-cancel", new Model("Cancel")) {
            override def onClick(target: AjaxRequestTarget) {
              namePanel.replaceWith(createPanel)
              target.addComponent(createPanel)
            }
          })

          override def onSubmit() {
            val name = stylename.getObject().asInstanceOf[String]
            createCssTemplate(name)

            val params = new org.apache.wicket.PageParameters
            params.put("layer", layerInfo.getPrefixedName())
            params.put("style", name)
            setResponsePage(classOf[CssDemoPage], params)
            setRedirect(true)
          }
        })
      }

      layerSelectionForm.add(createPanel)

      add(layerSelectionForm)

      val map = new OpenLayersMapPanel("map", layerInfo, styleInfo)
      add(map)

      val feedback2 =
        new org.apache.wicket.markup.html.panel.FeedbackPanel("feedback-low")
      feedback2.setOutputMarkupId(true)
      add(feedback2)

      val tabs = new java.util.ArrayList[ITab]
      val model = new CompoundPropertyModel[CssDemoPage](CssDemoPage.this)
      tabs.add(new PanelCachingTab(new AbstractTab(new Model("Style")) {
        override def getPanel(id: String): Panel = new StylePanel(id, model, map)
      }))
      tabs.add(new PanelCachingTab(new AbstractTab(new Model("Data")) {
        override def getPanel(id: String): Panel = new DataPanel(id, model)
      }))

      add(new AjaxTabbedPanel("tabs", tabs))
    }

    add(mainContent) 
  } else {
    add(new Fragment("main-content", "loading-failure", this))
  }
}
