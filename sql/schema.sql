-- Apartment Maintenance and Resident Complaint Management System
-- Database: SQLite

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS apartment (
    apartment_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    block_no       TEXT NOT NULL,
    apartment_no   TEXT NOT NULL UNIQUE,
    apartment_type TEXT NOT NULL CHECK (apartment_type IN ('1BHK','2BHK','3BHK')),
    occupancy      TEXT NOT NULL CHECK (occupancy IN ('Occupied','Vacant'))
);

CREATE TABLE IF NOT EXISTS resident (
    resident_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT NOT NULL,
    contact      TEXT NOT NULL,
    apartment_id INTEGER NOT NULL REFERENCES apartment(apartment_id)
);

CREATE TABLE IF NOT EXISTS maintenance_payment (
    payment_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    apartment_id INTEGER NOT NULL REFERENCES apartment(apartment_id),
    month        TEXT NOT NULL,               -- format YYYY-MM
    amount       REAL NOT NULL CHECK (amount > 0),
    status       TEXT NOT NULL CHECK (status IN ('Paid','Pending')),
    paid_date    TEXT,
    UNIQUE (apartment_id, month)
);

CREATE TABLE IF NOT EXISTS complaint (
    complaint_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    resident_id    INTEGER NOT NULL REFERENCES resident(resident_id),
    category       TEXT NOT NULL CHECK (category IN ('Water','Electrical','Cleanliness','Other')),
    description    TEXT NOT NULL,
    complaint_date TEXT NOT NULL,
    status         TEXT NOT NULL DEFAULT 'Open' CHECK (status IN ('Open','In Progress','Resolved')),
    resolved_date  TEXT
);

CREATE INDEX IF NOT EXISTS idx_payment_status   ON maintenance_payment(status);
CREATE INDEX IF NOT EXISTS idx_complaint_status ON complaint(status);
