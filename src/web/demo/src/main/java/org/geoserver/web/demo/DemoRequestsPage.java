/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geotools.util.logging.Logging;

/**
 * @author Gabriel Roldan
 * @since 1.8.x
 * @version $Id$
 */
@SuppressWarnings("serial")
public class DemoRequestsPage extends GeoServerBasePage {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web.demo");

    final Resource demoDir;

    private TextField urlTextField;

    private CodeMirrorEditor body;

    private TextField username;

    private PasswordTextField password;

    public DemoRequestsPage() {
        try {
            GeoServerResourceLoader loader = this.getGeoServer().getCatalog().getResourceLoader();
            demoDir = Resources.serializable(loader.get("demo"));
        } catch (Exception e) {
            throw new WicketRuntimeException(
                    "Can't access demo requests directory: " + e.getMessage());
        }
        DemoRequest model = new DemoRequest(demoDir.path());
        setDefaultModel(new Model(model));

        setUpDemoRequestsForm(demoDir);
    }

    /** Package visible constructor aimed to help in setting up unit tests for this class */
    DemoRequestsPage(final Resource demoDir) {
        this.demoDir = Resources.serializable(demoDir);
        DemoRequest model = new DemoRequest(demoDir.path());
        setDefaultModel(new Model(model));
        setUpDemoRequestsForm(demoDir);
    }

    /**
     * Loads the contents of the demo request file named {@code reqFileName} and located in the demo
     * directory.
     *
     * @param reqFileName the file name to load the contents for
     * @return the file contents
     * @throws IOException if an io exception occurs opening or loading the file
     */
    private String getFileContents(final String reqFileName) throws IOException {
        final Resource file = demoDir.get(reqFileName);
        final StringBuilder sb = new StringBuilder();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(file.in()));
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

    private void setUpDemoRequestsForm(final Resource demoDir) {
        final IModel requestModel = getDefaultModel();

        final Form demoRequestsForm;
        demoRequestsForm = new Form("demoRequestsForm");
        demoRequestsForm.setOutputMarkupId(true);
        demoRequestsForm.setModel(requestModel);
        add(demoRequestsForm);

        final List<String> demoList = getDemoList(demoDir);
        final DropDownChoice demoRequestsList;
        final IModel reqFileNameModel = new PropertyModel(requestModel, "requestFileName");
        demoRequestsList =
                new Select2DropDownChoice(
                        "demoRequestsList",
                        reqFileNameModel,
                        demoList,
                        new ChoiceRenderer() {
                            public String getIdValue(Object obj, int index) {
                                return String.valueOf(obj);
                            }

                            public Object getDisplayValue(Object obj) {
                                return obj;
                            }
                        });
        demoRequestsForm.add(demoRequestsList);

        /*
         * Wanted to use a simpler OnChangeAjaxBehavior but target.add(body) does not make
         * the EditAreaBehavior to update the body contents inside it, but instead puts the plain
         * TextArea contents above the empty xml editor
         */
        demoRequestsList.add(
                new AjaxFormSubmitBehavior(demoRequestsForm, "change") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        final String reqFileName = demoRequestsList.getModelValue();
                        final String contents;
                        String proxyBaseUrl;
                        final String baseUrl;
                        {
                            HttpServletRequest httpServletRequest =
                                    getGeoServerApplication()
                                            .servletRequest(DemoRequestsPage.this.getRequest());
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
                            throw new WicketRuntimeException(
                                    "Can't load demo file " + reqFileName, e);
                        }

                        boolean demoRequestIsHttpGet = reqFileName.endsWith(".url");
                        final String service =
                                reqFileName.substring(0, reqFileName.indexOf('_')).toLowerCase();
                        if (demoRequestIsHttpGet) {
                            String url = ResponseUtils.appendPath(baseUrl, contents);
                            urlTextField.setModelObject(url);
                            body.setModelObject("");
                        } else {
                            String serviceUrl = ResponseUtils.appendPath(baseUrl, service);
                            urlTextField.setModelObject(serviceUrl);
                            body.setModelObject(contents);
                        }

                        // target.add(urlTextField);
                        // target.add(body);
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
        // body.add(new EditAreaBehavior());
        demoRequestsForm.add(body);

        username = new TextField("username", new PropertyModel(requestModel, "userName"));
        demoRequestsForm.add(username);

        password = new PasswordTextField("password", new PropertyModel(requestModel, "password"));
        password.setRequired(false);
        demoRequestsForm.add(password);

        final ModalWindow responseWindow;

        responseWindow = new ModalWindow("responseWindow");
        add(responseWindow);

        // responseWindow.setPageMapName("demoResponse");
        responseWindow.setCookieName("demoResponse");

        responseWindow.setPageCreator(
                new ModalWindow.PageCreator() {

                    public Page createPage() {
                        return new DemoRequestResponse(requestModel);
                    }
                });

        demoRequestsForm.add(
                new AjaxSubmitLink("submit", demoRequestsForm) {
                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form testWfsPostForm) {
                        responseWindow.show(target);
                    }

                    @Override
                    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                        super.updateAjaxAttributes(attributes);
                        // we need to force EditArea to update the textarea contents (which it
                        // hides)
                        // before submitting the form, otherwise the contents won't be the ones the
                        // user
                        // edited
                        attributes
                                .getAjaxCallListeners()
                                .add(
                                        new AjaxCallListener() {
                                            @Override
                                            public CharSequence getBeforeHandler(
                                                    Component component) {
                                                return "document.getElementById('requestBody').value = document.gsEditors.requestBody.getValue();";
                                            }
                                        });
                    }
                });
    }

    private List<String> getDemoList(final Resource demoDir) {
        final List<String> demoList = new ArrayList<String>();
        for (Resource file : demoDir.list()) {
            if (file.getType() != Type.DIRECTORY) {
                final String name = file.name();
                if (name.endsWith(".url") || name.endsWith(".xml")) {
                    demoList.add(name);
                } else {
                    LOGGER.warning(
                            "Ignoring file "
                                    + name
                                    + " in demo requests directory, only .url and .xml files allowed");
                }
            }
        }
        Collections.sort(demoList);
        return demoList;
    }
}
