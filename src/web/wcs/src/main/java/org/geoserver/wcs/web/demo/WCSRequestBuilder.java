/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.demo;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs.web.demo.GetCoverageRequest.Version;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.demo.DemoRequest;
import org.geoserver.web.demo.DemoRequestResponse;
import org.geoserver.web.demo.PlainCodePage;
import org.geotools.xml.transform.TransformerBase;

/**
 * Small embedded WCS client enabling users to build a wcs GetCoverage request (and as a side effect
 * also showing what capabilities and describe process would provide) using
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class WCSRequestBuilder extends GeoServerBasePage {

    ModalWindow responseWindow;

    WCSRequestBuilderPanel builder;

    public WCSRequestBuilder() {
        // the form
        Form form = new Form("form");
        add(form);

        // the actual request builder component
        builder = new WCSRequestBuilderPanel("requestBuilder", new GetCoverageRequest());
        form.add(builder);

        // the xml popup window
        final ModalWindow xmlWindow = new ModalWindow("xmlWindow");
        add(xmlWindow);
        xmlWindow.setPageCreator(
                new ModalWindow.PageCreator() {

                    public Page createPage() {
                        return new PlainCodePage(xmlWindow, responseWindow, getRequestXML());
                    }
                });

        // the output response window
        responseWindow = new ModalWindow("responseWindow");
        add(responseWindow);
        // responseWindow.setPageMapName("demoResponse");
        responseWindow.setCookieName("demoResponse");

        responseWindow.setPageCreator(
                new ModalWindow.PageCreator() {

                    public Page createPage() {
                        DemoRequest request = new DemoRequest(null);
                        HttpServletRequest http = GeoServerApplication.get().servletRequest();
                        String url =
                                ResponseUtils.buildURL(
                                        ResponseUtils.baseURL(http),
                                        "ows",
                                        Collections.singletonMap("strict", "true"),
                                        URLType.SERVICE);
                        request.setRequestUrl(url);
                        request.setRequestBody((String) responseWindow.getDefaultModelObject());
                        return new DemoRequestResponse(new Model(request));
                    }
                });

        form.add(
                new AjaxSubmitLink("execute") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        responseWindow.setDefaultModel(new Model(getRequestXML()));
                        responseWindow.show(target);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form form) {
                        super.onError(target, form);
                        target.add(builder.getFeedbackPanel());
                    }
                });

        form.add(
                new AjaxSubmitLink("executeXML") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form form) {
                        try {
                            getRequestXML();
                            xmlWindow.show(target);
                        } catch (Exception e) {
                            error(e.getMessage());
                            addFeedbackPanels(target);
                        }
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form form) {
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
            CoverageResponseDelegateFinder responseFactory =
                    (CoverageResponseDelegateFinder)
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
}
