/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

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
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.demo.DemoRequest;
import org.geoserver.web.demo.DemoRequestResponse;
import org.geoserver.web.demo.PlainCodePage;

/**
 * Small embedded WPS client enabling users to visually build a WPS Execute request (and as a side
 * effect also showing what capabilities and describe process would provide).
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
@SuppressWarnings({"serial", "rawtypes"})
public class WPSRequestBuilder extends GeoServerBasePage {

    public static final String PARAM_NAME = "name";

    ModalWindow responseWindow;
    WPSRequestBuilderPanel builder;

    public WPSRequestBuilder(PageParameters parameters) {
        this(parameters.get(PARAM_NAME).toOptionalString());
    }

    public WPSRequestBuilder() {
        this((String) null);
    }

    public WPSRequestBuilder(String procName) {
        // the form
        Form form = new Form("form");
        add(form);

        // the actual request builder component
        ExecuteRequest execRequest = new ExecuteRequest();
        if (procName != null) execRequest.processName = procName;

        builder = new WPSRequestBuilderPanel("requestBuilder", execRequest);
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
        // removed, don't know what it did, but page maps are gone in 1.5...
        // responseWindow.setPageMapName("demoResponse");
        responseWindow.setCookieName("demoResponse");

        responseWindow.setPageCreator(
                new ModalWindow.PageCreator() {

                    @SuppressWarnings("unchecked")
                    public Page createPage() {
                        DemoRequest request = new DemoRequest(null);
                        HttpServletRequest http =
                                (HttpServletRequest)
                                        WPSRequestBuilder.this.getRequest().getContainerRequest();
                        String url =
                                ResponseUtils.buildURL(
                                        ResponseUtils.baseURL(http),
                                        "ows",
                                        Collections.singletonMap("strict", "true"),
                                        URLType.SERVICE);
                        request.setRequestUrl(url);
                        request.setRequestBody((String) responseWindow.getDefaultModelObject());
                        request.setUserName(builder.username);
                        request.setPassword(builder.password);
                        return new DemoRequestResponse(new Model(request));
                    }
                });

        form.add(
                new AjaxSubmitLink("execute") {

                    @SuppressWarnings("unchecked")
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
}
