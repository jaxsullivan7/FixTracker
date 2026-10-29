import csv
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parent.parent
md = (root / "docs" / "BACKLOG.md").read_text(encoding="utf-8")

epics = []
current = None
for raw in md.splitlines():
    line = raw.strip()
    heading = re.match(r"^## (FIX-E\d+) · (.+)$", line)
    if heading:
        current = {
            "id": heading.group(1),
            "summary": heading.group(2).strip(),
            "goal": "",
            "state": "",
            "stories": [],
        }
        epics.append(current)
        continue

    if not line.startswith("|"):
        continue

    cells = [c.strip() for c in line.strip("|").split("|")]
    if len(cells) < 4:
        continue

    if re.fullmatch(r"FIX-E\d+", cells[0]):
        epic = next((e for e in epics if e["id"] == cells[0]), None)
        if epic:
            epic["summary"] = cells[1]
            epic["goal"] = cells[2]
            epic["state"] = cells[3] if len(cells) > 3 else ""
        continue

    if re.fullmatch(r"FIX-\d+", cells[0]) and current is not None and len(cells) >= 5:
        current["stories"].append({
            "id": cells[0],
            "summary": cells[1],
            "criteria": cells[2],
            "priority": cells[3],
            "points": int(cells[4]) if cells[4].isdigit() else None,
        })

implemented = {
    "FIX-101", "FIX-102", "FIX-103", "FIX-104",
    "FIX-201", "FIX-202", "FIX-203", "FIX-204",
    "FIX-301", "FIX-302",
    "FIX-401", "FIX-402", "FIX-403",
    "FIX-501", "FIX-502",
    "FIX-601", "FIX-602", "FIX-605",
    "FIX-701", "FIX-702",
}

partial = {"FIX-304", "FIX-603", "FIX-606", "FIX-706"}

for epic in epics:
    for story in epic["stories"]:
        if story["id"] in implemented:
            story["status"] = "done"
        elif story["id"] in partial:
            story["status"] = "partial"
        else:
            story["status"] = "todo"

data = {
    "project": "FIX",
    "source": "docs/BACKLOG.md",
    "version": "0.1.0",
    "epics": epics,
}
out = root / "docs" / "jira-backlog.json"
out.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

csv_path = root / "docs" / "jira-backlog.csv"
with csv_path.open("w", newline="", encoding="utf-8-sig") as fh:
    writer = csv.writer(fh)
    writer.writerow(["Issue Type", "Summary", "Description", "Priority", "Epic Name", "Epic Link", "Story Points", "Labels"])
    for epic in epics:
        writer.writerow([
            "Epic",
            epic["summary"],
            epic["goal"],
            "",
            epic["summary"],
            "",
            "",
            "boceto v0.1",
        ])
        for story in epic["stories"]:
            writer.writerow([
                "Story",
                story["summary"],
                f"{story['criteria']}\n\nID: {story['id']}\nPrioridad: {story['priority']}\nEstado boceto: {story['status']}",
                "",
                "",
                epic["summary"],
                story["points"] or "",
                f"boceto v0.1 {story['priority'].lower()} {story['id'].lower()}",
            ])

print(f"epics={len(epics)} stories={sum(len(e['stories']) for e in epics)}")
for epic in epics:
    print(epic["id"], epic["summary"], len(epic["stories"]))
