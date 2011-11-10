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

class CssSubmitButton(id: String,
  styleEditor: Form[_],
  page: CssDemoPage,
  cssSource: String,
  styleBody: String
) extends AjaxButton("submit", styleEditor) {
  override def onSubmit(target: AjaxRequestTarget, form: Form[_]) = {
    try {
      val file = page.findStyleFile(cssSource)

      page.cssText2sldText(styleBody) match {
        case Left(noSuccess) => println(noSuccess.toString)
        case Right(sld) => {
          val writer = new FileWriter(file)
          writer.write(styleBody)
          writer.close()
          page.catalog.getResourcePool.writeStyle(
            page.styleInfo,
            new ByteArrayInputStream(sld.getBytes)
          )
        }
      }
    } catch {
      case e => throw new WicketRuntimeException(e);
    }

    page.catalog.save(page.styleInfo)

    if (page.sldPreview != null) target.addComponent(page.sldPreview)
    if (page.map != null) target.appendJavascript(page.map.getUpdateCommand())
  }
}

class MultipleLayerChooser(id: String, demo: CssDemoPage) extends Panel(id) {
  import GeoServerDataProvider.{ AbstractProperty, Property }

  def usesEditedStyle(l: LayerInfo): Boolean =
    (l.getStyles.asScala + l.getDefaultStyle).exists { _.getName == demo.styleInfo.getName }

  object layerProvider extends GeoServerDataProvider[LayerInfo] {
    override def getItems(): java.util.List[LayerInfo] = 
      demo.catalog.getLayers().asScala.sortBy(_.getName).asJava

    val workspace =
      new AbstractProperty[LayerInfo]("Workspace") {
        override def getPropertyValue(x: LayerInfo) = x.getResource.getStore.getWorkspace.getName
      }
    val name =
      new AbstractProperty[LayerInfo]("Layer") {
        override def getPropertyValue(x: LayerInfo) = x.getName
      }
    val associated =
      new AbstractProperty[LayerInfo]("Associated") {
        override def getPropertyValue(x: LayerInfo) = Boolean.box(usesEditedStyle(x))
      }

    override def getProperties(): java.util.List[Property[LayerInfo]] =
      List[Property[LayerInfo]](workspace, name, associated).asJava
  }

  object layerTable extends GeoServerTablePanel[LayerInfo]("layer.table", layerProvider) {
    override def getComponentForProperty(
      id: String, value: IModel[_],
      property: Property[LayerInfo]
    ): Component = {
      val layer = value.getObject.asInstanceOf[LayerInfo]
      val text = property.getPropertyValue(layer).toString
      if (property eq layerProvider.associated) {
        val model = 
          new IModel[java.lang.Boolean] {
            def getObject: java.lang.Boolean = usesEditedStyle(layer)
            def setObject(b: java.lang.Boolean): Unit = {
              if (Boolean.unbox(b)) {
                layer.getStyles().add(demo.styleInfo)
              } else {
                if (layer.getDefaultStyle.getName == demo.styleInfo.getName) {
                  if (layer.getStyles.asScala.isEmpty) {
                    layer.setDefaultStyle(demo.catalog.getStyleByName("point"))
                  } else {
                    val s = layer.getStyles().iterator.next
                    layer.setDefaultStyle(s)
                    layer.getStyles.remove(s)
                  }
                } else {
                  layer.getStyles.asScala --= layer.getStyles.asScala.filter(_.getName == demo.styleInfo.getName)
                }
              }
              demo.catalog.save(layer)
            }
            override def detach() {}
          }
        new Fragment(id, "layer.association.checkbox", MultipleLayerChooser.this) {
          add(new AjaxCheckBox("selected", model) { override def onUpdate(target: AjaxRequestTarget) {} })
        }
      } else
        new Label(id, text)
    }
  }

  add(layerTable)
}
/**
 * A Wicket page using the GeoServer extension system.  It adds a simple form
 * and an OpenLayers map that can be used to try out CSS styling interactively
 * with the layers loaded in the GeoServer catalog.
 *
 * @author David Winslow <cdwinslow@gmail.com>
 */
class CssDemoPage(params: PageParameters) extends GeoServerSecuredPage {
  def this() = this(new PageParameters)

  val defaultStyle = """ * {
  fill: lightgrey;
  stroke: black;
  mark: symbol(square);
}"""

  val styledir = datadir.findStyleDir()

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
    if ((params containsKey "style") && catalog.getStyleByName(params.getString("style")) != null)
      catalog.getStyleByName(params.getString("style"))
    else
      catalog.getLayers(layerInfo).get(0).getDefaultStyle()

  var map: OpenLayersMapPanel = null
  var sldPreview: Label = null

