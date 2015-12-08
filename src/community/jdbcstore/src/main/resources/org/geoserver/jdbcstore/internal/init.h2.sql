CREATE TABLE resource
(
  oid integer AUTO_INCREMENT NOT NULL,
  name character varying NOT NULL,
  parent integer,
  last_modified timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  content binary,
  CONSTRAINT resource_pkey PRIMARY KEY (oid),
  CONSTRAINT resource_parent_fkey FOREIGN KEY (parent)
      REFERENCES resource (oid)
      ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT resource_parent_name_key UNIQUE (parent, name),
  CONSTRAINT resource_only_one_root_check CHECK (parent IS NOT NULL OR oid = 0)
);

CREATE INDEX resource_parent_name_idx
  ON resource (parent NULLS FIRST, name NULLS FIRST);

INSERT INTO resource (oid, name, parent, content) VALUES (0, '', NULL, NULL);

ALTER TABLE resource ALTER COLUMN oid RESTART WITH 1;

