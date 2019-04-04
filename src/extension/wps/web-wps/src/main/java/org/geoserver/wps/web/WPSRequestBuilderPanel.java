/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.ows.Ows11Util;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.demo.DemoRequest;
import org.geoserver.web.demo.DemoRequestResponse;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;

/**
 * Small embedded WPS client enabling users to visually build a WPS Execute request (and as a side
 * effect also showing what capabilities and describe process would provide)
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class WPSRequestBuilderPanel extends Panel {
    static final Logger LOGGER = Logging.getLogger(WPSRequestBuilderPanel.class);

    ExecuteRequest execute;

    String description;

    ModalWindow responseWindow;

    private Component feedback;

    private WebMarkupContainer descriptionContainer;

    private WebMarkupContainer inputContainer;

    private WebMarkupContainer outputContainer;

    private ListView<InputParameterValues> inputView;

    private ListView<OutputParameter> outputView;

    String username;

    String password;

    boolean authenticate;

    /**
     * Creates a panel to display a process and its parameters. Invoked with one of:
     *
     * <ul>
     *   <li>an empty executeRequest, which displays only the process dropdown
     *   <li<an executeRequest with the processName set, which displays the process and parameters
     * </ul>
     *
     * @param id id of the panel
     * @param executeRequest execute request, possibly with processName set
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public WPSRequestBuilderPanel(String id, ExecuteRequest executeRequest) {
        super(id);
        setOutputMarkupId(true);
        this.execute = executeRequest;

        final DropDownChoice<String> processChoice =
                new Select2DropDownChoice<String>(
                        "process",
                        new PropertyModel<String>(execute, "processName"),
                        buildProcessList());
        add(processChoice);

        descriptionContainer = new WebMarkupContainer("descriptionContainer");
        descriptionContainer.setVisible(false);
        add(descriptionContainer);

        // the process description
        final Label descriptionLabel =
                new Label("processDescription", new PropertyModel(this, "description"));
        descriptionContainer.add(descriptionLabel);
        // description value is set later in initProcessView()

        inputContainer = new WebMarkupContainer("inputContainer");
        inputContainer.setVisible(false);
        add(inputContainer);
        inputView =
                new ListView<InputParameterValues>("inputs", new PropertyModel(execute, "inputs")) {

                    @Override
                    protected void populateItem(ListItem item) {
                        InputParameterValues pv = (InputParameterValues) item.getModelObject();
                        Parameter p = pv.getParameter();
                        item.add(new Label("param", buildParamSpec(p)));
                        item.add(
                                new Label(
                                        "paramDescription",
                                        p.description.toString(Locale.ENGLISH)));
                        // TODO: roll out an extension point for these editors
                        final PropertyModel property = new PropertyModel(pv, "values[0].value");
                        if (pv.isBoundingBox()) {
                            EnvelopePanel envelope = new EnvelopePanel("paramValue", property);
                            envelope.setCRSFieldVisible(true);
                            item.add(envelope);
                        } else if (pv.isCoordinateReferenceSystem()) {
                            CRSPanel crs = new CRSPanel("paramValue", property);
                            item.add(crs);
                        } else if (pv.isEnum()) {
                            EnumPanel panel =
                                    new EnumPanel(
                                            "paramValue",
                                            ((Class<Enum>) pv.getParameter().type),
                                            property);
                            item.add(panel);
                        } else if (pv.isComplex()) {
                            ComplexInputPanel input = new ComplexInputPanel("paramValue", pv, 0);
                            item.add(input);
                        } else {
                            Fragment f =
                                    new Fragment(
                                            "paramValue", "literal", WPSRequestBuilderPanel.this);
                            FormComponent literal = new TextField("literalValue", property);
                            literal.setRequired(p.minOccurs > 0);
                            literal.setLabel(new Model<String>(p.key));
                            f.add(literal);
                            item.add(f);
                        }
                    }
                };
        inputView.setReuseItems(true);
        inputContainer.add(inputView);

        outputContainer = new WebMarkupContainer("outputContainer");
        outputContainer.setVisible(false);
        add(outputContainer);
        outputView =
                new ListView("outputs", new PropertyModel(execute, "outputs")) {

                    @Override
                    protected void populateItem(ListItem item) {
                        OutputParameter pv = (OutputParameter) item.getModelObject();
                        Parameter p = pv.getParameter();
                        item.add(
                                new CheckBox("include", new PropertyModel<Boolean>(pv, "include")));
                        item.add(new Label("param", buildParamSpec(p)));
                        item.add(
                                new Label(
                                        "paramDescription",
                                        p.description.toString(Locale.ENGLISH)));
                        if (pv.isComplex()) {
                            DropDownChoice mime =
                                    new DropDownChoice(
                                            "mime",
                                            new PropertyModel(pv, "mimeType"),
                                            pv.getSupportedMime());
                            item.add(mime);
                        } else {
                            item.add(new Label("mime", "").setVisible(false)); // placeholder
                        }
                    }
                };
        outputView.setReuseItems(true);
        outputContainer.add(outputView);

        // the output response window
        responseWindow = new ModalWindow("responseWindow");
        add(responseWindow);
        // responseWindow.setPageMapName("demoResponse");
        responseWindow.setCookieName("demoResponse");

        responseWindow.setPageCreator(
                new ModalWindow.PageCreator() {

                    public Page createPage() {
                        DemoRequest request = new DemoRequest(null);
                        HttpServletRequest http =
                                (HttpServletRequest)
                                        WPSRequestBuilderPanel.this
                                                .getRequest()
                                                .getContainerRequest();
                        String url =
                                ResponseUtils.buildURL(
                                        ResponseUtils.baseURL(http),
                                        "ows",
                                        Collections.singletonMap("strict", "true"),
                                        URLType.SERVICE);
                        request.setRequestUrl(url);
                        request.setRequestBody((String) responseWindow.getDefaultModelObject());
                        return new DemoRequestResponse(new Model<DemoRequest>(request));
                    }
                });

        // the describe process link
        final GeoServerAjaxFormLink describeLink =
                new GeoServerAjaxFormLink("describeProcess") {

                    @Override
                    protected void onClick(AjaxRequestTarget target, Form form) {
                        processChoice.processInput();
                        if (execute.processName != null) {
                            responseWindow.setDefaultModel(
                                    new Model<String>(getDescribeXML(execute.processName)));
                            responseWindow.show(target);
                        }
                    }
                };
        descriptionContainer.add(describeLink);

        // the feedback panel, for validation errors
        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        // the process choice dropdown ajax behavior
        processChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        initProcessView();
                        target.add(WPSRequestBuilderPanel.this);

                        // ensure the parent page feedback panel gets refreshed to clear any
                        // existing err msg
                        // check for GeoServerBasePage, because parent page can also be a
                        // SubProcessBuilder
                        WebPage page = getWebPage();
                        if (page instanceof GeoServerBasePage) {
                            ((GeoServerBasePage) page).addFeedbackPanels(target);
                        }
                    }
                });
        // handle process name submitted as request param
        if (execute.processName != null) initProcessView();

        // username and password for authenticated requests
        final WebMarkupContainer authenticationContainer =
                new WebMarkupContainer("authenticationContainer");
        authenticationContainer.setOutputMarkupId(true);
        add(authenticationContainer);

        final WebMarkupContainer userpwdContainer = new WebMarkupContainer("userpwdContainer");
        userpwdContainer.setOutputMarkupId(true);
        userpwdContainer.setVisible(false);
        authenticationContainer.add(userpwdContainer);

        final TextField username = new TextField("username", new PropertyModel(this, "username"));
        userpwdContainer.add(username);

        final PasswordTextField password =
                new PasswordTextField("password", new PropertyModel(this, "password"));
        password.setRequired(false);
        userpwdContainer.add(password);

        CheckBox checkbox = new CheckBox("authenticate", new PropertyModel(this, "authenticate"));
        checkbox.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        userpwdContainer.setVisible(authenticate);
                        target.add(authenticationContainer);
                    }
                });
        authenticationContainer.add(checkbox);
    }

    private void initProcessView() {
        Name name = Ows11Util.name(execute.processName);
        ProcessFactory pf = Processors.createProcessFactory(name);
        if (pf == null) {
            error("No such process: " + execute.processName);
            descriptionContainer.setVisible(false);
            inputContainer.setVisible(false);
            outputContainer.setVisible(false);
        } else {
            description = pf.getDescription(name).toString(Locale.ENGLISH);
            execute.inputs = buildInputParameters(pf, name);
            execute.outputs = buildOutputParameters(pf, name);
            inputView.removeAll();
            outputView.removeAll();
            descriptionContainer.setVisible(true);
            inputContainer.setVisible(true);
            outputContainer.setVisible(true);
        }
    }

    protected String getDescribeXML(String processId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<DescribeProcess service=\"WPS\" version=\"1.0.0\" "
                + "xmlns=\"http://www.opengis.net/wps/1.0.0\" "
                + "xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                + "    <ows:Identifier>"
                + processId
                + "</ows:Identifier>\n"
                + "</DescribeProcess>";
    }

    String buildParamSpec(Parameter<?> p) {
        String spec = p.key;
        if (p.minOccurs > 0) {
            spec += "*";
        }
        spec += " - " + p.type.getSimpleName();
        if (p.minOccurs > 1 || p.maxOccurs != 1) {
            spec += "(" + p.minOccurs + "-";
            if (p.maxOccurs == -1) {
                spec += "unbounded";
            } else {
                spec += p.maxOccurs;
            }
            spec += ")";
        }
        return spec;
    }

    protected List<InputParameterValues> buildInputParameters(ProcessFactory pf, Name processName) {
        Map<String, Parameter<?>> params = pf.getParameterInfo(processName);
        List<InputParameterValues> result = new ArrayList<InputParameterValues>();
        for (String key : params.keySet()) {
            result.add(new InputParameterValues(processName, key));
        }

        return result;
    }

    protected List<OutputParameter> buildOutputParameters(ProcessFactory pf, Name processName) {
        Map<String, Parameter<?>> params = pf.getResultInfo(processName, null);
        List<OutputParameter> result = new ArrayList<OutputParameter>();
        for (String key : params.keySet()) {
            result.add(new OutputParameter(processName, key));
        }

        return result;
    }

    /** Builds a list of process ids */
    List<String> buildProcessList() {
        List<String> result = new ArrayList<String>();

        for (ProcessFactory pf : GeoServerProcessors.getProcessFactories()) {
            for (Name name : pf.getNames()) {
                result.add(name.getURI());
            }
        }
        Collections.sort(result);

        return result;
    }

    public Component getFeedbackPanel() {
        return feedback;
    }
}
