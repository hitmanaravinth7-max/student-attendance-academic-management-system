from flask import Flask, render_template, request, redirect, url_for, session, flash, jsonify
import os
from datetime import datetime
from functools import wraps
from database import (
    get_db, init_db, get_student_attendance_stats, 
    get_student_academic_summary, generate_password_hash, check_password_hash
)

app = Flask(__name__)
app.secret_key = os.environ.get('SECRET_KEY', 'college_attendance_super_secret_key_2026')

# Initialize DB on start
init_db()

# --- Authentication Helpers & Decorators ---
def login_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if 'user_id' not in session:
            flash('Please log in to access this portal.', 'warning')
            return redirect(url_for('login'))
        return f(*args, **kwargs)
    return decorated_function

def admin_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if session.get('user_type') != 'admin':
            flash('Access restricted to college administrators and faculty only.', 'danger')
            return redirect(url_for('login'))
        return f(*args, **kwargs)
    return decorated_function

def student_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if session.get('user_type') != 'student':
            flash('Access restricted to enrolled students only.', 'danger')
            return redirect(url_for('login'))
        return f(*args, **kwargs)
    return decorated_function

@app.context_processor
def inject_now():
    return {'current_year': datetime.now().year, 'today_date': datetime.now().strftime('%Y-%m-%d')}

# --- Public & Auth Routes ---
@app.route('/')
def index():
    if 'user_id' in session:
        if session.get('user_type') == 'admin':
            return redirect(url_for('admin_dashboard'))
        else:
            return redirect(url_for('student_dashboard'))
    return redirect(url_for('login'))

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        login_type = request.form.get('login_type', 'admin') # 'admin' or 'student'
        identifier = request.form.get('identifier', '').strip()
        password = request.form.get('password', '').strip()

        if not identifier or not password:
            flash('Please provide both username/email and password.', 'danger')
            return render_template('login.html', login_type=login_type)

        conn = get_db()
        cursor = conn.cursor()

        if login_type == 'admin':
            cursor.execute("SELECT * FROM admins WHERE username = ? OR email = ?", (identifier, identifier))
            user = cursor.fetchone()
            if user and check_password_hash(user['password_hash'], password):
                session['user_id'] = user['id']
                session['user_type'] = 'admin'
                session['user_name'] = user['full_name']
                session['user_email'] = user['email']
                flash(f"Welcome back, {user['full_name']}!", 'success')
                return redirect(url_for('admin_dashboard'))
            else:
                flash('Invalid Administrator credentials.', 'danger')

        else: # student
            cursor.execute("SELECT * FROM students WHERE roll_no = ? OR email = ?", (identifier, identifier))
            user = cursor.fetchone()
            if user and check_password_hash(user['password_hash'], password):
                session['user_id'] = user['id']
                session['user_type'] = 'student'
                session['user_name'] = user['full_name']
                session['roll_no'] = user['roll_no']
                session['department'] = user['department']
                session['semester'] = user['semester']
                flash(f"Welcome, {user['full_name']}!", 'success')
                return redirect(url_for('student_dashboard'))
            else:
                flash('Invalid Student Roll Number or Password.', 'danger')

        conn.close()

    return render_template('login.html')

