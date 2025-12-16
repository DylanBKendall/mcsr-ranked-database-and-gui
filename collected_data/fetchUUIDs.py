import csv

uuids = set()

with open("matches.csv", newline="", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        field = row.get("player_uuids") or ""
        for u in field.split(","):
            u = u.strip()
            if u:
                uuids.add(u)

print(f"Found {len(uuids)} unique UUIDs")

with open("uuids.csv", "w", newline="", encoding="utf-8") as f:
    w = csv.writer(f)
    w.writerow(["uuid"])
    for u in sorted(uuids):
        w.writerow([u])
