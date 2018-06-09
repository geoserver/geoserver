/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.factory.AnnotatedBeanProcessFactory;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.SimpleInternationalString;
import org.opengis.util.ProgressListener;

@DescribeProcess(title = "Monkey", description = "Process used to test asynch calls")
public class MonkeyProcess {

    enum CommandType {
        Exit,
        SetProgress,
        Exception,
        Wait
    }

    static Map<String, BlockingQueue<Command>> commands =
            new ConcurrentHashMap<String, BlockingQueue<MonkeyProcess.Command>>();

    private static class Command {
        CommandType type;

        Object value;

        public Command(CommandType type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    public static void exit(String id, SimpleFeatureCollection value, boolean wait)
            throws InterruptedException {
        getCommandQueue(id).put(new Command(CommandType.Exit, value));
        if (wait) {
            while (getCommandQueue(id).size() > 0) {
                Thread.sleep(10);
            }
        }
    }

    private static synchronized BlockingQueue<Command> getCommandQueue(String id) {
        BlockingQueue<Command> queue = commands.get(id);
        if (queue == null) {
            queue = new LinkedBlockingQueue<MonkeyProcess.Command>();
            commands.put(id, queue);
        }

        return queue;
    }

    public static void progress(String id, float progress, boolean wait)
            throws InterruptedException {
        getCommandQueue(id).put(new Command(CommandType.SetProgress, progress));
        if (wait) {
            while (getCommandQueue(id).size() > 0) {
                Thread.sleep(10);
            }
        }
    }

    public static void wait(String id, long wait) throws InterruptedException {
        getCommandQueue(id).put(new Command(CommandType.Wait, wait));
    }

    public static void exception(String id, ProcessException exception, boolean wait)
            throws InterruptedException {
        getCommandQueue(id).put(new Command(CommandType.Exception, exception));
        if (wait) {
            while (getCommandQueue(id).size() > 0) {
                Thread.sleep(10);
            }
        }
    }

    @DescribeResult(name = "result")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "id") String id,
            @DescribeParameter(name = "fc", min = 0) SimpleFeatureCollection fc,
            @DescribeParameter(name = "extra", min = 0) String extra,
            ProgressListener listener)
            throws Exception {
        BlockingQueue<Command> queue = getCommandQueue(id);
        while (true) {
            Command command = queue.take();
            if (command.type == CommandType.Exit) {
                listener.progress(100f);
                listener.complete();
                commands.remove(id);
                return (SimpleFeatureCollection) command.value;
            } else if (command.type == CommandType.SetProgress) {
                float progress = ((Number) command.value).floatValue();
                listener.progress(progress);
                listener.setTask(new SimpleInternationalString("Currently at " + progress));
            } else if (command.type == CommandType.Wait) {
                long wait = ((Number) command.value).longValue();
                Thread.sleep(wait);
            } else {
                ProcessException exception = (ProcessException) command.value;
                listener.exceptionOccurred(exception);
                throw exception;
            }
        }
    }

    public static final ProcessFactory getFactory() {
        return new MonkeyProcessFactory();
    }

    public static void clearCommands() {
        for (Map.Entry<String, BlockingQueue<MonkeyProcess.Command>> entry : commands.entrySet()) {
            if (entry.getValue().size() > 0) {
                throw new IllegalStateException(
                        "The command queue is not clean, queue "
                                + entry.getKey()
                                + " still has commands in: "
                                + entry.getValue());
            }
        }

        commands.clear();
    }

    private static class MonkeyProcessFactory extends AnnotatedBeanProcessFactory {

        public MonkeyProcessFactory() {
            super(new SimpleInternationalString("Monkey process"), "gs", MonkeyProcess.class);
        }
    }
}
