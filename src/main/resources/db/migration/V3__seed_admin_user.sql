-- Usuario administrador de demonstracao:
--
--   admin.demo / admin12345 -> ROLE_ADMIN
--
-- Este seed existe porque ROLE_ADMIN NAO pode ser obtido pelo autocadastro publico: o
-- UserService rejeita a solicitacao com 403 (Etapas 9 e 10). O seed e, portanto, a via
-- legitima de criar o primeiro administrador.
--
-- Fica em migration separada da V2 de proposito: quem quiser um ambiente sem administrador
-- de demonstracao remove apenas esta.

INSERT INTO users (name, email, login, password) VALUES
    ('Administrador Demonstracao', 'admin.demo@corretoria.app', 'admin.demo',
     '$2a$10$tvqL6fessVJgUGQDDtl7zuvJ/dwwP8LfaH7xzCnl9SryLx4EEPAGS');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.name = 'ROLE_ADMIN'
WHERE u.login = 'admin.demo';
