/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.thoughtworks.xstream.XStream;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class XStreamTest extends AbstractTaskManagerTest {

    @Autowired
    private TaskManagerDao dao;

    @Autowired
    private TaskManagerFactory fac;

    @Autowired
    private TaskManagerDataUtil util;

    private Configuration config;

    @Before
    public void setupConfig() {
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType("testTask");
        util.addTaskToConfiguration(config, task1);

        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType("testTask");
        util.addTaskToConfiguration(config, task2);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");

        Batch batch = fac.createBatch();
        util.addBatchElement(batch, task1);
        util.addBatchElement(batch, task2);
        batch.setEnabled(true);
        batch.setName("my_batch");
        util.addBatchToConfiguration(config, batch);

        config = dao.save(config);
    }

    @After
    public void clearDataFromDatabase() {
        if (config != null) {
            dao.delete(config);
        }
    }

    @Test
    public void testExportThenImport() {
        config = dao.init(config);
        final XStream xs = XStreamUtil.xs();
        String xml = xs.toXML(config);
        assertTrue(xml.indexOf("<name>my_config</name>") >= 0);
        assertTrue(xml.indexOf("<id>") < 0);
        Configuration newConfig = (Configuration) xs.fromXML(xml);
        assertNull(newConfig.getId());
        assertEquals(config.getWorkspace(), newConfig.getWorkspace());
        assertEquals(config.getTasks().size(), newConfig.getTasks().size());
        assertEquals(config.getBatches().size(), newConfig.getBatches().size());
    }

    @Test
    public void testImportFromXml() {
        config = dao.init(config);
        String xml =
                """
                <org.geoserver.taskmanager.data.impl.ConfigurationImpl>
                  <name>my_config</name>
                  <id>1</id>
                  <template>false</template>
                  <workspace>some_ws</workspace>
                  <attributes/>
                  <tasks>
                    <entry>
                      <string>task1</string>
                      <org.geoserver.taskmanager.data.impl.TaskImpl>
                        <name>task1</name>
                        <type>testTask</type>
                        <configuration reference="../../../.."/>
                        <parameters/>
                        <batchElements>
                          <org.geoserver.taskmanager.data.impl.BatchElementImpl>
                            <batch>
                              <elements>
                                <org.geoserver.taskmanager.data.impl.BatchElementImpl reference="../../.."/>
                                <org.geoserver.taskmanager.data.impl.BatchElementImpl>
                                  <batch reference="../../.."/>
                                  <task>
                                    <name>task2</name>
                                    <type>testTask</type>
                                    <configuration reference="../../../../../../../../../.."/>
                                    <parameters/>
                                    <batchElements>
                                      <org.geoserver.taskmanager.data.impl.BatchElementImpl reference="../../.."/>
                                    </batchElements>
                                  </task>
                                  <index>1</index>
                                </org.geoserver.taskmanager.data.impl.BatchElementImpl>
                              </elements>
                              <name>my_batch</name>
                              <configuration reference="../../../../../../.."/>
                              <enabled>true</enabled>
                            </batch>
                            <task reference="../../.."/>
                            <index>0</index>
                          </org.geoserver.taskmanager.data.impl.BatchElementImpl>
                        </batchElements>
                      </org.geoserver.taskmanager.data.impl.TaskImpl>
                    </entry>
                    <entry>
                      <string>task2</string>
                      <org.geoserver.taskmanager.data.impl.TaskImpl reference="../../entry/org.geoserver.taskmanager.data.impl.TaskImpl/batchElements/org.geoserver.taskmanager.data.impl.BatchElementImpl/batch/elements/org.geoserver.taskmanager.data.impl.BatchElementImpl[2]/task"/>
                    </entry>
                  </tasks>
                  <batches>
                    <entry>
                      <string>my_batch</string>
                      <org.geoserver.taskmanager.data.impl.BatchImpl reference="../../../tasks/entry/org.geoserver.taskmanager.data.impl.TaskImpl/batchElements/org.geoserver.taskmanager.data.impl.BatchElementImpl/batch"/>
                    </entry>
                  </batches>
                </org.geoserver.taskmanager.data.impl.ConfigurationImpl>
                """;
        final XStream xs = XStreamUtil.xs();
        Configuration newConfig = (Configuration) xs.fromXML(xml);
        assertNull(newConfig.getId());
        assertEquals(config.getWorkspace(), newConfig.getWorkspace());
        assertEquals(config.getTasks().size(), newConfig.getTasks().size());
        assertEquals(config.getBatches().size(), newConfig.getBatches().size());
    }
}
