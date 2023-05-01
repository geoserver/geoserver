/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateLayerConfig;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.exception.GeoServerException;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class TemplatePreviewPanel extends Panel {

    private CodeMirrorEditor previewEditor;

    private static final String PREVIEW_RULE_FILTER = "requestParam('gsPreviewTemplate') = 'true'";

    private static final String PREVIEW_REQUEST_PARAM = "gsPreviewTemplate";

    private Form<PreviewInfoModel> previewInfoForm;

    private DropDownChoice<FeatureTypeInfo> featureTypesDD;

    private OutputFormatsDropDown outputFormatsDropDown;

    private DropDownChoice<WorkspaceInfo> workspaceInfoDropDownChoice;

    private TextField<String> featureIdField;

    private TextField<String> cqlFilterField;

    private TemplateConfigurationPage page;

    private FeedbackPanel previewFeedback;

    private String previewResult;

    public TemplatePreviewPanel(String id, TemplateConfigurationPage page) {
        super(id);
        this.page = page;
        initUI();
    }

    private void initUI() {
        Model<PreviewInfoModel> previewModel = new Model<>(new PreviewInfoModel());

        previewInfoForm = new Form<>("previewForm", previewModel);
        outputFormatsDropDown =
                new OutputFormatsDropDown(
                        "outputFormats",
                        new PropertyModel<>(previewModel, "outputFormat"),
                        page.getForm().getModelObject().getExtension());
        outputFormatsDropDown.add(
                new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                        SupportedFormat outputFormat = outputFormatsDropDown.getModelObject();
                        if (SupportedFormat.GML.equals(outputFormat)
                                || SupportedFormat.HTML.equals(outputFormat)) {
                            previewEditor.setMode("xml");
                        } else {
                            previewEditor.setModeAndSubMode("javascript", "json");
                        }
                        if (previewFeedback.hasFeedbackMessage()) {
                            clearFeedbackMessages();
                            ajaxRequestTarget.add(previewFeedback);
                        }
                        ajaxRequestTarget.add(previewEditor);
                    }
                });

        previewInfoForm.add(outputFormatsDropDown);
        IModel<TemplateInfo> templateInfo = page.getTemplateInfoModel();
        boolean hasFeatureType = templateInfo.getObject().getFeatureType() != null;
        boolean hasWorkspace = templateInfo.getObject().getWorkspace() != null;
        List<WorkspaceInfo> workspaces = getWorkspaces(getCatalog());
        ChoiceRenderer<WorkspaceInfo> wsRenderer = new ChoiceRenderer<>("name", "name");
        workspaceInfoDropDownChoice =
                new DropDownChoice<>(
                        "workspaces",
                        new PropertyModel<>(previewModel, "ws"),
                        workspaces,
                        wsRenderer);
        WorkspaceInfo wi = null;
        if (hasWorkspace) {
            wi = setWorkspaceValue(templateInfo.getObject().getWorkspace(), workspaces);
        }

        workspaceInfoDropDownChoice.add(
                new OnChangeAjaxBehavior() {

                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                        WorkspaceInfo wi = workspaceInfoDropDownChoice.getModelObject();
                        featureTypesDD.setChoices(getFeatureTypes(getCatalog(), wi));
                        featureTypesDD.setEnabled(true);
                        ajaxRequestTarget.add(featureTypesDD);
                        if (previewFeedback.hasFeedbackMessage()) {
                            clearFeedbackMessages();
                            ajaxRequestTarget.add(previewFeedback);
                        }
                    }
                });
        previewInfoForm.add(workspaceInfoDropDownChoice);
        List<FeatureTypeInfo> featureTypes;
        if (hasWorkspace) {
            featureTypes = getFeatureTypes(getCatalog(), wi);
        } else {
            featureTypes = Collections.emptyList();
        }
        ChoiceRenderer<FeatureTypeInfo> ftiChoiceRenderer = new ChoiceRenderer<>("name", "name");
        featureTypesDD =
                new DropDownChoice<>(
                        "featureTypes",
                        new PropertyModel<>(previewModel, "featureType"),
                        featureTypes,
                        ftiChoiceRenderer);
        featureTypesDD.setOutputMarkupId(true);
        if (!hasWorkspace) featureTypesDD.setEnabled(false);
        if (hasFeatureType) setFeatureTypeInfoValue(templateInfo.getObject().getFeatureType());
        previewInfoForm.add(featureTypesDD);
        previewInfoForm.add(
                previewEditor =
                        new CodeMirrorEditor(
                                "previewArea", "xml", new PropertyModel<>(this, "previewResult")));
        previewEditor.setOutputMarkupId(true);
        previewEditor.setTextAreaMarkupId("previewEditor");
        previewEditor.setMarkupId("previewArea");
        String extension = templateInfo.getObject().getExtension();
        if (extension.equals("json")) previewEditor.setModeAndSubMode("javascript", extension);
        previewInfoForm.add(previewEditor);
        previewInfoForm.add(previewFeedback = new FeedbackPanel("validateFeedback"));
        previewFeedback.setOutputMarkupId(true);
        featureIdField =
                new TextField<>("featureId", new PropertyModel<>(previewModel, "featureId"));
        previewInfoForm.add(featureIdField);
        cqlFilterField =
                new TextField<>("cqlFilterField", new PropertyModel<>(previewModel, "cqlFilter"));
        previewInfoForm.add(cqlFilterField);
        previewInfoForm.add(getSubmit());
        previewInfoForm.add(getValidate());
        previewInfoForm.setMultiPart(true);
        add(previewInfoForm);
    }

    private List<WorkspaceInfo> getWorkspaces(Catalog catalog) {
        return catalog.getWorkspaces();
    }

    private List<FeatureTypeInfo> getFeatureTypes(Catalog catalog, WorkspaceInfo ws) {
        if (ws != null) {
            NamespaceInfo nsi = catalog.getNamespaceByPrefix(ws.getName());
            return catalog.getFeatureTypesByNamespace(nsi);
        }
        return Collections.emptyList();
    }

    private String buildWFSLink(PreviewInfoModel previewInfoModel) {
        SupportedFormat outputFormat = previewInfoModel.getOutputFormat();
        WorkspaceInfo ws = previewInfoModel.getWs();
        FeatureTypeInfo featureType = previewInfoModel.getFeatureType();
        boolean canBuildLink = outputFormat != null && ws != null && featureType != null;
        if (canBuildLink) {
            TemplateLayerConfig layerConfig =
                    featureType
                            .getMetadata()
                            .get(TemplateLayerConfig.METADATA_KEY, TemplateLayerConfig.class);
            TemplateRule rule = new TemplateRule();
            IModel<TemplateInfo> templateInfo = page.getTemplateInfoModel();
            rule.setTemplateIdentifier(templateInfo.getObject().getIdentifier());
            rule.setTemplateName(templateInfo.getObject().getFullName());
            rule.setOutputFormat(outputFormat);
            rule.setForceRule(true);
            rule.setCqlFilter(PREVIEW_RULE_FILTER);
            if (layerConfig == null) {
                layerConfig = new TemplateLayerConfig();
            }
            layerConfig.addTemplateRule(rule);
            featureType.getMetadata().put(TemplateLayerConfig.METADATA_KEY, layerConfig);
            getCatalog().save(featureType);
            String mime = getOutputFormat(outputFormat);
            String typeName = ws.getName() + ":" + featureType.getName();
            return buildWfsLink(
                    typeName,
                    mime,
                    previewInfoModel.getFeatureId(),
                    previewInfoModel.getCqlFilter(),
                    ws);
        } else {
            error("please fill all the field to preview the template response");
        }
        return null;
    }

    String buildWfsLink(
            String typeName,
            String outputFormat,
            String featureId,
            String cqlFilter,
            WorkspaceInfo ws) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "WFS");
        params.put("version", "2.0.0");
        params.put("request", "GetFeature");
        params.put("typeNames", typeName);
        params.put("outputFormat", outputFormat);
        if (featureId != null) params.put("featureID", featureId);
        if (cqlFilter != null) params.put("cql_filter", cqlFilter);
        else params.put("count", "1");
        params.put(PREVIEW_REQUEST_PARAM, "true");
        return ResponseUtils.buildURL(
                getBaseURL(), getPath("ows", false, ws), params, URLMangler.URLType.SERVICE);
    }

    private String getBaseURL() {
        HttpServletRequest req = request();
        return ResponseUtils.baseURL(req);
    }

    private HttpServletRequest request() {
        return GeoServerApplication.get().servletRequest();
    }

    String getPath(String service, boolean useGlobalRef, WorkspaceInfo wi) {
        String ws = wi.getName();
        if (ws == null || useGlobalRef) {
            // global reference
            return service;
        } else {
            return ws + "/" + service;
        }
    }

    private String getOutputFormat(SupportedFormat outputFormatName) {
        String realOutputFormat = null;
        if (outputFormatName.equals(SupportedFormat.GEOJSON)) {
            realOutputFormat = TemplateIdentifier.JSON.getOutputFormat();
        } else if (outputFormatName.equals(SupportedFormat.JSONLD)) {
            realOutputFormat = TemplateIdentifier.JSONLD.getOutputFormat();
        } else if (outputFormatName.equals(SupportedFormat.GML)) {
            realOutputFormat = "application/gml+xml; version=3.2";
        } else if (outputFormatName.equals(SupportedFormat.HTML))
            realOutputFormat = TemplateIdentifier.HTML.getOutputFormat();
        return realOutputFormat;
    }

    private void removeTemplatePreviewRule(FeatureTypeInfo featureType) {
        TemplateLayerConfig config =
                featureType
                        .getMetadata()
                        .get(TemplateLayerConfig.METADATA_KEY, TemplateLayerConfig.class);
        Set<TemplateRule> rules = config.getTemplateRules();
        rules.removeIf(
                r -> r.getCqlFilter() != null && r.getCqlFilter().equals(PREVIEW_RULE_FILTER));
        featureType.getMetadata().put(TemplateLayerConfig.METADATA_KEY, config);
        getCatalog().save(featureType);
    }

    private AjaxSubmitLink getSubmit() {
        AjaxSubmitLink submitLink =
                new AjaxSubmitLink("preview", previewInfoForm) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onSubmit(target, form);
                        clearFeedbackMessages();
                        target.add(previewFeedback);
                        previewEditor.clearInput();
                        IModel<TemplateInfo> templateInfo = page.getTemplateInfoModel();
                        String rawTemplate = page.getStringTemplateFromInput();
                        page.saveTemplateInfo(templateInfo.getObject(), rawTemplate);
                        Form<PreviewInfoModel> previewForm = (Form<PreviewInfoModel>) form;

                        if (!validateAndReport(previewForm.getModelObject())) return;
                        String url = buildWFSLink(previewForm.getModelObject());
                        previewResult = performWfsRequest(url);
                        previewEditor.setModelObject(previewResult);
                        previewEditor.modelChanged();
                        target.add(previewEditor);
                    }

                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        FeatureTypeInfo featureTypeInfo = featureTypesDD.getModelObject();
                        if (featureTypeInfo != null) {
                            // clean cache
                            TemplateInfo ti = page.getTemplateInfoModel().getObject();
                            TemplateLoader.get().cleanCache(featureTypeInfo, ti.getIdentifier());
                            // remove the rule
                            removeTemplatePreviewRule(featureTypeInfo);
                        }
                        if (previewEditor.hasFeedbackMessage()) {
                            target.add(previewFeedback);
                        }
                    }
                };
        return submitLink;
    }

    private boolean validateAndReport(PreviewInfoModel info) {
        try {
            TemplateModelsValidator validator = new TemplateModelsValidator();
            validator.validate(info);
        } catch (GeoServerException e) {
            previewEditor.error(e.getMessage());
            return false;
        }
        return true;
    }

    private AjaxSubmitLink getValidate() {
        AjaxSubmitLink submitLink =
                new AjaxSubmitLink("validate", previewInfoForm) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onSubmit(target, form);
                        if (previewResult != null) {
                            SupportedFormat outputFormat = outputFormatsDropDown.getModelObject();
                            TemplateOutputValidator validator =
                                    new TemplateOutputValidator(outputFormat);
                            boolean result = validator.validate(previewResult);
                            String message = validator.getMessage();
                            if (!result) previewEditor.error(message);
                            else previewEditor.info(message);
                        }
                    }

                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        if (previewEditor.hasFeedbackMessage()) target.add(previewFeedback);
                    }
                };
        return submitLink;
    }

    private Catalog getCatalog() {
        return (Catalog) GeoServerExtensions.bean("catalog");
    }

    CloseableHttpClient buildHttpClient() {
        RequestConfig clientConfig =
                RequestConfig.custom().setConnectTimeout(60000).setSocketTimeout(60000).build();
        CookieStore cookieStore = new BasicCookieStore();
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            BasicClientCookie cookie =
                    new BasicClientCookie("JSESSIONID", attributes.getSessionId());
            HttpServletRequest request = request();
            cookie.setPath(request.getContextPath());
            // gets a calendar using the default time zone and locale.
            Calendar calendar = Calendar.getInstance();

            // use the session timeout to set the JSESSIONID cookie timeout
            int maxInactive = request.getSession().getMaxInactiveInterval();
            calendar.add(Calendar.SECOND, maxInactive > 0 ? maxInactive : 30);
            cookie.setExpiryDate(calendar.getTime());
            cookie.setDomain(request.getServerName());
            cookieStore.addCookie(cookie);
        }
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(clientConfig)
                .setDefaultCookieStore(cookieStore)
                .build();
    }

    private String performWfsRequest(String url) {
        String result = "";
        try (CloseableHttpClient client = buildHttpClient()) {
            HttpGet get = new HttpGet(url);
            try (CloseableHttpResponse httpResponse = client.execute(get)) {
                HttpEntity entity = httpResponse.getEntity();
                result = IOUtils.toString(entity.getContent(), Charset.forName("UTF-8"));
            }
        } catch (Exception e) {
            result = e.getMessage();
        }
        SupportedFormat outputFormat = outputFormatsDropDown.getModelObject();
        result = prettyPrintData(outputFormat, result);
        return result.trim();
    }

    WorkspaceInfo setWorkspaceValue(String workspaceValue) {
        List<WorkspaceInfo> workspaces =
                (List<WorkspaceInfo>) workspaceInfoDropDownChoice.getChoices();
        if (workspaces.isEmpty()) workspaces = getWorkspaces(getCatalog());
        return setWorkspaceValue(workspaceValue, workspaces);
    }

    WorkspaceInfo setWorkspaceValue(String workspaceValue, List<WorkspaceInfo> workspaces) {
        WorkspaceInfo result = null;
        if (workspaceInfoDropDownChoice != null && !workspaces.isEmpty()) {
            Optional<WorkspaceInfo> selectedWs =
                    workspaces.stream()
                            .filter(ws -> ws.getName().equals(workspaceValue))
                            .findFirst();
            if (selectedWs.isPresent()) {
                result = selectedWs.get();
                workspaceInfoDropDownChoice.setEnabled(false);
                workspaceInfoDropDownChoice.setDefaultModelObject(result);
            }
        } else if (workspaces.isEmpty()) {
            workspaceInfoDropDownChoice.setDefaultModelObject(result);
        }
        return result;
    }

    void setFeatureTypeInfoValue(String featureTypeInfoValue) {
        List<FeatureTypeInfo> featureTypeInfos =
                (List<FeatureTypeInfo>) featureTypesDD.getChoices();
        if (featureTypeInfos.isEmpty())
            featureTypeInfos =
                    getFeatureTypes(getCatalog(), workspaceInfoDropDownChoice.getModelObject());
        setFeatureTypeInfoValue(featureTypeInfoValue, featureTypeInfos);
    }

    void setOutputFormatsDropDownValues(String extension) {
        if (this.outputFormatsDropDown != null)
            this.outputFormatsDropDown.setChoices(SupportedFormat.getByExtension(extension));
    }

    void setFeatureTypeInfoValue(
            String featureTypeInfoValue, List<FeatureTypeInfo> featureTypeInfos) {
        if (featureTypesDD != null && !featureTypeInfos.isEmpty()) {
            Optional<FeatureTypeInfo> op =
                    featureTypeInfos.stream()
                            .filter(fti -> fti.getName().equals(featureTypeInfoValue))
                            .findFirst();

            if (op.isPresent()) {
                featureTypesDD.setEnabled(false);
                featureTypesDD.setDefaultModelObject(op.get());
            }
        } else if (featureTypeInfos.isEmpty()) {
            featureTypesDD.setDefaultModelObject(null);
        }
    }

    private String prettyPrintXML(String input) {
        Source xmlInput = new StreamSource(new StringReader(input));
        StringWriter stringWriter = new StringWriter();
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(xmlInput, new StreamResult(stringWriter));

            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String prettyPrintJson(String input) {
        try {
            ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
            Object json = objectMapper.readValue(input, Object.class);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String prettyPrintData(SupportedFormat outputFormat, String data) {
        String prettyPrint;
        String exceptionPrefix = "<ows:";
        if (outputFormat.equals(SupportedFormat.GML)
                || data.contains(exceptionPrefix)
                || outputFormat.equals(SupportedFormat.HTML)) prettyPrint = prettyPrintXML(data);
        else prettyPrint = prettyPrintJson(data);
        return prettyPrint;
    }

    public static class PreviewInfoModel implements Serializable {

        private WorkspaceInfo ws;

        private FeatureTypeInfo featureType;

        private SupportedFormat outputFormat;

        private String featureId;

        private String cqlFilter;

        public WorkspaceInfo getWs() {
            return ws;
        }

        public void setWs(WorkspaceInfo ws) {
            this.ws = ws;
        }

        public FeatureTypeInfo getFeatureType() {
            return featureType;
        }

        public void setFeatureType(FeatureTypeInfo fti) {
            this.featureType = fti;
        }

        public SupportedFormat getOutputFormat() {
            return outputFormat;
        }

        public void setOutputFormat(SupportedFormat outputFormat) {
            this.outputFormat = outputFormat;
        }

        public String getFeatureId() {
            return featureId;
        }

        public void setFeatureId(String featureId) {
            this.featureId = featureId;
        }

        public String getCqlFilter() {
            return cqlFilter;
        }

        public void setCqlFilter(String cqlFilter) {
            this.cqlFilter = cqlFilter;
        }
    }

    private void clearFeedbackMessages() {
        previewFeedback.getFeedbackMessages().clear();
    }
}
