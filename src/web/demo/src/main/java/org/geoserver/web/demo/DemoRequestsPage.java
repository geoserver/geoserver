/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.global.ConfigurationException;
import org.geoserver.config.GeoServer;

/**
 * 
 * @author Gabriel Roldan
 * @since 1.8.x
 * @version $Id$
 */
@SuppressWarnings("serial")
public class DemoRequestsPage extends GeoServerBasePage {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web.demo");

    private final File demoDir;

    private TextField urlTextField;

    private CodeMirrorEditor body;

    private TextField username;

    private PasswordTextField password;

    public DemoRequestsPage() {
        try {
            GeoServerResourceLoader loader = this.getGeoServer().getCatalog().getResourceLoader();
            Resource demo = loader.get("demo");
            demoDir = demo.dir(); // find or create
        } catch (Exception e) {
            throw new WicketRuntimeException("Can't access demo requests directory: "
                    + e.getMessage());
        }
        DemoRequest model = new DemoRequest(demoDir);
        setDefaultModel(new Model(model));

        setUpDemoRequestsForm(demoDir);
    }

    /**
     * Package visible constructor aimed to help in setting up unit tests for this class
     * 
     * @param demoDir
     */
    DemoRequestsPage(final File demoDir) {
        this.demoDir = demoDir;
        DemoRequest model = new DemoRequest(demoDir);
        setDefaultModel(new Model(model));
        setUpDemoRequestsForm(demoDir);
    }

    /**
     * Loads the contents of the demo request file named {@code reqFileName} and located in the
     * {@link #getDemoDir() demo directory}.
     * 
     * @param reqFileName
     *            the file name to load the contents for
     * @return the file contents
     * @throws IOException
     *             if an io exception occurs opening or loading the file
     */
    private String getFileContents(final String reqFileName) throws IOException {
        final File file = new File(demoDir, reqFileName);
        final StringBuilder sb = new StringBuilder();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    private void setUpDemoRequestsForm(final File demoDir) {
        final IModel requestModel = getDefaultModel();

        final Form demoRequestsForm;
        demoRequestsForm = new Form("demoRequestsForm");
        demoRequestsForm.setOutputMarkupId(true);
        demoRequestsForm.setModel(requestModel);
        add(demoRequestsForm);

        final List<String> demoList = getDemoList(demoDir);
        final DropDownChoice demoRequestsList;
        final IModel reqFileNameModel = new PropertyModel(requestModel, "requestFileName");
        demoRequestsList = new DropDownChoice("demoRequestsList", reqFileNameModel, demoList,
                new IChoiceRenderer() {
                    public String getIdValue(Object obj, int index) {
                        return String.valueOf(obj);
                    }

                    public Object getDisplayValue(Object obj) {
                        return obj;
                    }
                });
        demoRequestsForm.add(demoRequestsList);

        /*
         * Wanted to use a simpler OnChangeAjaxBehavior but target.addComponent(body) does not make
         * the EditAreaBehavior to update the body contents inside it, but instead puts the plain
         * TextArea contents above the empty xml editor
         */
        demoRequestsList.add(new AjaxFormSubmitBehavior(demoRequestsForm, "onchange") {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                final String reqFileName = demoRequestsList.getModelValue();
                final String contents;
                String proxyBaseUrl;
                final String baseUrl;
                {
                    WebRequest request = (WebRequest) DemoRequestsPage.this.getRequest();
                    HttpServletRequest httpServletRequest;
                    httpServletRequest = ((WebRequest) request).getHttpServletRequest();                   
                    proxyBaseUrl = GeoServerExtensions.getProperty("PROXY_BASE_URL");                    
                    if (StringUtils.isEmpty(proxyBaseUrl)) {
                        GeoServer gs = getGeoServer();
                        proxyBaseUrl = gs.getGlobal().getSettings().getProxyBaseUrl();
                        if (StringUtils.isEmpty(proxyBaseUrl)) {
                            baseUrl = ResponseUtils.baseURL(httpServletRequest);
                        } else {
                            baseUrl = proxyBaseUrl;
                        }
                    } else {
                        baseUrl = proxyBaseUrl;
                    }
                }
                try {
                    contents = getFileContents(reqFileName);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Can't load demo file " + reqFileName, e);
                    throw new WicketRuntimeException("Can't load demo file " + reqFileName, e);
                }

                boolean demoRequestIsHttpGet = reqFileName.endsWith(".url");
                final String service = reqFileName.substring(0, reqFileName.indexOf('_'))
                        .toLowerCase();
                if (demoRequestIsHttpGet) {
                    String url = baseUrl + contents;
                    urlTextField.setModelObject(url);
                    body.setModelObject("");
                } else {
                    String serviceUrl = baseUrl + service;
                    urlTextField.setModelObject(serviceUrl);
                    body.setModelObject(contents);
                }

                // target.addComponent(urlTextField);
                // target.addComponent(body);
                /*
                 * Need to setResponsePage, addComponent causes the EditAreaBehavior to sometimes
                 * not updating properly
                 */
                setResponsePage(DemoRequestsPage.this);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                // nothing to do
            }
        });

        urlTextField = new TextField("url", new PropertyModel(requestModel, "requestUrl"));
        urlTextField.setMarkupId("requestUrl");
        urlTextField.setOutputMarkupId(true);
        demoRequestsForm.add(urlTextField);

        body = new CodeMirrorEditor("body", new PropertyModel(requestModel, "requestBody"));
        // force the id otherwise this blasted thing won't be usable from other forms
        // body.setMarkupId("requestBody");
        // body.setOutputMarkupId(true);
        body.setTextAreaMarkupId("requestBody");
        //body.add(new EditAreaBehavior());
        demoRequestsForm.add(body);

        username = new TextField("username", new PropertyModel(requestModel, "userName"));
        demoRequestsForm.add(username);

        password = new PasswordTextField("password", new PropertyModel(requestModel, "password"));
        password.setRequired(false);
        demoRequestsForm.add(password);

        final ModalWindow responseWindow;

        responseWindow = new ModalWindow("responseWindow");
        add(responseWindow);
        responseWindow.setPageMapName("demoResponse");
        responseWindow.setCookieName("demoResponse");

        responseWindow.setPageCreator(new ModalWindow.PageCreator() {

            public Page createPage() {
                return new DemoRequestResponse(requestModel);
            }
        });

        demoRequestsForm.add(new AjaxSubmitLink("submit", demoRequestsForm) {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form testWfsPostForm) {
                responseWindow.show(target);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                // we need to force EditArea to update the textarea contents (which it hides)
                // before submitting the form, otherwise the contents won't be the ones the user
                // edited
                return new AjaxCallDecorator() {
                    @Override
                    public CharSequence decorateScript(CharSequence script) {
                        return "document.getElementById('requestBody').value = document.gsEditors.requestBody.getValue();"
                                + script;
                    }
                };
            }

        });
    }

    private List<String> getDemoList(final File demoDir) {
        final List<String> demoList = new ArrayList<String>();
        for (File file : demoDir.listFiles()) {
            if (!file.isDirectory()) {
                final String name = file.getName();
                if (name.endsWith(".url") || name.endsWith(".xml")) {
                    demoList.add(name);
                } else {
                    LOGGER.warning("Ignoring file " + name
                            + " in demo requests directory, only .url and .xml files allowed");
                }
            }
        }
        Collections.sort(demoList);
        return demoList;
    }
}
