import csv
import time
import os
import json
import requests

BASE = "https://mcsrranked.com/api/users/"

def fetch_user(uuid, retries=5):
    """
    Fetch a single user's data from the MCSR Ranked API.
    Returns the inner `data` object, or None if not found.
    """
    url = BASE + uuid
    for attempt in range(retries):
        try:
            r = requests.get(url)
            if r.status_code == 400:
                print(f"[WARN] User {uuid} not found (400). Skipping.")
                return None

            r.raise_for_status()
            j = r.json()
            return j.get("data")
        except Exception as e:
            print(f"[ERROR] {uuid}: {e} (attempt {attempt+1}/{retries})")
            time.sleep(2 + attempt * 2)

    print(f"[ERROR] {uuid}: repeated failures, final attempt...")
    r = requests.get(url)
    if r.status_code == 400:
        print(f"[WARN] User {uuid} not found (400). Skipping.")
        return None
    r.raise_for_status()
    j = r.json()
    return j.get("data")


def get_nested(d, path, default=None):
    """
    Safely get a nested value from dict `d` using a list of keys.
    Example: get_nested(user, ["statistics", "total", "wins", "ranked"])
    """
    cur = d
    for key in path:
        if not isinstance(cur, dict) or key not in cur:
            return default
        cur = cur[key]
    return cur


