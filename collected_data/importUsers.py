import csv
import mysql.connector

DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "database": "mcsr",
}

INT_FIELDS = {
    "roleType",
    "eloRate",
    "eloRank",
    "firstOnline",
    "lastOnline",
    "lastRanked",
    "nextDecay",
    "season_bestTime_ranked",
    "season_playedMatches_ranked",
    "season_wins_ranked",
    "season_loses_ranked",
    "season_completions_ranked",
    "season_playtime_ranked",
    "season_forfeits_ranked",
    "season_highestWinStreak_ranked",
    "season_currentWinStreak_ranked",
    "total_bestTime_ranked",
    "total_playedMatches_ranked",
    "total_wins_ranked",
    "total_loses_ranked",
    "total_completions_ranked",
    "total_playtime_ranked",
    "total_forfeits_ranked",
    "total_highestWinStreak_ranked",
    "total_currentWinStreak_ranked",
    "seasonResult_last_eloRate",
    "seasonResult_last_eloRank",
    "seasonResult_last_phasePoint",
    "seasonResult_highest",
    "seasonResult_lowest",
}

def normalize_row(row: dict) -> dict:
    """Convert '' -> None for ints; leave text/json fields alone."""
    out = {}
    for k, v in row.items():
        if v is None:
            out[k] = None
            continue
        v = v.strip()
        if k in INT_FIELDS:
            if v == "":
                out[k] = None
            else:
                try:
                    out[k] = int(v)
                except ValueError:
                    out[k] = None
        else:
            out[k] = v if v != "" else None
    return out

