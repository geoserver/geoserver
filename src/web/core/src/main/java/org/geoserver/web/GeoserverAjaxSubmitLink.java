/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;

/**
 * {@link AjaxSubmitLink} subclass that adds the {@link GeoServerBasePage} feedback panels on every
 * interaction
 */
public abstract class GeoserverAjaxSubmitLink extends AjaxSubmitLink {

    private final GeoServerBasePage page;

    public GeoserverAjaxSubmitLink(String id, Form<?> form, GeoServerBasePage page) {
        super(id, form);
        this.page = page;
    }

    public GeoserverAjaxSubmitLink(String id, GeoServerBasePage page) {
        super(id);
        this.page = page;
    }

    @Override
    protected void onError(AjaxRequestTarget target, Form<?> form) {
        super.onError(target, form);
        page.addFeedbackPanels(target);
    }

    @Override
    protected final void onSubmit(AjaxRequestTarget target, Form<?> form) {
        try {
            onSubmitInternal(target, form);
        } finally {
            page.addFeedbackPanels(target);
        }
    }

    protected abstract void onSubmitInternal(AjaxRequestTarget target, Form<?> form);
}
