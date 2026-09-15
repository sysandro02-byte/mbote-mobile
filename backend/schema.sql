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


-- One active password-reset challenge per account. Only a SHA-256 hash is stored.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    code_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_password_reset_expiry ON password_reset_tokens(expires_at);

-- Authentication challenges used by real registration and login OTP flows.
CREATE TABLE IF NOT EXISTS auth_challenges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    flow VARCHAR(20) NOT NULL CHECK (flow IN ('LOGIN', 'REGISTER')),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    code_hash CHAR(64) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_auth_challenges_expiry ON auth_challenges(expires_at);
CREATE INDEX IF NOT EXISTS idx_auth_challenges_email_flow ON auth_challenges(email, flow);

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

-- Per-user message bookmarks.  The unique key makes the API toggle atomic.
CREATE TABLE IF NOT EXISTS message_stars (
    message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (message_id, user_id)
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
    media_type VARCHAR(50) DEFAULT 'TEXT',
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    shares_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Safe upgrades for databases created by versions prior to 1.5.0.
ALTER TABLE news_posts ADD COLUMN IF NOT EXISTS media_type VARCHAR(50) DEFAULT 'TEXT';
ALTER TABLE news_posts ADD COLUMN IF NOT EXISTS shares_count INT DEFAULT 0;

CREATE TABLE IF NOT EXISTS news_post_likes (
    news_post_id UUID REFERENCES news_posts(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (news_post_id, user_id)
);

CREATE TABLE IF NOT EXISTS news_post_comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    news_post_id UUID REFERENCES news_posts(id) ON DELETE CASCADE,
    author_id UUID REFERENCES users(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS short_video_comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    short_video_id UUID REFERENCES short_videos(id) ON DELETE CASCADE,
    author_id UUID REFERENCES users(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_participants_user ON chat_participants(user_id, chat_id);
CREATE INDEX IF NOT EXISTS idx_messages_chat_created ON messages(chat_id, created_at);
CREATE INDEX IF NOT EXISTS idx_news_posts_created ON news_posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_short_videos_created ON short_videos(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_news_post_comments_post ON news_post_comments(news_post_id, created_at);
CREATE INDEX IF NOT EXISTS idx_short_video_comments_video ON short_video_comments(short_video_id, created_at);

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

-- 10. USER-OWNED APPLICATION STATE
CREATE TABLE IF NOT EXISTS user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    value JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS blocked_users (
    blocker_id UUID REFERENCES users(id) ON DELETE CASCADE,
    blocked_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);

CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS call_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    peer_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    direction VARCHAR(20) NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    duration_seconds INT NOT NULL DEFAULT 0,
    CHECK (duration_seconds >= 0)
);

CREATE TABLE IF NOT EXISTS meetings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    host_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    code VARCHAR(32) UNIQUE NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    duration_minutes INT NOT NULL DEFAULT 30,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS meeting_participants (
    meeting_id UUID REFERENCES meetings(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (meeting_id, user_id)
);

CREATE TABLE IF NOT EXISTS statuses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_type VARCHAR(20) NOT NULL,
    media_url TEXT,
    text TEXT,
    background_color VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (NOW() + INTERVAL '24 hours'),
    CHECK (media_url IS NOT NULL OR text IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS status_views (
    status_id UUID REFERENCES statuses(id) ON DELETE CASCADE,
    viewer_id UUID REFERENCES users(id) ON DELETE CASCADE,
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (status_id, viewer_id)
);

CREATE INDEX IF NOT EXISTS idx_reports_status_created ON reports(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_calls_user_started ON call_history(user_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_meetings_scheduled ON meetings(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_statuses_expiry ON statuses(expires_at);


ALTER TABLE short_videos ADD COLUMN IF NOT EXISTS duration_seconds INT NOT NULL DEFAULT 0;
ALTER TABLE short_videos ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'public';

CREATE TABLE IF NOT EXISTS short_video_bookmarks (
    short_video_id UUID REFERENCES short_videos(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (short_video_id, user_id)
);

CREATE TABLE IF NOT EXISTS user_follows (
    follower_id UUID REFERENCES users(id) ON DELETE CASCADE,
    followed_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (follower_id, followed_id),
    CHECK (follower_id <> followed_id)
);

CREATE TABLE IF NOT EXISTS short_video_views (
    short_video_id UUID REFERENCES short_videos(id) ON DELETE CASCADE,
    viewer_id UUID REFERENCES users(id) ON DELETE CASCADE,
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (short_video_id, viewer_id)
);

CREATE TABLE IF NOT EXISTS short_video_shares (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    short_video_id UUID REFERENCES short_videos(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    target_chat_id UUID REFERENCES chats(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_short_bookmarks_user ON short_video_bookmarks(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_follows_followed ON user_follows(followed_id);
CREATE INDEX IF NOT EXISTS idx_short_views_video ON short_video_views(short_video_id);
CREATE INDEX IF NOT EXISTS idx_short_shares_video ON short_video_shares(short_video_id);



-- 11. REAL-TIME COLLABORATION AND INTERACTION STATE
CREATE TABLE IF NOT EXISTS message_reactions (
    message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    emoji VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (message_id, user_id, emoji)
);

CREATE TABLE IF NOT EXISTS chat_reads (
    chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    last_read_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS channel_profiles (
    chat_id UUID PRIMARY KEY REFERENCES chats(id) ON DELETE CASCADE,
    description TEXT NOT NULL DEFAULT '',
    slug VARCHAR(120) UNIQUE NOT NULL,
    privacy VARCHAR(20) NOT NULL DEFAULT 'public',
    category VARCHAR(100),
    banner_url TEXT
);

CREATE TABLE IF NOT EXISTS channel_subscriptions (
    channel_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (channel_id, user_id)
);

CREATE TABLE IF NOT EXISTS status_reactions (
    status_id UUID REFERENCES statuses(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    emoji VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (status_id, user_id)
);

CREATE TABLE IF NOT EXISTS status_comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    status_id UUID REFERENCES statuses(id) ON DELETE CASCADE,
    author_id UUID REFERENCES users(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS status_shares (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    status_id UUID REFERENCES statuses(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS news_post_shares (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    news_post_id UUID REFERENCES news_posts(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS job_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID REFERENCES job_offers(id) ON DELETE CASCADE,
    applicant_id UUID REFERENCES users(id) ON DELETE CASCADE,
    cv_url TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (job_id, applicant_id)
);

CREATE TABLE IF NOT EXISTS group_call_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    room_code VARCHAR(32) UNIQUE NOT NULL,
    host_id UUID REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    is_video BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    ended_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS group_call_participants (
    session_id UUID REFERENCES group_call_sessions(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    audio_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    video_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    screen_sharing BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    left_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (session_id, user_id)
);

CREATE TABLE IF NOT EXISTS device_push_tokens (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    platform VARCHAR(20) NOT NULL DEFAULT 'android',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (user_id, token)
);

CREATE TABLE IF NOT EXISTS payment_intents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_reference VARCHAR(255),
    amount_fcfa BIGINT NOT NULL CHECK (amount_fcfa > 0),
    phone VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS app_content (
    content_key VARCHAR(120) PRIMARY KEY,
    value JSONB NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_message_reactions_message ON message_reactions(message_id);
CREATE INDEX IF NOT EXISTS idx_channel_subscriptions_user ON channel_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_status_comments_status ON status_comments(status_id, created_at);
CREATE INDEX IF NOT EXISTS idx_job_applications_user ON job_applications(applicant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_group_calls_status ON group_call_sessions(status, created_at DESC);
