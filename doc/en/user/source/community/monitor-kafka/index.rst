.. _monitor_kafka_extension:

Monitoring Kafka storage
============================


The Monitor Kafka storage extension allows to track the requests made against a GeoServer instance
in an `Apache Kafka <https://kafka.apache.org/>`_  topic, as opposed to keeping the data in memory for a short time, or
logging it on a audit file.

.. toctree::
   :maxdepth: 2

   installation/
   configuration/
   usage/


:Limitations:

* Message keys are not supported. This means that the messages will be distributed in a round robbin
  fashion between the partitions.
* The extension tries to connect and describe the topic at startup, if the topic does not exist or the connection
  cannot be established the extension will fail to start and disable itself. Subsequent requests to
  this instance will not be logged. The timeout for the describe command is set to 10 seconds.
* The only used serialization format is avro. Avro might be used in other extensions like GeoMesa where a conflict with
  the version of kafka-avro library might be possible.

:Permissions:

The Kafka extension needs the following permissions on the Kafka topic in order to work:

- DESCRIBE
- WRITE

