-- ============================================================
-- Ledger LMS – complete MySQL schema
-- ============================================================
-- Setup:
--   1. mysql -u root -p < schema-mysql.sql
--   2. Edit application.properties (DB password, mail, firebase)
--   3. mvn spring-boot:run
-- Hibernate ddl-auto=update also creates/updates tables on startup.
-- Passwords are stored as BCrypt hashes (never plain text).
-- Firebase is used only for Google Sign-In on login pages.
-- ============================================================

CREATE DATABASE IF NOT EXISTS ledgerlms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ledgerlms;

-- ---------- users (admin + student) ----------
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  department VARCHAR(80),
  status VARCHAR(30) DEFAULT 'Active',
  batch VARCHAR(40),
  section VARCHAR(20),
  roll_no VARCHAR(40),
  gender VARCHAR(20),
  mobile VARCHAR(15),
  course_name VARCHAR(120),
  academic_year VARCHAR(40),
  branch VARCHAR(80),
  email_verified BIT(1) DEFAULT 0,
  otp_code VARCHAR(10),
  otp_expiry DATETIME,
  reset_token VARCHAR(80),
  reset_token_expiry DATETIME,
  failed_login_attempts INT DEFAULT 0,
  lock_until DATETIME,
  joined_at DATETIME,
  last_active DATETIME,
  INDEX idx_users_role (role),
  INDEX idx_users_email (email)
);

-- ---------- platform_settings ----------
CREATE TABLE IF NOT EXISTS platform_settings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  platform_name VARCHAR(120) DEFAULT 'Ledger LMS',
  support_email VARCHAR(160) DEFAULT 'support@lms.edu',
  allow_self_registration BIT(1) DEFAULT 1,
  require_enrollment_approval BIT(1) DEFAULT 0,
  student_portal_enabled BIT(1) DEFAULT 1
);

-- ---------- courses (branch field lives on course; no admin Branches menu) ----------
CREATE TABLE IF NOT EXISTS courses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  code VARCHAR(40),
  category VARCHAR(80),
  branch VARCHAR(80),
  instructor_name VARCHAR(120),
  instructor_id BIGINT,
  enrolled_count INT DEFAULT 0,
  status VARCHAR(40) DEFAULT 'Draft',
  published BIT(1) DEFAULT 0,
  description TEXT,
  created_at DATETIME
);

CREATE TABLE IF NOT EXISTS course_materials (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT,
  title VARCHAR(200),
  type VARCHAR(40),
  file_path VARCHAR(500),
  content LONGTEXT,
  order_index INT DEFAULT 0,
  CONSTRAINT fk_cm_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS enrollments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT,
  student_name VARCHAR(120),
  course_id BIGINT,
  course_title VARCHAR(200),
  status VARCHAR(40) DEFAULT 'Active',
  progress INT DEFAULT 0,
  enrolled_at DATETIME,
  INDEX idx_enr_student (student_id),
  INDEX idx_enr_course (course_id)
);

CREATE TABLE IF NOT EXISTS material_progress (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT,
  material_id BIGINT,
  course_id BIGINT,
  completed BIT(1) DEFAULT 0,
  progress_percent INT DEFAULT 0,
  updated_at DATETIME,
  INDEX idx_mp_student (student_id)
);

-- ---------- assignments ----------
CREATE TABLE IF NOT EXISTS assignments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  serial_code VARCHAR(40),
  title VARCHAR(200),
  description TEXT,
  course_id BIGINT,
  course_title VARCHAR(200),
  branch VARCHAR(80),
  subject VARCHAR(120),
  max_marks INT DEFAULT 20,
  due_date DATETIME,
  status VARCHAR(40) DEFAULT 'Open',
  created_at DATETIME,
  created_by_admin_id BIGINT
);

CREATE TABLE IF NOT EXISTS assignment_submissions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  assignment_id BIGINT,
  student_id BIGINT,
  student_name VARCHAR(120),
  roll_no VARCHAR(40),
  file_path VARCHAR(500),
  marks DOUBLE,
  feedback TEXT,
  status VARCHAR(40) DEFAULT 'Submitted',
  submitted_at DATETIME,
  INDEX idx_asub_assignment (assignment_id),
  INDEX idx_asub_student (student_id)
);

-- ---------- quizzes ----------
CREATE TABLE IF NOT EXISTS quizzes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  serial_code VARCHAR(40),
  title VARCHAR(200),
  course_id BIGINT,
  course_title VARCHAR(200),
  branch VARCHAR(80),
  subject VARCHAR(120),
  duration_minutes INT DEFAULT 30,
  max_attempts INT DEFAULT 1,
  total_marks INT DEFAULT 20,
  status VARCHAR(40) DEFAULT 'Open',
  created_at DATETIME,
  created_by_admin_id BIGINT
);

CREATE TABLE IF NOT EXISTS quiz_questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  quiz_id BIGINT,
  question_text TEXT,
  option_a VARCHAR(500),
  option_b VARCHAR(500),
  option_c VARCHAR(500),
  option_d VARCHAR(500),
  correct_option VARCHAR(5),
  marks INT DEFAULT 1,
  order_index INT DEFAULT 0,
  CONSTRAINT fk_qq_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quiz_attempts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  quiz_id BIGINT,
  student_id BIGINT,
  student_name VARCHAR(120),
  roll_no VARCHAR(40),
  score DOUBLE,
  max_score DOUBLE,
  percent DOUBLE,
  answers_json TEXT,
  status VARCHAR(40),
  started_at DATETIME,
  submitted_at DATETIME,
  INDEX idx_qa_quiz (quiz_id),
  INDEX idx_qa_student (student_id)
);

