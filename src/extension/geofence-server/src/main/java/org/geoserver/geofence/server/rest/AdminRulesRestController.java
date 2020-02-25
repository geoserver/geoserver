/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.geofence.core.model.AdminRule;
import org.geoserver.geofence.rest.xml.JaxbAdminRule;
import org.geoserver.geofence.rest.xml.JaxbAdminRuleList;
import org.geoserver.geofence.services.AdminRuleAdminService;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.geofence.services.dto.RuleFilter.SpecialFilterType;
import org.geoserver.geofence.services.dto.RuleFilter.TextFilter;
import org.geoserver.geofence.services.dto.ShortAdminRule;
import org.geoserver.geofence.services.exception.NotFoundServiceEx;
import org.geoserver.rest.RestBaseController;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/geofence")
public class AdminRulesRestController extends RestBaseController {

    private AdminRuleAdminService adminService;

    public AdminRulesRestController(AdminRuleAdminService adminService) {
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

    @RequestMapping(
        value = "/adminrules",
        method = RequestMethod.GET,
        produces = {"application/xml", "application/json"}
    )
    public @ResponseBody JaxbAdminRuleList get(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "entries", required = false) Integer entries,
            @RequestParam(value = "full", required = false, defaultValue = "false") boolean full,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "userAny", required = false) Boolean userDefault,
            @RequestParam(value = "roleName", required = false) String roleName,
            @RequestParam(value = "roleAny", required = false) Boolean roleDefault,
            @RequestParam(value = "workspace", required = false) String workspace,
            @RequestParam(value = "workspaceAny", required = false) Boolean workspaceDefault) {
        RuleFilter filter =
                buildFilter(
                        userName, userDefault, roleName, roleDefault, workspace, workspaceDefault);

        return new JaxbAdminRuleList(adminService.getListFull(filter, page, entries));
    }

    @RequestMapping(
        value = "/adminrules/id/{id}",
        method = RequestMethod.GET,
        produces = {"application/xml", "application/json"}
    )
    public @ResponseBody JaxbAdminRule get(@PathVariable("id") Long id) {
        return new JaxbAdminRule(adminService.get(id));
    }

    @RequestMapping(
        value = "/adminrules/count",
        method = RequestMethod.GET,
        produces = {"application/xml", "application/json"}
    )
    public @ResponseBody JaxbAdminRuleList count(
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "userAny", required = false) Boolean userDefault,
            @RequestParam(value = "roleName", required = false) String roleName,
            @RequestParam(value = "roleAny", required = false) Boolean roleDefault,
            @RequestParam(value = "workspace", required = false) String workspace,
            @RequestParam(value = "workspaceAny", required = false) Boolean workspaceDefault) {
        RuleFilter filter =
                buildFilter(
                        userName, userDefault, roleName, roleDefault, workspace, workspaceDefault);

        return new JaxbAdminRuleList(adminService.count(filter));
    }

    @RequestMapping(value = "/adminrules", method = RequestMethod.POST)
    public ResponseEntity<Long> insert(@RequestBody JaxbAdminRule rule) {

        long priority = rule.getPriority() == null ? 0 : rule.getPriority();
        if (adminService.getRuleByPriority(priority) != null) {
            adminService.shift(priority, 1);
        }

        return new ResponseEntity<>(adminService.insert(rule.toRule()), HttpStatus.CREATED);
    }

    @RequestMapping(
        value = "/adminrules/id/{id}",
        method = RequestMethod.POST,
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public @ResponseStatus(HttpStatus.OK) void update(
            @PathVariable("id") Long id, @RequestBody JaxbAdminRule rule) {
        if (rule.getPriority() != null) {
            ShortAdminRule priorityRule = adminService.getRuleByPriority(rule.getPriority());
            if (priorityRule != null && priorityRule.getId().longValue() != id) {
                adminService.shift(rule.getPriority(), 1);
            }
        }
        adminService.update(rule.toRule(adminService.get(id)));
    }

    @RequestMapping(value = "/adminrules/id/{id}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void delete(@PathVariable("id") Long id) {
        adminService.delete(id);
    }

    protected RuleFilter buildFilter(
            String userName,
            Boolean userDefault,
            String roleName,
            Boolean groupDefault,
            String workspace,
            Boolean workspaceDefault) {

        RuleFilter filter = new RuleFilter(SpecialFilterType.ANY, true);

        setFilter(filter.getUser(), userName, userDefault);
        setFilter(filter.getRole(), roleName, groupDefault);
        setFilter(filter.getWorkspace(), workspace, workspaceDefault);
        return filter;
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
        value = "/adminrules/move",
        method = RequestMethod.GET,
        produces = {"application/xml", "application/json"}
    )
    public @ResponseBody ResponseEntity<JaxbAdminRuleList> move(
            @RequestParam(value = "targetPriority", required = true) int targetPriority,
            @RequestParam(value = "rulesIds", required = true) String rulesIds) {
        // let's find the rules that need to be moved
        List<AdminRule> rules = findRules(rulesIds);
        if (rules.isEmpty()) {
            return ResponseEntity.ok().build();
        }
        // shift priorities of rules with a priority equal or lower than the target priority
        adminService.shift(targetPriority, rules.size());
        // update moved rules priority
        long priority = targetPriority;
        for (AdminRule rule : rules) {
            rule.setPriority(priority);
            adminService.update(rule);
            priority++;
        }
        // return moved rules with their priority updated
        return ResponseEntity.ok(new JaxbAdminRuleList(rules));
    }

    /** Helper method that will parse and retrieve the provided rules sorted by their priority. */
    private List<AdminRule> findRules(String rulesIds) {
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

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid adminrules ids")
    private class InvalidRulesIds extends RuntimeException {}
}
