/* (c) 2015-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.rest;

import com.thoughtworks.xstream.XStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geofence.core.model.Rule;
import org.geofence.core.services.RuleAdminService;
import org.geofence.core.services.dto.RuleFilter;
import org.geofence.core.services.dto.RuleFilter.IdNameFilter;
import org.geofence.core.services.dto.RuleFilter.SpecialFilterType;
import org.geofence.core.services.dto.RuleFilter.TextFilter;
import org.geofence.core.services.dto.ShortRule;
import org.geofence.core.services.exception.BadRequestServiceEx;
import org.geofence.core.services.exception.NotFoundServiceEx;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.geofence.server.rest.xml.JaxbRule;
import org.geoserver.geofence.server.rest.xml.JaxbRuleList;
import org.geoserver.geofence.server.rest.xml.MultiPolygonAdapter;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.SequentialExecutionController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/geofence")
public class RulesRestController extends RestBaseController implements SequentialExecutionController {

    private static final Logger LOGGER = Logging.getLogger(RulesRestController.class);

    // Resolved per request, not injected, so this controller doesn't force the lazy engine to boot at startup.
    private RuleAdminService adminService() {
        return (RuleAdminService) GeoServerExtensions.bean("ruleAdminService");
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xs = persister.getXStream();
        // configure a local persister, avoiding problems of deserialization of request body if some
        // other module has a global persister aliasing with name "Rule"
        xs.alias("Rule", JaxbRule.class);
        xs.allowTypes(new Class[] {JaxbRule.class, MultiPolygonAdapter.class});
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return JaxbRule.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @ExceptionHandler(NotFoundServiceEx.class)
    public void ruleNotFound(NotFoundServiceEx exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(404, exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public void rule(DuplicateKeyException exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(409, exception.getMessage());
    }

    @ExceptionHandler(BadRequestServiceEx.class)
    public void badRequest(BadRequestServiceEx exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void messageNotReadableException(
            HttpMessageNotReadableException exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @RequestMapping(
            value = "/rules",
            method = RequestMethod.GET,
            produces = {"application/xml", "application/json"})
    public JaxbRuleList get(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "entries", required = false) Integer entries,
            @RequestParam(value = "full", required = false, defaultValue = "false") boolean full,
            @RequestParam(value = "userName", required = false) String userName,
            @Deprecated @RequestParam(value = "userAny", required = false) Boolean userAny,
            @RequestParam(value = "userDefault", required = false) Boolean userDefault,
            @RequestParam(value = "roleName", required = false) String roleName,
            @Deprecated @RequestParam(value = "roleAny", required = false) Boolean roleAny,
            @RequestParam(value = "roleDefault", required = false) Boolean roleDefault,
            @Deprecated @RequestParam(value = "instanceId", required = false) Long instanceId,
            @RequestParam(value = "instanceName", required = false) String instanceName,
            @Deprecated @RequestParam(value = "instanceAny", required = false) Boolean instanceAny,
            @RequestParam(value = "instanceDefault", required = false) Boolean instanceDefault,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @Deprecated @RequestParam(value = "ipAddressAny", required = false) Boolean ipAddressAny,
            @RequestParam(value = "ipAddressDefault", required = false) Boolean ipAddressDefault,
            @RequestParam(value = "date", required = false) String date,
            @Deprecated @RequestParam(value = "dateAny", required = false) Boolean dateAny,
            @RequestParam(value = "dateDefault", required = false) Boolean dateDefault,
            @RequestParam(value = "service", required = false) String serviceName,
            @Deprecated @RequestParam(value = "serviceAny", required = false) Boolean serviceAny,
            @RequestParam(value = "serviceDefault", required = false) Boolean serviceDefault,
            @RequestParam(value = "request", required = false) String requestName,
            @Deprecated @RequestParam(value = "requestAny", required = false) Boolean requestAny,
            @RequestParam(value = "requestDefault", required = false) Boolean requestDefault,
            @RequestParam(value = "subfield", required = false) String subfield,
            @Deprecated @RequestParam(value = "subfieldAny", required = false) Boolean subfieldAny,
            @RequestParam(value = "subfieldDefault", required = false) Boolean subfieldDefault,
            @RequestParam(value = "workspace", required = false) String workspace,
            @Deprecated @RequestParam(value = "workspaceAny", required = false) Boolean workspaceAny,
            @RequestParam(value = "workspaceDefault", required = false) Boolean workspaceDefault,
            @RequestParam(value = "layer", required = false) String layer,
            @Deprecated @RequestParam(value = "layerAny", required = false) Boolean layerAny,
            @RequestParam(value = "layerDefault", required = false) Boolean layerDefault) {
        RuleFilter filter = buildFilter(
                userName,
                userAny,
                userDefault,
                roleName,
                roleAny,
                roleDefault,
                instanceId,
                instanceName,
                instanceAny,
                instanceDefault,
                ipAddress,
                ipAddressAny,
                ipAddressDefault,
                date,
                dateAny,
                dateDefault,
                serviceName,
                serviceAny,
                serviceDefault,
                requestName,
                requestAny,
                requestDefault,
                subfield,
                subfieldAny,
                subfieldDefault,
                workspace,
                workspaceAny,
                workspaceDefault,
                layer,
                layerAny,
                layerDefault);

        return new JaxbRuleList(adminService().getListFull(filter, page, entries));
    }

    @RequestMapping(
            value = "/rules/id/{id}",
            method = RequestMethod.GET,
            produces = {"application/xml", "application/json"})
    public JaxbRule get(@PathVariable("id") Long id) {
        return new JaxbRule(adminService().get(id));
    }

    @RequestMapping(
            value = "/rules/count",
            method = RequestMethod.GET,
            produces = {"application/xml", "application/json"})
    public JaxbRuleList count(
            @RequestParam(value = "userName", required = false) String userName,
            @Deprecated @RequestParam(value = "userAny", required = false) Boolean userAny,
            @RequestParam(value = "userDefault", required = false) Boolean userDefault,
            @RequestParam(value = "roleName", required = false) String roleName,
            @Deprecated @RequestParam(value = "roleAny", required = false) Boolean roleAny,
            @RequestParam(value = "roleDefault", required = false) Boolean roleDefault,
            @Deprecated @RequestParam(value = "instanceId", required = false) Long instanceId,
            @RequestParam(value = "instanceName", required = false) String instanceName,
            @Deprecated @RequestParam(value = "instanceAny", required = false) Boolean instanceAny,
            @RequestParam(value = "instanceDefault", required = false) Boolean instanceDefault,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @Deprecated @RequestParam(value = "ipAddressAny", required = false) Boolean ipAddressAny,
            @RequestParam(value = "ipAddressDefault", required = false) Boolean ipAddressDefault,
            @RequestParam(value = "date", required = false) String date,
            @Deprecated @RequestParam(value = "dateAny", required = false) Boolean dateAny,
            @RequestParam(value = "dateDefault", required = false) Boolean dateDefault,
            @RequestParam(value = "service", required = false) String serviceName,
            @Deprecated @RequestParam(value = "serviceAny", required = false) Boolean serviceAny,
            @RequestParam(value = "serviceDefault", required = false) Boolean serviceDefault,
            @RequestParam(value = "request", required = false) String requestName,
            @Deprecated @RequestParam(value = "requestAny", required = false) Boolean requestAny,
            @RequestParam(value = "requestDefault", required = false) Boolean requestDefault,
            @RequestParam(value = "subfield", required = false) String subfield,
            @Deprecated @RequestParam(value = "subfieldAny", required = false) Boolean subfieldAny,
            @RequestParam(value = "subfieldDefault", required = false) Boolean subfieldDefault,
            @RequestParam(value = "workspace", required = false) String workspace,
            @Deprecated @RequestParam(value = "workspaceAny", required = false) Boolean workspaceAny,
            @RequestParam(value = "workspaceDefault", required = false) Boolean workspaceDefault,
            @RequestParam(value = "layer", required = false) String layer,
            @Deprecated @RequestParam(value = "layerAny", required = false) Boolean layerAny,
            @RequestParam(value = "layerDefault", required = false) Boolean layerDefault) {
        RuleFilter filter = buildFilter(
                userName,
                userAny,
                userDefault,
                roleName,
                roleAny,
                roleDefault,
                instanceId,
                instanceName,
                instanceAny,
                instanceDefault,
                ipAddress,
                ipAddressAny,
                ipAddressDefault,
                date,
                dateAny,
                dateDefault,
                serviceName,
                serviceAny,
                serviceDefault,
                requestName,
                requestAny,
                requestDefault,
                subfield,
                subfieldAny,
                subfieldDefault,
                workspace,
                workspaceAny,
                workspaceDefault,
                layer,
                layerAny,
                layerDefault);

        return new JaxbRuleList(adminService().count(filter));
    }

    @RequestMapping(
            value = "/rules",
            method = RequestMethod.POST,
            consumes = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            },
            produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public String insert(@RequestBody(required = true) JaxbRule rule) {
        long priority = rule.getPriority() == null ? 0 : rule.getPriority();
        if (adminService().getRuleByPriority(priority) != null) {
            adminService().shift(priority, 1);
        }

        Long id = adminService().insert(rule.toRule());

        if (rule.getLimits() != null && rule.getAccess().equals("LIMIT")) {
            adminService().setLimits(id, rule.getLimits().toRuleLimits(null));
        }
        if (rule.getLayerDetails() != null && !rule.getAccess().equals("LIMIT")) {
            adminService().setDetails(id, rule.getLayerDetails().toLayerDetails(null));
        }

        return String.valueOf(id);
    }

    @RequestMapping(value = "/rules/id/{id}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.OK) void update(@PathVariable("id") Long id, @RequestBody JaxbRule rule) {
        if (rule.getPriority() != null) {
            ShortRule priorityRule = adminService().getRuleByPriority(rule.getPriority());
            if (priorityRule != null && !Objects.equals(priorityRule.getId(), id)) {
                adminService().shift(rule.getPriority(), 1);
            }
        }
        Rule theRule = adminService().get(id);
        adminService().update(rule.toRule(theRule));
        if (rule.getLimits() != null) {
            adminService().setLimits(id, rule.getLimits().toRuleLimits(theRule.getRuleLimits()));
        }
        if (rule.getLayerDetails() != null) {
            adminService().setDetails(id, rule.getLayerDetails().toLayerDetails(theRule.getLayerDetails()));
        }
    }

    @RequestMapping(value = "/rules/id/{id}", method = RequestMethod.PUT)
    public @ResponseStatus(HttpStatus.OK) void clearAndUpdate(@PathVariable("id") Long id, @RequestBody JaxbRule rule) {
        if (rule.getPriority() != null) {
            ShortRule priorityRule = adminService().getRuleByPriority(rule.getPriority());
            if (priorityRule != null && !Objects.equals(priorityRule.getId(), id)) {
                adminService().shift(rule.getPriority(), 1);
            }
        }
        Rule theRule = new Rule();
        theRule.setId(id);
        adminService().update(rule.toRule(theRule));
        if (rule.getLimits() != null) {
            adminService().setLimits(id, rule.getLimits().toRuleLimits(null));
        } else {
            adminService().setLimits(id, null);
        }
        if (rule.getLayerDetails() != null) {
            adminService().setDetails(id, rule.getLayerDetails().toLayerDetails(null));
        } else {
            adminService().setDetails(id, null);
        }
    }

    @RequestMapping(value = "/rules/id/{id}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void delete(@PathVariable("id") Long id) {
        adminService().delete(id);
    }

    protected RuleFilter buildFilter(
            String userName,
            Boolean userAny,
            Boolean userDefault,
            String roleName,
            Boolean roleAny,
            Boolean roleDefault,
            Long instanceId,
            String instanceName,
            Boolean instanceAny,
            Boolean instanceDefault,
            String ipAddress,
            Boolean ipAddressAny,
            Boolean ipAddressDefault,
            String date,
            Boolean dateAny,
            Boolean dateDefault,
            String serviceName,
            Boolean serviceAny,
            Boolean serviceDefault,
            String requestName,
            Boolean requestAny,
            Boolean requestDefault,
            String subfield,
            Boolean subfieldAny,
            Boolean subfieldDefault,
            String workspace,
            Boolean workspaceAny,
            Boolean workspaceDefault,
            String layer,
            Boolean layerAny,
            Boolean layerDefault) {

        RuleFilter filter = new RuleFilter(SpecialFilterType.ANY, true);

        setFilter(filter.getUser(), userName, resolveIncludeDefault(userAny, userDefault));
        setFilter(filter.getRole(), roleName, resolveIncludeDefault(roleAny, roleDefault));
        warnIfDeprecatedInstanceIdUsed(instanceId);
        setFilter(filter.getInstance(), instanceId, instanceName, resolveIncludeDefault(instanceAny, instanceDefault));
        setFilter(filter.getSourceAddress(), ipAddress, resolveIncludeDefault(ipAddressAny, ipAddressDefault));
        setFilter(filter.getDate(), date, resolveIncludeDefault(dateAny, dateDefault));
        setFilter(filter.getService(), serviceName, resolveIncludeDefault(serviceAny, serviceDefault));
        setFilter(filter.getRequest(), requestName, resolveIncludeDefault(requestAny, requestDefault));
        setFilter(filter.getSubfield(), subfield, resolveIncludeDefault(subfieldAny, subfieldDefault));
        setFilter(filter.getWorkspace(), workspace, resolveIncludeDefault(workspaceAny, workspaceDefault));
        setFilter(filter.getLayer(), layer, resolveIncludeDefault(layerAny, layerDefault));
        return filter;
    }

    /** The new {@code *Default} parameter wins when both it and the deprecated {@code *Any} one are set. */
    private static Boolean resolveIncludeDefault(Boolean deprecatedAny, Boolean byDefault) {
        return byDefault != null ? byDefault : deprecatedAny;
    }

    /** {@code instanceId} is deprecated in favor of {@code instanceName}; log when a client still sends it. */
    private void warnIfDeprecatedInstanceIdUsed(Long instanceId) {
        if (instanceId != null) {
            LOGGER.log(
                    Level.WARNING,
                    "Received deprecated GeoFence rule filter parameter instanceId; use instanceName instead.");
        }
    }

    private void setFilter(IdNameFilter filter, Long id, String name, Boolean includeDefault) {

        if (id != null && name != null) {
            throw new IllegalArgumentException("Id and name can't be both defined (id:" + id + " name:" + name + ")");
        }

        if (id != null) {
            filter.setId(id);
            if (includeDefault != null) {
                filter.setIncludeDefault(includeDefault);
            }
        } else if (name != null) {
            filter.setName(name);
            if (includeDefault != null) {
                filter.setIncludeDefault(includeDefault);
            }
        } else {
            if (includeDefault != null && includeDefault) {
                filter.setType(SpecialFilterType.DEFAULT);
            } else {
                filter.setType(SpecialFilterType.ANY);
            }
        }
    }

    private void setFilter(TextFilter filter, String name, Boolean includeDefault) {

        if (name != null) {
            filter.setText(name);
            if (includeDefault != null) {
                filter.setIncludeDefault(includeDefault);
            }
        } else {
            if (includeDefault != null && includeDefault) {
                filter.setType(SpecialFilterType.DEFAULT);
            } else {
                filter.setType(SpecialFilterType.ANY);
            }
        }
    }

    /**
     * Move the provided rules to the target priority. Rules will be sorted by their priority, first rule will be
     * updated with a priority equal to the target priority and the next ones will get an incremented priority value.
     */
    @RequestMapping(
            value = "/rules/move",
            method = RequestMethod.GET,
            produces = {"application/xml", "application/json"})
    public ResponseEntity<JaxbRuleList> move(
            @RequestParam(value = "targetPriority", required = true) int targetPriority,
            @RequestParam(value = "rulesIds", required = true) String rulesIds) {
        // let's find the rules that need to be moved
        List<Rule> rules = findRules(rulesIds);
        if (rules.isEmpty()) {
            return ResponseEntity.ok().build();
        }
        // shift priorities of rules with a priority equal or lower than the target
        // priority
        adminService().shift(targetPriority, rules.size());
        // update moved rules priority
        long priority = targetPriority;
        for (Rule rule : rules) {
            rule.setPriority(priority);
            adminService().update(rule);
            priority++;
        }
        // return moved rules with their priority updated
        return ResponseEntity.ok(new JaxbRuleList(rules));
    }

    /** Helper method that will parse and retrieve the provided rules sorted by their priority. */
    private List<Rule> findRules(String rulesIds) {
        return Arrays.stream(rulesIds.split(","))
                .map(ruleId -> {
                    try {
                        // parsing the rule id
                        return Long.parseLong(ruleId);
                    } catch (NumberFormatException exception) {
                        // error parsing the rule id
                        throw new InvalidRulesIds();
                    }
                })
                .map(ruleId -> {
                    // search the rule by id
                    return adminService().get(ruleId);
                })
                .filter(rule -> rule != null)
                .sorted((ruleA, ruleB) -> Long.compare(ruleA.getPriority(), ruleB.getPriority()))
                .collect(Collectors.toList());
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid rules ids")
    private static class InvalidRulesIds extends RuntimeException {
        private static final long serialVersionUID = -5682676569555830473L;
    }
}