-- ---------- exams ----------
CREATE TABLE IF NOT EXISTS exams (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  serial_code VARCHAR(40),
  title VARCHAR(200),
  subject VARCHAR(120),
  branch VARCHAR(80),
  course_id BIGINT,
  duration_minutes INT DEFAULT 60,
  total_marks INT DEFAULT 20,
  status VARCHAR(40) DEFAULT 'Scheduled',
  start_time DATETIME,
  end_time DATETIME,
  created_at DATETIME,
  created_by_admin_id BIGINT
);

CREATE TABLE IF NOT EXISTS exam_questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT,
  question_text TEXT,
  option_a VARCHAR(500),
  option_b VARCHAR(500),
  option_c VARCHAR(500),
  option_d VARCHAR(500),
  correct_option VARCHAR(5),
  marks INT DEFAULT 1,
  order_index INT DEFAULT 0,
  CONSTRAINT fk_eq_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS exam_attempts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT,
  student_id BIGINT,
  student_name VARCHAR(120),
  roll_no VARCHAR(40),
  score DOUBLE,
  max_score DOUBLE,
  grade VARCHAR(40),
  answers_json TEXT,
  tab_switches INT DEFAULT 0,
  camera_blackouts INT DEFAULT 0,
  video_path VARCHAR(500),
  status VARCHAR(40),
  started_at DATETIME,
  submitted_at DATETIME,
  resume_from_question INT DEFAULT 0,
  INDEX idx_ea_exam (exam_id),
  INDEX idx_ea_student (student_id)
);

-- ---------- labs (assignments-style) ----------
CREATE TABLE IF NOT EXISTS labs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200),
  description TEXT,
  branch VARCHAR(80),
  course_id BIGINT,
  due_date DATETIME,
  status VARCHAR(40) DEFAULT 'Open',
  created_at DATETIME
);

CREATE TABLE IF NOT EXISTS lab_submissions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  lab_id BIGINT,
  student_id BIGINT,
  student_name VARCHAR(120),
  file_path VARCHAR(500),
  marks DOUBLE,
  status VARCHAR(40),
  submitted_at DATETIME
);

-- ---------- lab VMs (Docker Ubuntu/Kali GUI) ----------
CREATE TABLE IF NOT EXISTS lab_vms (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200),
  os_type VARCHAR(80),
  description TEXT,
  status VARCHAR(40),
  branch VARCHAR(80),
  assigned_student_id BIGINT,
  assigned_student_name VARCHAR(120),
  created_by_admin_id BIGINT,
  container_id VARCHAR(80),
  host_port INT,
  novnc_url VARCHAR(255),
  access_url VARCHAR(255),
  ssh_host VARCHAR(120),
  ssh_port INT,
  ssh_username VARCHAR(80),
  ssh_password VARCHAR(120),
  created_at DATETIME
);

-- Lab VM click / activity log (export PDF / Word from admin)
CREATE TABLE IF NOT EXISTS lab_activities (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  lab_vm_id BIGINT,
  user_id BIGINT,
  user_name VARCHAR(120),
  user_role VARCHAR(20),
  action VARCHAR(80),
  details VARCHAR(500),
  created_at DATETIME,
  INDEX idx_lab_act_vm (lab_vm_id),
  INDEX idx_lab_act_user (user_id)
);

-- ---------- notifications ----------
CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  title VARCHAR(200),
  message TEXT,
  read_flag BIT(1) DEFAULT 0,
  created_at DATETIME
);

-- ---------- chat ----------
CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sender_id BIGINT,
  sender_name VARCHAR(120),
  receiver_id BIGINT,
  message TEXT,
  created_at DATETIME
);

-- ---------- question bank ----------
CREATE TABLE IF NOT EXISTS question_bank_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  subject VARCHAR(120),
  question_text TEXT,
  option_a VARCHAR(500),
  option_b VARCHAR(500),
  option_c VARCHAR(500),
  option_d VARCHAR(500),
  correct_option VARCHAR(5),
  difficulty VARCHAR(40),
  created_at DATETIME
);

-- Optional reference list (not shown in admin menu; courses store branch as text)
CREATE TABLE IF NOT EXISTS branches (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(40) UNIQUE,
  name VARCHAR(120),
  description VARCHAR(255)
);

-- Seed platform settings
INSERT INTO platform_settings (platform_name, support_email, allow_self_registration, require_enrollment_approval, student_portal_enabled)
SELECT 'Ledger LMS', 'support@lms.edu', 1, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM platform_settings LIMIT 1);

-- Optional sample branches for course dropdown convenience
INSERT IGNORE INTO branches (code, name) VALUES
('CS', 'Computer Science'),
('DITISS', 'Diploma in IT Infrastructure Systems & Security'),
('BDA', 'Big Data Analytics'),
('EC', 'Electronics & Communication');

-- NOTE: Create admin via /admin/register (email OTP) or app DataLoader.
-- Passwords must be BCrypt (app hashes on register/login upgrade).
