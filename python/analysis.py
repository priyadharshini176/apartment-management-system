"""
Data analysis and visualisation for the Apartment Maintenance and
Resident Complaint Management System.

Steps: retrieve data from the database -> preprocess -> summarise -> visualise.

Run from the project root:
    python python/analysis.py
Charts are saved in outputs/charts/ and cleaned data in outputs/.
"""
import sqlite3
from pathlib import Path

import matplotlib
matplotlib.use("Agg")                      # save charts to files, no window needed
import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns

ROOT = Path(__file__).resolve().parent.parent
DB_PATH = ROOT / "apartment.db"
OUT_DIR = ROOT / "outputs"
CHART_DIR = OUT_DIR / "charts"

sns.set_theme(style="whitegrid")
CATEGORY_ORDER = ["Water", "Electrical", "Cleanliness", "Other"]
STATUS_ORDER = ["Open", "In Progress", "Resolved"]


# --------------------------------------------------------------------------
# 1. Retrieve data from the database
# --------------------------------------------------------------------------
def load_data():
    if not DB_PATH.exists():
        raise SystemExit("apartment.db not found. Run the Java program once, "
                         "or run:  python python/setup_db.py")
    con = sqlite3.connect(DB_PATH)
    payments = pd.read_sql_query(
        """SELECT p.payment_id, a.apartment_no, a.block_no, a.apartment_type,
                  p.month, p.amount, p.status, p.paid_date
           FROM maintenance_payment p
           JOIN apartment a ON p.apartment_id = a.apartment_id""", con)
    complaints = pd.read_sql_query(
        """SELECT c.complaint_id, r.name AS resident, a.apartment_no, a.block_no,
                  c.category, c.description, c.complaint_date, c.status, c.resolved_date
           FROM complaint c
           JOIN resident r  ON c.resident_id = r.resident_id
           JOIN apartment a ON r.apartment_id = a.apartment_id""", con)
    con.close()
    return payments, complaints


# --------------------------------------------------------------------------
# 2. Preprocess
# --------------------------------------------------------------------------
def preprocess_payments(df):
    df = df.copy()
    df = df.drop_duplicates(subset="payment_id")
    df["status"] = df["status"].str.strip().str.title()
    df["amount"] = pd.to_numeric(df["amount"], errors="coerce")
    df = df.dropna(subset=["amount"])                       # remove rows with unusable amounts
    df["paid_date"] = pd.to_datetime(df["paid_date"], errors="coerce")   # Pending -> NaT (expected)
    df["month"] = pd.PeriodIndex(df["month"], freq="M")
    return df


def preprocess_complaints(df):
    df = df.copy()
    df = df.drop_duplicates(subset="complaint_id")
    df["category"] = df["category"].str.strip().str.title()
    df["status"] = df["status"].str.strip()
    df["complaint_date"] = pd.to_datetime(df["complaint_date"], errors="coerce")
    df["resolved_date"] = pd.to_datetime(df["resolved_date"], errors="coerce")
    df = df.dropna(subset=["complaint_date"])
    df["resolution_days"] = (df["resolved_date"] - df["complaint_date"]).dt.days   # NaN while unresolved
    df["complaint_month"] = df["complaint_date"].dt.to_period("M")
    df["category"] = pd.Categorical(df["category"], categories=CATEGORY_ORDER, ordered=True)
    df["status"] = pd.Categorical(df["status"], categories=STATUS_ORDER, ordered=True)
    return df


# --------------------------------------------------------------------------
# 3. Visualise
# --------------------------------------------------------------------------
def save(fig, name):
    fig.tight_layout()
    fig.savefig(CHART_DIR / name, dpi=150)
    plt.close(fig)
    print("saved", name)


def chart_category(complaints):
    counts = complaints["category"].value_counts().reindex(CATEGORY_ORDER, fill_value=0)
    fig, ax = plt.subplots(figsize=(7, 4.5))
    sns.barplot(x=counts.index, y=counts.values, color="#3b6ea5", ax=ax)
    for i, v in enumerate(counts.values):
        ax.text(i, v + 0.1, str(v), ha="center")
    ax.set_title("Complaints by category")
    ax.set_xlabel("Category")
    ax.set_ylabel("Number of complaints")
    save(fig, "01_complaints_by_category.png")


def chart_status(complaints):
    counts = complaints["status"].value_counts().reindex(STATUS_ORDER, fill_value=0)
    fig, ax = plt.subplots(figsize=(5.5, 5.5))
    ax.pie(counts.values, labels=counts.index, autopct="%1.0f%%", startangle=90,
           colors=["#d9534f", "#f0ad4e", "#5cb85c"], wedgeprops={"edgecolor": "white"})
    ax.set_title("Complaint status")
    save(fig, "02_complaint_status.png")


