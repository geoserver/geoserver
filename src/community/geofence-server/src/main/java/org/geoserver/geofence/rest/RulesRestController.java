/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.geofence.rest.xml.JaxbRule;
import org.geoserver.geofence.rest.xml.JaxbRuleList;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.geofence.services.dto.RuleFilter.IdNameFilter;
import org.geoserver.geofence.services.dto.RuleFilter.SpecialFilterType;
import org.geoserver.geofence.services.dto.RuleFilter.TextFilter;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geoserver.geofence.services.exception.NotFoundServiceEx;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class RulesRestController {
    
    private RuleAdminService adminService;    
    
    public RulesRestController(RuleAdminService adminService) {
        this.adminService = adminService;
    }    
     
    @ExceptionHandler(NotFoundServiceEx.class)
    public void ruleNotFound(NotFoundServiceEx exception, HttpServletRequest request, HttpServletResponse response) throws IOException {
    	response.sendError(404, exception.getMessage());
    }
    
    @ExceptionHandler(DuplicateKeyException.class)
    public void rule(DuplicateKeyException exception, HttpServletRequest request, HttpServletResponse response) throws IOException {
    	response.sendError(409, exception.getMessage());
    }
    
    @RequestMapping(value = "/rest/rules", method = RequestMethod.GET, produces={"application/xml", "application/json"})
    public @ResponseBody JaxbRuleList get(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "entries", required = false) Integer entries,
            @RequestParam(value = "full", required = false, defaultValue = "false")  boolean full,
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "userAny", required = false)  Boolean userDefault,
            @RequestParam(value = "roleName", required = false) String roleName,
            @RequestParam(value = "roleAny", required = false)  Boolean roleDefault,
            @RequestParam(value = "instanceId", required = false)   Long instanceId,
            @RequestParam(value = "instanceName", required = false) String  instanceName,
            @RequestParam(value = "instanceAny", required = false)  Boolean instanceDefault,
            @RequestParam(value = "service", required = false)     String  serviceName,
            @RequestParam(value = "serviceAny", required = false)  Boolean serviceDefault,
            @RequestParam(value = "request", required = false)     String  requestName,
            @RequestParam(value = "requestAny", required = false)  Boolean requestDefault,
            @RequestParam(value = "workspace", required = false) String  workspace,
            @RequestParam(value = "workspaceAny", required = false)  Boolean workspaceDefault,
            @RequestParam(value = "layer", required = false) String  layer,
            @RequestParam(value = "layerAny", required = false)  Boolean layerDefault
    ) {
    	RuleFilter filter = buildFilter(
                userName, userDefault,
                roleName, roleDefault,
                instanceId, instanceName, instanceDefault,
                serviceName, serviceDefault,
                requestName, requestDefault,
                workspace, workspaceDefault,
                layer, layerDefault);

       return new JaxbRuleList(adminService.getListFull(filter, page, entries));
    }
    
    @RequestMapping(value = "/rest/rules/id/{id}", method = RequestMethod.GET, produces={"application/xml", "application/json"})
    public @ResponseBody JaxbRule get(@PathVariable ("id") Long id) {
    	return new JaxbRule(adminService.get(id));
    }    

    @RequestMapping(value = "/rest/rules/count", method = RequestMethod.GET, produces={"application/xml", "application/json"})
    public @ResponseBody JaxbRuleList count(
        @RequestParam(value = "userName", required = false) String userName,
        @RequestParam(value = "userAny", required = false) Boolean userDefault,
        @RequestParam(value = "roleName", required = false) String roleName,
        @RequestParam(value = "roleAny", required = false) Boolean roleDefault,
        @RequestParam(value = "instanceId", required = false) Long instanceId,
        @RequestParam(value = "instanceName", required = false) String  instanceName,
        @RequestParam(value = "instanceAny", required = false) Boolean instanceDefault,
        @RequestParam(value = "service", required = false) String  serviceName,
        @RequestParam(value = "serviceAny", required = false) Boolean serviceDefault,
        @RequestParam(value = "request", required = false) String  requestName,
        @RequestParam(value = "requestAny", required = false) Boolean requestDefault,
        @RequestParam(value = "workspace", required = false) String  workspace,
        @RequestParam(value = "workspaceAny", required = false) Boolean workspaceDefault,
        @RequestParam(value = "layer", required = false) String layer,
        @RequestParam(value = "layerAny", required = false) Boolean layerDefault
    ) {
    	RuleFilter filter = buildFilter(
                userName, userDefault,
                roleName, roleDefault,
                instanceId, instanceName, instanceDefault,
                serviceName, serviceDefault,
                requestName, requestDefault,
                workspace, workspaceDefault,
                layer, layerDefault);

        return new JaxbRuleList(adminService.count(filter));
    }
    

    @RequestMapping(value = "/rest/rules", method = RequestMethod.POST)
    public ResponseEntity<Long> insert(@RequestBody JaxbRule rule) {
        
        long priority = rule.getPriority() == null ? 0 : rule.getPriority().longValue();
        if (adminService.getRuleByPriority(priority) != null) {
            adminService.shift(priority, 1);
        }

        return new ResponseEntity<Long>(adminService.insert(rule.toRule()),
        		HttpStatus.CREATED);
    }

    @RequestMapping(value = "/rest/rules/id/{id}", method = RequestMethod.POST)
    public @ResponseStatus(HttpStatus.OK) void update(@PathVariable ("id") Long id, @RequestBody JaxbRule rule) {
        if (rule.getPriority() != null) {
            ShortRule priorityRule = adminService.getRuleByPriority(rule.getPriority().longValue());
            if (priorityRule != null && priorityRule.getId() != id) {
                adminService.shift(rule.getPriority().longValue(), 1);
            }
        }        
    	adminService.update(rule.toRule(adminService.get(id)));
    }
    
    @RequestMapping(value = "/rest/rules/id/{id}", method = RequestMethod.DELETE)
    public @ResponseStatus(HttpStatus.OK) void delete(@PathVariable("id") Long id) {
        adminService.delete(id);
    }
    
    protected RuleFilter buildFilter(
            String userName, Boolean userDefault,
            String roleName, Boolean groupDefault,
            Long instanceId, String instanceName, Boolean instanceDefault,
            String serviceName, Boolean serviceDefault,
            String requestName, Boolean requestDefault,
            String workspace, Boolean workspaceDefault,
            String layer, Boolean layerDefault) {

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


}
