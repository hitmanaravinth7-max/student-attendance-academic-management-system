# Student Attendance and Academic Management System (EduTrack)

An end-to-end, production-ready Full Stack Academic Management System built with **Python Flask**, **SQLite**, and **Bootstrap 5**. Designed specifically for colleges, universities, and internship portfolios to manage daily student attendance, automatic percentage calculations, defaulter alerts, and academic assessment marks.

---

## 🌟 Key Features

### 1. 🛡️ Role-Based Authentication & Access Control
- **Faculty / Administrator Login**: Access to class-wide registers, student records, and marks entry.
- **Student Portal Login & Registration**: Enrolled students can self-register and securely monitor their attendance and academic standing.
- **Session Security**: Password hashing with Werkzeug/SHA-256 and role-enforced route decorators.

### 2. 👨‍🎓 Comprehensive Student Directory (CRUD)
- **Add New Students**: Capture Roll Number, Legal Name, Email, Department, Semester, Phone, DOB, and Address.
- **Search & Multi-Filter**: Real-time search by Roll Number, Name, Email, Department, or Semester.
- **Low Attendance Filter**: Instant toggle to filter students with attendance below 75%.
- **360° Profile View**: View contact details, cumulative attendance meters, daily history log, and full academic gradecards.
- **Edit & Delete**: Update information or delete with cascade deletion of related logs.

### 3. 📅 Daily Class Attendance Register
- **Session-Based Marking**: Select Date, Department, and Semester to load the class roll.
- **Quick Status Toggle**: Mark individual students as **Present**, **Late**, or **Absent**.
- **1-Click "Mark All Present"**: Accelerate daily roll call for large classes.
- **Conflict Prevention**: Automatic upsert prevents duplicate entries for the same student on the same date.

### 4. 📊 Automated Attendance Percentage & Defaulters Alert
- **Real-Time Calculations**: Formula: `(Attended Days / Total Days) * 100`.
- **Institutional Threshold Warning (< 75%)**: Highlights exam-ineligible students with red warning badges and alerts.
- **Official Ledger Reports**: Filter cumulative attendance registers by department and semester with print-ready layouts (`window.print()`).

### 5. 🏆 Academic Marks & Assessment Management
- **Continuous Internal Evaluation**: Enter scores for *Internal 1*, *Internal 2*, *Assignments*, and *Semester Finals*.
- **Cumulative GPA & Grade Computation**: Automatic grade assignment:
  - `A+ (>= 90%)`, `A (>= 80%)`, `B+ (>= 70%)`, `B (>= 60%)`, `C (>= 50%)`, `F (< 50%)`.
- **Custom Feedback**: Record individual student remarks.

### 6. 📱 Responsive Modern UI
- Clean, responsive dashboard designed with Bootstrap 5.3, Plus Jakarta Sans typography, and FontAwesome 6 icons.
- Fully accessible across mobile devices, tablets, and desktop workstations.

---

## 📂 Project Directory Structure

```text
Student-Attendance-Management/
├── app.py                      # Flask Application Controller & Routes
├── database.py                 # SQLite Schema, Seeding & Query Helpers
├── attendance_system.db        # SQLite Database (Auto-generated)
├── requirements.txt            # Python Dependencies
├── README.md                   # Project Documentation
├── static/
│   ├── css/
│   │   └── style.css           # Custom Modern Stylesheet & Print Media
│   └── js/
│       └── main.js             # Client-side Interactions & Utilities
└── templates/
    ├── base.html               # Base Master Layout with Navbar & Footer
    ├── login.html              # Unified Admin/Student Login Page
    ├── register.html           # Student Self-Registration Form
    ├── admin_dashboard.html    # Admin Analytics & Defaulters Overview
    ├── student_dashboard.html  # Student Personal Attendance & Grade Meter
    ├── student_attendance.html # Chronological Student Attendance History
    ├── student_profile.html    # Student Profile Management
    ├── students/
    │   ├── index.html          # Student Directory (Search & Filter)
    │   ├── form.html           # Add / Edit Student Form
    │   └── view.html           # 360-Degree Student Profile & Report
    ├── attendance/
    │   ├── mark.html           # Daily Attendance Marking Matrix
    │   └── report.html         # Cumulative Attendance Register & Report
    └── marks/
        └── manage.html         # Subject-Wise Academic Marks Entry
```

---

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| **Backend Framework** | Python 3 (Flask 3.0.3) |
| **Database** | SQLite 3 |
| **Frontend Templates** | Jinja2 Template Engine |
| **Styling & UI** | Bootstrap 5.3.3 & Custom CSS |
| **Iconography & Fonts** | FontAwesome 6.5.1 & Plus Jakarta Sans |
| **Client Scripting** | Vanilla JavaScript (ES6) |

---

## 🗄️ Database Architecture

The SQLite schema consists of 5 normalized tables:

1. **`admins`**: Stores faculty credentials (`username`, `password_hash`, `full_name`, `email`).
2. **`students`**: Student records (`roll_no`, `full_name`, `email`, `department`, `semester`, `phone`, `dob`, `address`).
3. **`subjects`**: Course catalog (`subject_code`, `subject_name`, `department`, `semester`, `credits`).
4. **`attendance`**: Daily log (`student_id`, `date`, `status` ['Present', 'Absent', 'Late'], `marked_by`). Unique on `(student_id, date)`.
5. **`marks`**: Academic evaluation records (`student_id`, `subject_id`, `exam_type`, `marks_obtained`, `max_marks`, `remarks`).

---

## 🚀 Installation & Setup Instructions

### Prerequisites
- Python 3.8+ installed on your computer.
- `pip` (Python package manager).

### Step 1: Clone or Extract the Project
```bash
cd student-attendance-management
```

### Step 2: Create and Activate a Virtual Environment (Recommended)
```bash
# On Windows:
python -m venv venv
venv\Scripts\activate

# On macOS/Linux:
python3 -m venv venv
source venv/bin/activate
```

### Step 3: Install Required Dependencies
```bash
pip install -r requirements.txt
```

### Step 4: Initialize the Database (Optional)
The application initializes and seeds the database automatically on first startup. You can also manually seed the database:
```bash
python database.py
```

### Step 5: Run the Flask Development Server
```bash
python app.py
```

Open your browser and navigate to:
```text
http://127.0.0.1:5000/
```

---

## 🔑 Default Demo Login Credentials

The database comes pre-seeded with faculty and student records for instant testing:

### Administrator / Faculty Account
- **Username / Email**: `admin` or `admin@college.edu`
- **Password**: `admin123`
- *Access*: Full dashboard, add/edit/delete students, mark attendance, enter marks, generate reports.

### Sample Student Account
- **Roll Number**: `CS2023001`
- **Password**: `student123`
- *Access*: Personal attendance meter, exam eligibility status, academic marks, and personal history.

*(Additional pre-seeded student roll numbers: `CS2023002`, `CS2023003`, `CS2023004`, `CS2023005`)*

---

## 🔮 Future Enhancements
- Biometric & RFID card integration for automated classroom check-ins.
- Automated email / SMS alerts to parents when student attendance drops below 75%.
- Direct export of marks and attendance reports to Excel (.xlsx) and PDF format.
