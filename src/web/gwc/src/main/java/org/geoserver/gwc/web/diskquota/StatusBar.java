/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.diskquota;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

public class StatusBar extends Panel {

    private static final long serialVersionUID = 1L;

    public StatusBar(
            final String id,
            final IModel<Number> limitModel,
            final IModel<Number> progressModel,
            final IModel<String> progressMessageModel) {
        super(id);
        setOutputMarkupId(true);
        add(
                new Behavior() {
                    private static final long serialVersionUID = -8058471260136015254L;

                    @Override
                    public void renderHead(Component component, IHeaderResponse response) {
                        response.render(
                                CssHeaderItem.forReference(
                                        new PackageResourceReference(
                                                StatusBar.class, "statusbar.css")));
                    }
                });

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

        usageBar.add(
                new AttributeModifier(
                        "style",
                        new Model<String>(
                                "width: "
                                        + usedPercentage
                                        + "px; left: 5px; border-left: inherit;")));

        String redStyle =
                "width: " + excessPercentage + "px; left: " + (5 + usedPercentage) + "px;";
        excessBar.add(new AttributeModifier("style", new Model<String>(redStyle)));

        add(usageBar);
        add(excessBar);
        add(new Label("progressLabel", progressMessageModel));

        // TODO:make the argument models truly dynamic
        // add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5)));
    }
}
