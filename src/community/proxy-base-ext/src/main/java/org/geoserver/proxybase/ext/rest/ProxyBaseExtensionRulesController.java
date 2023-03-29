/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext.rest;

import com.thoughtworks.xstream.XStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.proxybase.ext.ProxyBaseExtensionRuleBuilder;
import org.geoserver.proxybase.ext.config.ProxyBaseExtRuleConverter;
import org.geoserver.proxybase.ext.config.ProxyBaseExtRuleDAO;
import org.geoserver.proxybase.ext.config.ProxyBaseExtensionRule;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.catalog.AbstractCatalogController;
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
        path = ProxyBaseExtensionRulesController.RULES_ROOT,
        produces = {
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE
        })
/** Controller for the {@link ProxyBaseExtensionRule}s. */
public class ProxyBaseExtensionRulesController extends AbstractCatalogController {
    static final String RULES_ROOT = RestBaseController.ROOT_PATH + "/proxy-base-ext/rules";

    public ProxyBaseExtensionRulesController(Catalog catalog) {
        super(catalog);
    }

    /**
     * Returns the list of {@link ProxyBaseExtensionRule}s.
     *
     * @return the list of {@link ProxyBaseExtensionRule}s.
     */
    @GetMapping
    public RestListWrapper<ProxyBaseExtensionRule> getRules() {
        return new RestListWrapper<>(
                ProxyBaseExtRuleDAO.getRules(), ProxyBaseExtensionRule.class, this, "id", null);
    }

    /**
     * Returns the {@link ProxyBaseExtensionRule} with the given id.
     *
     * @param id the id of the {@link ProxyBaseExtensionRule} to return.
     * @return the {@link ProxyBaseExtensionRule} with the given id.
     */
    @GetMapping(path = "{id}")
    public RestWrapper<ProxyBaseExtensionRule> getRule(@PathVariable String id) {
        ProxyBaseExtensionRule result =
                ProxyBaseExtRuleDAO.getRules().stream()
                        .filter(r -> id.equals(r.getId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new RestException(
                                                "Rule with id " + id + " not found",
                                                HttpStatus.NOT_FOUND));
        return new RestWrapperAdapter<>(result, ProxyBaseExtensionRule.class, this);
    }

    /**
     * Creates a new {@link ProxyBaseExtensionRule}.
     *
     * @param newValue the new {@link ProxyBaseExtensionRule} to create.
     * @return the location of the created {@link ProxyBaseExtensionRule}.
     * @throws URISyntaxException if an issue occurs while creating the rule
     */
    @PostMapping(
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.TEXT_XML_VALUE
            })
    public ResponseEntity<String> postRule(@RequestBody ProxyBaseExtensionRule newValue)
            throws URISyntaxException {
        // force a new id like the UI would, using a random UUID
        String newId = UUID.randomUUID().toString();
        newValue = new ProxyBaseExtensionRuleBuilder().copy(newValue).withId(newId).build();
        ProxyBaseExtRuleDAO.saveOrUpdateProxyBaseExtRule(newValue);

        // return the location of the created echo parameter
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(new URI(RequestInfo.get().pageURI(newValue.getId())));
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(newValue.getId(), headers, HttpStatus.CREATED);
    }

    /**
     * Deletes the {@link ProxyBaseExtensionRule} with the given id.
     *
     * @param id the id of the {@link ProxyBaseExtensionRule} to delete.
     */
    @DeleteMapping(path = "{id}")
    public void deleteRule(@PathVariable String id) {
        // just want the 404 side effect here
        getRule(id);
        // go and nuke now
        ProxyBaseExtRuleDAO.deleteProxyBaseExtRules(id);
    }

    /**
     * Updates the {@link ProxyBaseExtensionRule} with the given id.
     *
     * @param newValue the new {@link ProxyBaseExtensionRule} to use in the update.
     * @param id the id of the {@link ProxyBaseExtensionRule} to update.
     */
    @PutMapping(
            path = "{id}",
            consumes = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.TEXT_XML_VALUE
            })
    public void putRule(@RequestBody ProxyBaseExtensionRule newValue, @PathVariable String id) {
        // just want the 404 side effect here
        getRule(id);
        // validate consistency
        if (newValue.getId() == null) {
            newValue = new ProxyBaseExtensionRuleBuilder().copy(newValue).withId(id).build();
        } else if (!newValue.getId().equals(id)) {
            throw new RestException(
                    "Incosistent identifier, body uses "
                            + newValue.getId()
                            + " but REST API path has "
                            + id,
                    HttpStatus.BAD_REQUEST);
        }
        // overwrite
        ProxyBaseExtRuleDAO.saveOrUpdateProxyBaseExtRule(newValue);
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        super.configurePersister(persister, converter);

        XStream xStream = persister.getXStream();
        xStream.alias("ProxyBaseExtensionRule", ProxyBaseExtensionRule.class);
        xStream.allowTypeHierarchy(ProxyBaseExtensionRule.class);
        // use the existing XML storage format for the ProxyBaseExtension
        if (converter instanceof XStreamXMLMessageConverter) {
            xStream.registerConverter(new ProxyBaseExtRuleConverter());
        }
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return ProxyBaseExtensionRule.class.isAssignableFrom(methodParameter.getParameterType());
    }
}
