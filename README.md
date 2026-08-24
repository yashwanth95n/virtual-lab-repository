# Ledger LMS (Spring Boot + MySQL + Docker Labs)

## Features
- Separate **Admin** and **Student** portals
- MySQL storage for all entities
- **BCrypt** password hashing (admin + student)
- **Email OTP** (6 digits) on registration
- **Password reset** via email code
- **Firebase Google Sign-In** (optional; password login always available)
- Student portal **enable/disable** from Admin → Profile
- **5 failed logins → 5 minute lockout**
- Phone must be **exactly 10 digits** on register
- Branch is set on **Courses** (no Branches menu)
- Lab VM: Docker Ubuntu/Kali GUI (noVNC), activity log export **PDF + Word**
- Profile (renamed from Settings) in both portals

## Tables (see `schema-mysql.sql`)
users, platform_settings, courses, course_materials, enrollments, material_progress,
assignments, assignment_submissions, quizzes, quiz_questions, quiz_attempts,
exams, exam_questions, exam_attempts, labs, lab_submissions, lab_vms, lab_activities,
notifications, chat_messages, question_bank_items, branches (optional reference only)

## Setup
```bash
# 1. MySQL
mysql -u root -p < schema-mysql.sql
# or let the app create DB (createDatabaseIfNotExist=true)

# 2. Edit src/main/resources/application.properties
#    - spring.datasource.password
#    - spring.mail.*  (Gmail app password for OTP)
#    - firebase.*     (optional Google Sign-In)

# 3. Run
mvn spring-boot:run
# App: http://localhost:8085
```

Default admin (first run): `admin@ledger.lms` / `admin123`

## Docker Lab
Install Docker Desktop. First launch pulls `lscr.io/linuxserver/webtop:ubuntu-xfce`.
Admin → Lab VM → Create → Launch → Open desktop (embedded noVNC).
Activity: Admin → Lab VM → View/export lab activity (PDF & Word).