def main():
    conn = mysql.connector.connect(**DB_CONFIG)
    cur = conn.cursor()

    with open("users.csv", newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = 0
        for row in reader:
            row = normalize_row(row)

            cur.execute("""
                INSERT INTO users (
                    uuid,
                    nickname,
                    roleType,
                    eloRate,
                    eloRank,
                    country,
                    firstOnline,
                    lastOnline,
                    lastRanked,
                    nextDecay,
                    season_bestTime_ranked,
                    season_playedMatches_ranked,
                    season_wins_ranked,
                    season_loses_ranked,
                    season_completions_ranked,
                    season_playtime_ranked,
                    season_forfeits_ranked,
                    season_highestWinStreak_ranked,
                    season_currentWinStreak_ranked,
                    total_bestTime_ranked,
                    total_playedMatches_ranked,
                    total_wins_ranked,
                    total_loses_ranked,
                    total_completions_ranked,
                    total_playtime_ranked,
                    total_forfeits_ranked,
                    total_highestWinStreak_ranked,
                    total_currentWinStreak_ranked,
                    achievements_display_json,
                    achievements_total_json,
                    connections_discord_id,
                    connections_discord_name,
                    connections_youtube_id,
                    connections_youtube_name,
                    connections_twitch_id,
                    connections_twitch_name,
                    seasonResult_last_eloRate,
                    seasonResult_last_eloRank,
                    seasonResult_last_phasePoint,
                    seasonResult_highest,
                    seasonResult_lowest,
                    seasonResult_phases_json,
                    weeklyRaces_json
                ) VALUES (
                    %(uuid)s,
                    %(nickname)s,
                    %(roleType)s,
                    %(eloRate)s,
                    %(eloRank)s,
                    %(country)s,
                    %(firstOnline)s,
                    %(lastOnline)s,
                    %(lastRanked)s,
                    %(nextDecay)s,
                    %(season_bestTime_ranked)s,
                    %(season_playedMatches_ranked)s,
                    %(season_wins_ranked)s,
                    %(season_loses_ranked)s,
                    %(season_completions_ranked)s,
                    %(season_playtime_ranked)s,
                    %(season_forfeits_ranked)s,
                    %(season_highestWinStreak_ranked)s,
                    %(season_currentWinStreak_ranked)s,
                    %(total_bestTime_ranked)s,
                    %(total_playedMatches_ranked)s,
                    %(total_wins_ranked)s,
                    %(total_loses_ranked)s,
                    %(total_completions_ranked)s,
                    %(total_playtime_ranked)s,
                    %(total_forfeits_ranked)s,
                    %(total_highestWinStreak_ranked)s,
                    %(total_currentWinStreak_ranked)s,
                    %(achievements_display_json)s,
                    %(achievements_total_json)s,
                    %(connections_discord_id)s,
                    %(connections_discord_name)s,
                    %(connections_youtube_id)s,
                    %(connections_youtube_name)s,
                    %(connections_twitch_id)s,
                    %(connections_twitch_name)s,
                    %(seasonResult_last_eloRate)s,
                    %(seasonResult_last_eloRank)s,
                    %(seasonResult_last_phasePoint)s,
                    %(seasonResult_highest)s,
                    %(seasonResult_lowest)s,
                    %(seasonResult_phases_json)s,
                    %(weeklyRaces_json)s
                )
                ON DUPLICATE KEY UPDATE
                    nickname = VALUES(nickname),
                    roleType = VALUES(roleType),
                    eloRate = VALUES(eloRate),
                    eloRank = VALUES(eloRank),
                    country = VALUES(country),
                    firstOnline = VALUES(firstOnline),
                    lastOnline = VALUES(lastOnline),
                    lastRanked = VALUES(lastRanked),
                    nextDecay = VALUES(nextDecay),
                    season_bestTime_ranked = VALUES(season_bestTime_ranked),
                    season_playedMatches_ranked = VALUES(season_playedMatches_ranked),
                    season_wins_ranked = VALUES(season_wins_ranked),
                    season_loses_ranked = VALUES(season_loses_ranked),
                    season_completions_ranked = VALUES(season_completions_ranked),
                    season_playtime_ranked = VALUES(season_playtime_ranked),
                    season_forfeits_ranked = VALUES(season_forfeits_ranked),
                    season_highestWinStreak_ranked = VALUES(season_highestWinStreak_ranked),
                    season_currentWinStreak_ranked = VALUES(season_currentWinStreak_ranked),
                    total_bestTime_ranked = VALUES(total_bestTime_ranked),
                    total_playedMatches_ranked = VALUES(total_playedMatches_ranked),
                    total_wins_ranked = VALUES(total_wins_ranked),
                    total_loses_ranked = VALUES(total_loses_ranked),
                    total_completions_ranked = VALUES(total_completions_ranked),
                    total_playtime_ranked = VALUES(total_playtime_ranked),
                    total_forfeits_ranked = VALUES(total_forfeits_ranked),
                    total_highestWinStreak_ranked = VALUES(total_highestWinStreak_ranked),
                    total_currentWinStreak_ranked = VALUES(total_currentWinStreak_ranked),
                    achievements_display_json = VALUES(achievements_display_json),
                    achievements_total_json = VALUES(achievements_total_json),
                    connections_discord_id = VALUES(connections_discord_id),
                    connections_discord_name = VALUES(connections_discord_name),
                    connections_youtube_id = VALUES(connections_youtube_id),
                    connections_youtube_name = VALUES(connections_youtube_name),
                    connections_twitch_id = VALUES(connections_twitch_id),
                    connections_twitch_name = VALUES(connections_twitch_name),
                    seasonResult_last_eloRate = VALUES(seasonResult_last_eloRate),
                    seasonResult_last_eloRank = VALUES(seasonResult_last_eloRank),
                    seasonResult_last_phasePoint = VALUES(seasonResult_last_phasePoint),
                    seasonResult_highest = VALUES(seasonResult_highest),
                    seasonResult_lowest = VALUES(seasonResult_lowest),
                    seasonResult_phases_json = VALUES(seasonResult_phases_json),
                    weeklyRaces_json = VALUES(weeklyRaces_json)
            """, row)

            rows += 1
            if rows % 1000 == 0:
                conn.commit()
                print(f"Upserted {rows} users...")

    conn.commit()
    print(f"Done. Upserted {rows} users.")
    cur.close()
    conn.close()

if __name__ == "__main__":
    main()
