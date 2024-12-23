/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.diskquota;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

public class StatusBar extends Panel {

    private static final long serialVersionUID = 1L;

    private final String script;

    public StatusBar(
            final String id,
            final IModel<Number> limitModel,
            final IModel<Number> progressModel,
            final IModel<String> progressMessageModel) {
        super(id);
        setOutputMarkupId(true);

        WebMarkupContainer usageBar = new WebMarkupContainer("statusBarProgress");
        WebMarkupContainer excessBar = new WebMarkupContainer("statusBarExcess");

        final double limit = limitModel.getObject().doubleValue();
        final double used = progressModel.getObject().doubleValue();
        final double excess = used - limit;

        int usedPercentage;
        int excessPercentage;

        final int progressWidth = 200; // progress bar with, i.e. 100%

        if (excess > 0) {
            excessPercentage = (int) Math.round((excess * progressWidth) / used);
            usedPercentage = progressWidth - excessPercentage;
        } else {
            usedPercentage = (int) Math.round(used * progressWidth / limit);
            excessPercentage = 0;
        }

        this.script = ""
                + "document.getElementsByClassName('statusBarProgress')[0].style.width = '"
                + usedPercentage
                + "px';\n"
                + "document.getElementsByClassName('statusBarExcess')[0].style.width = '"
                + excessPercentage
                + "px';\n"
                + "document.getElementsByClassName('statusBarExcess')[0].style.left = '"
                + (5 + usedPercentage)
                + "px';";

        add(usageBar);
        add(excessBar);
        add(new Label("progressLabel", progressMessageModel));

        // TODO:make the argument models truly dynamic
        // add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5)));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new PackageResourceReference(StatusBar.class, "statusbar.css")));
        response.render(OnLoadHeaderItem.forScript(this.script));
    }
}
