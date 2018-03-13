package org.geoserver.taskmanager.web;

import java.util.logging.Level;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.util.InitConfigUtil;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.Trigger.TriggerState;

public class InitConfigurationPage extends GeoServerSecuredPage {
    
    private static final long serialVersionUID = -1979472322459593225L;
    
    private IModel<Configuration> configurationModel;    
    
    public InitConfigurationPage(IModel<Configuration> configurationModel) {
        this.configurationModel = configurationModel;
    }
    
    @Override
    public void onInitialize() {
        super.onInitialize();

        final Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(InitConfigUtil.getInitBatch(configurationModel.getObject()).getFullName())
                .startNow().build();

        try {
            GeoServerApplication.get().getBeanOfType(Scheduler.class).scheduleJob(trigger);
        } catch (SchedulerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            error(rootCause == null ? e.getLocalizedMessage() : rootCause.getLocalizedMessage());
        }

        add(new AbstractAjaxTimerBehavior(Duration.seconds(1)) {

            private static final long serialVersionUID = -8006498530965431853L;

            @Override
            protected void onTimer(AjaxRequestTarget target) {
                try {
                    TriggerState status = GeoServerApplication.get().getBeanOfType(Scheduler.class)
                            .getTriggerState(trigger.getKey());
                    if (status == TriggerState.COMPLETE || status == TriggerState.ERROR
                            || status == TriggerState.NONE) {
                        // reload page
                        setResponsePage(new ConfigurationPage(
                                InitConfigUtil.unwrap(configurationModel.getObject())));
                    }
                } catch (SchedulerException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        });
    }

}
