.. _monitor_kafka_configuration:

Kafka storage Configuration
===============================

Many aspects of the monitor extension are configurable. The configuration files
are stored in the data directory under the ``monitoring`` directory::

  <data_directory>
      monitoring/
          monitor.properties


In particular:

* **monitor.properties** - Can be extended to set the connection details to Apache Kafka.

Monitor Storage
---------------

How request data is persisted is configurable via the ``storage`` property defined in the 
``monitor.properties`` file. The following values are supported for the ``storage`` property:

* **memory** - Request data is to be persisted in memory alone.
* **hibernate** - Request data is to be persisted in a relational database via Hibernate.
* **kafka** - Request data is to be persisted in Apache Kafka.

The default value is ``memory``, in order to use Apache Kafka the ``storage`` configuration needs
to be switched to ``kafka``.

In addition you can set the topic name with the ``kafka.topic`` property. The default value is ``geoserver-monitor``.

You can set all the Kafka properties for a kafka producer by prefixing it with the ``kafka`` keyword e.g. set the acks to 1 with ``kafka.acks=1``.
For further details on the Kafka producer properties see the `Kafka documentation <https://kafka.apache.org/documentation.html#producerconfigs>`_.

The following is an example of the ``monitor.properties`` file configured to use Apache Kafka::

  storage=kafka
  ... other properties ...
  kafka.bootstrap.servers=localhost:9092
  kafka.topic=monitor
  kafka.acks=1
  kafka.retries=3
  kafka.batch.size=65536
  kafka.linger.ms=200
  kafka.compression.type=snappy
  kafka.schema.registry.url=http://localhost:8081


In order to use Confluent Cloud you need to configure these properties::

  storage=kafka
  ... other properties ...
  kafka.bootstrap.servers=pkc-def12.europe-west.gcp.confluent.cloud:9092
  kafka.topic=monitor
  kafka.security.protocol=SASL_SSL
  kafka.sasl.mechanism=PLAIN
  kafka.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username='KAFKA_API_KEY' password='KAFKA_SECRET';
  kafka.schema.registry.url=https://psrc-abc12.europe-west.gcp.confluent.cloud
  kafka.schema.registry.basic.auth.credentials.source=USER_INFO
  kafka.schema.registry.basic.auth.user.info=SR_API_KEY:SR_API_SECRET

  kafka.acks=1
  kafka.retries=0
  kafka.batch.size=65536
  kafka.linger.ms=200
  kafka.compression.type=snappy


It might be a good idea to set the ``kafka.linger.ms`` to avoid too many requests to the Kafka broker and get the benefits from batching and compression. One request can cause multiple messages to be sent to Kafka as a load of multiple tiles may does.
Also the compressions should be quite effective as the messages repeat some data. Make sure to also set the ``kafka.compression.type``.