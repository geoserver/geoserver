/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.demo;

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
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs.web.demo.GetCoverageRequest.Version;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.demo.DemoRequestsPage;
import org.geoserver.web.demo.PlainCodePage;
import org.geoserver.web.wicket.GSModalWindow;
import org.geotools.xml.transform.TransformerBase;

/**
 * Small embedded WCS client enabling users to build a wcs GetCoverage request (and as a side effect also showing what
 * capabilities and describe process would provide) using
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class WCSRequestBuilder extends GeoServerBasePage {

    WCSRequestBuilderPanel builder;

    TextField<String> xml;

    public WCSRequestBuilder() {
        // the form
        Form form = new Form<>("form");
        add(form);

        Model<WCSRequestModel> model = new Model<>(new WCSRequestModel());
        form.setDefaultModel(model);

        xml = new TextField<>("xml", new PropertyModel<>(model, "xml"));
        xml.setOutputMarkupId(true);
        form.add(xml);

        // the actual request builder component
        builder = new WCSRequestBuilderPanel("requestBuilder", new GetCoverageRequest());
        form.add(builder);

        // the xml popup window
        final GSModalWindow xmlWindow = new GSModalWindow("xmlWindow");
        add(xmlWindow);

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
                target.appendJavaScript("getCoverage()");
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                addFeedbackPanels(target);
            }
        });
    }

    String getRequestXML() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        TransformerBase tx;
        if (builder.getCoverage.version == Version.v1_0_0) {
            tx = new WCS10GetCoverageTransformer(getCatalog());
        } else {
            CoverageResponseDelegateFinder responseFactory = (CoverageResponseDelegateFinder)
                    getGeoServerApplication().getBean("coverageResponseDelegateFactory");
            tx = new WCS11GetCoverageTransformer(getCatalog(), responseFactory);
        }

        try {
            tx.setIndentation(2);
            tx.transform(builder.getCoverageRequest(), out);
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

        response.render(CssHeaderItem.forCSS("#xml {display: none;}", "wcsRequestBuilderCSS"));
    }

    public static class WCSRequestModel implements Serializable {
        public String xml;

        public String getXml() {
            return xml;
        }

        public void setXml(String xml) {
            this.xml = xml;
        }
    }
}
