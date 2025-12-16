def parse_variations(s):
    if not s:
        return []
    return [x.strip() for x in s.split("|") if x.strip()]


def load_seed_variations(conn):
    cur = conn.cursor(dictionary=True)
    insert_cur = conn.cursor()

    cur.execute("SELECT id, seed_variations FROM matches")

    rows_inserted = 0

    for row in cur:
        match_id = row["id"]
        variations = parse_variations(row["seed_variations"])

        for idx, var in enumerate(variations):
            insert_cur.execute("""
                INSERT INTO match_seed_variations (match_id, var_index, variation)
                VALUES (%s, %s, %s)
                ON DUPLICATE KEY UPDATE variation = VALUES(variation)
            """, (match_id, idx, var))
            rows_inserted += 1

    conn.commit()
    print(f"Inserted/updated {rows_inserted} variation rows.")
