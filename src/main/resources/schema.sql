CREATE TABLE IF NOT EXISTS student_results (
    id SERIAL PRIMARY KEY,
    student_id VARCHAR(255) NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    score INTEGER NOT NULL,
    grade VARCHAR(10)
);