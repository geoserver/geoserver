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
import org.geoserver.catalog.LayerInfo;
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
public class LayerSecuritySyncTaskTypeImpl implements TaskType {

    public static final String NAME = "LayerSecuritySync";

    public static final String PARAM_EXT_GS = "external-geoserver";

    public static final String PARAM_WORKSPACE = "workspace";

    public static final String PARAM_LAYER = "layer";

    @Autowired protected ExtTypes extTypes;

    @Autowired protected DataAccessRuleDAO dao;

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    @PostConstruct
    public void initParamInfo() {
        paramInfo.put(PARAM_EXT_GS, new ParameterInfo(PARAM_EXT_GS, extTypes.extGeoserver, true));
        ParameterInfo paramWorkspace =
                new ParameterInfo(PARAM_WORKSPACE, extTypes.workspace, false);
        paramInfo.put(PARAM_WORKSPACE, paramWorkspace);
        paramInfo.put(
                PARAM_LAYER,
                new ParameterInfo(PARAM_LAYER, extTypes.internalLayer, true)
                        .dependsOn(false, paramWorkspace));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        final ExternalGS extGS = (ExternalGS) ctx.getParameterValues().get(PARAM_EXT_GS);
        final LayerInfo layer =
                (LayerInfo) ctx.getBatchContext().get(ctx.getParameterValues().get(PARAM_LAYER));
        String prefixedName = (String) ctx.getBatchContext().get(layer.prefixedName());
        String[] split = prefixedName.split(":");
        final String layerName = split.length > 1 ? split[1] : split[0];
        final String ws = split.length > 1 ? split[0] : "";

        final GeoServerRESTManager restManager;
        try {
            restManager = extGS.getRESTManager();
        } catch (MalformedURLException e) {
            throw new TaskException(e);
        }

        if (!restManager.getReader().existGeoserver()) {
            throw new TaskException("Failed to connect to geoserver " + extGS.getUrl());
        }

        Set<String> rolesRead = null, rolesWrite = null;

        for (DataAccessRule rule : dao.getRules()) {
            if (rule.getRoot().equals(ws) && rule.getLayer().equals(layer.getName())) {
                switch (rule.getAccessMode()) {
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
        final boolean createR = dataRules.getRule(ws, layerName, RuleType.R) == null;
        final boolean createW = dataRules.getRule(ws, layerName, RuleType.W) == null;

        GSDataRulesEncoder encoderCreate = new GSDataRulesEncoder();
        GSDataRulesEncoder encoderUpdate = new GSDataRulesEncoder();

        if (rolesRead != null) {
            if (createR) {
                encoderCreate.addRule(ws, layerName, RuleType.R, rolesRead);
            } else {
                encoderUpdate.addRule(ws, layerName, RuleType.R, rolesRead);
            }
        }
        if (rolesWrite != null) {
            if (createW) {
                encoderCreate.addRule(ws, layerName, RuleType.W, rolesWrite);
            } else {
                encoderUpdate.addRule(ws, layerName, RuleType.W, rolesWrite);
            }
        }

        if (!createR && rolesRead == null) {
            restManager.getSecurityManager().deleteDataRule(ws, layerName, RuleType.R);
        }
        if (!createW && rolesWrite == null) {
            restManager.getSecurityManager().deleteDataRule(ws, layerName, RuleType.W);
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
        /*final ExternalGS extGS = (ExternalGS) ctx.getParameterValues().get(PARAM_EXT_GS);
        final LayerInfo layer = (LayerInfo) ctx.getParameterValues().get(PARAM_LAYER);
        final ResourceInfo resource = layer.getResource();
        final StoreInfo store = resource.getStore();
        final String ws = store.getWorkspace().getName();

        final GeoServerRESTManager restManager;
        try {
            restManager = extGS.getRESTManager();
        } catch (MalformedURLException e) {
            throw new TaskException(e);
        }

        RESTDataRules dataRules = restManager.getSecurityManager().getDataRules();
        if (dataRules.getRule(ws, layer.getName(), RuleType.R) != null) {
            restManager.getSecurityManager().deleteDataRule(ws, layer.getName(), RuleType.R);
        }
        if (dataRules.getRule(ws, layer.getName(), RuleType.W) != null) {
            restManager.getSecurityManager().deleteDataRule(ws, layer.getName(), RuleType.W);
        }*/
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
