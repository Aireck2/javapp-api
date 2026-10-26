INSERT INTO roles (name)
VALUES ('admin'),
       ('student'),
       ('professor')
ON CONFLICT (name) DO NOTHING;
