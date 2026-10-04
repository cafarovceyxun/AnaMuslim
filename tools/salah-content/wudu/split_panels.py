"""
Dəstəmaz şəkilləri — istifadəçinin 15 kadrlıq vərəqindən (`destemaz-wudu-ag-qara.svg`, 2026-10-04)
yalnız bələdçinin addımlarına uyğun kadrları vektor drawable kimi kəsir.

Vərəqdə şəkil tək `<path>`-dır; kadr çərçivələri (`<rect>`) və nömrələr (`<text>`) ayrı elementlərdir,
ona görə burada **götürülmür**. Hər alt-yol (M…Z) mərkəzinin düşdüyü kadra yazılır, sonra kadr öz
şəklinin sərhədinə qədər kəsilir.

İşlətmək: python3 split_panels.py <vərəq.svg> <composeResources/drawable>
"""
import re
import sys

# Vərəqdəki nömrə → drawable adı. Planda olmayanlar (6/8 qolu ovuşdurmaq, 11 boyun, 12–13 qulaqlar)
# qəsdən yoxdur: hədisdə həmin addımlar yoxdur.
PANELS = {
    1: "hands",
    2: "mouth",
    3: "nose",
    4: "face",
    5: "arm_right",
    7: "arm_left",
    9: "head",
    10: "head_back",
    14: "foot_right",
    15: "foot_left",
}

src_path, out_dir = sys.argv[1], sys.argv[2]
src = open(src_path).read()
d = re.search(r'<path[^>]* d="([^"]+)"', src).group(1)
rects = [tuple(map(float, r)) for r in re.findall(
    r'<rect x="([\d.]+)" y="([\d.]+)" width="([\d.]+)" height="([\d.]+)"', src)]

buckets = {i: [] for i in range(len(rects))}
for sub in re.findall(r"M[^M]*", d):
    nums = list(map(float, re.findall(r"-?\d+\.?\d*", sub)))
    xs, ys = nums[0::2], nums[1::2]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    for i, (x, y, w, h) in enumerate(rects):
        if x - 2 <= cx <= x + w + 2 and y - 2 <= cy <= y + h + 2:
            buckets[i].append((sub, xs, ys))
            break


def fmt(v: float) -> str:
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


PAD = 1.5
for number, name in PANELS.items():
    subs = buckets[number - 1]
    x0 = min(min(xs) for _, xs, _ in subs) - PAD
    y0 = min(min(ys) for _, _, ys in subs) - PAD
    x1 = max(max(xs) for _, xs, _ in subs) + PAD
    y1 = max(max(ys) for _, _, ys in subs) + PAD
    w, h = x1 - x0, y1 - y0

    def shift(sub: str) -> str:
        out, toggle = [], [0]

        def repl(m):
            v = float(m.group(0))
            v = v - (x0 if toggle[0] % 2 == 0 else y0)
            toggle[0] += 1
            return fmt(v)

        for cmd, args in re.findall(r"([MCLZ])([^MCLZ]*)", sub):
            toggle[0] = 0
            out.append(cmd + re.sub(r"-?\d+\.?\d*", repl, args).strip())
        return "".join(out)

    data = "".join(shift(s) for s, _, _ in subs)
    xml = f"""<!-- Dəstəmaz, vərəqin {number}-ci kadrı. tools/salah-content/wudu/split_panels.py ilə yaradılıb, əl ilə dəyişmə. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{fmt(w)}dp"
    android:height="{fmt(h)}dp"
    android:viewportWidth="{fmt(w)}"
    android:viewportHeight="{fmt(h)}">
    <path
        android:fillColor="#FF1B1B1F"
        android:fillType="evenOdd"
        android:pathData="{data}" />
</vector>
"""
    path = f"{out_dir}/dr_salah_wudu_{name}.xml"
    open(path, "w").write(xml)
    print(f"{number:>2} → {path.split('/')[-1]}  {w:.1f}×{h:.1f}  {len(subs)} yol  {len(xml) // 1024} KB")
