/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.rest;

import com.thoughtworks.xstream.XStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.params.extractor.Rule;
import org.geoserver.params.extractor.RuleBuilder;
import org.geoserver.params.extractor.RuleConverter;
import org.geoserver.params.extractor.RulesDao;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.converters.XStreamXMLMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.rest.wrapper.RestWrapperAdapter;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping(
    path = RulesController.RULES_ROOT,
    produces = {
        MediaType.APPLICATION_XML_VALUE,
        MediaType.APPLICATION_JSON_VALUE,
        MediaTypeExtensions.TEXT_JSON_VALUE
    }
)
public class RulesController extends RestBaseController {
    static final String RULES_ROOT = RestBaseController.ROOT_PATH + "/params-extractor/rules";

    @GetMapping
    public RestListWrapper<Rule> getRules() {
        return new RestListWrapper<>(RulesDao.getRules(), Rule.class, this, "id", null);
    }

    @PostMapping(
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public ResponseEntity<String> postRule(@RequestBody Rule newValue) throws URISyntaxException {
        // force a new id like the UI would, using a random UUID
        newValue = new RuleBuilder().copy(newValue).withId(UUID.randomUUID().toString()).build();
        RulesDao.saveOrUpdateRule(newValue);

        // return the location of the created echo parameter
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI(RequestInfo.get().pageURI(newValue.getId())));
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(newValue.getId(), headers, HttpStatus.CREATED);
    }

    @GetMapping(path = "{id}")
    public RestWrapper<Rule> getRule(@PathVariable String id) {
        Rule result =
                RulesDao.getRules()
                        .stream()
                        .filter(r -> id.equals(r.getId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new RestException(
                                                "Rule with id " + id + " not found",
                                                HttpStatus.NOT_FOUND));
        return new RestWrapperAdapter(result, Rule.class, this);
    }

    @DeleteMapping(path = "{id}")
    public void deleteRule(@PathVariable String id) {
        // just want the 404 side effect here
        getRule(id);
        // go and nuke now
        RulesDao.deleteRules(id);
    }

    @PutMapping(
        path = "{id}",
        consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_XML_VALUE
        }
    )
    public void putEchoParameter(@RequestBody Rule newValue, @PathVariable String id) {
        // just want the 404 side effect here
        getRule(id);
        // validate consistency
        if (newValue.getId() == null) {
            newValue = new RuleBuilder().copy(newValue).withId(id).build();
        } else if (!newValue.getId().equals(id)) {
            throw new RestException(
                    "Incosistent identifier, body uses "
                            + newValue.getId()
                            + " but REST API path has "
                            + id,
                    HttpStatus.BAD_REQUEST);
        }
        // overwrite
        RulesDao.saveOrUpdateRule(newValue);
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        super.configurePersister(persister, converter);

        XStream xStream = persister.getXStream();
        xStream.alias("Rule", Rule.class);
        xStream.allowTypeHierarchy(Rule.class);
        // use the existing XML storage format for the EchoParameter
        if (converter instanceof XStreamXMLMessageConverter) {
            xStream.registerConverter(new RuleConverter());
        }
        xStream.omitField(Rule.class, "matchPattern");
        xStream.omitField(Rule.class, "activationPattern");
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return Rule.class.isAssignableFrom(methodParameter.getParameterType());
    }
}
