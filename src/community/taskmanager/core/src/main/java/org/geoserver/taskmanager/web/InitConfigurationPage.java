package org.geoserver.taskmanager.web;

import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.util.InitConfigUtil;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.web.GeoServerSecuredPage;

public class InitConfigurationPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = -1979472322459593225L;

    private IModel<Configuration> configurationModel;

    public InitConfigurationPage(IModel<Configuration> configurationModel) {
        this.configurationModel = configurationModel;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        final String schedulerReference =
                TaskManagerBeans.get()
                        .getBjService()
                        .scheduleNow(InitConfigUtil.getInitBatch(configurationModel.getObject()));

        if (schedulerReference == null) { // empty batch
            setResponsePage(
                    new ConfigurationPage(InitConfigUtil.unwrap(configurationModel.getObject())));
        } else {
            add(
                    new AbstractAjaxTimerBehavior(Duration.seconds(1)) {

                        private static final long serialVersionUID = -8006498530965431853L;

                        @Override
                        protected void onTimer(AjaxRequestTarget target) {
                            BatchRun br =
                                    TaskManagerBeans.get()
                                            .getDao()
                                            .getBatchRunBySchedulerReference(schedulerReference);

                            if (br != null && br.getStatus().isClosed()) {
                                // reload page
                                setResponsePage(
                                        new ConfigurationPage(
                                                TaskManagerBeans.get()
                                                        .getDao()
                                                        .init(
                                                                InitConfigUtil.unwrap(
                                                                        configurationModel
                                                                                .getObject()))));
                            }
                        }
                    });
        }
    }
}
