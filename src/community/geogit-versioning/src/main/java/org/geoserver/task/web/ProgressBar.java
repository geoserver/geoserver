package org.geoserver.task.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class ProgressBar extends Panel {

    private static final long serialVersionUID = 1L;

    public ProgressBar(final String id, final IModel<Number> limitModel,
            final IModel<Number> progressModel, final IModel<String> progressMessageModel) {
        this(id, limitModel, progressModel, progressMessageModel, null);
    }

    public ProgressBar(final String id, final IModel<Number> limitModel,
            final IModel<Number> progressModel, final IModel<String> progressMessageModel,
            final IModel<String> tooltipModel) {

        super(id);
        setOutputMarkupId(true);

        WebMarkupContainer usageBar = new WebMarkupContainer("statusBarProgress");
        WebMarkupContainer excessBar = new WebMarkupContainer("statusBarExcess");

        final double limit = limitModel.getObject().doubleValue();
        final double used = progressModel.getObject().doubleValue();
        final double excess = used - limit;

        int usedPercentage;
        int excessPercentage;

        final int progressWidth = 100;// progress bar with, i.e. 100%

        if (excess > 0) {
            excessPercentage = (int) Math.round((excess * progressWidth) / used);
            usedPercentage = progressWidth - excessPercentage;
        } else {
            usedPercentage = (int) Math.round(used * progressWidth / limit);
            excessPercentage = 0;
        }

        String greenStyle = "width: " + usedPercentage + "px; left: 0px;";
        usageBar.add(new AttributeModifier("style", true, new Model<String>(greenStyle)));

        String redStyle = "width: " + excessPercentage + "px; left: " + usedPercentage + "px;";
        excessBar.add(new AttributeModifier("style", true, new Model<String>(redStyle)));

        add(usageBar);
        add(excessBar);
        Label progressMessage = new Label("progressLabel", progressMessageModel);
        add(progressMessage);
        if (tooltipModel != null) {
            progressMessage.add(new AttributeModifier("title", true, tooltipModel));
        }
    }
}
