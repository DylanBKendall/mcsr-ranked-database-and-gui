# MCSR Ranked — Database & Desktop GUI

A desktop application for exploring **Minecraft Speedrunning Ranked (MCSR)** data through a normalized **MySQL database** and an interactive **Java Swing GUI**.  
The project supports player lookup, detailed profiles, favorites, match browsing, statistical comparisons, and leaderboards.

Built as a full-stack database application with emphasis on **relational design**, **query performance**, and **usability**.

---

## Features

### Player Exploration
- Search players by nickname
- View detailed player profiles
- Season-specific and all-time statistics
- Elo rating and rank breakdowns

### Favorites
- Mark players as favorites
- Favorites persist locally (no login required)
- Quick access from a dedicated tab

### Match Browser
- Browse matches by season or across all seasons
- View match metadata (type, category, result, time, forfeits)
- Sortable columns for data exploration

### Player Comparison
- Compare two players head-to-head
- Season-specific or all-time comparisons
- Side-by-side statistical breakdowns

### Leaderboards
- Elo leaderboard
- Fastest single completion time
- Fastest average completion time (100+ matches)
- Best win percentage (100+ matches)
- All leaderboards support sorting and season filters

---

## Tech Stack

### Language & Runtime
- Java 21

### GUI
- Java Swing (standard JDK components)

### Database
- MySQL Community Server
- Fully normalized relational schema (3NF)

### Database Access
- JDBC
- MySQL Connector/J
- HikariCP (connection pooling)

### Tooling & Utilities
- SLF4J (logging)
- DBeaver (database administration)
- MySQL CLI
- draw.io (ER diagrams)

---

## Installation & Setup

### Prerequisites
- Java 21+
- MySQL 8.x
- MySQL server running locally

---

### 1. Database Setup

1. Start the MySQL server
2. Open a SQL client (MySQL CLI or DBeaver)
3. Create and populate the database:
   ```sql
   SOURCE db_sql/schema.sql;
   SOURCE db_exports/users_export.sql;
   SOURCE db_exports/matches_export.sql;
   SOURCE db_exports/user_achievements_export.sql;
   -- additional exports as provided
