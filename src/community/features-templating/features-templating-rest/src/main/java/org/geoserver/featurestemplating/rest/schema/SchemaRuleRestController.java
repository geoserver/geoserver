/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.rest.schema;

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
import org.geoserver.featurestemplating.configuration.schema.SchemaInfo;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfoDAO;
import org.geoserver.featurestemplating.configuration.schema.SchemaLayerConfig;
import org.geoserver.featurestemplating.configuration.schema.SchemaRule;
import org.geoserver.featurestemplating.configuration.schema.SchemaRuleService;
import org.geoserver.featurestemplating.rest.PatchMergeHandler;
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
public class SchemaRuleRestController extends AbstractCatalogController {

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.registerConverter(new SchemaRuleListConverter());
        xstream.alias("RulesList", SchemaRuleList.class);
        xstream.allowTypes(new Class[] {SchemaRuleList.class});
        // configure a local persister, avoiding problems of deserialization of request body if some
        // other module has a global persister aliasing with name "Rule"
        xstream.alias("Rule", SchemaRule.class);
        xstream.allowTypes(new Class[] {SchemaRuleList.class, SchemaRule.class});
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return SchemaRule.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Autowired
    public SchemaRuleRestController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
            value = "/workspaces/{workspace}/featuretypes/{featuretype}/schemarules",
            produces = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    public RestWrapper<SchemaRuleList> findAll(
            @PathVariable(name = "workspace") String workspace,
            @PathVariable(name = "featuretype") String featuretype) {
        FeatureTypeInfo info = checkFeatureType(workspace, featuretype);
        SchemaLayerConfig layerConfig = checkRules(info);
        return wrapObject(new SchemaRuleList(layerConfig.getSchemaRules()), SchemaRuleList.class);
    }