  if (layerInfo != null && styleInfo != null) {
    val mainContent = new Fragment("main-content", "normal", this) {
      val popup = new ModalWindow("popup")
      add(popup)
      add(new Label("style.name", new PropertyModel(styleInfo, "name")))
      add(new Label("layer.name", new PropertyModel(layerInfo, "prefixedName")))
      add(new AjaxLink("change.style", new ParamResourceModel("CssDemoPage.changeStyle", this)) {
        def onClick(target: AjaxRequestTarget) {
          target.appendJavascript("Wicket.Window.unloadConfirmation = false;"); 
          popup.setInitialHeight(400)
          popup.setInitialWidth(600)
          popup.setTitle(new Model("Choose style to edit"))
          popup.setContent(new StyleChooser(popup.getContentId, CssDemoPage.this))
          popup.show(target)
        }
      })
      add(new AjaxLink("change.layer", new ParamResourceModel("CssDemoPage.changeLayer", this)) {
        def onClick(target: AjaxRequestTarget) {
          target.appendJavascript("Wicket.Window.unloadConfirmation = false;"); 
          popup.setInitialHeight(400)
          popup.setInitialWidth(600)
          popup.setTitle(new Model("Choose layer to preview"))
          popup.setContent(new LayerChooser(popup.getContentId, CssDemoPage.this))
          popup.show(target)
        }
      })
      add(new AjaxLink("create.style", new ParamResourceModel("CssDemoPage.createStyle", this)) {
        def onClick(target: AjaxRequestTarget) {
          target.appendJavascript("Wicket.Window.unloadConfirmation = false;"); 
          popup.setInitialHeight(200)
          popup.setInitialWidth(300)
          popup.setTitle(new Model("Choose name for new style"))
          popup.setContent(new LayerNameInput(popup.getContentId, CssDemoPage.this))
          popup.show(target)
        }
      })
      add(new AjaxLink("associate.styles", new ParamResourceModel("CssDemoPage.associateStyles", this)) {
        def onClick(target: AjaxRequestTarget) {
          target.appendJavascript("Wicket.Window.unloadConfirmation = false;"); 
          popup.setInitialHeight(400)
          popup.setInitialWidth(600)
          popup.setTitle(new Model("Choose layers to associate"))
          popup.setContent(new MultipleLayerChooser(popup.getContentId, CssDemoPage.this))
          popup.show(target)
        }
      })

      val model = new CompoundPropertyModel[CssDemoPage](CssDemoPage.this)
      val tabs = new java.util.ArrayList[ITab]
      tabs.add(new AbstractTab(new Model("Collapse")) {
        override def getPanel(id: String): Panel = new EmptyPanel(id)
      })
      tabs.add(new PanelCachingTab(new AbstractTab(new Model("Map")) {
        override def getPanel(id: String): Panel = {
          map = new OpenLayersMapPanel(id, layerInfo, styleInfo)
          map
        }
      }))
      tabs.add(new PanelCachingTab(new AbstractTab(new Model("Data")) {
        override def getPanel(id: String): Panel = new DataPanel(id, model, layerInfo)
      }))
      tabs.add(new PanelCachingTab(new AbstractTab(new Model("Generated SLD")) {
        override def getPanel(id: String): Panel = {
          val panel = new SLDPreviewPanel(id, sldModel)
          sldPreview = panel.getLabel()
          panel
        }
      }))
      tabs.add(new AbstractTab(new Model("CSS Reference")) {
        override def getPanel(id: String): Panel = new DocsPanel(id)
      })
      add(new AjaxTabbedPanel("context", tabs))

      val feedback2 =
        new org.apache.wicket.markup.html.panel.FeedbackPanel("feedback-low")
      feedback2.setOutputMarkupId(true)
      add(feedback2)

      add(new StylePanel(
        "style.editing", model, CssDemoPage.this, getFeedbackPanel(), cssSource
      ))
    }

    add(mainContent) 
  } else {
    add(new Fragment("main-content", "loading-failure", this))
  }

  def Translator = new Translator(Option(styledir).map { _.toURI.toURL })

  private def styleSheetXML(stylesheet: Seq[Rule]): String = {
    val style = Translator.css2sld(stylesheet)
    val sldBytes = new java.io.ByteArrayOutputStream
    val xform = new org.geotools.styling.SLDTransformer
    xform.setIndentation(2)
    xform.transform(style, sldBytes)
    sldBytes.toString
  }

  def cssText2sldText(css: String): Either[NoSuccess, String] =
    parse(css) match {
      case Success(rules, in) => Right(styleSheetXML(rules))
      case ns: NoSuccess => Left(ns)
    }

  def datadir = new GeoServerDataDirectory(getCatalog().getResourceLoader())
  def findStyleFile(f: String): java.io.File = new java.io.File(styledir, f)

  def catalog = getCatalog

  object sldModel extends org.apache.wicket.model.AbstractReadOnlyModel[String] {
    override def getObject() = {
      val filename = styleInfo.getFilename()
      val file = findStyleFile(filename)
      if (file != null)
        Source.fromFile(file).mkString
      else
        """
        |No SLD file found for this style.  One will be generated automatically
        |if you submit a CSS file.
        """.stripMargin
    }
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
}
