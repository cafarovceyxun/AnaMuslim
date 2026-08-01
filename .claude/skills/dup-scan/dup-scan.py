#!/usr/bin/env python3
"""app/ və shared/ arasında eyni FQN-li Kotlin tiplərini tapır.

Niyə: commonMain-ə tip köçürəndə app-dakı kopya silinməzsə, kompilyator SUSUR
(fərqli modullardır), amma APK-da ART başqa dex-dəki sinfi yükləyir →
NoWhenBranchMatchedException / NoSuchMethodError runtime-da.

İki yoxlama:
  A) eyni (paket, fayl adı)  — fayl kopyalanıb, köhnəsi silinməyib
  B) eyni (paket, tip adı)   — tip başqa fayla köçüb, köhnəsi qalıb (A bunu tutmur)
"""
import os
import re
import sys

APP_ROOTS = ["app/src/main"]
SHARED_ROOTS = ["shared/src/commonMain", "shared/src/androidMain", "shared/src/iosMain"]

PKG_RE = re.compile(r"^\s*package\s+([\w.]+)", re.M)
# yalnız top-level (girinti olmayan) elanlar — daxili siniflər FQN toqquşması yaratmır
DECL_RE = re.compile(
    r"^(?:@\w+(?:\([^)]*\))?\s*)*"
    r"(?:public\s+|internal\s+|private\s+|abstract\s+|open\s+|sealed\s+|data\s+|value\s+|annotation\s+|enum\s+)*"
    r"(?:class|object|interface)\s+([A-Z]\w*)",
    re.M,
)


def scan(roots):
    by_file, by_type = {}, {}
    for root in roots:
        if not os.path.isdir(root):
            continue
        for dirpath, _, filenames in os.walk(root):
            for fn in filenames:
                if not fn.endswith(".kt"):
                    continue
                path = os.path.join(dirpath, fn)
                try:
                    txt = open(path, errors="replace").read()
                except OSError:
                    continue
                m = PKG_RE.search(txt)
                pkg = m.group(1) if m else "<root>"
                by_file.setdefault((pkg, fn), []).append(path)
                for line in txt.splitlines():
                    if line[:1].isspace() or not line:
                        continue
                    d = DECL_RE.match(line)
                    if d:
                        by_type.setdefault(f"{pkg}.{d.group(1)}", []).append(path)
    return by_file, by_type


app_files, app_types = scan(APP_ROOTS)
sh_files, sh_types = scan(SHARED_ROOTS)

print(f"app: {len(app_files)} fayl / {len(app_types)} top-level tip")
print(f"shared: {len(sh_files)} fayl / {len(sh_types)} top-level tip")
print()

dup_files = sorted(set(app_files) & set(sh_files))
dup_types = sorted(set(app_types) & set(sh_types))

if dup_files:
    print(f"❌ A) EYNİ (paket, fayl adı) — {len(dup_files)}")
    for k in dup_files:
        print(f"   {k[0]}.{k[1]}")
        for p in app_files[k] + sh_files[k]:
            print(f"      {p}")
else:
    print("✅ A) eyni (paket, fayl adı) dublikatı yoxdur")

print()
if dup_types:
    print(f"❌ B) EYNİ FQN TİP — {len(dup_types)}")
    for t in dup_types:
        print(f"   {t}")
        for p in app_types[t] + sh_types[t]:
            print(f"      {p}")
else:
    print("✅ B) eyni FQN-li top-level tip yoxdur")

print()
if dup_files or dup_types:
    print("Həll: app-dakı kopyanı SİL (shared saxlanılır), sonra /verify işlət.")
    print("Diqqət: eyni paketdə qəsdən bölünmüş fayllar ola bilər (məs. mapper-lər")
    print("app-də, entity-lər shared-də) — o halda fayl adları fərqlidir və B boş qalır.")
    sys.exit(1)
print("Dublikat tapılmadı.")
