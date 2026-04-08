/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.CharStreams;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptContentHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
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
 */
// TODO WICKET8 - Verify this page works OK
@SuppressWarnings("serial")
public class DemoRequestsPage extends GeoServerBasePage {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.web.demo");

    /**
     * Javascript required by the Demo Request page to do the client-side requests. This is shared among other modules
     * (cf WCS Request Builder, and WPS Request Builder).
     *
     * <p>See static block, below.
     */
    public static String demoRequestsJavascript;

    /**
     * Style sheet (Content-Security-Policy does not allow inline-styles)
     *
     * <p>See static block, below.
     */
    public static String demoRequestsCSS;

    static {
        try {
            String demo_request_js = CharStreams.toString(new InputStreamReader(
                    DemoRequestsPage.class.getResourceAsStream("/org/geoserver/web/demo/demo-requests.js"), UTF_8));
            String xml_pretty_print_js = CharStreams.toString(new InputStreamReader(
                    DemoRequestsPage.class.getResourceAsStream("/org/geoserver/web/demo/xml-pretty-print.js"), UTF_8));
            String js = demo_request_js + "\n" + xml_pretty_print_js;
            demoRequestsJavascript = js;

            demoRequestsCSS = CharStreams.toString(new InputStreamReader(
                    DemoRequestsPage.class.getResourceAsStream("/org/geoserver/web/demo/demo-requests.css"), UTF_8));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "error occurred reading demoRequestsJavascript", e);
        }
    }

    Resource demoDir;

    private TextField<String> urlTextField;

    private CodeMirrorEditor body;

    private TextField<String> username;

    private PasswordTextField password;

    private CheckBox prettyXML;
    private CheckBox openNewPage;

    public DemoRequestsPage(PageParameters parameters) {
        super(parameters);
        setup();
        if (parameters != null) {
            if (parameters.get("xml") != null) {
                ((DemoRequest) this.getDefaultModel().getObject())
                        .setRequestBody(parameters.get("xml").toString());
            }
            if (parameters.get("url") != null) {
                ((DemoRequest) this.getDefaultModel().getObject())
                        .setRequestUrl(parameters.get("url").toString());
            }
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptContentHeaderItem.forScript(demoRequestsJavascript, null));
        response.render(CssHeaderItem.forCSS(demoRequestsCSS, "demoRequestsCSS"));

        // setup onClick events (Content-security-policy doesn't allow onClick events in the HTML)
        String script = "\n";

        script +=
                "$('#linkSubmit').on('click',function() {\ndocument.getElementById('openNewWindow').checked = false; \nsubmitRequest();\n} \n);\n\n";
        script +=
                "$('#linkSubmitNewWin').on('click',function() {\ndocument.getElementById('openNewWindow').checked = true;\nsubmitRequest();\n} );\n\n";

        script += "\n";
        response.render(OnDomReadyHeaderItem.forScript(script));
    }

    public void setup() {
        try {
            GeoServerResourceLoader loader = this.getGeoServer().getCatalog().getResourceLoader();
            demoDir = Resources.serializable(loader.get("demo"));
        } catch (Exception e) {
            throw new WicketRuntimeException("Can't access demo requests directory: " + e.getMessage());
        }
        DemoRequest request = new DemoRequest(demoDir.path());
        setDefaultModel(new Model<>(request));

        setUpDemoRequestsForm(demoDir);
    }

    public DemoRequestsPage() {
        setup();
    }

    /** Package visible constructor aimed to help in setting up unit tests for this class */
    DemoRequestsPage(final Resource demoDir) {
        this.demoDir = Resources.serializable(demoDir);
        DemoRequest model = new DemoRequest(demoDir.path());
        setDefaultModel(new Model<>(model));
        setUpDemoRequestsForm(demoDir);
    }

    DemoRequestsPage(final Resource demoDir, PageParameters parameters) {
        this(demoDir);
        if (parameters != null) {
            if (parameters.get("xml") != null) {
                ((DemoRequest) this.getDefaultModel().getObject())
                        .setRequestBody(parameters.get("xml").toString());
            }
            if (parameters.get("url") != null) {
                ((DemoRequest) this.getDefaultModel().getObject())
                        .setRequestUrl(parameters.get("url").toString());
            }
        }
    }

    /**
     * Loads the contents of the demo request file named {@code reqFileName} and located in the demo directory.
     *
     * @param reqFileName the file name to load the contents for
     * @return the file contents
     * @throws IOException if an io exception occurs opening or loading the file
     */
    private String getFileContents(final String reqFileName) throws IOException {
        final Resource file = demoDir.get(reqFileName);
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.in()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void setUpDemoRequestsForm(final Resource demoDir) {
        @SuppressWarnings("unchecked")
        final IModel<DemoRequest> requestModel = (IModel<DemoRequest>) getDefaultModel();

        final Form<DemoRequest> demoRequestsForm = new Form<>("demoRequestsForm");
        demoRequestsForm.setOutputMarkupId(true);
        demoRequestsForm.setModel(requestModel);
        add(demoRequestsForm);

        final List<String> demoList = getDemoList(demoDir);
        final IModel<String> reqFileNameModel = new PropertyModel<>(requestModel, "requestFileName");
        final DropDownChoice<String> demoRequestsList =
                new Select2DropDownChoice<>("demoRequestsList", reqFileNameModel, demoList, new ChoiceRenderer<>() {
                    @Override
                    public String getIdValue(String obj, int index) {
                        return obj;
                    }

                    @Override
                    public Object getDisplayValue(String obj) {
                        return obj;
                    }
                });
        demoRequestsForm.add(demoRequestsList);

        /*
         * Wanted to use a simpler OnChangeAjaxBehavior but target.add(body) does not make
         * the EditAreaBehavior to update the body contents inside it, but instead puts the plain
         * TextArea contents above the empty xml editor
         */
        demoRequestsList.add(new AjaxFormSubmitBehavior(demoRequestsForm, "change") {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                final String reqFileName = demoRequestsList.getModelValue();
                final String contents;
                String proxyBaseUrl;
                final String baseUrl;
                {
                    HttpServletRequest httpServletRequest =
                            getGeoServerApplication().servletRequest(DemoRequestsPage.this.getRequest());
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

        urlTextField = new TextField<>("url", new PropertyModel<>(requestModel, "requestUrl"));
        urlTextField.setMarkupId("requestUrl");
        urlTextField.setOutputMarkupId(true);
        demoRequestsForm.add(urlTextField);

        body = new CodeMirrorEditor("body", new PropertyModel<>(requestModel, "requestBody"));
        // force the id otherwise this blasted thing won't be usable from other forms
        // body.setMarkupId("requestBody");
        // body.setOutputMarkupId(true);
        body.setTextAreaMarkupId("requestBody");
        // body.add(new EditAreaBehavior());
        demoRequestsForm.add(body);

        username = new TextField<>("username", new PropertyModel<>(requestModel, "userName"));
        demoRequestsForm.add(username);

        password = new PasswordTextField("password", new PropertyModel<>(requestModel, "password"));
        password.setRequired(false);
        password.setResetPassword(false);
        demoRequestsForm.add(password);

        prettyXML = new CheckBox("prettyXML", new PropertyModel<>(requestModel, "prettyXML"));
        demoRequestsForm.add(prettyXML);

        openNewPage = new CheckBox("openNewWindow", new PropertyModel<>(requestModel, "openNewWindow"));
        demoRequestsForm.add(openNewPage);
    }

    private List<String> getDemoList(final Resource demoDir) {
        final List<String> demoList = new ArrayList<>();
        for (Resource file : demoDir.list()) {
            if (file.getType() != Type.DIRECTORY) {
                final String name = file.name();
                if (name.endsWith(".url") || name.endsWith(".xml")) {
                    demoList.add(name);
                } else {
                    LOGGER.warning(
                            "Ignoring file " + name + " in demo requests directory, only .url and .xml files allowed");
                }
            }
        }
        Collections.sort(demoList);
        return demoList;
    }
}
