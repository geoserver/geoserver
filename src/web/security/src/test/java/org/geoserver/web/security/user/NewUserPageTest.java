package org.geoserver.web.security.user;

import java.util.Locale;

import org.springframework.security.userdetails.UserDetails;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.impl.GeoserverUserDao;
import org.geoserver.web.GeoServerWicketTestSupport;

public class NewUserPageTest extends GeoServerWicketTestSupport {

    
    private GeoserverUserDao dao;

    @Override
    protected void setUpInternal() throws Exception {
        dao = GeoserverUserDao.get();
        login();
        tester.startPage(new NewUserPage());
    }
    
    public void testRenders() {
        tester.assertRenderedPage(NewUserPage.class);
    }

    public void testFill() throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        
        // make sure the recorder is where we think it is, it contains the palette selection
        tester.assertComponent("userForm:roles:roles:recorder", Recorder.class);
        
        // try to add a new user
        FormTester form = tester.newFormTester("userForm");
        form.setValue("username", "user");
        form.setValue("password", "pwd");
        form.setValue("confirmPassword", "pwd");
        // note: use a known role, there is no way to add a new role using wickettester support
        form.setValue("roles:roles:recorder", dao.getRoles().get(0));
        form.submit("save");
        
        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(UserPage.class);
        
        dao.reload();
        UserDetails user = dao.loadUserByUsername("user");
        assertEquals("pwd", user.getPassword());
        assertEquals(1, user.getAuthorities().length);
    }
    
    public void testPasswordsDontMatch() {
        Locale.setDefault(Locale.ENGLISH);
        
        FormTester form = tester.newFormTester("userForm");
        form.setValue("username", "user");
        form.setValue("password", "pwd1");
        form.setValue("confirmPassword", "pwd2");
        form.setValue("roles:roles:recorder", dao.getRoles().get(0));
        form.submit("save");
        
        tester.assertErrorMessages(new String[] {"'pwd1' from Password and 'pwd2' from Confirm password must be equal."});
        tester.assertRenderedPage(NewUserPage.class);
    }

}
