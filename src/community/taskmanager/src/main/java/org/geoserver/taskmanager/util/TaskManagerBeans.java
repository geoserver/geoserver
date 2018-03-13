/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import java.util.List;

import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.report.ReportBuilder;
import org.geoserver.taskmanager.report.ReportService;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.web.GeoServerApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskManagerBeans {
    
    @Autowired
    private LookupService<TaskType> taskTypes;

    @Autowired
    private TaskManagerFactory fac;
    
    @Autowired
    private TaskManagerDao dao;
    
    @Autowired
    private TaskManagerDataUtil dataUtil;
    
    @Autowired
    private TaskManagerTaskUtil taskUtil;
    
    @Autowired
    private ReportBuilder reportBuilder;
    
    @Autowired
    private List<ReportService> reportServices;
    
    @Autowired
    private BatchJobService bjService;

    @Autowired 
    private TaskManagerSecurityUtil secUtil;

    @Autowired 
    private InitConfigUtil initConfigUtil;
        
    public LookupService<TaskType> getTaskTypes() {
        return taskTypes;
    }
    
    public TaskManagerFactory getFac() {
        return fac;
    }

    public TaskManagerDao getDao() {
        return dao;
    }

    public TaskManagerDataUtil getDataUtil() {
        return dataUtil;
    }

    public TaskManagerTaskUtil getTaskUtil() {
        return taskUtil;
    }

    public ReportBuilder getReportBuilder() {
        return reportBuilder;
    }

    public List<ReportService> getReportServices() {
        return reportServices;
    }
    
    public BatchJobService getBjService() {
        return bjService;
    }
    
    public TaskManagerSecurityUtil getSecUtil() {
        return secUtil;
    }

    public InitConfigUtil getInitConfigUtil() {
        return initConfigUtil;
    }
        
    public static TaskManagerBeans get() {
        return GeoServerApplication.get().getApplicationContext().getBean(TaskManagerBeans.class);
    }

}
