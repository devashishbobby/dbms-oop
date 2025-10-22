-- =============================================================================
-- FilmFolio Project Master Database Setup Script
-- =============================================================================
-- This script will drop all existing project tables and sequences
-- and recreate them with the final, correct schema.
-- Run this entire file in SQL*Plus to reset the database.
-- =============================================================================

-- Step 1: Drop all existing objects to ensure a clean slate.
-- (It is normal to see "table or view does not exist" errors here on the first run).
PROMPT Dropping existing tables and sequences...
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Review_Likes'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Watchlist'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Reviews'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Movies'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE Users'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE like_id_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE watchlist_id_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE review_id_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE movie_id_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE user_id_seq'; EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- Step 2: Recreate all sequences for auto-incrementing primary keys.
PROMPT Creating sequences...
CREATE SEQUENCE user_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE movie_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE review_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE watchlist_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE like_id_seq START WITH 1 INCREMENT BY 1;

-- Step 3: Recreate all tables with the final, correct schema and constraints.

-- Users Table
PROMPT Creating table: Users...
CREATE TABLE Users (
    user_id NUMBER PRIMARY KEY,
    username VARCHAR2(50) UNIQUE NOT NULL,
    password_hash VARCHAR2(100) NOT NULL,
    email VARCHAR2(100),
    profile_picture_path VARCHAR2(255)
);

-- Movies Table (The central table for both movies and anime)
PROMPT Creating table: Movies...
CREATE TABLE Movies (
    movie_id NUMBER PRIMARY KEY,
    title VARCHAR2(255) NOT NULL,
    release_year NUMBER,
    director VARCHAR2(255),
    poster_link VARCHAR2(512),
    media_type VARCHAR2(20) NOT NULL,
    genres VARCHAR2(255),
    synopsis VARCHAR2(4000),
    runtime VARCHAR2(50),
    age_rating VARCHAR2(50),
    external_score NUMBER(4,2),
    episodes NUMBER,
    status VARCHAR2(50),
    studios VARCHAR2(255),
    producers VARCHAR2(1000)
);

-- Reviews Table
PROMPT Creating table: Reviews...
CREATE TABLE Reviews (
    review_id NUMBER PRIMARY KEY,
    movie_id NUMBER NOT NULL,
    user_id NUMBER NOT NULL,
    rating NUMBER(2,1) NOT NULL,
    review_text VARCHAR2(4000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_movie FOREIGN KEY (movie_id) REFERENCES Movies(movie_id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
);

-- Watchlist Table
PROMPT Creating table: Watchlist...
CREATE TABLE Watchlist (
    watchlist_id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    movie_id NUMBER NOT NULL,
    CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_watchlist_movie FOREIGN KEY (movie_id) REFERENCES Movies(movie_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_movie_watchlist UNIQUE (user_id, movie_id)
);

-- Review_Likes Table
PROMPT Creating table: Review_Likes...
CREATE TABLE Review_Likes (
    like_id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    review_id NUMBER NOT NULL,
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_like_review FOREIGN KEY (review_id) REFERENCES Reviews(review_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_review_like UNIQUE (user_id, review_id)
);

-- Step 4: Commit all changes to the database.
PROMPT Committing changes...
COMMIT;

PROMPT =====================================================
PROMPT Database setup is complete.
PROMPT =====================================================