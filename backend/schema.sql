-- =========================================================================
-- MBOTÉ DATABASE SCHEMA (PostgreSQL / Supabase)
-- LoukaTech Panafrican Messaging & Social Platform
-- =========================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. USERS & PROFILES
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    username VARCHAR(100) UNIQUE,
    phone VARCHAR(50),
    avatar_url TEXT,
    cover_url TEXT,
    bio TEXT,
    country VARCHAR(100) DEFAULT 'Congo',
    city VARCHAR(100) DEFAULT 'Brazzaville',
    role VARCHAR(50) DEFAULT 'USER', -- USER, CREATOR, ADMIN, MODERATOR
    is_verified BOOLEAN DEFAULT FALSE,
    wallet_balance_fcfa BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. CHATS & CONVERSATIONS
CREATE TABLE IF NOT EXISTS chats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255),
    avatar_url TEXT,
    is_group BOOLEAN DEFAULT FALSE,
    is_channel BOOLEAN DEFAULT FALSE,
    created_by UUID REFERENCES users(id),
    disappearing_timer_sec INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. CHAT PARTICIPANTS
CREATE TABLE IF NOT EXISTS chat_participants (
    chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) DEFAULT 'MEMBER',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (chat_id, user_id)
);

-- 4. MESSAGES
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES users(id),
    text TEXT,
    media_type VARCHAR(50) DEFAULT 'NONE',
    media_url TEXT,
    audio_duration_sec INT DEFAULT 0,
    is_encrypted BOOLEAN DEFAULT TRUE,
    reply_to_id UUID REFERENCES messages(id),
    status VARCHAR(50) DEFAULT 'SENT', -- SENT, DELIVERED, READ
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. SHORT VIDEOS (ShortMBoté)
CREATE TABLE IF NOT EXISTS short_videos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    creator_id UUID REFERENCES users(id),
    video_url TEXT NOT NULL,
    thumbnail_url TEXT,
    caption TEXT,
    location VARCHAR(255),
    music_track VARCHAR(255),
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    bookmarks_count INT DEFAULT 0,
    shares_count INT DEFAULT 0,
    tips_count_fcfa BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. SHORT VIDEO REACTIONS
CREATE TABLE IF NOT EXISTS short_video_reactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    short_video_id UUID REFERENCES short_videos(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    emoji VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (short_video_id, user_id)
);

-- 7. NEWS & ACTUS POSTS
CREATE TABLE IF NOT EXISTS news_posts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    author_id UUID REFERENCES users(id),
    category VARCHAR(100) DEFAULT 'Actualités',
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    image_url TEXT,
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 8. JOB OFFERS (MBoté Emploi)
CREATE TABLE IF NOT EXISTS job_offers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    company_logo TEXT,
    location VARCHAR(255) NOT NULL,
    domain VARCHAR(100) DEFAULT 'Tech & Télécoms',
    contract_type VARCHAR(50) DEFAULT 'CDI',
    work_mode VARCHAR(50) DEFAULT 'Hybride',
    salary VARCHAR(100),
    description TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 9. ADMIN SYSTEM LOGS
CREATE TABLE IF NOT EXISTS admin_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_email VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    target_id VARCHAR(255),
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
