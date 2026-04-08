/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.logging.Level;
import javax.xml.transform.TransformerException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptContentHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.demo.DemoRequestsPage;
import org.geoserver.web.demo.PlainCodePage;
import org.geoserver.web.wicket.GSModalWindow;

/**
 * Small embedded WPS client enabling users to visually build a WPS Execute request (and as a side effect also showing
 * what capabilities and describe process would provide).
 *
 * <p>Parameters:
 *
 * <ul>
 *   <li><b>name=</b>procName - display the page showing given process
 * </ul>
 *
 * @author Andrea Aime - OpenGeo
 * @author Martin Davis - OpenGeo
 */
// TODO WICKET8 - Verify this page works OK
@SuppressWarnings("serial")
public class WPSRequestBuilder extends GeoServerBasePage {

    public static final String PARAM_NAME = "name";

    WPSRequestBuilderPanel builder;

    TextField<String> xml;

    public WPSRequestBuilder(PageParameters parameters) {
        this(parameters.get(PARAM_NAME).toOptionalString());
    }

    public WPSRequestBuilder() {
        this((String) null);
    }

    public WPSRequestBuilder(String procName) {
        // the form
        Form form = new Form<>("form");
        add(form);

        Model<WPSRequestModel> model = new Model<>(new WPSRequestModel());
        form.setDefaultModel(model);

        xml = new TextField<>("xml", new PropertyModel<>(model, "xml"));
        xml.setOutputMarkupId(true);
        form.add(xml);

        // the actual request builder component
        ExecuteRequest execRequest = new ExecuteRequest();
        if (procName != null) execRequest.processName = procName;

        builder = new WPSRequestBuilderPanel("requestBuilder", execRequest);
        form.add(builder);

        // the xml popup window
        final GSModalWindow xmlWindow = new GSModalWindow("xmlWindow");
        add(xmlWindow);

        form.add(new AjaxSubmitLink("setXml") {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                try {
                    String xmlText = getRequestXML();
                    xml.setModelObject(xmlText);
                    target.add(xml);
                } catch (Exception e) {
                    error(e.getMessage());
                    addFeedbackPanels(target);
                }
                target.appendJavaScript("executeWPS()");
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                addFeedbackPanels(target);
            }
        });

        form.add(new AjaxSubmitLink("execute") {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                HttpServletRequest http = GeoServerApplication.get().servletRequest();

                String url = ResponseUtils.buildURL(
                        ResponseUtils.baseURL(http),
                        "ows",
                        Collections.singletonMap("strict", "true"),
                        URLType.SERVICE);
                String xml = getRequestXML();

                PageParameters parameters = new PageParameters();
                parameters.add("url", url);
                parameters.add("xml", xml);

                getRequestCycle().setResponsePage(DemoRequestsPage.class, parameters);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                target.add(builder.getFeedbackPanel());
            }
        });

        form.add(new AjaxSubmitLink("executeXML") {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                try {
                    xmlWindow.setContent(new PlainCodePage(xmlWindow.getContentId(), getRequestXML()));
                    xmlWindow.show(target);
                } catch (Exception e) {
                    error(e.getMessage());
                    addFeedbackPanels(target);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                addFeedbackPanels(target);
            }
        });
    }

    String getRequestXML() {
        // turn the GUI request into an actual WPS request
        WPSExecuteTransformer tx = new WPSExecuteTransformer(getCatalog());
        tx.setEntityResolver(getCatalog().getResourcePool().getEntityResolver());
        tx.setIndentation(2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            tx.transform(builder.execute, out);
        } catch (TransformerException e) {
            LOGGER.log(Level.SEVERE, "Error generating xml request", e);
            error(e);
        }
        return out.toString();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptContentHeaderItem.forScript(DemoRequestsPage.demoRequestsJavascript, null));
        response.render(CssHeaderItem.forCSS("#xml {display: none;}", "wpsRequestBuilderCSS"));
    }

    public static class WPSRequestModel implements Serializable {
        public String xml;

        public String getXml() {
            return xml;
        }

        public void setXml(String xml) {
            this.xml = xml;
        }
    }
}
