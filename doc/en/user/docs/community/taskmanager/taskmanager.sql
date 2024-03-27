CREATE TABLE taskmanager.attributeimpl (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    value bytea NOT NULL,
    configuration bigint
);

CREATE TABLE taskmanager.batchelementimpl (
    id bigint NOT NULL,
    index integer,
    removestamp bigint NOT NULL,
    batch bigint,
    task bigint
);

CREATE TABLE taskmanager.batchimpl (
    id bigint NOT NULL,
    description character varying(255),
    enabled boolean NOT NULL,
    frequency character varying(255),
    name character varying(255) NOT NULL,
    namenoconfig character varying(255),
    removestamp bigint NOT NULL,
    workspace character varying(255),
    configuration bigint
);

CREATE TABLE taskmanager.batchrunimpl (
    id bigint NOT NULL,
    batch bigint,
    schedulerreference character varying(255),
    interruptme boolean DEFAULT false NOT NULL
);

CREATE TABLE taskmanager.configurationimpl (
    id bigint NOT NULL,
    description character varying(255),
    name character varying(255) NOT NULL,
    removestamp bigint NOT NULL,
    template boolean NOT NULL,
    workspace character varying(255),
    validated boolean DEFAULT false NOT NULL
);

CREATE TABLE taskmanager.parameterimpl (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255),
    task bigint
);

CREATE TABLE taskmanager.runimpl (
    id bigint NOT NULL,
    "runEnd" timestamp without time zone,
    message bytea,
    start timestamp without time zone NOT NULL,
    status integer NOT NULL,
    batchelement bigint,
    batchrun bigint,
    runend timestamp without time zone
);

CREATE TABLE taskmanager.taskimpl (
    id bigint NOT NULL,
    name character varying(255),
    removestamp bigint NOT NULL,
    type character varying(255),
    configuration bigint
);

ALTER TABLE ONLY taskmanager.attributeimpl
    ADD CONSTRAINT attributeimpl_name_configuration_key UNIQUE (name, configuration);

ALTER TABLE ONLY taskmanager.attributeimpl
    ADD CONSTRAINT attributeimpl_pkey PRIMARY KEY (id);

ALTER TABLE ONLY taskmanager.batchelementimpl
    ADD CONSTRAINT batchelementimpl_pkey PRIMARY KEY (id);

ALTER TABLE ONLY taskmanager.batchelementimpl
    ADD CONSTRAINT batchelementimpl_task_batch_key UNIQUE (task, batch);

ALTER TABLE ONLY taskmanager.batchimpl
    ADD CONSTRAINT batchimpl_name_configuration_removestamp_key UNIQUE (name, configuration, removestamp);

ALTER TABLE ONLY taskmanager.batchimpl
    ADD CONSTRAINT batchimpl_namenoconfig_removestamp_key UNIQUE (namenoconfig, removestamp);

ALTER TABLE ONLY taskmanager.batchimpl
    ADD CONSTRAINT batchimpl_pkey PRIMARY KEY (id);

ALTER TABLE ONLY taskmanager.batchrunimpl
    ADD CONSTRAINT batchrunimpl_pkey PRIMARY KEY (id);

ALTER TABLE ONLY taskmanager.configurationimpl
    ADD CONSTRAINT configurationimpl_name_removestamp_key UNIQUE (name, removestamp);

ALTER TABLE ONLY taskmanager.configurationimpl
    ADD CONSTRAINT configurationimpl_pkey PRIMARY KEY (id);

ALTER TABLE ONLY taskmanager.parameterimpl
    ADD CONSTRAINT parameterimpl_name_task_key UNIQUE (name, task);

ALTER TABLE ONLY taskmanager.parameterimpl
    ADD CONSTRAINT parameterimpl_pkey PRIMARY KEY (id);

ALTER TABLE ONLY taskmanager.runimpl
    ADD CONSTRAINT runimpl_pkey PRIMARY KEY (id);

ALTER TABLE ONLY taskmanager.taskimpl
    ADD CONSTRAINT taskimpl_name_configuration_removestamp_key UNIQUE (name, configuration, removestamp);

ALTER TABLE ONLY taskmanager.taskimpl
    ADD CONSTRAINT taskimpl_pkey PRIMARY KEY (id);


CREATE INDEX idx_attributeimpl_configuration ON taskmanager.attributeimpl USING btree (configuration);
CREATE INDEX idx_batchelementimpl_batch ON taskmanager.batchelementimpl USING btree (batch);
CREATE INDEX idx_batchelementimpl_task ON taskmanager.batchelementimpl USING btree (task);
CREATE INDEX idx_batchimpl_configuration ON taskmanager.batchimpl USING btree (configuration);
CREATE INDEX idx_batchrunimpl_batch ON taskmanager.batchrunimpl USING btree (batch);
CREATE INDEX idx_parameterimpl_task ON taskmanager.parameterimpl USING btree (task);
CREATE INDEX idx_runimpl_batchrun ON taskmanager.runimpl USING btree (batchrun);
CREATE INDEX idx_taskimpl_configuration ON taskmanager.taskimpl USING btree (configuration);


ALTER TABLE ONLY taskmanager.parameterimpl
    ADD CONSTRAINT fkParameterTask FOREIGN KEY (task) REFERENCES taskmanager.taskimpl(id);

ALTER TABLE ONLY taskmanager.batchimpl
    ADD CONSTRAINT fkBatchConfiguration FOREIGN KEY (configuration) REFERENCES taskmanager.configurationimpl(id);

ALTER TABLE ONLY taskmanager.batchrunimpl
    ADD CONSTRAINT fkBatchRunBatch FOREIGN KEY (batch) REFERENCES taskmanager.batchimpl(id);

ALTER TABLE ONLY taskmanager.batchelementimpl
    ADD CONSTRAINT fkBatchElementTask FOREIGN KEY (task) REFERENCES taskmanager.taskimpl(id);

ALTER TABLE ONLY taskmanager.batchelementimpl
    ADD CONSTRAINT fkBatchElementBatch FOREIGN KEY (batch) REFERENCES taskmanager.batchimpl(id);

ALTER TABLE ONLY taskmanager.runimpl
    ADD CONSTRAINT fkRunBatchElement FOREIGN KEY (batchelement) REFERENCES taskmanager.batchelementimpl(id);

ALTER TABLE ONLY taskmanager.runimpl
    ADD CONSTRAINT fkRunBatchRun FOREIGN KEY (batchrun) REFERENCES taskmanager.batchrunimpl(id);

ALTER TABLE ONLY taskmanager.attributeimpl
    ADD CONSTRAINT fkAttributeConfiguration FOREIGN KEY (configuration) REFERENCES taskmanager.configurationimpl(id);

ALTER TABLE ONLY taskmanager.taskimpl
    ADD CONSTRAINT fkTaskConfiguration FOREIGN KEY (configuration) REFERENCES taskmanager.configurationimpl(id);



CREATE SEQUENCE taskmanager.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;