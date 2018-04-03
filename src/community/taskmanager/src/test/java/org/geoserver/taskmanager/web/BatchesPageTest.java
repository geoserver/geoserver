/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.geoserver.taskmanager.AbstractWicketTaskManagerTest;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BatchesPageTest extends AbstractWicketTaskManagerTest {
    
    private TaskManagerFactory fac;
    private TaskManagerDao dao;
    
    private Batch batch;
    
    @Before
    public void before() {
        fac = TaskManagerBeans.get().getFac();
        dao = TaskManagerBeans.get().getDao();
        
        batch = fac.createBatch();
        batch.setName("my_batch");
        batch = dao.save(batch);
    }
    

    @After
    public void clearDataFromDatabase() {
        dao.delete(batch);
    }
    
    private Batch dummyBatch1() {
        Batch batch = fac.createBatch();
        batch.setName("Z-BATCH");
        return batch;
    }
    
    private Batch dummyBatch2() {
        Batch batch = fac.createBatch();
        batch.setName("A-BATCH");
        return batch;
    }
    
    @Test
    public void testPage() {
        BatchesPage page = new BatchesPage();

        tester.startPage(page);
        tester.assertRenderedPage(BatchesPage.class);
        
        tester.assertComponent("batchesPanel:form:batchesPanel", GeoServerTablePanel.class);
        tester.assertComponent("batchesPanel:dialog", GeoServerDialog.class);
        
        tester.assertComponent("batchesPanel:addNew", AjaxLink.class);
        tester.assertComponent("batchesPanel:removeSelected", AjaxLink.class);
    }    

    @Test
    public void testBatches() throws Exception {        
        BatchesPage page = new BatchesPage();
        
        Batch dummy1 = dao.save(dummyBatch1());
                        
        List<Batch> Batches = dao.getBatches(false);
        
        tester.startPage(page);        

        @SuppressWarnings("unchecked")
                GeoServerTablePanel<Batch> table = (GeoServerTablePanel<Batch>) 
                tester.getComponentFromLastRenderedPage("batchesPanel:form:batchesPanel");
        
        assertEquals(Batches.size(), table.getDataProvider().size());
        assertTrue(containsConfig(getBatchesFromTable(table), dummy1));  
         
        Batch dummy2 = dao.save(dummyBatch2());
        
        assertEquals(Batches.size() + 1, table.getDataProvider().size());    
        assertTrue(containsConfig(getBatchesFromTable(table), dummy2));
        
        dao.delete(dummy1);
        dao.delete(dummy2);
        
    }
    
    @Test
    public void testNew() {
        login();
        
        BatchesPage page = new BatchesPage();
        tester.startPage(page);        
        
        tester.clickLink("batchesPanel:addNew", true);
                
        tester.assertRenderedPage(BatchPage.class);
    }
    
    @Test
    public void testDelete() throws Exception {
        BatchesPage page = new BatchesPage();
        tester.startPage(page);   
        
        @SuppressWarnings("unchecked")
        GeoServerTablePanel<Batch> table = (GeoServerTablePanel<Batch>) 
            tester.getComponentFromLastRenderedPage("batchesPanel:form:batchesPanel");
                
        Batch dummy1 = dao.save(dummyBatch1());
        Batch dummy2 = dao.save(dummyBatch2());
                                
        assertTrue(containsConfig(dao.getBatches(false), dummy1));  
        assertTrue(containsConfig(dao.getBatches(false), dummy2));
        
        //sort descending on name
        tester.clickLink("batchesPanel:form:batchesPanel:listContainer:sortableLinks:1:header:link", true);
        tester.clickLink("batchesPanel:form:batchesPanel:listContainer:sortableLinks:1:header:link", true);
        
        //select
        CheckBox selector = ((CheckBox) tester.getComponentFromLastRenderedPage("batchesPanel:form:batchesPanel:listContainer:items:3:selectItemContainer:selectItem"));
        tester.getRequest().setParameter(selector.getInputName(), "true");
        tester.executeAjaxEvent(selector, "click");
                
        assertEquals(1, table.getSelection().size());        
        assertEquals(dummy1.getId(), table.getSelection().get(0).getId());
        
        //click delete
        ModalWindow w  = (ModalWindow) tester.getComponentFromLastRenderedPage("batchesPanel:dialog:dialog");
        assertFalse(w.isShown());            
        tester.clickLink("batchesPanel:removeSelected", true);
        assertTrue(w.isShown());
                
        //confirm      
        tester.executeAjaxEvent("batchesPanel:dialog:dialog:content:form:submit", "click");    

        assertFalse(containsConfig(dao.getBatches(false), dummy1));
        assertTrue(containsConfig(dao.getBatches(false), dummy2));
        
        assertFalse(containsConfig(getBatchesFromTable(table), dummy1));
        assertTrue(containsConfig(getBatchesFromTable(table), dummy2));
        
    }
    
    protected List<Batch> getBatchesFromTable(GeoServerTablePanel<Batch> table) {
        List<Batch> result = new ArrayList<Batch>();
        Iterator<Batch> it = table.getDataProvider().iterator(0, table.size());
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
        
    }
    
    protected boolean containsConfig(Collection<Batch> coll, Batch config) {
        for (Batch c : coll) {
            if (config.getId().equals(c.getId())) {
                return true;
            }
        }
        return false;
    }
}
