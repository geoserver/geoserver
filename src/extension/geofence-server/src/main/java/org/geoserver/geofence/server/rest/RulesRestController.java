/* (c) 2015-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.rest.xml.JaxbRule;
import org.geoserver.geofence.rest.xml.JaxbRuleList;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.geofence.services.dto.RuleFilter.IdNameFilter;
import org.geoserver.geofence.services.dto.RuleFilter.SpecialFilterType;
import org.geoserver.geofence.services.dto.RuleFilter.TextFilter;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geoserver.geofence.services.exception.BadRequestServiceEx;
import org.geoserver.geofence.services.exception.NotFoundServiceEx;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
public class RulesRestController extends RestBaseController {

    private RuleAdminService adminService;

    @Autowired
    public RulesRestController(RuleAdminService adminService) {
        this.adminService = adminService;
    }

    @ExceptionHandler(NotFoundServiceEx.class)
    public void ruleNotFound(
            NotFoundServiceEx exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(404, exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public void rule(
            DuplicateKeyException exception,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {
        response.sendError(409, exception.getMessage());
    }

    @ExceptionHandler(BadRequestServiceEx.class)
    public void badRequest(
            BadRequestServiceEx exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void messageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {
        response.sendError(400, exception.getMessage());
    }

    @RequestMapping(
        value = "/rules",
        method = RequestMethod.GET,
        produces = {"application/xml", "application/json"}
    )
    public JaxbRuleList get(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "entries", required = false) Integer entries,
            @RequestParam(value = "full", required = false, defaultValue = "false") boolean full,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "userAny", required = false) Boolean userDefault,
            @RequestParam(value = "roleName", required = false) String roleName,
            @RequestParam(value = "roleAny", required = false) Boolean roleDefault,
            @RequestParam(value = "instanceId", required = false) Long instanceId,
            @RequestParam(value = "instanceName", required = false) String instanceName,
            @RequestParam(value = "instanceAny", required = false) Boolean instanceDefault,
            @RequestParam(value = "service", required = false) String serviceName,
            @RequestParam(value = "serviceAny", required = false) Boolean serviceDefault,
            @RequestParam(value = "request", required = false) String requestName,
            @RequestParam(value = "requestAny", required = false) Boolean requestDefault,
            @RequestParam(value = "workspace", required = false) String workspace,
            @RequestParam(value = "workspaceAny", required = false) Boolean workspaceDefault,
            @RequestParam(value = "layer", required = false) String layer,
            @RequestParam(value = "layerAny", required = false) Boolean layerDefault) {
        RuleFilter filter =
                buildFilter(
                        userName,
                        userDefault,
                        roleName,
                        roleDefault,
                        instanceId,
                        instanceName,
                        instanceDefault,
                        serviceName,
                        serviceDefault,
                        requestName,
                        requestDefault,
                        workspace,
                        workspaceDefault,
                        layer,
                        layerDefault);

        return new JaxbRuleList(adminService.getListFull(filter, page, entries));
    }

    @RequestMapping(
        value = "/rules/id/{id}",
        method = RequestMethod.GET,
        produces = {"application/xml", "application/json"}
    )
    public JaxbRule get(@PathVariable("id") Long id) {
        return new JaxbRule(adminService.get(id));
    }

    @RequestMapping(
        value = "/rules/count",
        method = RequestMethod.GET,
        produces = {"application/xml", "application/json"}
    )
    public JaxbRuleList count(
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "userAny", required = false) Boolean userDefault,
            @RequestParam(value = "roleName", required = false) String roleName,
            @RequestParam(value = "roleAny", required = false) Boolean roleDefault,
            @RequestParam(value = "instanceId", required = false) Long instanceId,
            @RequestParam(value = "instanceName", required = false) String instanceName,
            @RequestParam(value = "instanceAny", required = false) Boolean instanceDefault,
            @RequestParam(value = "service", required = false) String serviceName,
            @RequestParam(value = "serviceAny", required = false) Boolean serviceDefault,
            @RequestParam(value = "request", required = false) String requestName,
            @RequestParam(value = "requestAny", required = false) Boolean requestDefault,
            @RequestParam(value = "workspace", required = false) String workspace,
            @RequestParam(value = "workspaceAny", required = false) Boolean workspaceDefault,
            @RequestParam(value = "layer", required = false) String layer,
            @RequestParam(value = "layerAny", required = false) Boolean layerDefault) {
        RuleFilter filter =
                buildFilter(
                        userName,
                        userDefault,
                        roleName,
                        roleDefault,
                        instanceId,
                        instanceName,
                        instanceDefault,
                        serviceName,
                        serviceDefault,
                        requestName,
                        requestDefault,
                        workspace,
                        workspaceDefault,
                        layer,
                        layerDefault);

        return new JaxbRuleList(adminService.count(filter));
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
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public String insert(@RequestBody(required = true) JaxbRule rule) {
        long priority = rule.getPriority() == null ? 0 : rule.getPriority().longValue();
        if (adminService.getRuleByPriority(priority) != null) {
            adminService.shift(priority, 1);
        }

        Long id = adminService.insert(rule.toRule());

        if (rule.getLimits() != null && rule.getAccess().equals("LIMIT")) {
            adminService.setLimits(id, rule.getLimits().toRuleLimits(null));
        }
        if (rule.getLayerDetails() != null && !rule.getAccess().equals("LIMIT")) {
            adminService.setDetails(id, rule.getLayerDetails().toLayerDetails(null));
        }

        return String.valueOf(id);
    }

    @RequestMapping(value = "/rules/id/{id}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.OK) void update(
            @PathVariable("id") Long id, @RequestBody JaxbRule rule) {
        if (rule.getPriority() != null) {
            ShortRule priorityRule = adminService.getRuleByPriority(rule.getPriority().longValue());
            if (priorityRule != null && !Objects.equals(priorityRule.getId(), id)) {
                adminService.shift(rule.getPriority().longValue(), 1);
            }
        }
        Rule theRule = adminService.get(id);
        adminService.update(rule.toRule(theRule));
        if (rule.getLimits() != null) {
            adminService.setLimits(id, rule.getLimits().toRuleLimits(theRule.getRuleLimits()));
        }
        if (rule.getLayerDetails() != null) {
            adminService.setDetails(
                    id, rule.getLayerDetails().toLayerDetails(theRule.getLayerDetails()));
        }
    }

    @RequestMapping(value = "/rules/id/{id}", method = RequestMethod.PUT)
    public @ResponseStatus(HttpStatus.OK) void clearAndUpdate(
            @PathVariable("id") Long id, @RequestBody JaxbRule rule) {
        if (rule.getPriority() != null) {
            ShortRule priorityRule = adminService.getRuleByPriority(rule.getPriority().longValue());
            if (priorityRule != null && !Objects.equals(priorityRule.getId(), id)) {
                adminService.shift(rule.getPriority().longValue(), 1);
            }
        }
        Rule theRule = new Rule();
        theRule.setId(id);
        adminService.update(rule.toRule(theRule));
        if (rule.getLimits() != null) {
            adminService.setLimits(id, rule.getLimits().toRuleLimits(null));
        } else {
            adminService.setLimits(id, null);
        }
        if (rule.getLayerDetails() != null) {
            adminService.setDetails(id, rule.getLayerDetails().toLayerDetails(null));
        } else {
            adminService.setDetails(id, null);
        }
    }

    @RequestMapping(value = "/rules/id/{id}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void delete(@PathVariable("id") Long id) {
        adminService.delete(id);
    }

    protected RuleFilter buildFilter(
            String userName,
            Boolean userDefault,
            String roleName,
            Boolean groupDefault,
            Long instanceId,
            String instanceName,
            Boolean instanceDefault,
            String serviceName,
            Boolean serviceDefault,
            String requestName,
            Boolean requestDefault,
            String workspace,
            Boolean workspaceDefault,
            String layer,
            Boolean layerDefault) {

        RuleFilter filter = new RuleFilter(SpecialFilterType.ANY, true);

        setFilter(filter.getUser(), userName, userDefault);
        setFilter(filter.getRole(), roleName, groupDefault);
        setFilter(filter.getInstance(), instanceId, instanceName, instanceDefault);
        setFilter(filter.getService(), serviceName, serviceDefault);
        setFilter(filter.getRequest(), requestName, requestDefault);
        setFilter(filter.getWorkspace(), workspace, workspaceDefault);
        setFilter(filter.getLayer(), layer, layerDefault);
        return filter;
    }

    private void setFilter(IdNameFilter filter, Long id, String name, Boolean includeDefault) {

        if (id != null && name != null) {
            throw new IllegalArgumentException(
                    "Id and name can't be both defined (id:" + id + " name:" + name + ")");
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
     * Move the provided rules to the target priority. Rules will be sorted by their priority, first
     * rule will be updated with a priority equal to the target priority and the next ones will get
     * an incremented priority value.
     */
    @RequestMapping(
        value = "/rules/move",
        method = RequestMethod.GET,
        produces = {"application/xml", "application/json"}
    )
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
        adminService.shift(targetPriority, rules.size());
        // update moved rules priority
        long priority = targetPriority;
        for (Rule rule : rules) {
            rule.setPriority(priority);
            adminService.update(rule);
            priority++;
        }
        // return moved rules with their priority updated
        return ResponseEntity.ok(new JaxbRuleList(rules));
    }

    /** Helper method that will parse and retrieve the provided rules sorted by their priority. */
    private List<Rule> findRules(String rulesIds) {
        return Arrays.stream(rulesIds.split(","))
                .map(
                        ruleId -> {
                            try {
                                // parsing the rule id
                                return Long.parseLong(ruleId);
                            } catch (NumberFormatException exception) {
                                // error parsing the rule id
                                throw new InvalidRulesIds();
                            }
                        })
                .map(
                        ruleId -> {
                            // search the rule by id
                            return adminService.get(ruleId);
                        })
                .filter(rule -> rule != null)
                .sorted((ruleA, ruleB) -> Long.compare(ruleA.getPriority(), ruleB.getPriority()))
                .collect(Collectors.toList());
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid rules ids")
    private class InvalidRulesIds extends RuntimeException {
        private static final long serialVersionUID = -5682676569555830473L;
    }
}
