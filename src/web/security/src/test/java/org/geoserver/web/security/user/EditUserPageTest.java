package org.geoserver.web.security.user;

import org.geoserver.security.impl.GeoserverUserDao;
import org.geoserver.web.GeoServerWicketTestSupport;

public class EditUserPageTest extends GeoServerWicketTestSupport {

    
    private GeoserverUserDao dao;

    @Override
    protected void setUpInternal() throws Exception {
        dao = GeoserverUserDao.get();
        login();
        tester.startPage(new EditUserPage(dao.loadUserByUsername("admin")));
    }
    
    public void testRenders() {
        tester.assertRenderedPage(EditUserPage.class);
    }

//    public void testEditPassword() {
//        // make sure the recorder is where we think it is, it contains the palette selection
//        tester.assertComponent("userForm:roles:recorder", Recorder.class);
//        
//        FormTester form = tester.newFormTester("userForm");
//        form.setValue("password", "newpwd");
//        form.setValue("confirmPassword", "newpwd");
//        form.submit("save");
//        
//        tester.assertErrorMessages(new String[0]);
//        tester.assertRenderedPage(UserPage.class);
//        
//        dao.reload();
//        UserDetails user = dao.loadUserByUsername("admin");
//        assertEquals("newpwd", user.getPassword());
//    }
    

}
