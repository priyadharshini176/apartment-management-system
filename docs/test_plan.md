# Test Plan and Test Cases

## 1. Testing approach
| Technique | Where used |
|---|---|
| **Unit testing** | Each `ManagementService` method tested on its own (`java/src/TestRunner.java`) |
| **Integration testing** | Java service + real SQLite database + SQL scripts working together |
| **Black-box testing** | Test cases below use only inputs and expected outputs. Techniques: equivalence partitioning (valid / invalid inputs) and boundary values (amount = 0, contact length) |
| **White-box testing** | Every validation branch (`if` condition) in `ManagementService` has a test that triggers it |
| **Validation testing** | Cases check the business rules BR1 to BR8 in the SRS |
| **System testing** | Full menu session using `java/sample_io/sample_input.txt`; output saved in `sample_output.txt` |

Tests run against a temporary database built from `sql/schema.sql` and `sql/sample_data.sql`, so real data is never changed.

## 2. How to run
```bash
javac -d java/out java/src/*.java
java -cp "java/out:java/lib/sqlite-jdbc.jar" -Ddb.path=test_apartment.db TestRunner
```
On Windows, use `;` instead of `:` in the classpath.

## 3. Test cases (expected vs actual)
Actual results were taken from the run saved in `outputs/test_results.txt`.

| ID | Requirement | Input | Expected result | Actual result | Status |
|---|---|---|---|---|---|
| TC01 | FR2 | Add apartment C-101, 2BHK, Vacant | Saved, new ID returned | Saved | Pass |
| TC02 | BR2 | Add apartment C-101 again | Error: already exists | "Apartment C-101 already exists." | Pass |
| TC03 | FR2 | Apartment type 5BHK | Error: invalid type | "Type must be one of [1BHK, 2BHK, 3BHK]" | Pass |
| TC04 | FR1 | Resident "Test User", 9000000001, apartment 7 | Saved | Saved | Pass |
| TC05 | BR1 | Contact "98abc" | Error: 10 digits needed | "Contact must be exactly 10 digits." | Pass |
| TC06 | FR1 | Resident for apartment ID 999 | Error: apartment missing | "Apartment ID 999 does not exist." | Pass |
| TC07 | BR7 | Add resident to a Vacant apartment | Apartment becomes Occupied | Occupied | Pass |
| TC08 | FR3 | Charge 2000 for apartment 7, month 2026-12 | Saved as Pending | Pending | Pass |
| TC09 | BR3 | Same apartment and month again | Error: duplicate charge | "A charge for this apartment and month already exists." | Pass |
| TC10 | BR3 | Month "12-2026" | Error: wrong format | "Month must be in YYYY-MM format, e.g. 2026-12." | Pass |
| TC11 | BR3 (boundary) | Amount 0 | Error: amount must be > 0 | "Amount must be greater than zero." | Pass |
| TC12 | FR3, BR8 | Mark payment 3 as Paid | Status Paid, paid date set | Paid with date | Pass |
| TC13 | BR8 | Mark payment 1 (already Paid) | Error | "Payment is already marked Paid." | Pass |
| TC14 | FR6 | Pending total for A-102 | 4000 | 4000 | Pass |
| TC15 | FR4, BR4 | Register Water complaint, resident 1 | Saved with status Open | Open | Pass |
| TC16 | FR4 | Category "Noise" | Error: invalid category | "Category must be one of [Water, Electrical, Cleanliness, Other]" | Pass |
| TC17 | FR4 | Description with only spaces | Error: required | "Description is required." | Pass |
| TC18 | FR4 | Date "12/11/2026" | Error: wrong format | "Date must be in YYYY-MM-DD format." | Pass |
| TC19 | FR5 | Complaint 11: Open -> In Progress | Status updated | In Progress | Pass |
| TC20 | FR5, BR5 | Complaint 11: In Progress -> Resolved | Status Resolved, resolved date set | Resolved with date | Pass |
| TC21 | BR6 | Complaint 11: Resolved -> Open | Error | "A resolved complaint cannot be reopened." | Pass |
| TC22 | FR5 | Update complaint ID 999 | Error: not found | "Complaint ID 999 does not exist." | Pass |
| TC23 | FR6 | Water complaints count (5 sample + 1 new) | 6 | 6 | Pass |
| TC24 | FR6 | Average resolution days, Electrical | Greater than 0 | Greater than 0 (1.0) | Pass |

