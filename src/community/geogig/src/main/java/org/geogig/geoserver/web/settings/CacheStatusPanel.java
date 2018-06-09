/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.settings;

import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.storage.cache.CacheManagerBean;

public class CacheStatusPanel extends Panel {

    private static final long serialVersionUID = 7732030199323990637L;

    private static final Logger LOGGER = Logging.getLogger(CacheStatusPanel.class);

    private Component tableFragment;

    public CacheStatusPanel(String id) {
        super(id, new CacheManagerBeanModel());
        initUI();
    }

    public void initUI() {
        @SuppressWarnings("unchecked")
        IModel<CacheManagerBean> model = (IModel<CacheManagerBean>) getDefaultModel();

        this.tableFragment = createTableFragment(model);
        createFormFragment(model);
    }

    private void createFormFragment(IModel<CacheManagerBean> model) {

        Fragment formFragment = new Fragment("settings", "settingsFragment", this);
        formFragment.setOutputMarkupId(true);
        add(formFragment);

        Form<CacheManagerBean> form = new Form<>("settingsForm", model);
        formFragment.add(form);

        IModel<Double> absoluteMaximumSizeMB = new PropertyModel<>(model, "absoluteMaximumSizeMB");
        form.add(new Label("absoluteMaximumSizeMB", absoluteMaximumSizeMB));

        final NumberTextField<Double> maxSizeTextField;
        maxSizeTextField =
                new NumberTextField<Double>(
                        "maximumSize", new PropertyModel<>(model, "maximumSizeMB"));
        maxSizeTextField.setOutputMarkupId(true);
        maxSizeTextField.setRequired(true);
        maxSizeTextField.setMinimum(0d);
        maxSizeTextField.setMaximum(absoluteMaximumSizeMB);
        form.add(maxSizeTextField);

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        form.add(
                new AjaxSubmitLink("submit", form) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onAfterSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        target.add(tableFragment);
                        feedback.setVisible(false);
                        target.add(feedback);
                        feedback.setVisible(true);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        feedback.setVisible(true);
                        target.add(feedback);
                    }
                });

        form.add(
                new AjaxLink<CacheManagerBean>("reset", model) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CacheManagerBean manager = form.getModelObject();
                        final double maximumSizeMB = manager.getMaximumSizeMB();
                        final double defaultMaxSizeMB = manager.getDefaultSizeMB();
                        if (maximumSizeMB != defaultMaxSizeMB) {
                            manager.setMaximumSizeMB(defaultMaxSizeMB);
                        }
                        maxSizeTextField.clearInput();
                        target.add(maxSizeTextField);
                        target.add(tableFragment);
                        feedback.setVisible(false);
                        target.add(feedback);
                        feedback.setVisible(true);
                    }
                });
    }

    private Fragment createTableFragment(IModel<CacheManagerBean> model) {
        Fragment tableFragment = new Fragment("table", "tableFragment", this);
        tableFragment.setOutputMarkupId(true);
        add(tableFragment);

        tableFragment.add(new Label("size", new PropertyModel<>(model, "size")));
        tableFragment.add(new Label("sizeMB", new PropertyModel<>(model, "sizeMB")));
        tableFragment.add(new Label("maximumSizeMB", new PropertyModel<>(model, "maximumSizeMB")));
        tableFragment.add(
                new Label("maximumSizePercent", new PropertyModel<>(model, "maximumSizePercent")));
        tableFragment.add(
                new Label(
                        "absoluteMaximumSizeMB",
                        new PropertyModel<>(model, "absoluteMaximumSizeMB")));
        tableFragment.add(new Label("defaultSizeMB", new PropertyModel<>(model, "defaultSizeMB")));
        tableFragment.add(new Label("evictionCount", new PropertyModel<>(model, "evictionCount")));
        tableFragment.add(new Label("hitCount", new PropertyModel<>(model, "hitCount")));
        tableFragment.add(new Label("hitRate", new PropertyModel<>(model, "hitRate")));
        tableFragment.add(new Label("missCount", new PropertyModel<>(model, "missCount")));
        tableFragment.add(new Label("missRate", new PropertyModel<>(model, "missRate")));

        add(
                new AjaxLink<Void>("refresh") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        target.add(tableFragment);
                    }
                });

        add(
                new AjaxLink<CacheManagerBean>("clear", model) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        CacheManagerBean manager = getModelObject();
                        manager.clear();
                        target.add(tableFragment);
                    }
                });

        return tableFragment;
    }
}
