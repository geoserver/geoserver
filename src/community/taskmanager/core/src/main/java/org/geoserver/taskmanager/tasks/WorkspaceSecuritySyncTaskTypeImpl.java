/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.decoder.RESTDataRules;
import it.geosolutions.geoserver.rest.encoder.GSDataRulesEncoder;
import it.geosolutions.geoserver.rest.manager.GeoServerRESTSecurityManager.RuleType;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceSecuritySyncTaskTypeImpl implements TaskType {

    public static final String NAME = "WorkspaceSecuritySync";

    public static final String PARAM_EXT_GS = "external-geoserver";

    public static final String PARAM_WORKSPACE = "workspace";

    @Autowired protected ExtTypes extTypes;

    @Autowired protected DataAccessRuleDAO dao;

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_EXT_GS, new ParameterInfo(PARAM_EXT_GS, extTypes.extGeoserver, true));
        paramInfo.put(
                PARAM_WORKSPACE, new ParameterInfo(PARAM_WORKSPACE, extTypes.workspace, true));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        final ExternalGS extGS = (ExternalGS) ctx.getParameterValues().get(PARAM_EXT_GS);
        final WorkspaceInfo workspace =
                (WorkspaceInfo)
                        ctx.getBatchContext().get(ctx.getParameterValues().get(PARAM_WORKSPACE));

        final GeoServerRESTManager restManager;
        try {
            restManager = extGS.getRESTManager();
        } catch (MalformedURLException e) {
            throw new TaskException(e);
        }

        if (!restManager.getReader().existGeoserver()) {
            throw new TaskException("Failed to connect to geoserver " + extGS.getUrl());
        }

        Set<String> rolesAdmin = null, rolesRead = null, rolesWrite = null;

        for (DataAccessRule rule : dao.getRules()) {
            if (rule.getRoot().equals(workspace.getName())
                    && rule.getLayer().equals(DataAccessRule.ANY)) {
                switch (rule.getAccessMode()) {
                    case ADMIN:
                        rolesAdmin = rule.getRoles();
                        break;
                    case READ:
                        rolesRead = rule.getRoles();
                        break;
                    case WRITE:
                        rolesWrite = rule.getRoles();
                        break;
                    default:
                        break;
                }
            }
        }

        RESTDataRules dataRules = restManager.getSecurityManager().getDataRules();
        final boolean createR =
                dataRules.getRule(workspace.getName(), DataAccessRule.ANY, RuleType.R) == null;
        final boolean createW =
                dataRules.getRule(workspace.getName(), DataAccessRule.ANY, RuleType.W) == null;
        final boolean createA =
                dataRules.getRule(workspace.getName(), DataAccessRule.ANY, RuleType.A) == null;

        GSDataRulesEncoder encoderCreate = new GSDataRulesEncoder();
        GSDataRulesEncoder encoderUpdate = new GSDataRulesEncoder();

        if (rolesRead != null) {
            if (createR) {
                encoderCreate.addRule(
                        workspace.getName(), DataAccessRule.ANY, RuleType.R, rolesRead);
            } else {
                encoderUpdate.addRule(
                        workspace.getName(), DataAccessRule.ANY, RuleType.R, rolesRead);
            }
        }
        if (rolesWrite != null) {
            if (createW) {
                encoderCreate.addRule(
                        workspace.getName(), DataAccessRule.ANY, RuleType.W, rolesWrite);
            } else {
                encoderUpdate.addRule(
                        workspace.getName(), DataAccessRule.ANY, RuleType.W, rolesWrite);
            }
        }
        if (rolesAdmin != null) {
            if (createA) {
                encoderCreate.addRule(
                        workspace.getName(), DataAccessRule.ANY, RuleType.A, rolesAdmin);
            } else {
                encoderUpdate.addRule(
                        workspace.getName(), DataAccessRule.ANY, RuleType.A, rolesAdmin);
            }
        }

        if (!createR && rolesRead == null) {
            restManager
                    .getSecurityManager()
                    .deleteDataRule(workspace.getName(), DataAccessRule.ANY, RuleType.R);
        }
        if (!createW && rolesWrite == null) {
            restManager
                    .getSecurityManager()
                    .deleteDataRule(workspace.getName(), DataAccessRule.ANY, RuleType.W);
        }
        if (!createA && rolesAdmin == null) {
            restManager
                    .getSecurityManager()
                    .deleteDataRule(workspace.getName(), DataAccessRule.ANY, RuleType.A);
        }

        if (!encoderUpdate.isEmpty()) {
            restManager.getSecurityManager().modifyDataRules(encoderUpdate);
        }
        if (!encoderCreate.isEmpty()) {
            restManager.getSecurityManager().addDataRules(encoderCreate);
        }

        return new TaskResult() {
            @Override
            public void commit() throws TaskException {
                // nothing to do
            }

            @Override
            public void rollback() throws TaskException {
                throw new TaskException("Cannot roll-back layer security sync task");
            }
        };
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        throw new TaskException("unsupported");
    }

    @Override
    public boolean supportsCleanup() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
