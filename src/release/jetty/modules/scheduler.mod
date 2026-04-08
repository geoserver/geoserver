[description]
Configures the Scheduler used by various components.

[tags]
scheduler
server

[depends]
logging

[lib]
lib/jetty-util-${jetty.version}.jar

[xml]
etc/jetty-scheduler.xml

[ini-template]
# tag::documentation[]
## The scheduler thread name, defaults to "Scheduler-{hashCode()}" if blank.
# jetty.scheduler.name=

## Whether the server scheduler threads are daemon.
# jetty.scheduler.daemon=false

## The number of server scheduler threads.
# jetty.scheduler.threads=1
# end::documentation[]
