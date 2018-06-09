/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.StringResourceModel;

/**
 * Link that pops up a dialog containing contextual help for a component.
 *
 * <p>Usage:
 *
 * <pre>
 *   new HelpLink("theHelpLinkId", getPage()).setDialog(thePopupDialog)
 * </pre>
 *
 * <pre>
 *   &lt;fieldset>
 *     &lt;legend>
 *         &lt;span>...&lt;/span>
 *         &lt;a href="#" wicket:id="theHelpLinkId" class="help-link">&lt;/a>
 *       &lt;/legend>
 *   &lt;/fieldset>
 * </pre>
 *
 * <p>Help content of the dialog is looked up as a resource in the i18n
 * GEoServerApplication.properties file. One key is looked up for the help title and one for the
 * help content itself:
 *
 * <pre>
 * <containerName>.<id>.help.title=...
 * <containerName>.<id>.help=...
 * </pre>
 *
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("serial")
public class HelpLink extends AjaxLink<Void> {

    GeoServerDialog dialog;
    Component container;

    /**
     * Creates a new help link.
     *
     * @param id The link id, this value is used to generate lookup keys.
     */
    public HelpLink(String id) {
        this(id, null);
    }

    /**
     * Creates a new help link.
     *
     * @param id The link id, this value is used to generate lookup keys.
     * @param container Explicit container element from which lookup keys should be relative to, if
     *     not specified (ie null) then the containing page is used via getPage()
     */
    public HelpLink(String id, Component container) {
        super(id);
        this.container = container;
    }

    /** Sets the dialog upon which to display the help. */
    public HelpLink setDialog(GeoServerDialog dialog) {
        this.dialog = dialog;
        return this;
    }

    Component getContainer() {
        return container != null ? container : getPage();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onClick(AjaxRequestTarget target) {
        // load the help title
        StringResourceModel heading =
                new StringResourceModel(getId() + ".title", getContainer(), null);
        StringResourceModel content = new StringResourceModel(getId(), getContainer(), null);

        dialog.showInfo(target, heading, content);
    }
}
