/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.web.data;

import gmx.iderc.geoserver.tjs.web.TJSWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public class DataStorePageTest extends TJSWicketTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        try {
            login();
            tester.startPage(new DataStorePage());
        } catch (Exception ex) {
            Logger.getLogger(DataStorePageTest.class.getName()).log(Level.SEVERE, "Excepcion en setUpInternal(): " + ex.getMessage());
            throw ex;
        }
//        tester.assertRenderedPage(FrameworkPage.class);
//        tester.assertNoErrorMessage();
    }


    public void testBasicActions() {

//        if (!SecurityContextHolder.getContext().getAuthentication().isAuthenticated()){
//            Logger.getLogger(FrameworkPageTest.class.getName()).info("No se ha autenticado");
//            login();
//        }

        // test that we can load the page
        //System.out.println(tester.getLastRenderedPage().toString(true));
        // check it has one framework
        GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        System.out.println(String.valueOf(table.getDataProvider().size()) + " DataStores");
//        assertEquals(2, table.getDataProvider().size());
    }

}
