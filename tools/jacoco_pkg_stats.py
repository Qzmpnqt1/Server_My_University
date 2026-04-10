import xml.etree.ElementTree as ET
import sys

path = sys.argv[1] if len(sys.argv) > 1 else "build/reports/jacoco/jacocoFullReport/jacocoFullReport.xml"
root = ET.parse(path).getroot()

def instr(pkg):
    for c in pkg.findall("counter"):
        if c.get("type") == "INSTRUCTION":
            return int(c.get("missed")), int(c.get("covered"))
    return 0, 0

rows = []
for pkg in root.findall("package"):
    name = pkg.get("name", "")
    if "org/example/service/impl" in name or "org/example/security" in name:
        m, c = instr(pkg)
        t = m + c
        pct = 100 * c / t if t else 0
        rows.append((pct, c, t, name))

rows.sort()
print("Lowest packages (impl+security):")
for pct, c, t, name in rows[:20]:
    print(f"  {pct:5.1f}% {c:5}/{t} {name}")

all_m = all_c = 0
for pct, c, t, name in rows:
    all_m += t - c
    all_c += c
print(f"\nCombined impl+security: {100*all_c/(all_c+all_m):.2f}% ({all_c}/{all_c+all_m})")
