CREATE TABLE resources
(
  oid integer AUTO_INCREMENT NOT NULL,
  name character varying NOT NULL,
  parent integer,
  last_modified timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  content binary,
  CONSTRAINT resources_pkey PRIMARY KEY (oid),
  CONSTRAINT resources_parent_fkey FOREIGN KEY (parent)
      REFERENCES resources (oid)
      ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT resources_parent_name_key UNIQUE (parent, name),
  CONSTRAINT resources_only_one_root_check CHECK (parent IS NOT NULL OR oid = 0)
);

CREATE INDEX resources_parent_name_idx
  ON resources (parent NULLS FIRST, name NULLS FIRST);

INSERT INTO resources (oid, name, parent, content) VALUES (0, '', NULL, NULL);

ALTER TABLE resources ALTER COLUMN oid RESTART WITH 1;

