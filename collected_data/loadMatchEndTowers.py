def parse_end_towers(s):
    if not s:
        return []
    return [x.strip() for x in s.split("|") if x.strip()]


def load_end_towers(conn):
    cur = conn.cursor(dictionary=True)
    insert_cur = conn.cursor()

    cur.execute("SELECT id, seed_endTowers FROM matches")

    rows_inserted = 0

    for row in cur:
        match_id = row["id"]
        towers = parse_end_towers(row["seed_endTowers"])

        for idx, height_str in enumerate(towers):
            try:
                height = int(height_str)
            except ValueError:
                height = None

            insert_cur.execute("""
                INSERT INTO match_end_towers (match_id, tower_index, height)
                VALUES (%s, %s, %s)
                ON DUPLICATE KEY UPDATE height = VALUES(height)
            """, (match_id, idx, height))

            rows_inserted += 1

    conn.commit()
    print(f"Inserted/updated {rows_inserted} tower rows.")
