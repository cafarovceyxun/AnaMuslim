#!/usr/bin/env python3
"""~/.anamuslim-migrate.env-dəki XAM parollardan düzgün URI qurur → ~/.anamuslim-migrate.urls (chmod 600).

Gözlənilən sətirlər (dırnaqsız, olduğu kimi):
    OLD_DB_PASSWORD=...
    NEW_DB_PASSWORD=...

Heç nə çap etmir, yalnız hər layihə üçün nəticə kateqoriyası (OK / parol səhv / ...).
Pooler hostunu yoxlayaraq tapır; parol səhvi görən kimi dayanır (pooler çoxlu uğursuz cəhddə IP-ni bloklayır).
"""
import os
import subprocess
import sys
import urllib.parse

HOME = os.path.expanduser("~")
SRC = os.path.join(HOME, ".anamuslim-migrate.env")
DST = os.path.join(HOME, ".anamuslim-migrate.urls")
PSQL = os.path.join(os.environ["PGBIN"], "psql")  # bax README.md

PROJECTS = {
    "OLD": ("molyqwcaynvsdmixtcbc", "ap-northeast-1"),
    "NEW": ("vyacxuwhtqqbythsovzt", "eu-central-1"),
}

values = {}
with open(SRC, encoding="utf-8") as f:
    for raw in f:
        line = raw.rstrip("\n").rstrip("\r")
        if "=" not in line:
            continue
        key, val = line.split("=", 1)
        values[key.strip()] = val

def try_connect(url):
    p = subprocess.run([PSQL, url, "-X", "-At", "-c", "select 1"], capture_output=True, text=True, timeout=30)
    if p.returncode == 0:
        return "ok"
    err = p.stderr
    if "password authentication failed" in err:
        return "badpass"
    if "Tenant or user not found" in err:
        return "wronghost"
    if "translate host name" in err or "timeout" in err or "timed out" in err:
        return "unreachable"
    return "other"

out = {}
ok = True
for tag, (ref, region) in PROJECTS.items():
    pw = values.get(f"{tag}_DB_PASSWORD")
    if not pw:
        print(f"{tag}: {tag}_DB_PASSWORD sətri yoxdur")
        ok = False
        continue
    enc = urllib.parse.quote(pw, safe="")
    candidates = [
        f"postgresql://postgres.{ref}:{enc}@aws-1-{region}.pooler.supabase.com:5432/postgres?sslmode=require",
        f"postgresql://postgres.{ref}:{enc}@aws-0-{region}.pooler.supabase.com:5432/postgres?sslmode=require",
    ]
    result = None
    for url in candidates:
        r = try_connect(url)
        if r == "ok":
            out[tag] = url
            result = "OK"
            break
        if r == "badpass":
            result = "parol səhvdir (dayandım — təkrar cəhd IP blokuna səbəb ola bilər)"
            break
        if r == "wronghost":
            continue
        result = {"unreachable": "host əlçatan deyil", "other": "naməlum xəta (mətn gizlədilib)"}[r]
    print(f"{tag}: {result or 'pooler hostu tapılmadı'}")
    ok = ok and tag in out

if ok:
    fd = os.open(DST, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w") as f:
        f.write(f"OLD_DB_URL='{out['OLD']}'\nNEW_DB_URL='{out['NEW']}'\n")
    print("URI-lər yazıldı → ~/.anamuslim-migrate.urls")
else:
    sys.exit(1)
