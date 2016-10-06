# Logback: the reliable, generic, fast and flexible logging framework.
# Copyright (C) 1999-2010, QOS.ch. All rights reserved.
#
# See http://logback.qos.ch/license.html for the applicable licensing 
# conditions.


# This SQL script creates the required tables by ch.qos.logback.classic.db.DBAppender.
#
# It is intended for HSQL databases. It has been tested on HSQL 1.8.07.

# This file has been downloaded from https://github.com/qos-ch/logback/tree/master/logback-classic/src/main/resources/ch/qos/logback/classic/db/script

DROP TABLE logging_event_exception IF EXISTS;
DROP TABLE logging_event_property IF EXISTS;
DROP TABLE logging_event IF EXISTS;

CREATE TABLE logging_event (
  timestmp BIGINT NOT NULL,
  formatted_message LONGVARCHAR NOT NULL,
  logger_name VARCHAR(256) NOT NULL,
  level_string VARCHAR(256) NOT NULL,
  thread_name VARCHAR(256),
  reference_flag SMALLINT,
  arg0 VARCHAR(256),
  arg1 VARCHAR(256),
  arg2 VARCHAR(256),
  arg3 VARCHAR(256),
  caller_filename VARCHAR(256), 
  caller_class VARCHAR(256), 
  caller_method VARCHAR(256), 
  caller_line CHAR(4),
  event_id BIGINT NOT NULL IDENTITY);


CREATE TABLE logging_event_property (
  event_id BIGINT NOT NULL,
  mapped_key  VARCHAR(254) NOT NULL,
  mapped_value LONGVARCHAR,
  PRIMARY KEY(event_id, mapped_key),
  FOREIGN KEY (event_id) REFERENCES logging_event(event_id));

CREATE TABLE logging_event_exception (
  event_id BIGINT NOT NULL,
  i SMALLINT NOT NULL,
  trace_line VARCHAR(256) NOT NULL,
  PRIMARY KEY(event_id, i),
  FOREIGN KEY (event_id) REFERENCES logging_event(event_id));
