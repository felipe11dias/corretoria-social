-- Usuarios de demonstracao: permitem testar o login logo apos subir a aplicacao.
--
--   corretor.demo / corretor12345  -> ROLE_REALTOR
--   cliente.demo  / cliente12345   -> ROLE_CUSTOMER
--
-- As senhas estao em hash BCrypt (mesmo algoritmo do BCryptPasswordEncoder).
-- Sao apenas para teste: remova este seed antes de um ambiente real.
--
-- Nenhum INSERT informa "id": a PK e GENERATED ALWAYS AS IDENTITY e o PostgreSQL recusa
-- valor explicito. Os vinculos sao resolvidos por consulta ao login e ao nome do papel.

INSERT INTO users (name, email, login, password) VALUES
    ('Corretor Demonstracao', 'corretor.demo@corretoria.app', 'corretor.demo',
     '$2a$10$AtpAjpp6UkUggpvMCCARRuJRkpsVVTLwH10Rgf7HacSM31pkC3sbm'),
    ('Cliente Demonstracao', 'cliente.demo@corretoria.app', 'cliente.demo',
     '$2a$10$2byjLFFHhfsb913v.OPqW.e8jIk2SUTxkAfcc38/3pA6gwf85EfUq');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.name = 'ROLE_REALTOR'
WHERE u.login = 'corretor.demo';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.name = 'ROLE_CUSTOMER'
WHERE u.login = 'cliente.demo';

INSERT INTO addresses (user_id, street, number, complement, neighborhood, city, state, zip_code)
SELECT u.id, 'Avenida Dom Luis', '500', 'Sala 1201', 'Meireles', 'Fortaleza', 'CE', '60160230'
FROM users u
WHERE u.login = 'corretor.demo';

INSERT INTO addresses (user_id, street, number, complement, neighborhood, city, state, zip_code)
SELECT u.id, 'Rua Barao de Studart', '1980', NULL, 'Aldeota', 'Fortaleza', 'CE', '60120024'
FROM users u
WHERE u.login = 'cliente.demo';
