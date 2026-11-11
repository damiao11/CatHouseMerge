import json, math

V = {1: 2, 2: 5, 3: 12, 4: 28, 5: 65, 6: 150}
CN = {"wood": "木工", "metal": "五金", "fabric": "布艺", "light": "灯具",
      "ceramic": "陶瓷", "plant": "绿植", "relic": "旧物"}

# (day, type, [(chain, level, count), ...])
O = [
 (1,"daily",[("wood",2,6)]),
 (1,"daily",[("wood",2,8)]),
 (1,"cat",  [("wood",2,6),("wood",3,1)]),

 (2,"daily",[("wood",2,8),("wood",3,1)]),
 (2,"daily",[("wood",3,6)]),
 (2,"daily",[("wood",2,7),("wood",3,2)]),

 (3,"daily",[("wood",3,4),("wood",4,1)]),
 (3,"daily",[("wood",3,5)]),
 (3,"daily",[("wood",4,2)]),

 (4,"daily",[("metal",2,6),("metal",3,3)]),
 (4,"daily",[("metal",3,5)]),
 (4,"daily",[("metal",2,8),("metal",3,2)]),
 (4,"daily",[("metal",3,3),("metal",2,4)]),

 (5,"daily",[("metal",3,4),("metal",4,1)]),
 (5,"daily",[("metal",3,6)]),
 (5,"cat",  [("metal",4,2),("metal",3,3)]),
 (5,"daily",[("metal",3,5),("metal",2,2)]),

 (6,"daily",[("metal",4,3)]),
 (6,"daily",[("metal",4,2),("metal",3,2)]),
 (6,"daily",[("metal",3,5),("metal",4,1)]),
 (6,"daily",[("metal",4,4)]),

 (7,"daily",[("fabric",4,3)]),
 (7,"daily",[("fabric",4,2),("fabric",3,2)]),
 (7,"daily",[("fabric",4,4)]),
 (7,"daily",[("fabric",4,3),("fabric",3,2)]),
 (7,"daily",[("fabric",4,2),("fabric",3,1)]),

 (8,"daily",[("fabric",4,4)]),
 (8,"daily",[("fabric",4,3),("fabric",3,2)]),
 (8,"daily",[("fabric",4,5)]),
 (8,"cat",  [("fabric",4,2),("fabric",3,3)]),
 (8,"daily",[("fabric",4,3)]),

 (9,"daily",[("fabric",4,4),("fabric",5,1)]),
 (9,"daily",[("fabric",4,3)]),
 (9,"daily",[("fabric",5,2)]),
 (9,"daily",[("fabric",4,5)]),
 (9,"daily",[("fabric",4,3),("fabric",3,2)]),

 (10,"daily",[("light",5,3)]),
 (10,"daily",[("light",5,2),("light",4,2)]),
 (10,"daily",[("light",4,4),("light",5,1)]),
 (10,"daily",[("light",5,3)]),
 (10,"daily",[("light",5,2),("light",4,3)]),

 (11,"daily",[("light",5,3)]),
 (11,"daily",[("light",5,2),("light",4,3)]),
 (11,"cat",  [("light",5,4)]),
 (11,"daily",[("light",5,3),("light",4,2)]),
 (11,"daily",[("light",5,2),("light",4,2)]),

 (12,"daily",[("light",5,4)]),
 (12,"daily",[("light",5,3),("light",4,2)]),
 (12,"daily",[("light",5,2),("light",4,1)]),
 (12,"daily",[("light",5,5)]),
 (12,"daily",[("light",5,3)]),

 (13,"daily",[("ceramic",5,3)]),
 (13,"daily",[("ceramic",5,2),("ceramic",4,2)]),
 (13,"daily",[("ceramic",5,4)]),
 (13,"daily",[("ceramic",5,3),("ceramic",4,3)]),
 (13,"daily",[("ceramic",5,2),("ceramic",4,2)]),
 (13,"daily",[("ceramic",5,3)]),

 (14,"daily",[("ceramic",5,4)]),
 (14,"daily",[("ceramic",5,3),("ceramic",4,2)]),
 (14,"daily",[("ceramic",5,2),("ceramic",4,3)]),
 (14,"cat",  [("ceramic",5,5)]),
 (14,"daily",[("ceramic",5,3),("ceramic",4,2)]),
 (14,"daily",[("ceramic",5,4)]),

 (15,"daily",[("ceramic",5,4),("ceramic",6,1)]),
 (15,"daily",[("ceramic",5,3)]),
 (15,"daily",[("ceramic",5,5)]),
 (15,"daily",[("ceramic",5,2),("ceramic",4,3)]),
 (15,"daily",[("ceramic",5,4)]),
 (15,"daily",[("ceramic",5,3),("ceramic",4,2)]),

 (16,"daily",[("plant",5,4)]),
 (16,"daily",[("plant",5,3),("plant",4,2)]),
 (16,"daily",[("plant",5,5)]),
 (16,"daily",[("plant",5,4),("plant",6,1)]),
 (16,"daily",[("plant",5,3)]),
 (16,"daily",[("plant",5,2),("plant",4,3)]),

 (17,"daily",[("plant",5,4)]),
 (17,"daily",[("plant",5,4),("plant",6,1)]),
 (17,"daily",[("plant",5,3),("plant",4,2)]),
 (17,"cat",  [("plant",5,4)]),
 (17,"daily",[("plant",5,3),("plant",4,2)]),
 (17,"daily",[("plant",5,4)]),

 (18,"daily",[("plant",5,3),("plant",6,1)]),
 (18,"daily",[("plant",5,4)]),
 (18,"daily",[("plant",5,4)]),
 (18,"daily",[("plant",5,3),("plant",4,2)]),
 (18,"daily",[("plant",5,4)]),
 (18,"daily",[("plant",5,3),("plant",6,1)]),

 (19,"daily",[("relic",4,3),("relic",5,2)]),
 (19,"memory",[("relic",5,3)]),
 (19,"daily",[("relic",4,4),("relic",5,2)]),
 (19,"daily",[("relic",5,4)]),

 (20,"daily",[("relic",4,3),("relic",5,2)]),
 (20,"memory",[("relic",5,4)]),
 (20,"daily",[("relic",5,3)]),
 (20,"daily",[("relic",4,4),("relic",5,2)]),

 (21,"daily",[("relic",5,4)]),
 (21,"memory",[("relic",5,5)]),
 (21,"daily",[("relic",5,4)]),
 (21,"daily",[("relic",5,5)]),

 (22,"daily",[("wood",6,2)]),
 (22,"daily",[("wood",6,1),("fabric",5,2)]),
 (22,"daily",[("fabric",5,3),("ceramic",6,1)]),
 (22,"daily",[("ceramic",6,2)]),
 (22,"daily",[("plant",6,1),("light",5,1)]),

 (23,"daily",[("fabric",6,2)]),
 (23,"daily",[("plant",6,3)]),
 (23,"daily",[("wood",6,2),("ceramic",5,2)]),
 (23,"daily",[("plant",6,2)]),
 (23,"daily",[("ceramic",6,2),("plant",5,1)]),

 (24,"daily",[("wood",6,2)]),
 (24,"daily",[("fabric",6,1),("ceramic",5,2)]),
 (24,"story",[("plant",6,3)]),
]

