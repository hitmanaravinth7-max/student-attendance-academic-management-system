import sqlite3
import os
from datetime import datetime, timedelta

try:
    from werkzeug.security import generate_password_hash, check_password_hash
except ImportError:
    import hashlib
    def generate_password_hash(password):
        return "sha256$" + hashlib.sha256(password.encode('utf-8')).hexdigest()

    def check_password_hash(p_hash, password):
        if p_hash.startswith("sha256$"):
            return p_hash == ("sha256$" + hashlib.sha256(password.encode('utf-8')).hexdigest())
        return p_hash == password

DATABASE_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'attendance_system.db')

def get_db():
    conn = sqlite3.connect(DATABASE_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn

def init_db():
    conn = get_db()
    cursor = conn.cursor()

    # 1. Admins Table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS admins (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        full_name TEXT NOT NULL,
        email TEXT UNIQUE NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    ''')

    # 2. Students Table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS students (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        roll_no TEXT UNIQUE NOT NULL,
        full_name TEXT NOT NULL,
        email TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        department TEXT NOT NULL,
        semester INTEGER NOT NULL,
        phone TEXT,
        dob TEXT,
        address TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    ''')

    # 3. Subjects Table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS subjects (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        subject_code TEXT UNIQUE NOT NULL,
        subject_name TEXT NOT NULL,
        department TEXT NOT NULL,
        semester INTEGER NOT NULL,
        credits INTEGER DEFAULT 3
    )
    ''')

    # 4. Attendance Table (one status per student per day)
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS attendance (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        student_id INTEGER NOT NULL,
        date TEXT NOT NULL,
        status TEXT NOT NULL CHECK(status IN ('Present', 'Absent', 'Late')),
        marked_by TEXT DEFAULT 'Admin',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
        UNIQUE(student_id, date)
    )
    ''')

    # 5. Academic Marks Table
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS marks (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        student_id INTEGER NOT NULL,
        subject_id INTEGER NOT NULL,
        exam_type TEXT NOT NULL CHECK(exam_type IN ('Internal 1', 'Internal 2', 'Assignment', 'Semester Final')),
        marks_obtained REAL NOT NULL,
        max_marks REAL NOT NULL DEFAULT 100,
        remarks TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
        FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
    )
    ''')

    conn.commit()

    # Seed initial sample data if admins table is empty
    cursor.execute("SELECT COUNT(*) FROM admins")
    if cursor.fetchone()[0] == 0:
        seed_data(conn)

    conn.close()

def seed_data(conn):
    cursor = conn.cursor()
    print("Seeding initial database data...")

    # Default Admin (username: admin, password: admin123)
    admin_pw = generate_password_hash("admin123")
    cursor.execute('''
    INSERT INTO admins (username, password_hash, full_name, email)
    VALUES (?, ?, ?, ?)
    ''', ('admin', admin_pw, 'Dr. Rajesh Sharma (HOD)', 'admin@college.edu'))

    # Subjects
    subjects = [
        ('CS501', 'Database Management Systems', 'Computer Science', 5, 4),
        ('CS502', 'Web Development with Python', 'Computer Science', 5, 4),
        ('CS503', 'Operating Systems', 'Computer Science', 5, 3),
        ('CS504', 'Computer Networks', 'Computer Science', 5, 3),
        ('CS505', 'Software Engineering & Agile', 'Computer Science', 5, 3),
        ('IT501', 'Cloud Computing & DevOps', 'Information Technology', 5, 4),
        ('IT502', 'Information Security', 'Information Technology', 5, 3)
    ]
    cursor.executemany('''
    INSERT OR IGNORE INTO subjects (subject_code, subject_name, department, semester, credits)
    VALUES (?, ?, ?, ?, ?)
    ''', subjects)

    # Sample Students (all default passwords: student123)
    student_pw = generate_password_hash("student123")
    students = [
        ('CS2023001', 'Aarav Mehta', 'aarav.mehta@college.edu', student_pw, 'Computer Science', 5, '9876543210', '2003-05-14', '124 Park Avenue, City'),
        ('CS2023002', 'Priya Iyer', 'priya.iyer@college.edu', student_pw, 'Computer Science', 5, '9876543211', '2003-08-22', '45 Green Meadows, City'),
        ('CS2023003', 'Rohan Verma', 'rohan.verma@college.edu', student_pw, 'Computer Science', 5, '9876543212', '2002-11-03', '89 Royal Residency, City'),
        ('CS2023004', 'Ananya Sengupta', 'ananya.s@college.edu', student_pw, 'Computer Science', 5, '9876543213', '2003-02-19', '12 Lake View Road, City'),
        ('CS2023005', 'Karan Johar', 'karan.j@college.edu', student_pw, 'Computer Science', 5, '9876543214', '2003-09-30', '77 Sunset Boulevard, City'),
        ('CS2023006', 'Sneha Kulkarni', 'sneha.k@college.edu', student_pw, 'Computer Science', 5, '9876543215', '2003-04-10', '34 MG Road, City'),
        ('IT2023001', 'Vikramaditya Das', 'vikram.das@college.edu', student_pw, 'Information Technology', 5, '9876543216', '2003-07-25', '56 Hill Top Colony, City'),
        ('IT2023002', 'Divya Nair', 'divya.nair@college.edu', student_pw, 'Information Technology', 5, '9876543217', '2003-12-15', '90 Temple Street, City')
    ]
    cursor.executemany('''
    INSERT OR IGNORE INTO students (roll_no, full_name, email, password_hash, department, semester, phone, dob, address)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    ''', students)

    conn.commit()

    # Generate attendance history for last 14 instructional days
    today = datetime.now()
    dates = []
    current_day = today - timedelta(days=20)
    while len(dates) < 14:
        # Exclude Saturdays (5) and Sundays (6)
        if current_day.weekday() < 5:
            dates.append(current_day.strftime('%Y-%m-%d'))
        current_day += timedelta(days=1)

    cursor.execute("SELECT id, roll_no FROM students")
    student_records = cursor.fetchall()

    attendance_data = []
    for s in student_records:
        s_id = s['id']
        roll = s['roll_no']
        for i, dt in enumerate(dates):
            # Realistic varying attendance patterns
            if roll == 'CS2023001': # high attendance ~93%
                status = 'Present' if i != 3 else 'Absent'
            elif roll == 'CS2023002': # perfect 100%
                status = 'Present'
            elif roll == 'CS2023003': # low attendance ~64% (ALERT!)
                status = 'Present' if i % 3 != 0 else 'Absent'
            elif roll == 'CS2023004': # borderline 71% (ALERT!)
                status = 'Present' if i not in [1, 5, 8, 12] else 'Absent'
            elif roll == 'CS2023005': # high 86%
                status = 'Present' if i not in [2, 9] else 'Late'
            else:
                status = 'Present' if (i + s_id) % 5 != 0 else 'Absent'

            attendance_data.append((s_id, dt, status, 'Dr. Sharma'))

    cursor.executemany('''
    INSERT OR IGNORE INTO attendance (student_id, date, status, marked_by)
    VALUES (?, ?, ?, ?)
    ''', attendance_data)

    # Seed Sample Academic Marks
    cursor.execute("SELECT id FROM subjects WHERE department = 'Computer Science'")
    cs_subject_ids = [row['id'] for row in cursor.fetchall()]

    marks_data = []
    # Add marks for first 6 CS students
    for s in student_records[:6]:
        s_id = s['id']
        for sub_id in cs_subject_ids:
            # Internal 1 (out of 50)
            m1 = 35 + ((s_id * 3 + sub_id) % 15)
            marks_data.append((s_id, sub_id, 'Internal 1', m1, 50, 'Good comprehension'))

            # Internal 2 (out of 50)
            m2 = 32 + ((s_id * 5 + sub_id) % 17)
            marks_data.append((s_id, sub_id, 'Internal 2', m2, 50, 'Consistent progress'))

            # Assignment (out of 20)
            m3 = 15 + ((s_id + sub_id) % 5)
            marks_data.append((s_id, sub_id, 'Assignment', m3, 20, 'Timely submission'))

    cursor.executemany('''
    INSERT OR IGNORE INTO marks (student_id, subject_id, exam_type, marks_obtained, max_marks, remarks)
    VALUES (?, ?, ?, ?, ?, ?)
    ''', marks_data)

    conn.commit()
    print("Database seeding completed successfully!")

def get_student_attendance_stats(student_id):
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute('''
    SELECT 
        COUNT(*) as total_days,
        SUM(CASE WHEN status IN ('Present', 'Late') THEN 1 ELSE 0 END) as attended_days,
        SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) as present_days,
        SUM(CASE WHEN status = 'Late' THEN 1 ELSE 0 END) as late_days,
        SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END) as absent_days
    FROM attendance
    WHERE student_id = ?
    ''', (student_id,))
    row = cursor.fetchone()
    conn.close()

    total = row['total_days'] or 0
    attended = row['attended_days'] or 0
    percentage = round((attended / total) * 100, 1) if total > 0 else 0.0

    return {
        'total_days': total,
        'attended_days': attended,
        'present_days': row['present_days'] or 0,
        'late_days': row['late_days'] or 0,
        'absent_days': row['absent_days'] or 0,
        'percentage': percentage,
        'is_low_attendance': percentage < 75.0
    }

def get_student_academic_summary(student_id):
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute('''
    SELECT 
        m.id,
        m.exam_type,
        m.marks_obtained,
        m.max_marks,
        m.remarks,
        s.subject_code,
        s.subject_name,
        s.credits
    FROM marks m
    JOIN subjects s ON m.subject_id = s.id
    WHERE m.student_id = ?
    ORDER BY s.subject_code, m.exam_type
    ''', (student_id,))
    rows = cursor.fetchall()

    # Calculate overall marks percentage
    cursor.execute('''
    SELECT 
        SUM(marks_obtained) as total_obtained,
        SUM(max_marks) as total_max
    FROM marks
    WHERE student_id = ?
    ''', (student_id,))
    overall = cursor.fetchone()
    conn.close()

    tot_obt = overall['total_obtained'] or 0
    tot_max = overall['total_max'] or 0
    overall_pct = round((tot_obt / tot_max) * 100, 1) if tot_max > 0 else 0.0

    # Grade determination
    if overall_pct >= 90:
        grade = 'A+ (Outstanding)'
    elif overall_pct >= 80:
        grade = 'A (Excellent)'
    elif overall_pct >= 70:
        grade = 'B+ (Very Good)'
    elif overall_pct >= 60:
        grade = 'B (Good)'
    elif overall_pct >= 50:
        grade = 'C (Pass)'
    else:
        grade = 'F (Needs Improvement)'

    return {
        'marks_list': [dict(r) for r in rows],
        'total_obtained': tot_obt,
        'total_max': tot_max,
        'overall_percentage': overall_pct,
        'grade': grade
    }

if __name__ == '__main__':
    init_db()
