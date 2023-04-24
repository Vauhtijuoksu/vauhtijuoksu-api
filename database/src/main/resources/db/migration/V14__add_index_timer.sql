CREATE SEQUENCE timers_indexCol_seq;
ALTER TABLE timers ADD COLUMN indexcol INTEGER NOT NULL DEFAULT nextval('timers_indexCol_seq') UNIQUE;
