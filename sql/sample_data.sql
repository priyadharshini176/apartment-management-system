INSERT INTO apartment (apartment_id, block_no, apartment_no, apartment_type, occupancy) VALUES
(1,'A','A-101','1BHK','Occupied'),
(2,'A','A-102','2BHK','Occupied'),
(3,'A','A-201','2BHK','Occupied'),
(4,'B','B-101','3BHK','Occupied'),
(5,'B','B-102','1BHK','Vacant'),
(6,'B','B-201','3BHK','Occupied');

INSERT INTO resident (resident_id, name, contact, apartment_id) VALUES
(1,'Arun Kumar','9876500001',1),
(2,'Meena Devi','9876500002',2),
(3,'Ravi Shankar','9876500003',3),
(4,'Lakshmi Priya','9876500004',4),
(5,'Karthik Raja','9876500005',6);

INSERT INTO maintenance_payment (payment_id, apartment_id, month, amount, status, paid_date) VALUES
(1,1,'2026-09',1500,'Paid','2026-09-05'),
(2,1,'2026-10',1500,'Paid','2026-10-06'),
(3,1,'2026-11',1500,'Pending',NULL),
(4,2,'2026-09',2000,'Paid','2026-09-04'),
(5,2,'2026-10',2000,'Pending',NULL),
(6,2,'2026-11',2000,'Pending',NULL),
(7,3,'2026-09',2000,'Paid','2026-09-10'),
(8,3,'2026-10',2000,'Paid','2026-10-08'),
(9,3,'2026-11',2000,'Paid','2026-11-03'),
(10,4,'2026-09',2500,'Paid','2026-09-02'),
(11,4,'2026-10',2500,'Paid','2026-10-03'),
(12,4,'2026-11',2500,'Pending',NULL),
(13,6,'2026-09',2500,'Pending',NULL),
(14,6,'2026-10',2500,'Pending',NULL),
(15,6,'2026-11',2500,'Pending',NULL);

INSERT INTO complaint (complaint_id, resident_id, category, description, complaint_date, status, resolved_date) VALUES
(1,1,'Water','No water supply in morning','2026-09-03','Resolved','2026-09-05'),
(2,2,'Electrical','Corridor light not working','2026-09-10','Resolved','2026-09-11'),
(3,3,'Cleanliness','Garbage not collected','2026-09-15','Resolved','2026-09-16'),
(4,4,'Water','Low water pressure','2026-09-22','Resolved','2026-09-26'),
(5,5,'Water','Water tank leakage','2026-10-02','Resolved','2026-10-07'),
(6,1,'Electrical','Lift power failure','2026-10-09','Resolved','2026-10-10'),
(7,2,'Water','Dirty water in taps','2026-10-18','Resolved','2026-10-24'),
(8,3,'Cleanliness','Staircase not cleaned','2026-10-25','Resolved','2026-10-27'),
(9,5,'Other','Parking space dispute','2026-11-01','In Progress',NULL),
(10,4,'Water','No water supply','2026-11-05','In Progress',NULL),
(11,1,'Electrical','Street light broken','2026-11-08','Open',NULL),
(12,5,'Cleanliness','Drainage overflow','2026-11-10','Open',NULL);
