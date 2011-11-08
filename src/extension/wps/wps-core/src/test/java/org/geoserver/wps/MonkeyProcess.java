package org.geoserver.wps;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.geoserver.wps.jts.AnnotatedBeanProcessFactory;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.util.SimpleInternationalString;
import org.opengis.util.ProgressListener;

@org.geoserver.wps.jts.DescribeProcess(title = "Monkey", description = "Process used to test asynch calls")
public class MonkeyProcess {

    enum CommandType {
        Exit, SetProgress, Exception
    }

    static BlockingQueue<Command> commands = new LinkedBlockingQueue<MonkeyProcess.Command>();

    private static class Command {
        CommandType type;

        Object value;

        public Command(CommandType type, Object value) {
            this.type = type;
            this.value = value;
        }

    }

    public static void exit(String value, boolean wait) throws InterruptedException {
        commands.offer(new Command(CommandType.Exit, value));
        if(wait) {
            while(commands.size() > 0) {
                Thread.sleep(10);
            }
        }
    }

    public static void progress(float progress, boolean wait) throws InterruptedException {
        commands.offer(new Command(CommandType.SetProgress, progress));
        if(wait) {
            while(commands.size() > 0) {
                Thread.sleep(10);
            }
        }

    }

    public static void exception(ProcessException exception, boolean wait) throws InterruptedException {
        commands.offer(new Command(CommandType.Exception, exception));
        if(wait) {
            while(commands.size() > 0) {
                Thread.sleep(10);
            }
        }
    }
    
    @DescribeResult(name="result")
    public String execute(ProgressListener listener) throws InterruptedException, ProcessException {
        while (true) {
            Command command = commands.take();
            if (command.type == CommandType.Exit) {
                return (String) command.value;
            } else if (command.type == CommandType.SetProgress) {
                listener.progress(((Number) command.value).floatValue());
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
}
