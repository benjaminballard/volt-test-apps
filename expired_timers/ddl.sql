-- load java stored procedure classes from pre-compiled jar file
LOAD CLASSES procedures/procedures.jar;


-- use this command to execute the following DDL as a batch in a single transaction
file -inlinebatch END_OF_BATCH

CREATE TABLE Timer
   (document_id             VARCHAR(255) NOT NULL
   ,timer_name              VARCHAR(255) NOT NULL
   ,lastupdate_time         TIMESTAMP    DEFAULT NULL
   ,timer_value             TIMESTAMP    DEFAULT NULL
   ,timer_code              INTEGER      DEFAULT '0' NOT NULL
   ,entry_id                VARCHAR(255) NOT NULL
   ,affinity                VARCHAR(255) DEFAULT NULL
   ,context                 VARCHAR(512) DEFAULT NULL
   ,last_active_cluster_id  VARCHAR(3)   DEFAULT NULL
   ,PRIMARY KEY (entry_id, document_id, timer_name)
);
CREATE INDEX Timer_timer_affinity_value_idx ON Timer (affinity,timer_value);
CREATE INDEX Timer_timer_value_idx ON Timer (timer_value);
PARTITION TABLE Timer ON COLUMN entry_id;

CREATE PROCEDURE insert_timer
PARTITION ON TABLE timer COLUMN entry_id PARAMETER 3
AS
INSERT INTO timer (
  document_id,
  timer_name,
  lastupdate_time,
  timer_value,
  timer_code,
  entry_id,
  affinity,
  context,
  last_active_cluster_id
) values (?, ?, NULL, NOW, ?, ?, ?, ?, ?);


CREATE PROCEDURE PARTITION ON TABLE timer COLUMN entry_id FROM CLASS simple.GetExpiredTimersWithFilterWithAffinity;

-- execute the batch of DDL statements
END_OF_BATCH
