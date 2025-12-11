import sqlite3
import os
from datetime import datetime

# Create database directory
os.makedirs('data', exist_ok=True)

# Connect to SQLite database
conn = sqlite3.connect('data/gogidix_ai.db')
cursor = conn.cursor()

# Create tables
cursor.executescript('''
    -- Properties table
    CREATE TABLE IF NOT EXISTS properties (
        id TEXT PRIMARY KEY,
        property_type TEXT,
        address TEXT,
        city TEXT,
        state TEXT,
        bedrooms INTEGER,
        bathrooms INTEGER,
        square_feet INTEGER,
        year_built INTEGER,
        price REAL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- Users table
    CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        email TEXT UNIQUE,
        hashed_password TEXT,
        is_active BOOLEAN DEFAULT TRUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    -- API keys table
    CREATE TABLE IF NOT EXISTS api_keys (
        id TEXT PRIMARY KEY,
        key_id TEXT UNIQUE,
        hashed_key TEXT,
        user_id TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id)
    );

    -- ML models table
    CREATE TABLE IF NOT EXISTS ml_models (
        id TEXT PRIMARY KEY,
        name TEXT,
        version TEXT,
        model_path TEXT,
        metrics TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
''')

conn.commit()
conn.close()

print("✅ Database initialized successfully")