@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'POST':
        roll_no = request.form.get('roll_no', '').strip().upper()
        full_name = request.form.get('full_name', '').strip()
        email = request.form.get('email', '').strip().lower()
        password = request.form.get('password', '').strip()
        confirm_password = request.form.get('confirm_password', '').strip()
        department = request.form.get('department', '').strip()
        semester = request.form.get('semester', type=int)
        phone = request.form.get('phone', '').strip()
        dob = request.form.get('dob', '').strip()
        address = request.form.get('address', '').strip()

        # Validation
        if not roll_no or not full_name or not email or not password or not department or not semester:
            flash('Please fill in all mandatory fields.', 'danger')
            return render_template('register.html')

        if password != confirm_password:
            flash('Passwords do not match.', 'danger')
            return render_template('register.html')

        if len(password) < 6:
            flash('Password must be at least 6 characters.', 'danger')
            return render_template('register.html')

        conn = get_db()
        cursor = conn.cursor()

        # Check existing roll no or email
        cursor.execute("SELECT id FROM students WHERE roll_no = ? OR email = ?", (roll_no, email))
        if cursor.fetchone():
            flash('A student with this Roll Number or Email already exists.', 'warning')
            conn.close()
            return render_template('register.html')

        pw_hash = generate_password_hash(password)
        cursor.execute('''
        INSERT INTO students (roll_no, full_name, email, password_hash, department, semester, phone, dob, address)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (roll_no, full_name, email, pw_hash, department, semester, phone, dob, address))
        conn.commit()
        conn.close()

        flash('Registration successful! You can now log in with your Roll Number.', 'success')
        return redirect(url_for('login'))

    return render_template('register.html')

@app.route('/logout')
def logout():
    session.clear()
    flash('You have been safely logged out.', 'info')
    return redirect(url_for('login'))

# --- Admin Portal Routes ---
@app.route('/admin/dashboard')
@login_required
@admin_required
def admin_dashboard():
    conn = get_db()
    cursor = conn.cursor()

    # 1. Total Students Count
    cursor.execute("SELECT COUNT(*) FROM students")
    total_students = cursor.fetchone()[0]

    # 2. Total Attendance Records
    cursor.execute("SELECT COUNT(*) FROM attendance")
    total_attendance_records = cursor.fetchone()[0]

    # 3. Today's Attendance Overview
    today = datetime.now().strftime('%Y-%m-%d')
    cursor.execute('''
    SELECT 
        COUNT(*) as total_marked,
        SUM(CASE WHEN status IN ('Present', 'Late') THEN 1 ELSE 0 END) as present_count,
        SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END) as absent_count
    FROM attendance
    WHERE date = ?
    ''', (today,))
    today_stats = cursor.fetchone()

    # 4. Department Counts
    cursor.execute('''
    SELECT department, COUNT(*) as count 
    FROM students 
    GROUP BY department
    ''')
    dept_distribution = cursor.fetchall()

    # 5. Low Attendance Alert Students (< 75%)
    cursor.execute("SELECT id, roll_no, full_name, department, semester FROM students")
    all_students = cursor.fetchall()

    low_attendance_students = []
    for s in all_students:
        stats = get_student_attendance_stats(s['id'])
        if stats['total_days'] > 0 and stats['percentage'] < 75.0:
            low_attendance_students.append({
                'id': s['id'],
                'roll_no': s['roll_no'],
                'full_name': s['full_name'],
                'department': s['department'],
                'semester': s['semester'],
                'percentage': stats['percentage'],
                'attended': stats['attended_days'],
                'total': stats['total_days']
            })

    # Sort lowest percentage first
    low_attendance_students.sort(key=lambda x: x['percentage'])

    # 6. Recent Attendance Dates
    cursor.execute('''
    SELECT DISTINCT date, COUNT(*) as count 
    FROM attendance 
    GROUP BY date 
    ORDER BY date DESC 
    LIMIT 5
    ''')
    recent_dates = cursor.fetchall()

    conn.close()

    return render_template(
        'admin_dashboard.html',
        total_students=total_students,
        total_attendance_records=total_attendance_records,
        today_stats=today_stats,
        today_date=today,
        dept_distribution=dept_distribution,
        low_attendance_students=low_attendance_students,
        recent_dates=recent_dates
    )

# --- Students CRUD ---
@app.route('/admin/students')
@login_required
@admin_required
def manage_students():
    search_query = request.args.get('search', '').strip()
    department_filter = request.args.get('department', '').strip()
    semester_filter = request.args.get('semester', type=int)
    low_attendance_only = request.args.get('low_attendance') == '1'

    conn = get_db()
    cursor = conn.cursor()

    sql = "SELECT * FROM students WHERE 1=1"
    params = []

    if search_query:
        sql += " AND (full_name LIKE ? OR roll_no LIKE ? OR email LIKE ?)"
        term = f"%{search_query}%"
        params.extend([term, term, term])

    if department_filter:
        sql += " AND department = ?"
        params.append(department_filter)

    if semester_filter:
        sql += " AND semester = ?"
        params.append(semester_filter)

    sql += " ORDER BY roll_no ASC"

    cursor.execute(sql, params)
    students_raw = cursor.fetchall()

    # Attach live attendance percentage to each student
    students_list = []
    for s in students_raw:
        stats = get_student_attendance_stats(s['id'])
        if low_attendance_only and stats['percentage'] >= 75.0 and stats['total_days'] > 0:
            continue
        students_list.append({
            'student': s,
            'attendance': stats
        })

    # Distinct Departments for filter dropdown
    cursor.execute("SELECT DISTINCT department FROM students ORDER BY department")
    departments = [row['department'] for row in cursor.fetchall()]

    conn.close()

    return render_template(
        'students/index.html',
        students_list=students_list,
        departments=departments,
        search_query=search_query,
        selected_department=department_filter,
        selected_semester=semester_filter,
        low_attendance_only=low_attendance_only
    )

@app.route('/admin/students/add', methods=['GET', 'POST'])
@login_required
@admin_required
def add_student():
    if request.method == 'POST':
        roll_no = request.form.get('roll_no', '').strip().upper()
        full_name = request.form.get('full_name', '').strip()
        email = request.form.get('email', '').strip().lower()
        password = request.form.get('password', '').strip()
        department = request.form.get('department', '').strip()
        semester = request.form.get('semester', type=int)
        phone = request.form.get('phone', '').strip()
        dob = request.form.get('dob', '').strip()
        address = request.form.get('address', '').strip()

        if not roll_no or not full_name or not email or not department or not semester:
            flash('Please complete all required fields.', 'danger')
            return render_template('students/form.html', student=None)

        if not password:
            password = 'student123' # default password

        conn = get_db()
        cursor = conn.cursor()

        cursor.execute("SELECT id FROM students WHERE roll_no = ? OR email = ?", (roll_no, email))
        if cursor.fetchone():
            flash('A student with this Roll Number or Email already exists.', 'warning')
            conn.close()
            return render_template('students/form.html', student=None)

        pw_hash = generate_password_hash(password)
        cursor.execute('''
        INSERT INTO students (roll_no, full_name, email, password_hash, department, semester, phone, dob, address)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (roll_no, full_name, email, pw_hash, department, semester, phone, dob, address))
        conn.commit()
        conn.close()

        flash(f'Student {full_name} ({roll_no}) added successfully!', 'success')
        return redirect(url_for('manage_students'))

    return render_template('students/form.html', student=None)

