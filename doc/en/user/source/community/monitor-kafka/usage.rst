.. _monitor_kafka_usage:

Usage of Monitoring Kafka extension
==========================================

To try out there is a ``docker-compose.yml`` file in the ``test/resources`` directory.
This will start up a Kafka broker and a schema registry instance.
To start it up you need docker and docker-compose installed then run::

    # start kafka
    cd src/community/monitor-kafka/src/test/resources
    docker-compose up -d

This will start up the Kafka broker and the schema registry.

Once Kafka is running start geoserver with monitor and monitor-kafka extension from ``src/web/app``::

     mvn jetty:run -P monitor,monitor-kafka

This will initialize the data directory in ``src/web/app/src/main/webapp/data`` and start up geoserver.
Stop the geoserver again.

With this you will get the default monitor config installed automatically in ``src/web/app/src/main/webapp/data/monitoring/``. Edit it to use the Kafka storage.

Then you need to configure the ``monitor-kafka`` extension for it with::

    storage=kafka
    kafka.bootstrap.servers=localhost:9092
    kafka.schema.registry.url=http://localhost:8081



Create your topic with::

    docker exec -ti broker kafka-topics --bootstrap-server localhost:9092 --create --topic geoserver-monitor --partitions 1 --replication-factor 1 --if-not-exists

Start geoserver again with::

     mvn jetty:run -P monitor,monitor-kafka


Check that the monitoring extension is actually enabled by looking for the following log line::

    INFO   [geoserver.monitor] - Kafka connection established and topic geoserver-monitor exists

Head over to http://localhost:8080/geoserver and hit some map, so you get some data into the topic.

If you want to consume the data you need a consumer which has the right Deserializer configured (``io.confluent.kafka.serializers.KafkaAvroDeserializer``).

The easiest way to do this is to enter the schema registry container with::


    docker exec -ti schema-registry bash
    # from within the container run:
      kafka-avro-console-consumer \
        --bootstrap-server broker:29092 \
        --topic geoserver-monitor \
        --from-beginning

   # or directly from the host machine so you can pipe it into jq or a file...
    docker exec -ti schema-registry kafka-avro-console-consumer \
        --bootstrap-server broker:29092 \
        --topic geoserver-monitor \
        --from-beginning | tee >(grep -E "^{" | jq) | grep -vE "^{"


Then you will see messages like this:

.. code-block:: json

    {
        "id": 2,
        "status": "FINISHED",
        "category": "OWS",
        "path": "/tiger/wms",
        "queryString": "SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES&LAYERS=tiger:giant_polygon&exceptions=application/vnd.ogc.se_inimage&SRS=EPSG:4326&WIDTH=768&HEIGHT=384&BBOX=3.7958354296875,-40.4131489453125,71.2627583203125,-6.6962260546875",
        "body": "",
        "bodyContentLength": 0,
        "bodyContentType": "",
        "httpMethod": "GET",
        "startTime": 1697172148809,
        "endTime": 1697172148902,
        "totalTime": 93,
        "remoteAddress": "[0:0:0:0:0:0:0:1]",
        "remoteHost": "0:0:0:0:0:0:0:1",
        "host": "localhost",
        "internalHost": "client",
        "remoteUser": "anonymous",
        "remoteUserAgent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
        "remoteCountry": "",
        "remoteCity": "",
        "remoteLat": 0.0,
        "remoteLon": 0.0,
        "service": "WMS",
        "operation": "GetMap",
        "owsVersion": "1.1.1",
        "subOperation": "",
        "resources": [
            "tiger:giant_polygon"
        ],
        "responseLength": 6594,
        "responseContentType": "image/png",
        "errorMessage": "",
        "responseStatus": 0,
        "httpReferer": "",
        "coordinateReferenceSystem": "EPSG:WGS 84",
        "minx": 3.7958354296875,
        "miny": -40.4131489453125,
        "maxx": 71.2627583203125,
        "maxy": -6.6962260546875,
        "cacheResult": "",
        "missReason": "",
        "resourceProcessingTimes": [],
        "labelingProcessingTime": 0
    }
    {
        "id": 3,
        "status": "FINISHED",
        "category": "OWS",
        "path": "/tiger/wms",
        "queryString": "SERVICE=WMS&VERSION=1.1.1&REQUEST=GetFeatureInfo&FORMAT=image/png&TRANSPARENT=true&QUERY_LAYERS=tiger:giant_polygon&STYLES&LAYERS=tiger:giant_polygon&exceptions=application/vnd.ogc.se_inimage&INFO_FORMAT=text/html&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG:4326&WIDTH=101&HEIGHT=101&BBOX=39.23891004908502,-32.22561743843973,48.11586317408502,-23.348664313439727",
        "body": "",
        "bodyContentLength": 0,
        "bodyContentType": "",
        "httpMethod": "GET",
        "startTime": 1697172178181,
        "endTime": 1697172178213,
        "totalTime": 32,
        "remoteAddress": "[0:0:0:0:0:0:0:1]",
        "remoteHost": "",
        "host": "localhost",
        "internalHost": "client",
        "remoteUser": "anonymous",
        "remoteUserAgent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
        "remoteCountry": "",
        "remoteCity": "",
        "remoteLat": 0.0,
        "remoteLon": 0.0,
        "service": "WMS",
        "operation": "GetFeatureInfo",
        "owsVersion": "1.1.1",
        "subOperation": "",
        "resources": [
            "giant_polygon"
        ],
        "responseLength": 803,
        "responseContentType": "text/html;charset=utf-8",
        "errorMessage": "",
        "responseStatus": 0,
        "httpReferer": "",
        "coordinateReferenceSystem": "EPSG:WGS 84",
        "minx": 43.63344129908502,
        "miny": -27.743195563439727,
        "maxx": 43.63344129908502,
        "maxy": -27.743195563439727,
        "cacheResult": "",
        "missReason": "",
        "resourceProcessingTimes": [],
        "labelingProcessingTime": 0
    }