    @GetMapping(
            value = "/workspaces/{workspace}/featuretypes/{featuretype}/schemarules/{identifier}",
            produces = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    public RestWrapper<SchemaRule> findById(
            @PathVariable(name = "workspace") String workspace,
            @PathVariable(name = "featuretype") String featuretype,
            @PathVariable String identifier) {
        FeatureTypeInfo info = checkFeatureType(workspace, featuretype);
        SchemaLayerConfig layerConfig = checkRules(info);
        Optional<SchemaRule> rule = layerConfig.getSchemaRules().stream()
                .filter(r -> r.getRuleId().equals(identifier))
                .findFirst();
        if (!rule.isPresent()) {
            throw new RestException("Rule with id " + identifier + " not found", HttpStatus.NOT_FOUND);
        }
        return wrapObject(rule.get(), SchemaRule.class);
    }

    @PutMapping(
            value = "/workspaces/{workspace}/featuretypes/{featuretype}/schemarules/{identifier}",
            consumes = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    public ResponseEntity<String> putRule(
            @RequestBody SchemaRule rule,
            @PathVariable(name = "workspace") String workspace,
            @PathVariable(name = "featuretype") String featuretype,
            @PathVariable(name = "identifier") String identifier) {
        FeatureTypeInfo info = checkFeatureType(workspace, featuretype);
        checkRules(info);
        rule.setRuleId(identifier);
        validateRule(rule);
        SchemaRuleService service = new SchemaRuleService(info);
        checkRule(identifier, service);
        service.replaceRule(rule);
        return new ResponseEntity<>(rule.getRuleId(), HttpStatus.CREATED);
    }

    @PatchMapping(
            value = "/workspaces/{workspace}/featuretypes/{featuretype}/schemarules/{identifier}",
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
        SchemaRuleService service = new SchemaRuleService(info);
        checkRule(identifier, service);
        SchemaRule toPatch = service.getRule(identifier);
        try {
            String rule = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8.name());
            PatchMergeHandler<SchemaRule> patchHandler = new PatchMergeHandler<>(SchemaRule.class);
            toPatch = patchHandler.applyPatch(rule.trim(), toPatch, contentType);
            validateRule(toPatch);
            service.replaceRule(toPatch);
        } catch (Exception e) {
            throw new RestException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(toPatch.getRuleId(), HttpStatus.OK);
    }

    @PostMapping(
            value = "/workspaces/{workspace}/featuretypes/{featuretype}/schemarules",
            consumes = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    public ResponseEntity<String> postRule(
            @RequestBody SchemaRule rule,
            @PathVariable(name = "workspace") String workspace,
            @PathVariable(name = "featuretype") String featuretype,
            UriComponentsBuilder builder) {
        rule = new SchemaRule(rule);
        FeatureTypeInfo info = checkFeatureType(workspace, featuretype);
        validateRule(rule);
        SchemaRuleService service = new SchemaRuleService(info);
        service.saveRule(rule);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(getUri(rule.getRuleId(), workspace, featuretype, builder));
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(rule.getRuleId(), headers, HttpStatus.CREATED);
    }

    @DeleteMapping(value = "/workspaces/{workspace}/featuretypes/{featuretype}/schemarules/{identifier}")
    public ResponseEntity<String> deleteRule(
            @PathVariable(name = "workspace") String workspace,
            @PathVariable(name = "featuretype") String featuretype,
            @PathVariable(name = "identifier") String identifier) {
        FeatureTypeInfo info = checkFeatureType(workspace, featuretype);
        SchemaRuleService service = new SchemaRuleService(info);
        boolean removed = service.removeRule(identifier);
        if (!removed) {
            throw new RestException("Rule with id " + identifier + "not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(identifier, HttpStatus.NO_CONTENT);
    }

    private void validateRule(SchemaRule rule) {
        if (rule.getSchemaName().equals(null) && rule.getSchemaIdentifier().equals(null)) {
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
        SchemaInfoDAO dao = SchemaInfoDAO.get();
        SchemaInfo info;
        if (rule.getSchemaName() != null) {
            info = dao.findByFullName(rule.getSchemaName());
        } else {
            info = dao.findById(rule.getSchemaIdentifier());
        }
        if (info == null) {
            throw new RestException(
                    "The template with name " + rule.getSchemaName() + " does not exist", HttpStatus.BAD_REQUEST);
        }
        rule.setSchemaIdentifier(info.getIdentifier());
        rule.setSchemaName(info.getFullName());
    }

    private URI getUri(String ruleId, String workspace, String featureType, UriComponentsBuilder builder) {
        builder = builder.cloneBuilder();
        UriComponents uriComponents = builder.path(
                        "/workspaces/{workspace}/featuretypes/{featuretype}/schemarules/{id}")
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

    private SchemaLayerConfig checkRules(FeatureTypeInfo info) {
        SchemaLayerConfig layerConfig = info.getMetadata().get(SchemaLayerConfig.METADATA_KEY, SchemaLayerConfig.class);
        if (layerConfig == null)
            throw new ResourceNotFoundException("There are no rules defined for Feature Type " + info.getName());
        return layerConfig;
    }

    private SchemaRule checkRule(String ruleId, SchemaRuleService service) {
        SchemaRule rule = service.getRule(ruleId);
        if (rule == null) throw new ResourceNotFoundException("No rule with specified id " + ruleId);
        return rule;
    }

    class SchemaRuleListConverter implements Converter {

        @Override
        public void marshal(
                Object o, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {
            if (o instanceof SchemaRuleList) {
                SchemaRuleList list = (SchemaRuleList) o;
                for (SchemaRule rule : list.getRules()) {
                    hierarchicalStreamWriter.startNode("Rules");
                    hierarchicalStreamWriter.startNode("ruleId");
                    hierarchicalStreamWriter.setValue(rule.getRuleId());
                    hierarchicalStreamWriter.endNode();

                    hierarchicalStreamWriter.startNode("priority");
                    hierarchicalStreamWriter.setValue(rule.getPriority().toString());
                    hierarchicalStreamWriter.endNode();

                    hierarchicalStreamWriter.startNode("schemaName");
                    hierarchicalStreamWriter.setValue(rule.getSchemaName());
                    hierarchicalStreamWriter.endNode();

                    hierarchicalStreamWriter.startNode("schemaIdentifier");
                    hierarchicalStreamWriter.setValue(rule.getSchemaIdentifier());
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
            return aClass.isAssignableFrom(SchemaRuleList.class);
        }
    }

    class SchemaRuleList {
        Collection<SchemaRule> rules;

        SchemaRuleList(Collection<SchemaRule> rules) {
            this.rules = rules;
        }

        Collection<SchemaRule> getRules() {
            return rules;
        }
    }
}