@app.route('/admin/students/edit/<int:id>', methods=['GET', 'POST'])
@login_required
@admin_required
def edit_student(id):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM students WHERE id = ?", (id,))
    student = cursor.fetchone()

    if not student:
        flash('Student record not found.', 'danger')
        conn.close()
        return redirect(url_for('manage_students'))

    if request.method == 'POST':
        roll_no = request.form.get('roll_no', '').strip().upper()
        full_name = request.form.get('full_name', '').strip()
        email = request.form.get('email', '').strip().lower()
        department = request.form.get('department', '').strip()
        semester = request.form.get('semester', type=int)
        phone = request.form.get('phone', '').strip()
        dob = request.form.get('dob', '').strip()
        address = request.form.get('address', '').strip()
        new_password = request.form.get('password', '').strip()

        # Check unique constraint with other students
        cursor.execute("SELECT id FROM students WHERE (roll_no = ? OR email = ?) AND id != ?", (roll_no, email, id))
        if cursor.fetchone():
            flash('Another student already has this Roll Number or Email.', 'warning')
            conn.close()
            return render_template('students/form.html', student=student)

        if new_password:
            pw_hash = generate_password_hash(new_password)
            cursor.execute('''
            UPDATE students 
            SET roll_no = ?, full_name = ?, email = ?, department = ?, semester = ?, phone = ?, dob = ?, address = ?, password_hash = ?
            WHERE id = ?
            ''', (roll_no, full_name, email, department, semester, phone, dob, address, pw_hash, id))
        else:
            cursor.execute('''
            UPDATE students 
            SET roll_no = ?, full_name = ?, email = ?, department = ?, semester = ?, phone = ?, dob = ?, address = ?
            WHERE id = ?
            ''', (roll_no, full_name, email, department, semester, phone, dob, address, id))

        conn.commit()
        conn.close()
        flash('Student details updated successfully.', 'success')
        return redirect(url_for('manage_students'))

    conn.close()
    return render_template('students/form.html', student=student)

