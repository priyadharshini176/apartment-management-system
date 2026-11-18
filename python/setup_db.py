"""Creates apartment.db from the SQL scripts (use this if you have not run the Java program yet).

Run from the project root:
    python python/setup_db.py
"""
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB_PATH = ROOT / "apartment.db"


def main():
    if DB_PATH.exists():
        print(f"{DB_PATH.name} already exists - nothing to do. Delete it to rebuild.")
        return
    con = sqlite3.connect(DB_PATH)
    for script in ("schema.sql", "sample_data.sql"):
        con.executescript((ROOT / "sql" / script).read_text())
    con.commit()
    con.close()
    print(f"Created {DB_PATH.name} with sample data.")


if __name__ == "__main__":
    main()
