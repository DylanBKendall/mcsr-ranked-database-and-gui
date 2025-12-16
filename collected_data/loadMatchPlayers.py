import mysql.connector
from mysql.connector import errors
missing_uuids = set()

DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "database": "mcsr",
}


def parse_player_uuids(s):
    if not s:
        return []
    return [x.strip() for x in s.split(",") if x.strip()]


def parse_players_detail(s):
    """
    players_detail format:
    "uuid|nickname|roleType|eloRate|eloRank|country;uuid2|..."
    Returns: dict[uuid] = {
        "nickname": ...,
        "roleType": ...,
        "eloRate_before": ...,
        "eloRank_before": ...,
        "country": ...,
    }
    """
    result = {}
    if not s:
        return result

    records = [r for r in s.split(";") if r.strip()]
    for rec in records:
        parts = rec.split("|")
        if len(parts) != 6:
            print(f"Warning: bad players_detail record: {rec}")
            continue
        uuid, nickname, roleType, eloRate, eloRank, country = parts
        try:
            roleType = int(roleType)
        except ValueError:
            roleType = None
        try:
            eloRate_before = int(eloRate)
        except ValueError:
            eloRate_before = None
        try:
            eloRank_before = int(eloRank)
        except ValueError:
            eloRank_before = None

        result[uuid] = {
            "nickname": nickname or None,
            "roleType": roleType,
            "eloRate_before": eloRate_before,
            "eloRank_before": eloRank_before,
            "country": country or None,
        }

    return result


def parse_changes(s):
    """
    changes format:
    "uuid|change|eloRate;uuid2|..."
    change strings like '+15' or '-8'
    Returns: dict[uuid] = {
        "elo_change": int,
        "eloRate_after": int,
    }
    """
    result = {}
    if not s:
        return result

    records = [r for r in s.split(";") if r.strip()]
    for rec in records:
        parts = rec.split("|")
        if len(parts) != 3:
            print(f"Warning: bad changes record: {rec}")
            continue
        uuid, change_str, eloRate_after_str = parts

        try:
            elo_change = int(change_str)
        except ValueError:
            elo_change = None

        try:
            eloRate_after = int(eloRate_after_str)
        except ValueError:
            eloRate_after = None

        result[uuid] = {
            "elo_change": elo_change,
            "eloRate_after": eloRate_after,
        }

    return result


def main():
    conn = mysql.connector.connect(**DB_CONFIG)
    cur = conn.cursor(dictionary=True, buffered=True)
    insert_cur = conn.cursor()

    missing_uuids = set()

    cur.execute("""
        SELECT id, player_uuids, players_detail, changes
        FROM matches
    """)

    rows_processed = 0
    rows_inserted = 0

    for row in cur:
        match_id = row["id"]
        player_uuids = parse_player_uuids(row["player_uuids"])
        details_by_uuid = parse_players_detail(row["players_detail"])
        changes_by_uuid = parse_changes(row["changes"])

        for uuid in player_uuids:
            det = details_by_uuid.get(uuid, {})
            nickname_at_match = det.get("nickname")
            roleType_at_match = det.get("roleType")
            eloRate_before = det.get("eloRate_before")
            eloRank_before = det.get("eloRank_before")
            country_at_match = det.get("country")

            ch = changes_by_uuid.get(uuid, {})
            elo_change = ch.get("elo_change")
            eloRate_after = ch.get("eloRate_after")

            try:
                insert_cur.execute("""
                    INSERT INTO match_players (
                        match_id,
                        uuid,
                        nickname_at_match,
                        roleType_at_match,
                        eloRate_before,
                        eloRank_before,
                        country_at_match,
                        elo_change,
                        eloRate_after
                    ) VALUES (
                        %s,%s,%s,%s,%s,%s,%s,%s,%s
                    )
                    ON DUPLICATE KEY UPDATE
                        nickname_at_match = VALUES(nickname_at_match),
                        roleType_at_match = VALUES(roleType_at_match),
                        eloRate_before = VALUES(eloRate_before),
                        eloRank_before = VALUES(eloRank_before),
                        country_at_match = VALUES(country_at_match),
                        elo_change = VALUES(elo_change),
                        eloRate_after = VALUES(eloRate_after)
                """, (
                    match_id,
                    uuid,
                    nickname_at_match,
                    roleType_at_match,
                    eloRate_before,
                    eloRank_before,
                    country_at_match,
                    elo_change,
                    eloRate_after,
                ))
                rows_inserted += 1
            except mysql.connector.errors.IntegrityError as e:
                print(f"[FK WARN] match_id={match_id}, uuid={uuid} failed FK. Recording as missing.")
                missing_uuids.add(uuid)

        rows_processed += 1

    conn.commit()
    insert_cur.close()
    cur.close()
    conn.close()

    if missing_uuids:
        import csv
        with open("missing_uuids.csv", "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["uuid"])
            for u in sorted(missing_uuids):
                w.writerow([u])
        print(f"Wrote {len(missing_uuids)} missing UUIDs to missing_uuids.csv")

    print(f"Done. Processed {rows_processed} matches, inserted/updated {rows_inserted} rows.")



if __name__ == "__main__":
    main()
