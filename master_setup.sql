-- =================================================================
-- FilmFolio Master Database Setup Script (Builds and Populates)
-- =================================================================

-- Step 1: Drop all existing objects to ensure a clean slate.
PROMPT Dropping existing tables and sequences...
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Likes PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Comments PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Watchlist PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Reviews PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Movie_Cast PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Users PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Movies PURGE'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE user_id_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE movie_id_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE review_id_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE comment_id_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE like_id_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE watchlist_id_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- Step 2: Create all sequences.
PROMPT Creating sequences...
CREATE SEQUENCE user_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE movie_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE review_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE comment_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE like_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE watchlist_id_seq START WITH 1 INCREMENT BY 1;

-- Step 3: Create all tables.
PROMPT Creating tables...
CREATE TABLE Movies (
    movie_id NUMBER PRIMARY KEY,
    title VARCHAR2(255) NOT NULL,
    release_year NUMBER(4),
    director VARCHAR2(100)
);

CREATE TABLE Users (
    user_id NUMBER PRIMARY KEY,
    username VARCHAR2(50) NOT NULL UNIQUE,
    email VARCHAR2(100) NOT NULL UNIQUE,
    password_hash VARCHAR2(100) NOT NULL
);

CREATE TABLE Watchlist (
    watchlist_id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL REFERENCES Users(user_id),
    movie_id NUMBER NOT NULL REFERENCES Movies(movie_id),
    CONSTRAINT uq_user_movie_watchlist UNIQUE (user_id, movie_id)
);

-- Step 4: Populate the tables with sample data.
PROMPT Populating tables...

-- Insert movies
INSERT INTO Movies (movie_id, title, release_year, director) VALUES (movie_id_seq.NEXTVAL, 'Inception', 2010, 'Christopher Nolan');
INSERT INTO Movies (movie_id, title, release_year, director) VALUES (movie_id_seq.NEXTVAL, 'The Godfather', 1972, 'Francis Ford Coppola');
INSERT INTO Movies (movie_id, title, release_year, director) VALUES (movie_id_seq.NEXTVAL, 'Parasite', 2019, 'Bong Joon Ho');

-- Insert the one and only sample user
PROMPT Creating user 'jdoe' with password 'password123'...
INSERT INTO Users (user_id, username, email, password_hash) VALUES (user_id_seq.NEXTVAL, 'jdoe', 'j.doe@example.com', 'password123');

-- Populate the watchlist for 'jdoe'
PROMPT Populating watchlist for user 'jdoe'...
DECLARE
    v_user_id NUMBER;
    v_movie_id_godfather NUMBER;
    v_movie_id_parasite NUMBER;
BEGIN
    SELECT user_id INTO v_user_id FROM Users WHERE username = 'jdoe';
    SELECT movie_id INTO v_movie_id_godfather FROM Movies WHERE title = 'The Godfather';
    SELECT movie_id INTO v_movie_id_parasite FROM Movies WHERE title = 'Parasite';
    INSERT INTO Watchlist (watchlist_id, user_id, movie_id) VALUES (watchlist_id_seq.NEXTVAL, v_user_id, v_movie_id_godfather);
    INSERT INTO Watchlist (watchlist_id, user_id, movie_id) VALUES (watchlist_id_seq.NEXTVAL, v_user_id, v_movie_id_parasite);
END;
/

-- Step 5: Finalize all changes.
PROMPT Committing transaction...
COMMIT;
PROMPT Master setup complete. The database is ready.