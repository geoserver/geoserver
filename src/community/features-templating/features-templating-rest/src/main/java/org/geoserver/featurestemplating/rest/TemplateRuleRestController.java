/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.rest;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.TemplateLayerConfig;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.featurestemplating.configuration.TemplateRuleService;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.catalog.AbstractCatalogController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.feature.NameImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class TemplateRuleRestController extends AbstractCatalogController {

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.registerConverter(new TemplateRuleListConverter());
        xstream.alias("RulesList", TemplateRuleList.class);
        xstream.allowTypes(new Class[] {TemplateRuleList.class});
        // configure a local persister, avoiding problems of deserialization of request body if some
        // other module has a global persister aliasing with name "Rule"
        xstream.alias("Rule", TemplateRule.class);
        xstream.allowTypes(new Class[] {TemplateRuleList.class, TemplateRule.class});
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return TemplateRule.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Autowired
    public TemplateRuleRestController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
            value = "/workspaces/{workspace}/featuretypes/{featuretype}/templaterules",
            produces = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    public RestWrapper<TemplateRuleList> findAll(
            @PathVariable(name = "workspace") String workspace,
            @PathVariable(name = "featuretype") String featuretype) {
        FeatureTypeInfo info = checkFeatureType(workspace, featuretype);
        TemplateLayerConfig layerConfig = checkRules(info);
        return wrapObject(new TemplateRuleList(layerConfig.getTemplateRules()), TemplateRuleList.class);
    }

    @GetMapping(
            value = "/workspaces/{workspace}/featuretypes/{featuretype}/templaterules/{identifier}",
            produces = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    public RestWrapper<TemplateRule> findById(
            @PathVariable(name = "workspace") String workspace,
            @PathVariable(name = "featuretype") String featuretype,
            @PathVariable String identifier) {
        FeatureTypeInfo info = checkFeatureType(workspace, featuretype);
        TemplateLayerConfig layerConfig = checkRules(info);
        Optional<TemplateRule> rule = layerConfig.getTemplateRules().stream()
                .filter(r -> r.getRuleId().equals(identifier))
                .findFirst();
        if (rule.isEmpty()) {
            throw new RestException("Rule with id " + identifier + " not found", HttpStatus.NOT_FOUND);
        }
        return wrapObject(rule.get(), TemplateRule.class);
    }

    @PutMapping(
            value = "/workspaces/{workspace}/featuretypes/{featuretype}/templaterules/{identifier}",
            consumes = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    public ResponseEntity<String> putRule(
            @RequestBody TemplateRule rule,
            @PathVariable(name = "workspace") String workspace,
            @PathVariable(name = "featuretype") String featuretype,
            @PathVariable(name = "identifier") String identifier) {
        FeatureTypeInfo info = checkFeatureType(workspace, featuretype);
        checkRules(info);
        rule.setRuleId(identifier);
        validateRule(rule);
        TemplateRuleService service = new TemplateRuleService(info);
        checkRule(identifier, service);
        service.replaceRule(rule);
        return new ResponseEntity<>(rule.getRuleId(), HttpStatus.CREATED);
    }

    @PatchMapping(
            value = "/workspaces/{workspace}/featuretypes/{featuretype}/templaterules/{identifier}",
            consumes = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    public ResponseEntity<String> patchRule(
            HttpServletRequest request,
            @RequestHeader("Content-Type") String contentType,
            @PathVariable(name = "workspace") String workspace,
            @PathVariable(name = "featuretype") String featuretype,
            @PathVariable(name = "identifier") String identifier) {
        FeatureTypeInfo info = checkFeatureType(workspace, featuretype);
        checkRules(info);
        TemplateRuleService service = new TemplateRuleService(info);
        checkRule(identifier, service);
        TemplateRule toPatch = service.getRule(identifier);
        try {
            String rule = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8.name());
            PatchMergeHandler<TemplateRule> patchHandler = new PatchMergeHandler<>(TemplateRule.class);
            toPatch = patchHandler.applyPatch(rule.trim(), toPatch, contentType);
            validateRule(toPatch);
            service.replaceRule(toPatch);
        } catch (Exception e) {
            throw new RestException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(toPatch.getRuleId(), HttpStatus.OK);
    }

    @PostMapping(
            value = "/workspaces/{workspace}/featuretypes/{featuretype}/templaterules",
            consumes = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    public ResponseEntity<String> postRule(
            @RequestBody TemplateRule rule,
            @PathVariable(name = "workspace") String workspace,
            @PathVariable(name = "featuretype") String featuretype,
            UriComponentsBuilder builder) {
        rule = new TemplateRule(rule);
        FeatureTypeInfo info = checkFeatureType(workspace, featuretype);
        validateRule(rule);
        TemplateRuleService service = new TemplateRuleService(info);
        service.saveRule(rule);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(getUri(rule.getRuleId(), workspace, featuretype, builder));
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(rule.getRuleId(), headers, HttpStatus.CREATED);
    }

    @DeleteMapping(value = "/workspaces/{workspace}/featuretypes/{featuretype}/templaterules/{identifier}")
    public ResponseEntity<String> deleteRule(
            @PathVariable(name = "workspace") String workspace,
            @PathVariable(name = "featuretype") String featuretype,
            @PathVariable(name = "identifier") String identifier) {
        FeatureTypeInfo info = checkFeatureType(workspace, featuretype);
        TemplateRuleService service = new TemplateRuleService(info);
        boolean removed = service.removeRule(identifier);
        if (!removed) {
            throw new RestException("Rule with id " + identifier + "not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(identifier, HttpStatus.NO_CONTENT);
    }

    private void validateRule(TemplateRule rule) {
        if (rule.getTemplateName().equals(null) && rule.getTemplateIdentifier().equals(null)) {
            throw new RestException(
                    "Either templateName or templateIdentifier needs to be specified", HttpStatus.BAD_REQUEST);
        } else if (rule.getOutputFormat() == null) {
            String cqlFilter = rule.getCqlFilter();
            boolean noMimeType = cqlFilter == null || !cqlFilter.contains("mimeType");
            if (noMimeType)
                throw new RestException(
                        "Template Rule must have an output format " + "or a CQLFilter using the mimeType function",
                        HttpStatus.BAD_REQUEST);
        }
        TemplateInfoDAO dao = TemplateInfoDAO.get();
        TemplateInfo info;
        if (rule.getTemplateName() != null) {
            info = dao.findByFullName(rule.getTemplateName());
        } else {
            info = dao.findById(rule.getTemplateIdentifier());
        }
        if (info == null) {
            throw new RestException(
                    "The template with name " + rule.getTemplateName() + " does not exist", HttpStatus.BAD_REQUEST);
        }
        rule.setTemplateIdentifier(info.getIdentifier());
        rule.setTemplateName(info.getFullName());
    }

    private URI getUri(String ruleId, String workspace, String featureType, UriComponentsBuilder builder) {
        builder = builder.cloneBuilder();
        UriComponents uriComponents = builder.path(
                        "/workspaces/{workspace}/featuretypes/{featuretype}/templaterules/{id}")
                .buildAndExpand(workspace, featureType, ruleId);
        return uriComponents.toUri();
    }

    private FeatureTypeInfo checkFeatureType(String workspaceName, String featureTypeName) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(new NameImpl(workspaceName, featureTypeName));
        if (featureType == null) {
            throw new ResourceNotFoundException("Feature Type " + featureTypeName + " not found");
        }
        return featureType;
    }

    private TemplateLayerConfig checkRules(FeatureTypeInfo info) {
        TemplateLayerConfig layerConfig =
                info.getMetadata().get(TemplateLayerConfig.METADATA_KEY, TemplateLayerConfig.class);
        if (layerConfig == null)
            throw new ResourceNotFoundException("There are no rules defined for Feature Type " + info.getName());
        return layerConfig;
    }

    private TemplateRule checkRule(String ruleId, TemplateRuleService service) {
        TemplateRule rule = service.getRule(ruleId);
        if (rule == null) throw new ResourceNotFoundException("No rule with specified id " + ruleId);
        return rule;
    }

    class TemplateRuleListConverter implements Converter {

        @Override
        public void marshal(
                Object o, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {
            if (o instanceof TemplateRuleList list) {
                for (TemplateRule rule : list.getRules()) {
                    hierarchicalStreamWriter.startNode("Rules");
                    hierarchicalStreamWriter.startNode("ruleId");
                    hierarchicalStreamWriter.setValue(rule.getRuleId());
                    hierarchicalStreamWriter.endNode();

                    hierarchicalStreamWriter.startNode("priority");
                    hierarchicalStreamWriter.setValue(rule.getPriority().toString());
                    hierarchicalStreamWriter.endNode();

                    hierarchicalStreamWriter.startNode("templateName");
                    hierarchicalStreamWriter.setValue(rule.getTemplateName());
                    hierarchicalStreamWriter.endNode();

                    hierarchicalStreamWriter.startNode("templateIdentifier");
                    hierarchicalStreamWriter.setValue(rule.getTemplateIdentifier());
                    hierarchicalStreamWriter.endNode();

                    if (rule.getOutputFormat() != null) {
                        hierarchicalStreamWriter.startNode("outputFormat");
                        hierarchicalStreamWriter.setValue(rule.getOutputFormat().name());
                        hierarchicalStreamWriter.endNode();
                    }

                    if (rule.getCqlFilter() != null) {
                        hierarchicalStreamWriter.startNode("cqlFilter");
                        hierarchicalStreamWriter.setValue(rule.getCqlFilter());
                        hierarchicalStreamWriter.endNode();
                    }
                    hierarchicalStreamWriter.endNode();
                }
            }
        }

        @Override
        public Object unmarshal(
                HierarchicalStreamReader hierarchicalStreamReader, UnmarshallingContext unmarshallingContext) {
            return null;
        }

        @Override
        public boolean canConvert(Class aClass) {
            return aClass.isAssignableFrom(TemplateRuleList.class);
        }
    }

    class TemplateRuleList {
        Collection<TemplateRule> rules;

        TemplateRuleList(Collection<TemplateRule> rules) {
            this.rules = rules;
        }

        Collection<TemplateRule> getRules() {
            return rules;
        }
    }
}
