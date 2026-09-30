from database import get_db_connection, calculate_grade
from werkzeug.security import generate_password_hash, check_password_hash

class UserModel:
    @staticmethod
    def get_by_username(username):
        conn = get_db_connection()
        user = conn.execute("SELECT * FROM users WHERE username = ?", (username.strip().lower(),)).fetchone()
        conn.close()
        return user

    @staticmethod
    def get_by_id(user_id):
        conn = get_db_connection()
        user = conn.execute("SELECT * FROM users WHERE id = ?", (user_id,)).fetchone()
        conn.close()
        return user

    @staticmethod
    def verify_password(stored_hash, password):
        return check_password_hash(stored_hash, password)


class StudentModel:
    @staticmethod
    def get_all(search_query="", department="", semester=""):
        conn = get_db_connection()
        query = """
            SELECT s.*, u.username 
            FROM students s 
            JOIN users u ON s.user_id = u.id 
            WHERE 1=1
        """
        params = []

        if search_query:
            query += " AND (s.full_name LIKE ? OR s.roll_number LIKE ? OR s.email LIKE ?)"
            wildcard = f"%{search_query.strip()}%"
            params.extend([wildcard, wildcard, wildcard])

        if department:
            query += " AND s.department = ?"
            params.append(department)

        if semester:
            query += " AND s.semester = ?"
            params.append(semester)

        query += " ORDER BY s.roll_number ASC"
        students = conn.execute(query, params).fetchall()
        conn.close()
        return students

    @staticmethod
    def get_by_id(student_id):
        conn = get_db_connection()
        student = conn.execute("SELECT * FROM students WHERE id = ?", (student_id,)).fetchone()
        conn.close()
        return student

    @staticmethod
    def get_by_user_id(user_id):
        conn = get_db_connection()
        student = conn.execute("SELECT * FROM students WHERE user_id = ?", (user_id,)).fetchone()
        conn.close()
        return student

    @staticmethod
    def create(roll_number, full_name, email, phone, department, semester, section, password, address=""):
        conn = get_db_connection()
        try:
            pw_hash = generate_password_hash(password)
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                (roll_number.strip().lower(), pw_hash, "student")
            )
            user_id = cursor.lastrowid

            cursor.execute("""
                INSERT INTO students (user_id, roll_number, full_name, email, phone, department, semester, section, address)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (user_id, roll_number.strip().upper(), full_name.strip(), email.strip(), phone.strip(), department, semester, section, address.strip()))
            student_id = cursor.lastrowid
            conn.commit()
            return student_id
        except Exception as e:
            conn.rollback()
            raise e
        finally:
            conn.close()

    @staticmethod
    def update(student_id, full_name, email, phone, department, semester, section, address):
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("""
            UPDATE students
            SET full_name = ?, email = ?, phone = ?, department = ?, semester = ?, section = ?, address = ?
            WHERE id = ?
        """, (full_name.strip(), email.strip(), phone.strip(), department, semester, section, address.strip(), student_id))
        conn.commit()
        conn.close()

    @staticmethod
    def delete(student_id):
        conn = get_db_connection()
        cursor = conn.cursor()
        # Find user_id
        student = cursor.execute("SELECT user_id FROM students WHERE id = ?", (student_id,)).fetchone()
        if student:
            # Deleting from users cascades to students, attendance, marks
            cursor.execute("DELETE FROM users WHERE id = ?", (student["user_id"],))
            conn.commit()
        conn.close()


class AttendanceModel:
    @staticmethod
    def get_student_overall_stats(student_id):
        """Calculates total classes, attended classes, and overall attendance percentage."""
        conn = get_db_connection()
        rows = conn.execute("""
            SELECT 
                COUNT(*) as total_classes,
                SUM(CASE WHEN status IN ('Present', 'Late') THEN 1 ELSE 0 END) as attended_classes,
                SUM(CASE WHEN status = 'Absent' THEN 1 ELSE 0 END) as absent_classes
            FROM attendance
            WHERE student_id = ?
        """, (student_id,)).fetchone()
        conn.close()

        total = rows["total_classes"] or 0
        attended = rows["attended_classes"] or 0
        absent = rows["absent_classes"] or 0
        percentage = round((attended / total * 100), 1) if total > 0 else 0.0

        return {
            "total_classes": total,
            "attended_classes": attended,
            "absent_classes": absent,
            "percentage": percentage,
            "is_shortage": percentage < 75.0 and total > 0
        }

    @staticmethod
    def get_subject_wise_stats(student_id):
        """Calculates subject-wise attendance breakdown for a student."""
        conn = get_db_connection()
        rows = conn.execute("""
            SELECT 
                c.id as course_id,
                c.course_code,
                c.course_name,
                COUNT(a.id) as total_held,
                SUM(CASE WHEN a.status IN ('Present', 'Late') THEN 1 ELSE 0 END) as attended
            FROM courses c
            LEFT JOIN attendance a ON c.id = a.course_id AND a.student_id = ?
            GROUP BY c.id, c.course_code, c.course_name
            HAVING total_held > 0
            ORDER BY c.course_code ASC
        """, (student_id,)).fetchall()
        conn.close()

        results = []
        for r in rows:
            total = r["total_held"] or 0
            att = r["attended"] or 0
            pct = round((att / total * 100), 1) if total > 0 else 0.0
            results.append({
                "course_id": r["course_id"],
                "course_code": r["course_code"],
                "course_name": r["course_name"],
                "total_held": total,
                "attended": att,
                "percentage": pct,
                "is_shortage": pct < 75.0
            })
        return results

    @staticmethod
    def get_recent_records(student_id, limit=10):
        conn = get_db_connection()
        records = conn.execute("""
            SELECT a.*, c.course_code, c.course_name
            FROM attendance a
            JOIN courses c ON a.course_id = c.id
            WHERE a.student_id = ?
            ORDER BY a.date DESC
            LIMIT ?
        """, (student_id, limit)).fetchall()
        conn.close()
        return records

    @staticmethod
    def mark_bulk(course_id, date, attendance_dict, marked_by):
        """Records attendance for multiple students for a specific course and date."""
        conn = get_db_connection()
        cursor = conn.cursor()
        for student_id_str, status in attendance_dict.items():
            s_id = int(student_id_str)
            cursor.execute("""
                INSERT INTO attendance (student_id, course_id, date, status, marked_by)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(student_id, course_id, date) 
                DO UPDATE SET status = excluded.status, marked_by = excluded.marked_by
            """, (s_id, course_id, date, status, marked_by))
        conn.commit()
        conn.close()


class MarksModel:
    @staticmethod
    def get_by_student(student_id):
        conn = get_db_connection()
        marks = conn.execute("""
            SELECT m.*, c.course_code, c.course_name, c.credits
            FROM marks m
            JOIN courses c ON m.course_id = c.id
            WHERE m.student_id = ?
            ORDER BY c.course_code ASC, m.recorded_at DESC
        """, (student_id,)).fetchall()
        conn.close()
        return marks

    @staticmethod
    def add_mark(student_id, course_id, exam_type, max_marks, obtained_marks, remarks=""):
        percentage = (float(obtained_marks) / float(max_marks)) * 100
        grade = calculate_grade(percentage)
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO marks (student_id, course_id, exam_type, max_marks, obtained_marks, grade, remarks)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (student_id, course_id, exam_type, max_marks, obtained_marks, grade, remarks))
        conn.commit()
        conn.close()

    @staticmethod
    def delete(mark_id):
        conn = get_db_connection()
        conn.execute("DELETE FROM marks WHERE id = ?", (mark_id,))
        conn.commit()
        conn.close()


class CourseModel:
    @staticmethod
    def get_all():
        conn = get_db_connection()
        courses = conn.execute("SELECT * FROM courses ORDER BY course_code ASC").fetchall()
        conn.close()
        return courses

    @staticmethod
    def get_by_department_and_semester(department, semester):
        conn = get_db_connection()
        courses = conn.execute(
            "SELECT * FROM courses WHERE department = ? AND semester = ? ORDER BY course_code ASC",
            (department, semester)
        ).fetchall()
        conn.close()
        return courses