@app.route('/admin/students/delete/<int:id>', methods=['POST'])
@login_required
@admin_required
def delete_student(id):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT full_name, roll_no FROM students WHERE id = ?", (id,))
    student = cursor.fetchone()

    if student:
        cursor.execute("DELETE FROM students WHERE id = ?", (id,))
        conn.commit()
        flash(f"Student {student['full_name']} ({student['roll_no']}) deleted successfully.", 'success')
    else:
        flash("Student not found.", 'danger')

    conn.close()
    return redirect(url_for('manage_students'))

@app.route('/admin/students/<int:id>')
@login_required
@admin_required
def view_student(id):
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM students WHERE id = ?", (id,))
    student = cursor.fetchone()

    if not student:
        flash('Student record not found.', 'danger')
        conn.close()
        return redirect(url_for('manage_students'))

    # Attendance stats and daily history
    attendance_stats = get_student_attendance_stats(id)
    cursor.execute('''
    SELECT date, status, marked_by 
    FROM attendance 
    WHERE student_id = ? 
    ORDER BY date DESC
    ''', (id,))
    attendance_history = cursor.fetchall()

    # Academic Marks
    academic_summary = get_student_academic_summary(id)

    conn.close()

    return render_template(
        'students/view.html',
        student=student,
        attendance_stats=attendance_stats,
        attendance_history=attendance_history,
        academic_summary=academic_summary
    )

# --- Attendance Management Routes ---
@app.route('/admin/attendance', methods=['GET', 'POST'])
@login_required
@admin_required
def mark_attendance():
    conn = get_db()
    cursor = conn.cursor()

    selected_date = request.args.get('date', datetime.now().strftime('%Y-%m-%d'))
    selected_dept = request.args.get('department', 'Computer Science')
    selected_sem = request.args.get('semester', 5, type=int)

    if request.method == 'POST':
        # Batch save attendance
        form_date = request.form.get('date', selected_date)
        form_dept = request.form.get('department', selected_dept)
        form_sem = request.form.get('semester', selected_sem, type=int)

        cursor.execute('''
        SELECT id FROM students 
        WHERE department = ? AND semester = ?
        ''', (form_dept, form_sem))
        students = cursor.fetchall()

        marked_count = 0
        for s in students:
            student_id = s['id']
            status = request.form.get(f"status_{student_id}", 'Present')

            # Upsert into attendance
            cursor.execute('''
            INSERT INTO attendance (student_id, date, status, marked_by)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(student_id, date) DO UPDATE SET
                status = excluded.status,
                marked_by = excluded.marked_by
            ''', (student_id, form_date, status, session.get('user_name', 'Admin')))
            marked_count += 1

        conn.commit()
        flash(f"Attendance for {marked_count} students saved successfully for {form_date}.", 'success')
        return redirect(url_for('mark_attendance', date=form_date, department=form_dept, semester=form_sem))

    # Fetch students for current filter
    cursor.execute('''
    SELECT id, roll_no, full_name 
    FROM students 
    WHERE department = ? AND semester = ?
    ORDER BY roll_no ASC
    ''', (selected_dept, selected_sem))
    students = cursor.fetchall()

    # Fetch existing marked attendance for this date
    cursor.execute('''
    SELECT student_id, status 
    FROM attendance 
    WHERE date = ?
    ''', (selected_date,))
    existing_status = {row['student_id']: row['status'] for row in cursor.fetchall()}

    # All departments
    cursor.execute("SELECT DISTINCT department FROM students ORDER BY department")
    departments = [row['department'] for row in cursor.fetchall()]

    conn.close()

    return render_template(
        'attendance/mark.html',
        students=students,
        existing_status=existing_status,
        selected_date=selected_date,
        selected_dept=selected_dept,
        selected_sem=selected_sem,
        departments=departments
    )

