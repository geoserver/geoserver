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
import org.apache.wicket.Page;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.geoserver.taskmanager.AbstractWicketTaskManagerTest;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.web.model.BatchesModel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractBatchesPanelTest<T extends Page>
        extends AbstractWicketTaskManagerTest {

    protected TaskManagerFactory fac;
    protected TaskManagerDao dao;

    @Before
    public void before() {
        fac = TaskManagerBeans.get().getFac();
        dao = TaskManagerBeans.get().getDao();
        login();
    }

    @After
    public void after() {
        logout();
    }

    protected abstract Configuration getConfiguration();

    protected abstract Collection<Batch> getBatches();

    protected abstract T newPage();

    protected abstract String prefix();

    protected Batch dummyBatch1() {
        Batch batch = fac.createBatch();
        batch.setName("Z-BATCH");
        batch.setConfiguration(getConfiguration());
        return batch;
    }

    protected Batch dummyBatch2() {
        Batch batch = fac.createBatch();
        batch.setName("A-BATCH");
        batch.setConfiguration(getConfiguration());
        return batch;
    }

    @Test
    public void testPage() {
        T page = newPage();

        tester.startPage(page);

        tester.assertComponent(
                prefix() + "batchesPanel:form:batchesPanel", GeoServerTablePanel.class);
        tester.assertComponent(prefix() + "batchesPanel:dialog", GeoServerDialog.class);

        tester.assertComponent(prefix() + "batchesPanel:addNew", AjaxLink.class);
        tester.assertComponent(prefix() + "batchesPanel:removeSelected", AjaxLink.class);
    }

    @Test
    public void testBatches() throws Exception {
        Batch dummy1 = dao.save(dummyBatch1());
        Batch dummy2 = dao.save(dummyBatch2());

        T page = newPage();

        Collection<Batch> batches = getBatches();

        tester.startPage(page);

        @SuppressWarnings("unchecked")
        GeoServerTablePanel<Batch> table =
                (GeoServerTablePanel<Batch>)
                        tester.getComponentFromLastRenderedPage(
                                prefix() + "batchesPanel:form:batchesPanel");

        assertEquals(batches.size(), table.getDataProvider().size());
        assertTrue(containsBatch(getBatchesFromTable(table), dummy1));
        assertTrue(containsBatch(getBatchesFromTable(table), dummy2));

        dao.delete(dummy1);
        dao.delete(dummy2);
    }

    @Test
    public void testNew() {
        login();

        T page = newPage();
        tester.startPage(page);

        tester.clickLink(prefix() + "batchesPanel:addNew");

        tester.assertRenderedPage(BatchPage.class);

        logout();
    }

    @Test
    public void testEdit() {
        login();

        Batch dummy1 = dao.save(dummyBatch1());

        T page = newPage();
        tester.startPage(page);

        tester.clickLink(
                prefix()
                        + "batchesPanel:form:batchesPanel:listContainer:items:1:itemProperties:1:component:link");

        tester.assertRenderedPage(BatchPage.class);

        tester.assertModelValue("batchForm:name", dummy1.getName());

        dao.delete(dummy1);

        logout();
    }

    @Test
    public void testDelete() throws Exception {

        login();

        Batch dummy1 = dao.save(dummyBatch1());
        Batch dummy2 = dao.save(dummyBatch2());

        T page = newPage();
        tester.startPage(page);

        @SuppressWarnings("unchecked")
        GeoServerTablePanel<Batch> table =
                (GeoServerTablePanel<Batch>)
                        tester.getComponentFromLastRenderedPage(
                                prefix() + "batchesPanel:form:batchesPanel");

        assertTrue(containsBatch(getBatches(), dummy1));
        assertTrue(containsBatch(getBatches(), dummy2));

        // sort descending on name
        tester.clickLink(
                prefix()
                        + "batchesPanel:form:batchesPanel:listContainer:sortableLinks:1:header:link");
        tester.clickLink(
                prefix()
                        + "batchesPanel:form:batchesPanel:listContainer:sortableLinks:1:header:link");

        // select
        CheckBox selector =
                ((CheckBox)
                        tester.getComponentFromLastRenderedPage(
                                prefix()
                                        + "batchesPanel:form:batchesPanel:listContainer:items:1:selectItemContainer:selectItem"));
        tester.getRequest().setParameter(selector.getInputName(), "true");
        tester.executeAjaxEvent(selector, "click");

        assertEquals(1, table.getSelection().size());
        assertEquals(dummy1.getId(), table.getSelection().get(0).getId());

        // click delete
        ModalWindow w =
                (ModalWindow)
                        tester.getComponentFromLastRenderedPage(
                                prefix() + "batchesPanel:dialog:dialog");
        assertFalse(w.isShown());
        tester.clickLink(prefix() + "batchesPanel:removeSelected");
        assertTrue(w.isShown());

        // confirm
        tester.executeAjaxEvent(
                prefix() + "batchesPanel:dialog:dialog:content:form:submit", "click");

        assertFalse(containsBatch(getBatches(), dummy1));
        assertTrue(containsBatch(getBatches(), dummy2));

        ((BatchesModel) table.getDataProvider()).reset();

        assertFalse(containsBatch(getBatchesFromTable(table), dummy1));
        assertTrue(containsBatch(getBatchesFromTable(table), dummy2));

        dao.delete(dummy2);

        logout();
    }

    protected List<Batch> getBatchesFromTable(GeoServerTablePanel<Batch> table) {
        List<Batch> result = new ArrayList<Batch>();
        Iterator<Batch> it = table.getDataProvider().iterator(0, table.size());
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    protected boolean containsBatch(Collection<Batch> coll, Batch batch) {
        for (Batch c : coll) {
            if (batch.getId().equals(c.getId())) {
                return true;
            }
        }
        return false;
    }
}
