/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.security.ConcurrentAuthenticationException;
import org.geoserver.web.wicket.ParamResourceModel;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;

/**
 * This is a simple login form shown when the user tries to access a secured page directly
 *
 * @author aaime
 */
public class GeoServerLoginPage extends GeoServerBasePage {

    public GeoServerLoginPage(PageParameters parameters) {
        // avoid showing two login forms
        if (get("loginform") != null) {
            get("loginform").setVisible(false);
        }

        TextField field = new TextField("username");

        // TODO: (from the spring security sources):  @deprecated If you want to retain the
        // username, cache it in a customized {@code AuthenticationFailureHandler}
        // String lastUserName = (String)
        // session.getAttribute(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_LAST_USERNAME_KEY);
        // field.setModel(new Model(lastUserName));
        field.setModel(new Model());
        add(field);

        try {
            if (parameters.get("error").toBoolean()) {
                Exception exception = getAuthenticationException();
                if (exception instanceof ConcurrentAuthenticationException) {
                    ConcurrentAuthenticationException cae =
                            (ConcurrentAuthenticationException) exception;
                    error(
                            new ParamResourceModel(
                                            "concurrentAuthenticationError", this, cae.getCount())
                                    .getString());
                } else {
                    error(new ParamResourceModel("error", this).getString());
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private AuthenticationException getAuthenticationException() {
        Request request = getRequest();
        if (request == null || !(request.getContainerRequest() instanceof HttpServletRequest)) {
            return null;
        }
        HttpServletRequest hr = (HttpServletRequest) request.getContainerRequest();
        HttpSession session = hr.getSession(false);
        if (session == null) {
            return null;
        }

        Object exception = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (exception instanceof AuthenticationException) {
            return (AuthenticationException) exception;
        } else {
            return null;
        }
    }
}
