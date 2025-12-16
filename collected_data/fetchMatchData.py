import requests
import csv
import time

BASE = "https://api.mcsrranked.com/matches/"

def fetch_page(before=None, season=None, retries=5):
    params = {"count": 100}
    if before is not None:
        params["before"] = before
    if season is not None:
        params["season"] = season

    for attempt in range(retries):
        try:
            r = requests.get(BASE, params=params)
            r.raise_for_status()
            return r.json()["data"]
        except Exception as e:
            print(f"Error: {e} (attempt {attempt+1}/{retries})")
            time.sleep(2 + attempt*2)

    print("Repeated failures. Sleeping and trying one last time...")
    time.sleep(10)
    r = requests.get(BASE, params=params)
    r.raise_for_status()
    return r.json()["data"]


def scrape_all_matches():
    max_season = 7

    fieldnames = [
        "id",
        "type",
        "category",
        "gameMode",
        "season",
        "date",
        "seed_id",
        "seed_overworld",
        "seed_nether",
        "seed_endTowers",
        "seed_variations",
        "seedType",
        "bastionType",
        "result_uuid",
        "result_time",
        "forfeited",
        "decayed",
        "beginner",
        "tag",
        "player_uuids",
        "players_detail",
        "changes"
    ]

    with open("matches.csv", "a", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)

        for season in range(max_season, 0, -1):
            before = None
            if season is 7:
                before = 1800255
            print(f"=== Season {season} ===")

            while True:
                data = fetch_page(before=before, season=season)
                if not data:
                    print(f"No more matches in season {season}.")
                    break

                for match in data:
                    seed = match.get("seed") or {}
                    end_towers = seed.get("endTowers", []) or []
                    variations = seed.get("variations", []) or []

                    result = match.get("result") or {}
                    result_uuid = result.get("uuid")
                    result_time = result.get("time")

                    players = match.get("players", []) or []
                    player_uuids = []
                    players_detail_parts = []
                    for p in players:
                        puuid = p.get("uuid")
                        if not puuid:
                            continue
                        player_uuids.append(puuid)
                        players_detail_parts.append(
                            "|".join([
                                puuid,
                                str(p.get("nickname") or ""),
                                str(p.get("roleType") if p.get("roleType") is not None else ""),
                                str(p.get("eloRate") if p.get("eloRate") is not None else ""),
                                str(p.get("eloRank") if p.get("eloRank") is not None else ""),
                                str(p.get("country") or ""),
                            ])
                        )

                    changes = match.get("changes", []) or []
                    changes_parts = []
                    for c in changes:
                        cuuid = c.get("uuid") or ""
                        change = c.get("change")
                        elo_rate = c.get("eloRate")
                        changes_parts.append(
                            "|".join([
                                cuuid,
                                str(change) if change is not None else "",
                                str(elo_rate) if elo_rate is not None else "",
                            ])
                        )

                    row = {
                        "id": match["id"],
                        "type": match.get("type"),
                        "category": match.get("category"),
                        "gameMode": match.get("gameMode"),
                        "season": match.get("season"),
                        "date": match.get("date"),
                        "seed_id": seed.get("id"),
                        "seed_overworld": seed.get("overworld"),
                        "seed_nether": seed.get("nether"),
                        "seed_endTowers": "|".join(str(x) for x in end_towers),
                        "seed_variations": "|".join(variations),
                        "seedType": match.get("seedType"),
                        "bastionType": match.get("bastionType"),
                        "result_uuid": result_uuid,
                        "result_time": result_time,
                        "forfeited": match.get("forfeited"),
                        "decayed": match.get("decayed"),
                        "beginner": match.get("beginner"),
                        "tag": match.get("tag"),
                        "player_uuids": ",".join(player_uuids),
                        "players_detail": ";".join(players_detail_parts),
                        "changes": ";".join(changes_parts),
                    }

                    writer.writerow(row)

                before = data[-1]["id"]
                print(f"Season {season}: latest seed {before}...")

                time.sleep(1.3)

    print(f"Done.")

if __name__ == "__main__":
    scrape_all_matches()
