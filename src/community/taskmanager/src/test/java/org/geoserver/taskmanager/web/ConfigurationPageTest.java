package org.geoserver.taskmanager.web;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.tasks.CopyTableTaskTypeImpl;
import org.geoserver.taskmanager.tasks.CreateViewTaskTypeImpl;
import org.geoserver.taskmanager.tasks.DbRemotePublicationTaskTypeImpl;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationPageTest extends GeoServerWicketTestSupport {
    
    private TaskManagerFactory fac;
    private TaskManagerDao dao;
    private TaskManagerDataUtil util;
    private TaskManagerTaskUtil tutil;
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        fac = TaskManagerBeans.get().getFac();
        dao = TaskManagerBeans.get().getDao();
        util = TaskManagerBeans.get().getDataUtil();
        tutil = TaskManagerBeans.get().getTaskUtil();
    }
    
    public Configuration createConfiguration() {
        Configuration config = fac.createConfiguration();  
        config.setName("test_template");
        config.setDescription("my new configuration");
        
        Task task1 = tutil.initTask(CopyTableTaskTypeImpl.NAME, "task1");
        util.addTaskToConfiguration(config, task1);
        
        Task task2 = tutil.initTask(CreateViewTaskTypeImpl.NAME, "task2");
        util.addTaskToConfiguration(config, task2);
        
        Task task3 = tutil.initTask(DbRemotePublicationTaskTypeImpl.NAME, "task3");
        util.addTaskToConfiguration(config, task3);
        
        return dao.save(config);
    }
    
    @Before
    public void init() throws IOException {
        login();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCreate() {        
        ConfigurationPage page = new ConfigurationPage(new Model<>(createConfiguration()));
        
        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);
        
        TextField<String> descr = (TextField<String>) tester.getComponentFromLastRenderedPage("configurationForm:description");
        assertEquals("my new configuration", descr.getModelObject());
        
        dao.delete(dao.getConfiguration("test_template"));
    }
    

}
