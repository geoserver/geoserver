/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.markup.html.list.ListView;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.web.FormTestPage;

public abstract class AbstractConfirmRemovalPanelTest<T> extends AbstractSecurityWicketTestSupport
        implements Serializable {
    private static final long serialVersionUID = 1L;

    protected abstract void setupPanel(List<T> roots);

    protected abstract T getRemoveableObject() throws Exception;

    protected abstract T getProblematicObject() throws Exception;

    protected abstract String getProblematicObjectRegExp() throws Exception;

    protected abstract String getRemoveableObjectRegExp() throws Exception;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // disable url parameter encoding for these tests
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        config.setEncryptingUrlParams(false);
        getSecurityManager().saveSecurityConfig(config);
    }

    protected void removeObject() throws Exception {

        insertValues();

        T removeableObject = getRemoveableObject();
        assertNotNull(removeableObject);

        setupPanel(Collections.singletonList(removeableObject));

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        assertTrue(labelTextForRemovedObjects().matches(getRemoveableObjectRegExp()));

        tester.assertVisible("form:panel:removedObjects");
        tester.assertInvisible("form:panel:problematicObjects");

        T problematicObject = getProblematicObject();

        if (problematicObject != null) {

            setupPanel(Collections.singletonList(problematicObject));
            tester.assertRenderedPage(FormTestPage.class);
            tester.assertNoErrorMessage();

            assertTrue(labelTextForProblematicObjects().matches(getProblematicObjectRegExp()));

            tester.assertInvisible("form:panel:removedObjects");
            tester.assertVisible("form:panel:problematicObjects");
        }

        if (removeableObject != null && problematicObject != null) {
            List<T> objects = new ArrayList<T>();
            objects.add(removeableObject);
            objects.add(problematicObject);

            setupPanel(objects);
            tester.assertRenderedPage(FormTestPage.class);
            tester.assertNoErrorMessage();

            assertTrue(labelTextForRemovedObjects().matches(getRemoveableObjectRegExp()));
            assertTrue(labelTextForProblematicObjects().matches(getProblematicObjectRegExp()));

            tester.assertVisible("form:panel:removedObjects");
            tester.assertVisible("form:panel:problematicObjects");
        }
    }

    protected String labelTextForRemovedObjects() {
        ListView list =
                (ListView)
                        tester.getComponentFromLastRenderedPage(
                                "form:panel:removedObjects:rulesRemoved:rules");

        return list.getDefaultModelObjectAsString();
    }

    protected String labelTextForProblematicObjects() {
        ListView list =
                (ListView)
                        tester.getComponentFromLastRenderedPage(
                                "form:panel:problematicObjects:rulesNotRemoved:problems");
        return list.getDefaultModelObjectAsString();
    }
}
