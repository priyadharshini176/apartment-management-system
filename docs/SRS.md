# Software Requirements Specification (SRS)
**Project:** Apartment Maintenance and Resident Complaint Management System
**Student:** _<your name, register number>_  |  **Department:** Information Technology, VCET  |  **Version:** 1.0

---

## 1. Introduction

### 1.1 Purpose
This document specifies the requirements of a system that helps an apartment association maintain resident and apartment records, track monthly maintenance payments, manage resident complaints, and analyse facility-related problems.

### 1.2 Scope
The system is a Java application (console menu and browser-based web app) backed by a relational database (SQLite). A Python module retrieves the stored data, cleans it, and produces charts. It replaces manual registers, which cause delayed complaint resolution, difficulty tracking payments, and no view of recurring problems.

### 1.3 Definitions
| Term | Meaning |
|---|---|
| Admin | Association office-bearer who manages records, payments and complaint status |
| Resident | Person living in an apartment who can register complaints |
| Complaint status | One of Open, In Progress, Resolved |
| JDBC | Java Database Connectivity, used by Java to talk to the database |

### 1.4 Overview
Section 2 describes the product, Section 3 lists requirements, Section 4 covers design constraints and the process model.

---

## 2. Overall Description

### 2.1 Product perspective
Three layers: **Java application** (console menu or web app, business logic, validation) -> **Database** (SQLite) -> **Python analysis** (pandas, Matplotlib, Seaborn). See `docs/diagrams/architecture.png`.

### 2.2 User classes
- **Admin:** full access to all menu options.
- **Resident:** registers complaints and views their status (through the admin menu in this console version).

### 2.3 Operating environment
Any OS with Java 11+ and Python 3.9+. No database server is needed.

### 2.4 Assumptions and dependencies
- One resident record is linked to one apartment; an apartment can have many residents.
- Maintenance is charged per apartment per month, one charge per month.
- The SQLite JDBC driver (`java/lib/sqlite-jdbc.jar`) is available.

---

## 3. Requirements

### 3.1 Functional requirements
| ID | Requirement | Implemented by |
|---|---|---|
| FR1 | Maintain resident details: resident ID, name, apartment number, contact information | Menu 2, 4 |
| FR2 | Maintain apartment details: block number, apartment type, occupancy status | Menu 1, 3 |
| FR3 | Record monthly maintenance charges and payment status (Paid / Pending) for each apartment | Menu 5, 6, 7 |
| FR4 | Allow residents to register complaints with category, description and complaint date | Menu 8 |
| FR5 | Track complaint status as Open, In Progress or Resolved | Menu 9, 10 |
| FR6 | Generate reports: pending payments, complaint resolution, category-wise complaints | Menu 11, `sql/queries.sql` |
| FR7 | Retrieve data from the database, preprocess it and generate visualizations using Python | `python/analysis.py` |
| FR8 | Web interface with a dashboard of charts, forms and tables (extra feature) | `WebServer.java`, `web/` |

### 3.2 Business rules
- BR1: Contact number must be exactly 10 digits.
- BR2: Apartment number is unique.
- BR3: Only one maintenance charge per apartment per month; month format `YYYY-MM`; amount > 0.
- BR4: A new complaint always starts as **Open**.
- BR5: When a complaint becomes **Resolved**, the resolved date is stored automatically.
- BR6: A resolved complaint cannot be reopened.
- BR7: Adding a resident marks the apartment **Occupied**.
- BR8: Marking a payment Paid stores today's date as the paid date; an already-paid record cannot be paid again.

### 3.3 Non-functional requirements
| ID | Requirement |
|---|---|
| NFR1 | Usability: numbered console menu and a responsive browser interface, both with clear error messages |
| NFR2 | Reliability: invalid input never crashes the program; it shows a message and returns to the menu |
| NFR3 | Data integrity: primary keys, foreign keys, CHECK and UNIQUE constraints in the database |
| NFR4 | Portability: runs on Windows, Linux and macOS with no server installation |
| NFR5 | Maintainability: separate model, service, database and UI classes |
| NFR6 | Security: SQL uses PreparedStatements to prevent SQL injection |

### 3.4 Use cases
See `docs/diagrams/use_case.png`.

| Use case | Actor | Main flow |
|---|---|---|
| Register complaint | Resident | Enter resident ID, category, description, date -> system validates -> saved as Open |
| Update complaint status | Admin | Enter complaint ID and new status -> system validates rule BR6 -> updated |
| Record monthly charge | Admin | Enter apartment ID, month, amount -> saved as Pending |
| Record payment | Admin | Enter payment ID -> status becomes Paid with today's date |
| Generate reports | Admin | Choose Reports -> pending amounts, category counts, status summary, resolution time |
| View analysis | Admin | Run `analysis.py` -> charts saved in `outputs/charts/` |

---

## 4. Design and process

- **Process model:** Incremental model. Database -> Java application -> Python analysis -> testing -> documentation.
- **Design diagrams:** Use case, class, architecture and ER diagrams are in `docs/diagrams/`.
- **Database design:** see `docs/database_design.md` (ER diagram, relational model, normalization).
- **Testing:** see `docs/test_plan.md`.

## 5. Risks
| Risk | Mitigation |
|---|---|
| Wrong or duplicate data entry | Input validation and UNIQUE constraints |
| Database file lost | Keep `sql/` scripts to rebuild; back up `apartment.db` |
| Unresolved complaints forgotten | Report of unresolved complaints (query 8) |
