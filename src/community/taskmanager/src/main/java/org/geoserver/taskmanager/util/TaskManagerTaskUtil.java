/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.ValidationError.ValidationErrorType;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *  Implementation independent helper methods.
 *  
 * @author Niels Charlier
 *
 */
@Service
public class TaskManagerTaskUtil {

    private static final Logger LOGGER = Logging.getLogger(TaskManagerTaskUtil.class);
    
    @Autowired
    private LookupService<TaskType> taskTypes;
    
    @Autowired
    private TaskManagerFactory fac;

    @Autowired
    private TaskManagerDataUtil dataUtil;
            
    private String getRawParameterValue(Parameter parameter) {
        String attName = dataUtil.getAssociatedAttributeName(parameter);
        if (attName != null) {
            Attribute att = parameter.getTask().getConfiguration().getAttributes().get(attName);
            if (att != null) {
                return att.getValue();
            } else {
                return null;
            }
        } else {
            return parameter.getValue();
        }
    }
    
    /**
     * Compile raw parameters from task using the configuration.
     * 
     * @param task the task
     * @return the raw parameters.
     */
    public Map<String, String> getRawParameterValues(Task task) {
        Map<String, String> rawParameterValues = new HashMap<String, String>();
        for (Parameter parameter : task.getParameters().values()) {
            String rawValue = getRawParameterValue(parameter);
            if (rawValue != null) {
                rawParameterValues.put(parameter.getName(), rawValue);
            }
        }
        return rawParameterValues;
    }

    /**
     * Validate required parameters
     * 
     * @param taskType task type
     * @param rawParameters raw parameters
     * @throws TaskException
     */
    private void validateRequired(TaskType taskType, Map<String, String> rawParameters, List<ValidationError> validationErrors)  {
        for (ParameterInfo info : taskType.getParameterInfo().values()) {
            if (info.isRequired()) {
                if (!rawParameters.containsKey(info.getName()) ||
                        "".equals(rawParameters.get(info.getName()))) {
                    validationErrors.add(new ValidationError(ValidationErrorType.MISSING, 
                            info.getName(), null, taskType.getName()));
                }
            }
        }
    }
    
    /**
     * validate and parse the raw parameters.
     * 
     * @param taskTypeName the task type name.
     * @param taskType the task type.
     * @param rawParameters the raw parameters.
     * @return the parsed parameters
     * @throws TaskException if any of the parameters are invalid or missing.
     */
    public Map<String, Object> parseParameters(TaskType taskType, 
            Map<String, String> rawParameters) throws TaskException {
        List<ValidationError> validationErrors = new ArrayList<ValidationError>();
        
        //first check all required
        validateRequired(taskType, rawParameters, validationErrors);
        
        //parse
        Map<String, Object> result = new HashMap<String, Object>();        
        for (Entry<String, String> parameter : rawParameters.entrySet()) {
            ParameterInfo info = taskType.getParameterInfo().get(parameter.getKey());
            if (info == null) {
                validationErrors.add(new ValidationError(ValidationErrorType.INVALID_PARAM, 
                        parameter.getKey(), null, taskType.getName()));
                break;
            }
            ParameterType pt = info.getType();
            List<String> dependsOnValues = new ArrayList<String>();
            for (ParameterInfo dependsOn : info.getDependsOn()) {
                dependsOnValues.add(rawParameters.get(dependsOn.getName()));
            }
            Object value = pt.parse(parameter.getValue(), dependsOnValues);
            if (value == null) {
                validationErrors.add(new ValidationError(ValidationErrorType.INVALID_VALUE, 
                        parameter.getKey(), parameter.getValue(), taskType.getName()));
                break;
            } else {
                result.put(parameter.getKey(), value);
            }
        }
        
        if (!validationErrors.isEmpty()) {
            throw new TaskException("There were validation errors: " + validationErrors);
        }
        
        return result;
    }
    
    /**
     * Clean-up a configuration.
     * 
     * @param config the configuration.
     * @return true if the cleanup was entirely successful, false if one or more task clean-ups failed,
     * in which case the logs should be checked.
     */
    public boolean cleanup(Configuration config) {
        boolean success = true;
        for (Task task : config.getTasks().values()) {
            Map<String, String> rawParameterValues = getRawParameterValues(task);
            TaskType type = taskTypes.get(task.getType());
            try {
                Map<String, Object> parameterValues = parseParameters(type, rawParameterValues);
                type.cleanup(task, parameterValues);
            } catch (TaskException e) {
                LOGGER.log(Level.SEVERE, "Clean-up of task " + task.getFullName() + " failed", e);
                success = false;                
            }
        }
        return success;        
    }
    
