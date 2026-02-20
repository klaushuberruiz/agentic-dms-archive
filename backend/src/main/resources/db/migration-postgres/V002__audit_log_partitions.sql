-- ============================================================
-- Flyway migration: V002__audit_log_partitions.sql
-- Adds future quarterly partitions for audit_logs table
-- Rollback: DROP TABLE audit_logs_2026_q3, audit_logs_2026_q4,
--           audit_logs_2027_q1, audit_logs_2027_q2,
--           audit_logs_2027_q3, audit_logs_2027_q4;
-- ============================================================

-- V001 created partitions: 2025 Q1–Q4, 2026 Q1–Q2
-- This migration extends coverage through 2027 Q4

CREATE TABLE audit_logs_2026_q3 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-07-01') TO ('2026-10-01');

CREATE TABLE audit_logs_2026_q4 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-10-01') TO ('2027-01-01');

CREATE TABLE audit_logs_2027_q1 PARTITION OF audit_logs
    FOR VALUES FROM ('2027-01-01') TO ('2027-04-01');

CREATE TABLE audit_logs_2027_q2 PARTITION OF audit_logs
    FOR VALUES FROM ('2027-04-01') TO ('2027-07-01');

CREATE TABLE audit_logs_2027_q3 PARTITION OF audit_logs
    FOR VALUES FROM ('2027-07-01') TO ('2027-10-01');

CREATE TABLE audit_logs_2027_q4 PARTITION OF audit_logs
    FOR VALUES FROM ('2027-10-01') TO ('2028-01-01');
