/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.web.framework;

import gmx.iderc.geoserver.tjs.web.TJSWicketTestSupport;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */
public class FrameworkNewPageTest extends TJSWicketTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        try {
            login();
        } catch (Exception ex) {
            Logger.getLogger(FrameworkNewPageTest.class.getName()).log(Level.SEVERE, "Excepcion en setUpInternal(): " + ex.getMessage());
            throw ex;
        }
    }

    public void testBasicActions() {
        tester.startPage(new FrameworkNewPage());
        tester.assertRenderedPage(FrameworkNewPage.class);
        tester.assertNoErrorMessage();
    }

}
