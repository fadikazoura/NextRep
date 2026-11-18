CREATE TABLE users (
                       id            BIGSERIAL    PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_profiles (
                               user_id          BIGINT      PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
                               birth_date       DATE        NOT NULL,
                               sex              VARCHAR(10) NOT NULL CHECK (sex IN ('MALE', 'FEMALE')),
                               height_cm        SMALLINT    NOT NULL CHECK (height_cm BETWEEN 100 AND 250),
                               experience_level VARCHAR(20) NOT NULL CHECK (experience_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
                               goal             VARCHAR(20) NOT NULL CHECK (goal IN ('STRENGTH', 'HYPERTROPHY', 'ENDURANCE')),
                               session_minutes  SMALLINT    NOT NULL CHECK (session_minutes BETWEEN 20 AND 180),
                               equipment        VARCHAR(20) NOT NULL CHECK (equipment IN ('FULL_GYM', 'DUMBBELLS_ONLY', 'HOME')),
                               weight_unit      VARCHAR(5)  NOT NULL DEFAULT 'KG' CHECK (weight_unit IN ('KG', 'LB')),
                               injuries         TEXT,
                               updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_training_days (
                                    user_id     BIGINT     NOT NULL REFERENCES user_profiles (user_id) ON DELETE CASCADE,
                                    day_of_week VARCHAR(9) NOT NULL CHECK (day_of_week IN
                                                                           ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
                                    PRIMARY KEY (user_id, day_of_week)
);