@app.route('/admin/attendance/report')
@login_required
@admin_required
def attendance_report():
    selected_dept = request.args.get('department', '')
    selected_sem = request.args.get('semester', type=int)

    conn = get_db()
    cursor = conn.cursor()

    sql = "SELECT id, roll_no, full_name, department, semester FROM students WHERE 1=1"
    params = []
    if selected_dept:
        sql += " AND department = ?"
        params.append(selected_dept)
    if selected_sem:
        sql += " AND semester = ?"
        params.append(selected_sem)

    sql += " ORDER BY roll_no ASC"
    cursor.execute(sql, params)
    students = cursor.fetchall()

    report_list = []
    for s in students:
        stats = get_student_attendance_stats(s['id'])
        report_list.append({
            'student': s,
            'stats': stats
        })

    cursor.execute("SELECT DISTINCT department FROM students ORDER BY department")
    departments = [row['department'] for row in cursor.fetchall()]

    conn.close()

    return render_template(
        'attendance/report.html',
        report_list=report_list,
        departments=departments,
        selected_dept=selected_dept,
        selected_sem=selected_sem
    )

# --- Academic Marks Management ---
@app.route('/admin/marks', methods=['GET', 'POST'])
@login_required
@admin_required
def manage_marks():
    conn = get_db()
    cursor = conn.cursor()

    # Subjects for selection
    cursor.execute("SELECT id, subject_code, subject_name, department, semester FROM subjects ORDER BY subject_code")
    subjects = cursor.fetchall()

    selected_subject_id = request.args.get('subject_id', type=int)
    if not selected_subject_id and subjects:
        selected_subject_id = subjects[0]['id']

    selected_exam = request.args.get('exam_type', 'Internal 1')

    # Selected subject details
    selected_subject = next((s for s in subjects if s['id'] == selected_subject_id), None)

    if request.method == 'POST':
        sub_id = request.form.get('subject_id', type=int)
        exam_type = request.form.get('exam_type', 'Internal 1')
        max_marks = request.form.get('max_marks', 50, type=float)

        cursor.execute('''
        SELECT id FROM students 
        WHERE department = (SELECT department FROM subjects WHERE id = ?)
          AND semester = (SELECT semester FROM subjects WHERE id = ?)
        ''', (sub_id, sub_id))
        eligible_students = cursor.fetchall()

        saved_count = 0
        for s in eligible_students:
            s_id = s['id']
            marks_val = request.form.get(f"marks_{s_id}")
            remarks_val = request.form.get(f"remarks_{s_id}", '')

            if marks_val is not None and marks_val.strip() != '':
                try:
                    marks_num = float(marks_val)
                    # Check if entry already exists
                    cursor.execute('''
                    SELECT id FROM marks 
                    WHERE student_id = ? AND subject_id = ? AND exam_type = ?
                    ''', (s_id, sub_id, exam_type))
                    existing = cursor.fetchone()

                    if existing:
                        cursor.execute('''
                        UPDATE marks 
                        SET marks_obtained = ?, max_marks = ?, remarks = ?
                        WHERE id = ?
                        ''', (marks_num, max_marks, remarks_val, existing['id']))
                    else:
                        cursor.execute('''
                        INSERT INTO marks (student_id, subject_id, exam_type, marks_obtained, max_marks, remarks)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ''', (s_id, sub_id, exam_type, marks_num, max_marks, remarks_val))
                    saved_count += 1
                except ValueError:
                    pass

        conn.commit()
        flash(f"Marks successfully recorded for {saved_count} students.", 'success')
        return redirect(url_for('manage_marks', subject_id=sub_id, exam_type=exam_type))

    # Fetch eligible students for selected subject's department & semester
    students_with_marks = []
    if selected_subject:
        cursor.execute('''
        SELECT id, roll_no, full_name 
        FROM students 
        WHERE department = ? AND semester = ?
        ORDER BY roll_no ASC
        ''', (selected_subject['department'], selected_subject['semester']))
        students = cursor.fetchall()

        # Existing marks for this subject & exam
        cursor.execute('''
        SELECT student_id, marks_obtained, max_marks, remarks 
        FROM marks 
        WHERE subject_id = ? AND exam_type = ?
        ''', (selected_subject_id, selected_exam))
        marks_map = {row['student_id']: row for row in cursor.fetchall()}

        for s in students:
            students_with_marks.append({
                'student': s,
                'mark': marks_map.get(s['id'])
            })

    conn.close()

    return render_template(
        'marks/manage.html',
        subjects=subjects,
        selected_subject=selected_subject,
        selected_subject_id=selected_subject_id,
        selected_exam=selected_exam,
        students_with_marks=students_with_marks
    )