**Result: 24 passed, 0 failed.**

## 4. System test (menu session)
Input file: `java/sample_io/sample_input.txt`. Steps: view apartments (3), residents (4), pending payments (7, filter Pending), register a Water complaint (8), set it In Progress (9), view In Progress complaints (10), reports (11), exit (0).
Expected: the complaint is created as ID 13, then updated; reports show total pending Rs. 15500 and Water as the top category. Actual output is in `java/sample_io/sample_output.txt` and matches.

## 5. Python analysis check
`python/analysis.py` totals match the SQL reports: total charges Rs. 31,500, collected Rs. 16,000, pending Rs. 15,500, highest pending apartment B-201, most frequent category Water.

## 6. Web app tests (API and browser)
The web app (`WebServer.java`, `web/`) reuses the tested `ManagementService`. It was checked separately with API calls and a browser session.

| ID | Check | Expected | Actual | Status |
|---|---|---|---|---|
| W01 | `GET /api/reports` | JSON with totals, categories, monthly data | Returned all fields; pending Rs. 15,500, Water 5 | Pass |
| W02 | `GET /` and `/chart.umd.js` | Page and script served | HTTP 200 | Pass |
| W03 | Request `/../README.md` (path traversal) | Blocked | HTTP 404 | Pass |
| W04 | `POST /api/residents` with contact "123" | Error message, HTTP 400 | "Contact must be exactly 10 digits." | Pass |
| W05 | `POST /api/residents` with valid data | Resident created | `{"id":6,"message":"Resident added"}` | Pass |
| W06 | Resolve complaint 11, then reopen it | Resolve works, reopen rejected | "A resolved complaint cannot be reopened." | Pass |
| W07 | `POST /api/payments/pay` with `paymentId=abc` | Error, HTTP 400 | "Please enter valid numbers." | Pass |
| W08 | Browser: register complaint with description `<b>Tap</b> leaking` | Shown as plain text (no HTML injection) | Shown as text | Pass |
| W09 | Browser: complaint status dropdown Open -> In Progress | Status badge updates | Updated | Pass |
| W10 | Browser: Mark paid on a Pending payment | Row leaves the Pending list | 6 rows -> 5 rows | Pass |
| W11 | Browser: phone width (390 px), all 5 tabs | No sideways scrolling | No overflow | Pass |

Screenshots: `outputs/screenshots/`.

### 6.1 Demo mode (static hosting, no Java server)
`web/demo.js` repeats the business rules in the browser so the page works on GitHub Pages. It was tested by serving the project as plain static files.

| ID | Check | Expected | Actual | Status |
|---|---|---|---|---|
| D01 | Open repository root | Redirect to `web/`, demo banner visible, sample totals shown | Rs. 31,500 charged, Rs. 15,500 pending | Pass |
| D02 | Register complaint, then set it Resolved | Saved as Open, then Resolved | Both toasts shown | Pass |
| D03 | Resident with contact "123" | Error shown | "Contact must be exactly 10 digits." | Pass |
| D04 | Duplicate charge for same apartment and month | Error shown | "A charge for this apartment and month already exists." | Pass |
| D05 | Mark a pending payment paid, reload page | Change kept after reload | Pending Rs. 13,000 after reload | Pass |
| D06 | Reset demo data | Sample data restored | Pending Rs. 15,500 | Pass |
| D07 | Open the Java server instead | No demo banner, real backend used | Banner hidden | Pass |

