CREATE TABLE exercises (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL UNIQUE,
    muscle_group  VARCHAR(20)  NOT NULL CHECK (muscle_group IN ('CHEST', 'BACK', 'SHOULDERS', 'BICEPS', 'TRICEPS',
                                                                'QUADS', 'HAMSTRINGS', 'GLUTES', 'CALVES', 'CORE')),
    movement_type VARCHAR(20)  NOT NULL CHECK (movement_type IN ('COMPOUND', 'ISOLATION')),
    equipment     VARCHAR(20)  NOT NULL CHECK (equipment IN ('BARBELL', 'DUMBBELL', 'MACHINE', 'CABLE', 'BODYWEIGHT'))
);

CREATE INDEX idx_exercises_muscle_group ON exercises (muscle_group);
