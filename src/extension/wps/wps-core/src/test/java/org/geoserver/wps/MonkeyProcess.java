/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
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
import org.jdom.Text;
import org.opengis.util.ProgressListener;

@DescribeProcess(title = "Monkey", description = "Process used to test asynch calls")
public class MonkeyProcess {

    enum CommandType {
        Exit, SetProgress, Exception
    }

    static Map<String, BlockingQueue<Command>> commands = new ConcurrentHashMap<String, BlockingQueue<MonkeyProcess.Command>>();

    private static class Command {
        CommandType type;

        Object value;

        public Command(CommandType type, Object value) {
            this.type = type;
            this.value = value;
        }

    }

    public static void exit(String id, SimpleFeatureCollection value, boolean wait) throws InterruptedException {
        getCommandQueue(id).put(new Command(CommandType.Exit, value));
        if(wait) {
            while(getCommandQueue(id).size() > 0) {
                Thread.sleep(10);
            }
        }
    }

    private synchronized static BlockingQueue<Command> getCommandQueue(String id) {
        BlockingQueue<Command> queue = commands.get(id);
        if(queue == null) {
            queue = new LinkedBlockingQueue<MonkeyProcess.Command>();
            commands.put(id, queue);
        }
        
        return queue;
    }

    public static void progress(String id, float progress, boolean wait) throws InterruptedException {
        getCommandQueue(id).put(new Command(CommandType.SetProgress, progress));
        if(wait) {
            while(getCommandQueue(id).size() > 0) {
                Thread.sleep(10);
            }
        }

    }

    public static void exception(String id, ProcessException exception, boolean wait) throws InterruptedException {
        getCommandQueue(id).put(new Command(CommandType.Exception, exception));
        if(wait) {
            while(getCommandQueue(id).size() > 0) {
                Thread.sleep(10);
            }
        }
    }
    
    @DescribeResult(name="result")
    public SimpleFeatureCollection execute(@DescribeParameter(name = "id") String id, ProgressListener listener) throws Exception {
        while (true) {
            Command command = getCommandQueue(id).take();
            if (command.type == CommandType.Exit) {
                commands.remove(id);
                return (SimpleFeatureCollection) command.value;
            } else if (command.type == CommandType.SetProgress) {
                float progress = ((Number) command.value).floatValue();
                listener.progress(progress);
                listener.setTask(new SimpleInternationalString("Currently at " + progress));
            } else {
                ProcessException exception = (ProcessException) command.value;
                listener.exceptionOccurred(exception);
                throw exception;
            }
        }
    }

    static final ProcessFactory getFactory() {
        return new AnnotatedBeanProcessFactory(new SimpleInternationalString("Monkey process"),
                "gs", MonkeyProcess.class);
    }

    public static void clearCommands() {
        for (Map.Entry<String, BlockingQueue<MonkeyProcess.Command>> entry : commands.entrySet()) {
            if(entry.getValue().size() > 0) {
                throw new IllegalStateException("The command queue is not clean, queue " + entry.getKey() + " still has commands in: " + entry.getValue());
            }
        }

        commands.clear();
    }
}
