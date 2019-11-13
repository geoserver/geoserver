/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.panel;

import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidator;
import org.geoserver.web.wicket.browser.DirectoryInput;
import org.geoserver.web.wicket.browser.FileInput;

/**
 * A label, a text field, a directory chooser.
 *
 * @author Andrea Aime
 */
public class DirectoryParamPanel extends FileParamPanel {

    private static final long serialVersionUID = -8317791966175845831L;

    /**
     * @param validators any extra validator that should be added to the input field, or {@code
     *     null}
     */
    @SafeVarargs
    public DirectoryParamPanel(
            final String id,
            final IModel<String> paramValue,
            final IModel<String> paramLabelModel,
            final boolean required,
            IValidator<? super String>... validators) {
        // make the value of the text field the model of this panel, for easy value retrieval
        super(id, paramValue, paramLabelModel, required, validators);
    }

    @Override
    protected FileInput getFilePathInput(
            IModel<String> paramValue,
            IModel<String> paramLabelModel,
            boolean required,
            IValidator<? super String>[] validators) {
        // the file chooser
        return new DirectoryInput("fileInput", paramValue, paramLabelModel, required, validators);
    }
}
