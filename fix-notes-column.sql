-- Run once if notes still fail after update (existing MySQL DB may keep old VARCHAR):
USE ledgerlms;
ALTER TABLE course_materials MODIFY COLUMN content LONGTEXT;
ALTER TABLE courses MODIFY COLUMN description LONGTEXT;