def chart_monthly_collection(payments):
    pivot = (payments.pivot_table(index="month", columns="status", values="amount",
                                  aggfunc="sum", fill_value=0)
             .reindex(columns=["Paid", "Pending"], fill_value=0))
    fig, ax = plt.subplots(figsize=(7.5, 4.5))
    pivot.plot(kind="bar", stacked=True, ax=ax, color=["#5cb85c", "#d9534f"])
    ax.set_title("Monthly maintenance: collected vs pending")
    ax.set_xlabel("Month")
    ax.set_ylabel("Amount (Rs.)")
    ax.tick_params(axis="x", rotation=0)
    ax.legend(title="Status", loc="upper center", bbox_to_anchor=(0.5, -0.15), ncol=2)
    save(fig, "03_monthly_collection.png")


def chart_pending_by_apartment(payments):
    pending = (payments[payments["status"] == "Pending"]
               .groupby("apartment_no")["amount"].sum().sort_values())
    fig, ax = plt.subplots(figsize=(7, 4.5))
    ax.barh(pending.index, pending.values, color="#d9534f")
    for i, v in enumerate(pending.values):
        ax.text(v + 50, i, f"{v:,.0f}", va="center")
    ax.set_title("Total pending maintenance by apartment")
    ax.set_xlabel("Pending amount (Rs.)")
    save(fig, "04_pending_by_apartment.png")


def chart_resolution_time(complaints):
    avg = (complaints.dropna(subset=["resolution_days"])
           .groupby("category", observed=True)["resolution_days"].mean())
    fig, ax = plt.subplots(figsize=(7, 4.5))
    sns.barplot(x=avg.index.astype(str), y=avg.values, color="#8e6bbf", ax=ax)
    for i, v in enumerate(avg.values):
        ax.text(i, v + 0.05, f"{v:.1f}", ha="center")
    ax.set_title("Average days to resolve a complaint")
    ax.set_xlabel("Category")
    ax.set_ylabel("Days")
    save(fig, "05_avg_resolution_days.png")


def chart_block_heatmap(complaints):
    table = pd.crosstab(complaints["block_no"], complaints["category"]).reindex(
        columns=CATEGORY_ORDER, fill_value=0)
    fig, ax = plt.subplots(figsize=(6.5, 3.5))
    sns.heatmap(table, annot=True, fmt="d", cmap="YlOrRd", cbar=False, ax=ax)
    ax.set_title("Complaints by block and category")
    ax.set_xlabel("Category")
    ax.set_ylabel("Block")
    save(fig, "06_block_category_heatmap.png")


def chart_trend(complaints):
    trend = (complaints.groupby(["complaint_month", "category"], observed=True)
             .size().unstack(fill_value=0).reindex(columns=CATEGORY_ORDER, fill_value=0))
    fig, ax = plt.subplots(figsize=(7.5, 4.5))
    trend.plot(kind="line", marker="o", ax=ax)
    ax.set_title("Complaint trend by month")
    ax.set_xlabel("Month")
    ax.set_ylabel("Number of complaints")
    ax.set_yticks(range(0, int(trend.values.max()) + 2))
    save(fig, "07_complaint_trend.png")


# --------------------------------------------------------------------------
# 4. Summary
# --------------------------------------------------------------------------
def print_summary(payments, complaints):
    total = payments["amount"].sum()
    paid = payments.loc[payments["status"] == "Paid", "amount"].sum()
    print("\n===== SUMMARY =====")
    print(f"Total charges : Rs. {total:,.0f}")
    print(f"Collected     : Rs. {paid:,.0f} ({paid / total:.0%})")
    print(f"Pending       : Rs. {total - paid:,.0f}")
    worst = (payments[payments["status"] == "Pending"]
             .groupby("apartment_no")["amount"].sum().idxmax())
    print(f"Highest pending apartment: {worst}")
    print(f"Complaints    : {len(complaints)} "
          f"({(complaints['status'] != 'Resolved').sum()} unresolved)")
    top = complaints["category"].value_counts().idxmax()
    print(f"Most frequent complaint category: {top}")
    print("Average resolution days by category:")
    print(complaints.groupby("category", observed=True)["resolution_days"].mean().round(1).to_string())


def main():
    CHART_DIR.mkdir(parents=True, exist_ok=True)
    payments, complaints = load_data()
    payments = preprocess_payments(payments)
    complaints = preprocess_complaints(complaints)

    payments.to_csv(OUT_DIR / "cleaned_payments.csv", index=False)
    complaints.to_csv(OUT_DIR / "cleaned_complaints.csv", index=False)

    chart_category(complaints)
    chart_status(complaints)
    chart_monthly_collection(payments)
    chart_pending_by_apartment(payments)
    chart_resolution_time(complaints)
    chart_block_heatmap(complaints)
    chart_trend(complaints)
    print_summary(payments, complaints)


if __name__ == "__main__":
    main()