EV = {}
for d in range(1, 4):  EV[d] = 1.0
for d in range(4, 7):  EV[d] = 1.4
for d in range(7, 13): EV[d] = 2.3
for d in range(13, 19): EV[d] = 2.9
for d in range(19, 25): EV[d] = 4.0

def reward(v):
    return int(round(v * 1.6 / 5.0) * 5)

rows = []
day_i = {}
for day, typ, reqs in O:
    day_i[day] = day_i.get(day, 0) + 1
    order_id = f"o{day}_{day_i[day]}"
    v = sum(V[l] * c for _, l, c in reqs)
    # 旧物6级改为两个5级后，保留已编排的剧情订单估值与550金币奖励。
    if order_id == "o21_2":
        v = 345
    eq = sum((2 ** (l - 1)) * c for _, l, c in reqs)
    rows.append({
        "id": order_id, "day": day, "type": typ,
        "req": [{"chain": ch, "level": l, "count": c} for ch, l, c in reqs],
        "value": v, "reward": reward(v), "equiv": eq,
    })

days = {}
for r in rows:
    d = days.setdefault(r["day"], {"orders": 0, "items": 0, "value": 0, "coins": 0, "equiv": 0})
    d["orders"] += 1
    d["items"] += sum(q["count"] for q in r["req"])
    d["value"] += r["value"]
    d["coins"] += r["reward"]
    d["equiv"] += r["equiv"]

print("day orders items value coins equiv taps tubes build surplus")
tot = {"coins": 0, "build": 0}
for day in sorted(days):
    d = days[day]
    taps = d["equiv"] / EV[day]
    tubes = taps / 150
    build = int(round(d["coins"] * 0.72 / 5.0) * 5)
    tot["coins"] += d["coins"]; tot["build"] += build
    print(f'{day:>3} {d["orders"]:>3} {d["items"]:>5} {d["value"]:>5} {d["coins"]:>5} '
          f'{d["equiv"]:>5} {taps:>5.0f} {tubes:>5.2f} {build:>5} {d["coins"]-build:>6}')

print(f'TOTAL orders={len(rows)} coins={tot["coins"]} build={tot["build"]} surplus={tot["coins"]-tot["build"]}')
print(f'TOTAL tubes={sum(days[d]["equiv"]/EV[d] for d in days)/150:.1f}  orders/day={len(rows)/24:.1f}')

with open("orders.json", "w", encoding="utf-8") as f:
    json.dump(rows, f, ensure_ascii=False, indent=1)

with open("orders.md", "w", encoding="utf-8") as f:
    f.write("| 天 | 订单 | 需求 | 奖励 |\n|---|---|---|---|\n")
    for r in rows:
        req = " + ".join(f'{q["count"]}×{CN[q["chain"]]}{q["level"]}级' for q in r["req"])
        f.write(f'| {r["day"]} | {r["id"]} | {req} | {r["reward"]} |\n')
print("written orders.json / orders.md")


