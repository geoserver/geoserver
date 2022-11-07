/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.util.UUID;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;

/**
 * A password text field that obfuscates the password in the HTML source and does not reset the
 * value when the form is edited.
 */
public class PasswordTextFieldWriteOnlyModel extends PasswordTextField {
    /**
     * Used so that someone with access to the browser cannot read the HTML source of the page and
     * get the password. It replaces it with random text but updates the original value on write.
     *
     * @param id the component id
     * @param model the model
     */
    public PasswordTextFieldWriteOnlyModel(String id, IModel<String> model) {
        super(id, new WriteOnlyModel(model));
        this.setResetPassword(false);
    }

    /**
     * Model wraps component model so that someone with access to the browser cannot read the HTML
     * source of the page and get the password.
     */
    static class WriteOnlyModel implements IModel<String> {
        String fakePass = "_gs_pwd_" + UUID.randomUUID();
        IModel delegate;

        /**
         * Constructor to wrap the component model.
         *
         * @param delegate the model to wrap
         */
        public WriteOnlyModel(IModel<String> delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getObject() {
            return fakePass;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setObject(String object) {
            if (!fakePass.equals(object)) {
                delegate.setObject(object);
            }
        }

        @Override
        public void detach() {
            // nothing to do here
        }
    }
}