    /**
     * Creates and initializes a task of a particular type
     * 
     * @param type the type
     * @param string 
     * @return the task
     */
    public Task initTask(String type, String name) {
        Task task = fac.createTask();
        task.setType(type);
        task.setName(name);
        TaskType taskType = taskTypes.get(type);
        for (ParameterInfo info : taskType.getParameterInfo().values()) {
            Parameter param = fac.createParameter();
            param.setName(info.getName());
            param.setValue("${" + info.getName() + "}");
            param.setTask(task);
            task.getParameters().put(info.getName(), param);
        }
        return task;
    }
    
    private static class AttributeInfo {
        private ParameterType type;
        private List<String> dependsOn;
        
        public AttributeInfo(ParameterType type, List<String> dependsOn) {
            this.type = type;
            this.dependsOn = dependsOn;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof AttributeInfo) {
                return type.equals(((AttributeInfo) o).type)
                        && dependsOn.equals(((AttributeInfo) o).dependsOn);
            }
            return false;
        }

        public ParameterType getType() {
            return type;
        }

        public List<String> getDependsOn() {
            return dependsOn;
        }
    }
    
    /**
     * Get attribute domain based on associated parameters.
     * 
     * @param type the type
     * @param string 
     * @return the task
     */
    private List<String> mergeDomain(Attribute attribute) {
        List<Parameter> params = dataUtil.getAssociatedParameters(attribute);
        
        Set<AttributeInfo> attInfos = new HashSet<AttributeInfo>();
        for (Parameter param : params) {
            TaskType taskType = taskTypes.get(param.getTask().getType());
            ParameterInfo info = taskType.getParameterInfo().get(param.getName());
            List<String> dependsOn = new ArrayList<String>();
            for (ParameterInfo dependsOnInfo : info.getDependsOn()) {
                Parameter dependsOnParam = param.getTask().getParameters().get(dependsOnInfo.getName());
                dependsOn.add(dependsOnParam == null ? null : getRawParameterValue(dependsOnParam));
            }
            attInfos.add(new AttributeInfo(info.getType(), dependsOn));
        }
        
        Set<String> domain = null;
        //first add all inclusive domains
        for (AttributeInfo attInfo : attInfos) {
            List<String> thisDomain = attInfo.getType().getDomain(attInfo.getDependsOn());            
            if (thisDomain != null && thisDomain.contains("")) {
                if (domain == null) {
                    domain = new LinkedHashSet<String>(thisDomain);
                } else {
                    domain.addAll(thisDomain);
                }
            }
        }
        //then select on all the exclusive domains
        for (AttributeInfo attInfo : attInfos) {
            List<String> thisDomain = attInfo.getType().getDomain(attInfo.getDependsOn());            
            if (thisDomain != null && !thisDomain.contains("")) {
                if (domain == null) {
                    domain = new LinkedHashSet<String>(thisDomain);
                } else {
                    domain.retainAll(thisDomain);
                }
            }
        }
        return domain == null ? null : new ArrayList<String>(domain);
    }
    
    /**
     * Get domains of configuration
     * 
     * @param configuration the configuration
     * @return the domains
     */
    public Map<String, List<String>> getDomains(Configuration configuration) {
        Map<String, List<String>> domains = new HashMap<String, List<String>>();
        for (Attribute att : configuration.getAttributes().values()) {
            domains.put(att.getName(), mergeDomain(att));
        }
        return domains;
    }
    
    /**
     * Update domains of configuration
     * 
     * @param configuration the configuration
     * @param the domains
     */
    public void updateDomains(Configuration configuration, Map<String, List<String>> domains) {
        Iterator<String> it = domains.keySet().iterator();
        while(it.hasNext()) {
            String attName = it.next();
            if (!configuration.getAttributes().containsKey(attName)) {
                it.remove();
            }
        }
        for (Attribute att : configuration.getAttributes().values()) {
            if (!domains.containsKey(att.getName())) {
                domains.put(att.getName(), mergeDomain(att));
            }
        }
    }
    
    /**
     * Update dependent domains for a particular attribute.
     * 
     * @param attribute the attribute.
     * @param domains the domains.
     */
    public void updateDependentDomains(Attribute attribute, Map<String, List<String>> domains) {
        Set<String> dependentAttributes = new HashSet<String>();
        List<Parameter> params = dataUtil.getAssociatedParameters(attribute);
        for (Parameter param : params) {
            TaskType taskType = taskTypes.get(param.getTask().getType());
            ParameterInfo info = taskType.getParameterInfo().get(param.getName());
            for (ParameterInfo dependentInfo: info.getDependents()) {
                Parameter depedentParam = param.getTask().getParameters().get(dependentInfo.getName());
                if (depedentParam != null) {
                    String attName = dataUtil.getAssociatedAttributeName(depedentParam);
                    if (attName != null) {
                        dependentAttributes.add(attName);
                    }
                }
            }
        }
        
        for (String attName : dependentAttributes) {
            Attribute att = attribute.getConfiguration().getAttributes().get(attName);
            if (att != null) {
                domains.put(attName, mergeDomain(att));
            }
        }
    }

    /**
     * Validate configuration (at configuration time)
     * 
     * @param configuration the configuration
     * @throws TaskException
     */
    public List<ValidationError> validate(Configuration configuration) {
        List<ValidationError> validationErrors = new ArrayList<ValidationError>();
        
        for (Task task : configuration.getTasks().values()) {
            TaskType taskType = taskTypes.get(task.getType());
            Map<String, String> rawParameters = getRawParameterValues(task);
            //first check all required (except if template)
            if (!configuration.isTemplate()) {
                validateRequired(taskType, rawParameters, validationErrors);
            }
            
            for (Entry<String, String> parameter : rawParameters.entrySet()) {
                ParameterInfo info = taskType.getParameterInfo().get(parameter.getKey());
                if (info == null) {
                    validationErrors.add(new ValidationError(ValidationErrorType.INVALID_PARAM, 
                            parameter.getKey(), null, taskType.getName()));
                    break;
                }
                ParameterType pt = info.getType();
                List<String> dependsOnValues = new ArrayList<String>();
                for (ParameterInfo dependsOn : info.getDependsOn()) {
                    dependsOnValues.add(rawParameters.get(dependsOn.getName()));
                }
                if (!pt.validate(parameter.getValue(), dependsOnValues)) {
                    validationErrors.add(new ValidationError(ValidationErrorType.INVALID_VALUE, 
                            parameter.getKey(), parameter.getValue(), taskType.getName()));
                    break;
                }
            }
            
        }
        
        return validationErrors;
    }
    
    /**
     * Validate batch (at configuration time)
     * 
     * @param configuration the configuration
     * @throws TaskException
     */
    public List<ValidationError> validate(Batch batch) {
        List<ValidationError> validationErrors = new ArrayList<ValidationError>();
        
        for (BatchElement element : batch.getElements()) {
            Task task = element.getTask();
            TaskType taskType = taskTypes.get(task.getType());
            Map<String, String> rawParameters = getRawParameterValues(task);
            //first check all required (except if template)
            if (!task.getConfiguration().isTemplate()) {
                validateRequired(taskType, rawParameters, validationErrors);
            }
            
            for (Entry<String, String> parameter : rawParameters.entrySet()) {
                ParameterInfo info = taskType.getParameterInfo().get(parameter.getKey());
                if (info == null) {
                    validationErrors.add(new ValidationError(ValidationErrorType.INVALID_PARAM, 
                            parameter.getKey(), null, taskType.getName()));
                    break;
                }
                ParameterType pt = info.getType();
                List<String> dependsOnValues = new ArrayList<String>();
                for (ParameterInfo dependsOn : info.getDependsOn()) {
                    dependsOnValues.add(rawParameters.get(dependsOn.getName()));
                }
                if (!pt.validate(parameter.getValue(), dependsOnValues)) {
                    validationErrors.add(new ValidationError(ValidationErrorType.INVALID_VALUE, 
                            parameter.getKey(), parameter.getValue(), taskType.getName()));
                    break;
                }
            }
            
        }
        
        return validationErrors;
    }
    

}
