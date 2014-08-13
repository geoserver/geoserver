/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.web.data;

import gmx.iderc.geoserver.tjs.web.TJSWicketTestSupport;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public class DataStoreEditPageTest extends TJSWicketTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        try {
            login();
            tester.startPage(new DataStoreEditPage("tocororo"));
        } catch (Exception ex) {
            Logger.getLogger(DataStoreEditPageTest.class.getName()).log(Level.SEVERE, "Excepcion en setUpInternal(): " + ex.getMessage());
            throw ex;
        }
//        tester.assertRenderedPage(FrameworkPage.class);
//        tester.assertNoErrorMessage();
    }


    public void testBasicActions() {
        // test that we can load the page
        //System.out.println(tester.getLastRenderedPage().toString(true));
        // check it has one framework
    }

}