# --- Student Portal Routes ---
@app.route('/student/dashboard')
@login_required
@student_required
def student_dashboard():
    student_id = session.get('user_id')
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM students WHERE id = ?", (student_id,))
    student = cursor.fetchone()

    # Attendance Stats
    attendance_stats = get_student_attendance_stats(student_id)

    # Recent 7 attendance records
    cursor.execute('''
    SELECT date, status, marked_by 
    FROM attendance 
    WHERE student_id = ? 
    ORDER BY date DESC 
    LIMIT 7
    ''', (student_id,))
    recent_attendance = cursor.fetchall()

    # Academic Marks & Grades
    academic_summary = get_student_academic_summary(student_id)

    # Department Subjects
    cursor.execute('''
    SELECT subject_code, subject_name, credits 
    FROM subjects 
    WHERE department = ? AND semester = ?
    ''', (student['department'], student['semester']))
    registered_subjects = cursor.fetchall()

    conn.close()

    return render_template(
        'student_dashboard.html',
        student=student,
        attendance_stats=attendance_stats,
        recent_attendance=recent_attendance,
        academic_summary=academic_summary,
        registered_subjects=registered_subjects
    )

@app.route('/student/attendance')
@login_required
@student_required
def student_attendance_history():
    student_id = session.get('user_id')
    conn = get_db()
    cursor = conn.cursor()

    attendance_stats = get_student_attendance_stats(student_id)

    cursor.execute('''
    SELECT date, status, marked_by 
    FROM attendance 
    WHERE student_id = ? 
    ORDER BY date DESC
    ''', (student_id,))
    all_attendance = cursor.fetchall()
    conn.close()

    return render_template(
        'student_attendance.html',
        attendance_stats=attendance_stats,
        all_attendance=all_attendance
    )

@app.route('/student/profile', methods=['GET', 'POST'])
@login_required
@student_required
def student_profile():
    student_id = session.get('user_id')
    conn = get_db()
    cursor = conn.cursor()

    if request.method == 'POST':
        phone = request.form.get('phone', '').strip()
        address = request.form.get('address', '').strip()
        current_password = request.form.get('current_password', '').strip()
        new_password = request.form.get('new_password', '').strip()

        cursor.execute("SELECT password_hash FROM students WHERE id = ?", (student_id,))
        curr_user = cursor.fetchone()

        if new_password:
            if not current_password or not check_password_hash(curr_user['password_hash'], current_password):
                flash('Current password verification failed.', 'danger')
                conn.close()
                return redirect(url_for('student_profile'))

            if len(new_password) < 6:
                flash('New password must be at least 6 characters.', 'danger')
                conn.close()
                return redirect(url_for('student_profile'))

            new_hash = generate_password_hash(new_password)
            cursor.execute('''
            UPDATE students 
            SET phone = ?, address = ?, password_hash = ?
            WHERE id = ?
            ''', (phone, address, new_hash, student_id))
        else:
            cursor.execute('''
            UPDATE students 
            SET phone = ?, address = ?
            WHERE id = ?
            ''', (phone, address, student_id))

        conn.commit()
        flash('Profile contact details updated successfully.', 'success')
        conn.close()
        return redirect(url_for('student_profile'))

    cursor.execute("SELECT * FROM students WHERE id = ?", (student_id,))
    student = cursor.fetchone()
    conn.close()

    return render_template('student_profile.html', student=student)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