def main():
    uuids = []
    with open("missing_uuids.csv", newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            u = (row.get("uuid") or "").strip()
            if u:
                uuids.append(u)

    print(f"Loaded {len(uuids)} UUIDs from missing_uuids.csv")

    fieldnames = [
        "uuid",
        "nickname",
        "roleType",
        "eloRate",
        "eloRank",
        "country",

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
        "achievements_display_json",
        "achievements_total_json",

        "connections_discord_id",
        "connections_discord_name",
        "connections_youtube_id",
        "connections_youtube_name",
        "connections_twitch_id",
        "connections_twitch_name",

        "seasonResult_last_eloRate",
        "seasonResult_last_eloRank",
        "seasonResult_last_phasePoint",
        "seasonResult_highest",
        "seasonResult_lowest",
        "seasonResult_phases_json",

        "weeklyRaces_json",
    ]

    users_csv_exists = os.path.exists("users.csv")
    processed = set()

    if users_csv_exists:
        print("users.csv exists; loading processed UUIDs to resume...")
        with open("users.csv", newline="", encoding="utf-8") as f:
            r = csv.DictReader(f)
            for row in r:
                pu = (row.get("uuid") or "").strip()
                if pu:
                    processed.add(pu)
        print(f"Found {len(processed)} already-processed users.")

    with open("users.csv", "a", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        if not users_csv_exists:
            writer.writeheader()

        total_done = len(processed)

        for i, uuid in enumerate(uuids, start=1):
            if uuid in processed:
                continue

            print(f"[{i}/{len(uuids)}] Fetching user {uuid}...")
            data = fetch_user(uuid)
            if data is None:
                continue

            nickname = data.get("nickname")
            roleType = data.get("roleType")
            eloRate = data.get("eloRate")
            eloRank = data.get("eloRank")
            country = data.get("country")

            firstOnline = get_nested(data, ["timestamp", "firstOnline"])
            lastOnline = get_nested(data, ["timestamp", "lastOnline"])
            lastRanked = get_nested(data, ["timestamp", "lastRanked"])
            nextDecay = get_nested(data, ["timestamp", "nextDecay"])

            season_bestTime_ranked         = get_nested(data, ["statistics", "season", "bestTime", "ranked"])
            season_playedMatches_ranked    = get_nested(data, ["statistics", "season", "playedMatches", "ranked"])
            season_wins_ranked             = get_nested(data, ["statistics", "season", "wins", "ranked"])
            season_loses_ranked            = get_nested(data, ["statistics", "season", "loses", "ranked"])
            season_completions_ranked      = get_nested(data, ["statistics", "season", "completions", "ranked"])
            season_playtime_ranked         = get_nested(data, ["statistics", "season", "playtime", "ranked"])
            season_forfeits_ranked         = get_nested(data, ["statistics", "season", "forfeits", "ranked"])
            season_highestWinStreak_ranked = get_nested(data, ["statistics", "season", "highestWinStreak", "ranked"])
            season_currentWinStreak_ranked = get_nested(data, ["statistics", "season", "currentWinStreak", "ranked"])

            total_bestTime_ranked          = get_nested(data, ["statistics", "total", "bestTime", "ranked"])
            total_playedMatches_ranked     = get_nested(data, ["statistics", "total", "playedMatches", "ranked"])
            total_wins_ranked              = get_nested(data, ["statistics", "total", "wins", "ranked"])
            total_loses_ranked             = get_nested(data, ["statistics", "total", "loses", "ranked"])
            total_completions_ranked       = get_nested(data, ["statistics", "total", "completions", "ranked"])
            total_playtime_ranked          = get_nested(data, ["statistics", "total", "playtime", "ranked"])
            total_forfeits_ranked          = get_nested(data, ["statistics", "total", "forfeits", "ranked"])
            total_highestWinStreak_ranked  = get_nested(data, ["statistics", "total", "highestWinStreak", "ranked"])
            total_currentWinStreak_ranked  = get_nested(data, ["statistics", "total", "currentWinStreak", "ranked"])

            achievements = data.get("achievements") or {}
            achievements_display = achievements.get("display", [])
            achievements_total = achievements.get("total", [])
            achievements_display_json = json.dumps(achievements_display, separators=(",", ":"))
            achievements_total_json = json.dumps(achievements_total, separators=(",", ":"))

            connections = data.get("connections") or {}
            conn_disc = connections.get("discord") or {}
            conn_yt   = connections.get("youtube") or {}
            conn_tw   = connections.get("twitch") or {}

            connections_discord_id   = conn_disc.get("id")
            connections_discord_name = conn_disc.get("name")
            connections_youtube_id   = conn_yt.get("id")
            connections_youtube_name = conn_yt.get("name")
            connections_twitch_id    = conn_tw.get("id")
            connections_twitch_name  = conn_tw.get("name")

            season_result = data.get("seasonResult") or {}
            sr_last   = season_result.get("last") or {}
            sr_phases = season_result.get("phases", [])

            seasonResult_last_eloRate    = sr_last.get("eloRate")
            seasonResult_last_eloRank    = sr_last.get("eloRank")
            seasonResult_last_phasePoint = sr_last.get("phasePoint")
            seasonResult_highest         = season_result.get("highest")
            seasonResult_lowest          = season_result.get("lowest")
            seasonResult_phases_json     = json.dumps(sr_phases, separators=(",", ":"))

            weeklyRaces = data.get("weeklyRaces", [])
            weeklyRaces_json = json.dumps(weeklyRaces, separators=(",", ":"))

            row = {
                "uuid": uuid,
                "nickname": nickname,
                "roleType": roleType,
                "eloRate": eloRate,
                "eloRank": eloRank,
                "country": country,

                "firstOnline": firstOnline,
                "lastOnline": lastOnline,
                "lastRanked": lastRanked,
                "nextDecay": nextDecay,

                "season_bestTime_ranked": season_bestTime_ranked,
                "season_playedMatches_ranked": season_playedMatches_ranked,
                "season_wins_ranked": season_wins_ranked,
                "season_loses_ranked": season_loses_ranked,
                "season_completions_ranked": season_completions_ranked,
                "season_playtime_ranked": season_playtime_ranked,
                "season_forfeits_ranked": season_forfeits_ranked,
                "season_highestWinStreak_ranked": season_highestWinStreak_ranked,
                "season_currentWinStreak_ranked": season_currentWinStreak_ranked,

                "total_bestTime_ranked": total_bestTime_ranked,
                "total_playedMatches_ranked": total_playedMatches_ranked,
                "total_wins_ranked": total_wins_ranked,
                "total_loses_ranked": total_loses_ranked,
                "total_completions_ranked": total_completions_ranked,
                "total_playtime_ranked": total_playtime_ranked,
                "total_forfeits_ranked": total_forfeits_ranked,
                "total_highestWinStreak_ranked": total_highestWinStreak_ranked,
                "total_currentWinStreak_ranked": total_currentWinStreak_ranked,

                "achievements_display_json": achievements_display_json,
                "achievements_total_json": achievements_total_json,

                "connections_discord_id": connections_discord_id,
                "connections_discord_name": connections_discord_name,
                "connections_youtube_id": connections_youtube_id,
                "connections_youtube_name": connections_youtube_name,
                "connections_twitch_id": connections_twitch_id,
                "connections_twitch_name": connections_twitch_name,

                "seasonResult_last_eloRate": seasonResult_last_eloRate,
                "seasonResult_last_eloRank": seasonResult_last_eloRank,
                "seasonResult_last_phasePoint": seasonResult_last_phasePoint,
                "seasonResult_highest": seasonResult_highest,
                "seasonResult_lowest": seasonResult_lowest,
                "seasonResult_phases_json": seasonResult_phases_json,

                "weeklyRaces_json": weeklyRaces_json,
            }

            writer.writerow(row)
            total_done += 1
            processed.add(uuid)

            time.sleep(1.3)

        print(f"Done. Total users written: {total_done}")


if __name__ == "__main__":
    main()
