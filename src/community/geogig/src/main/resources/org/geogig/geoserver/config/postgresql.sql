-- Logback: the reliable, generic, fast and flexible logging framework.
-- Copyright (C) 1999-2010, QOS.ch. All rights reserved.
--
-- See http://logback.qos.ch/license.html for the applicable licensing 
-- conditions.

-- This SQL script creates the required tables by ch.qos.logback.classic.db.DBAppender
--
-- It is intended for PostgreSQL databases.

-- This file has been downloaded from https://github.com/qos-ch/logback/tree/master/logback-classic/src/main/resources/ch/qos/logback/classic/db/script

DROP TABLE IF EXISTS logging_event_property;
DROP TABLE IF EXISTS logging_event_exception;
DROP TABLE IF EXISTS logging_event;
DROP SEQUENCE IF EXISTS logging_event_id_seq;


CREATE SEQUENCE logging_event_id_seq MINVALUE 1 START 1;


CREATE TABLE logging_event 
  (
    timestmp         BIGINT NOT NULL,
    formatted_message  TEXT NOT NULL,
    logger_name       VARCHAR(254) NOT NULL,
    level_string      VARCHAR(254) NOT NULL,
    thread_name       VARCHAR(254),
    reference_flag    SMALLINT,
    arg0              VARCHAR(254),
    arg1              VARCHAR(254),
    arg2              VARCHAR(254),
    arg3              VARCHAR(254),
    caller_filename   VARCHAR(254) NOT NULL,
    caller_class      VARCHAR(254) NOT NULL,
    caller_method     VARCHAR(254) NOT NULL,
    caller_line       CHAR(4) NOT NULL,
    event_id          BIGINT DEFAULT nextval('logging_event_id_seq') PRIMARY KEY
  );

CREATE TABLE logging_event_property
  (
    event_id	      BIGINT NOT NULL,
    mapped_key        VARCHAR(254) NOT NULL,
    mapped_value      VARCHAR(1024),
    PRIMARY KEY(event_id, mapped_key),
    FOREIGN KEY (event_id) REFERENCES logging_event(event_id)
  );

CREATE TABLE logging_event_exception
  (
    event_id         BIGINT NOT NULL,
    i                SMALLINT NOT NULL,
    trace_line       VARCHAR(254) NOT NULL,
    PRIMARY KEY(event_id, i),
    FOREIGN KEY (event_id) REFERENCES logging_event(event_id)
  );
