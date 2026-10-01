#!/usr/bin/env python3
"""Yazılan, amma heç vaxt işə düşməyən kodu tapır: çağırılmayan funksiyalar və
yazılıb oxunmayan DataStore açarları.

Niyə: kompilyator istifadəsiz public/internal `fun`-a xəbərdarlıq vermir, testlər də
keçir — funksiya özü düzgündür, sadəcə heç kim onu çağırmır. 2026-09-18-də
`DuaPreferences.applyDefaultViewMode()` belə tapıldı: ayarlar vərəqi «Açılış rejimi»ni
yazırdı, onu tətbiq edəcək funksiya isə heç yerdən çağırılmırdı.

Yoxlamalar:
  A) obyekt üzvü — heç yerdə `Obyekt.ad` kimi çağırılmır            (dəqiq)
  B) qalan funksiyalar — adı başqa heç yerdə keçmir                 (dəqiq, az tutur)
  C) DataStore açarı — yazılır, amma oxunmur / heç istifadə olunmur
  T) yalnız testlərdən çağırılır                                    (məlumat)

A adla yox, obyekt adı ilə axtarır: `HadithPreferences.applyDefaultViewMode` beş yerdən
çağırılırdı və adla axtarış eyni adlı dua funksiyasını «istifadə olunur» sayırdı.

Ölü funksiyanın gövdəsindəki çağırışlar sayılmır (təkrarlanır, sabit nöqtəyə qədər): yalnız
ölü koddan çağırılan funksiya da ölüdür, yalnız ölü funksiyada oxunan açar da oxunmur.

İşlətmə:
  python3 .claude/skills/dead-scan/dead-scan.py                 # baseline-dan TƏZƏ tapıntılar
  python3 .claude/skills/dead-scan/dead-scan.py --all           # hamısı, baseline daxil
  python3 .claude/skills/dead-scan/dead-scan.py --changed       # yalnız main-dən dəyişən fayllar
  python3 .claude/skills/dead-scan/dead-scan.py <yol> ...       # yol süzgəci
  python3 .claude/skills/dead-scan/dead-scan.py --update-baseline
"""
import bisect
import os
import re
import subprocess
import sys
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
BASELINE = os.path.join(HERE, "baseline.txt")
os.chdir(ROOT)

KT_ROOTS = ["app/src", "shared/src"]
EXTRA_ROOTS = ["iosApp", "app/src/main/AndroidManifest.xml", "app/src/main/res/xml"]
TEST_DIR_RE = re.compile(r"/src/\w*[Tt]est\w*/")

# Sintaksisin özünün çağırdığı adlar — adla axtarışda heç vaxt görünmürlər.
SYNTAX_NAMES = {
    "invoke", "equals", "hashCode", "toString", "compareTo", "getValue", "setValue",
    "provideDelegate", "iterator", "hasNext", "next", "contains", "get", "set",
    "plus", "minus", "times", "div", "rem", "rangeTo", "not", "unaryMinus", "unaryPlus",
    "inc", "dec", "plusAssign", "minusAssign", "main",
} | {f"component{i}" for i in range(1, 10)}

# Bu modifikatorlu funksiyanı başqası çağırır: çərçivə, sintaksis, expect tərəfi.
SKIP_MODIFIERS = {"override", "operator", "actual", "external"}
# Bu annotasiyalı funksiyanı kod deyil, alət və ya runtime çağırır.
SKIP_ANNOTATIONS = {
    "Preview", "TypeConverter", "ProvidedTypeConverter", "Test", "BeforeTest", "AfterTest",
    "Before", "After", "BeforeClass", "AfterClass", "JavascriptInterface", "ObjCAction", "Keep",
}

READ_CALLS = {"read", "readFirst", "observe", "flow", "flowMultiple", "contains", "get"}
WRITE_CALLS = {"write", "remove", "removeAll", "writeAll", "set"}


# ---------------------------------------------------------------------------
# Leksik təmizləmə: şərhlər və sətir mətni boşluğa çevrilir (mövqelər qalır),
# `${...}` şablon ifadələri saxlanılır — orada da çağırış ola bilər.
# ---------------------------------------------------------------------------
def clean_source(src):
    out = list(src)
    n = len(src)

    def blank(a, b):
        for k in range(a, min(b, n)):
            if out[k] != "\n":
                out[k] = " "

    def lex_string(i, raw):
        """Sətrin içində irəliləyir. (mövqe, "tmpl") — `${` tapıldı; (mövqe, None) — sətir bitdi."""
        while i < n:
            if raw:
                if src.startswith('"""', i):
                    j = i + 3
                    while j < n and src[j] == '"':
                        j += 1
                    return j, None
            else:
                c = src[i]
                if c == "\\":
                    blank(i, i + 2)
                    i += 2
                    continue
                if c == '"':
                    return i + 1, None
                if c == "\n":
                    return i, None
            if src[i] == "$" and i + 1 < n and src[i + 1] == "{":
                return i + 2, "tmpl"
            if src[i] == "$" and i + 1 < n and (src[i + 1].isalpha() or src[i + 1] == "_"):
                j = i + 1
                while j < n and (src[j].isalnum() or src[j] == "_"):
                    j += 1
                i = j
                continue
            blank(i, i + 1)
            i += 1
        return i, None

    stack = []  # şablon ifadəsindən sonra qayıdılacaq sətirlər: (raw, brace_depth)
    depth = 0
    i = 0
    while i < n:
        c = src[i]
        if c == "/" and src.startswith("//", i):
            j = src.find("\n", i)
            j = n if j < 0 else j
            blank(i, j)
            i = j
            continue
        if c == "/" and src.startswith("/*", i):
            lvl, j = 1, i + 2
            while j < n and lvl:
                if src.startswith("/*", j):
                    lvl, j = lvl + 1, j + 2
                elif src.startswith("*/", j):
                    lvl, j = lvl - 1, j + 2
                else:
                    j += 1
            blank(i, j)
            i = j
            continue
        if c == "'":
            j = i + 1
            if j < n and src[j] == "\\":
                j += 2
                while j < n and src[j] not in "'\n":
                    j += 1
            else:
                j += 1
            if j < n and src[j] == "'":
                blank(i + 1, j)
                i = j + 1
            else:
                i += 1
            continue
        if c == '"':
            raw = src.startswith('"""', i)
            j, state = lex_string(i + (3 if raw else 1), raw)
            if state == "tmpl":
                stack.append((raw, depth))
                depth += 1
            i = j
            continue
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if stack and stack[-1][1] == depth:
                raw, _ = stack.pop()
                j, state = lex_string(i + 1, raw)
                if state == "tmpl":
                    stack.append((raw, depth))
                    depth += 1
                i = j
                continue
        i += 1
    return "".join(out)


# ---------------------------------------------------------------------------
# Korpus və identifikator indeksi
# ---------------------------------------------------------------------------
def walk(roots, exts):
    for root in roots:
        if os.path.isfile(root):
            if root.endswith(exts):
                yield root
            continue
        for dirpath, dirnames, filenames in os.walk(root):
            dirnames[:] = [d for d in dirnames if d not in ("build", ".gradle", "Pods", "DerivedData")]
            for fn in filenames:
                if fn.endswith(exts):
                    yield os.path.join(dirpath, fn)


def read(path):
    with open(path, encoding="utf-8", errors="replace") as f:
        return f.read()


kt_files = sorted(walk(KT_ROOTS, (".kt",)))
raw_kt = {p: read(p) for p in kt_files}
corpus = {p: clean_source(t) for p, t in raw_kt.items()}
for p in walk(KT_ROOTS, (".java",)):
    corpus[p] = clean_source(read(p))
for p in walk(EXTRA_ROOTS, (".swift", ".xml")):
    corpus[p] = read(p)


def is_test(path):
    return bool(TEST_DIR_RE.search("/" + path))


IDENT_RE = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")
IMPORT_RE = re.compile(r"^[ \t]*import[ \t][^\n]*", re.M)

index = defaultdict(list)  # ad → [(yol, mövqe)]; import sətirləri daxil deyil
for p, t in corpus.items():
    imports = [(m.start(), m.end()) for m in IMPORT_RE.finditer(t)]
    k = 0
    for m in IDENT_RE.finditer(t):
        s = m.start()
        while k < len(imports) and imports[k][1] <= s:
            k += 1
        if k < len(imports) and imports[k][0] <= s:
            continue
        index[m.group()].append((p, s))

# ObjC selector-ları sətir içindədir (`NSSelectorFromString("ad:")`) — təmizləmə onları silir.
selector_names = set()
for t in raw_kt.values():
    for m in re.finditer(r'(?:NSSelectorFromString|sel_registerName)\s*\(\s*"(\w+)', t):
        selector_names.add(m.group(1))

line_starts = {}


def line_of(path, pos):
    if path not in line_starts:
        t = corpus[path]
        line_starts[path] = [0] + [m.end() for m in re.finditer("\n", t)]
    return bisect.bisect_right(line_starts[path], pos)


# ---------------------------------------------------------------------------
# Elanlar
# ---------------------------------------------------------------------------
TOKEN_RE = re.compile(r"\b(companion\s+object|object|class|interface|fun|val|var)\b|[{}();=]")
TYPE_NAME_RE = re.compile(r"\s*(`[^`]+`|[A-Za-z_]\w*)")
FUN_RE = re.compile(
    r"fun\s*(?:<(?:[^<>]|<(?:[^<>]|<[^<>]*>)*>)*>\s*)?"
    r"(?:((?:[\w.]+(?:<(?:[^<>]|<[^<>]*>)*>)?\??)|\([^)]*\)\s*->\s*[\w.?]+)\s*\.\s*)?"
    r"(`[^`]+`|\w+)\s*\("
)
KEY_INIT_RE = re.compile(
    r"\s*(?:androidx\.datastore\.preferences\.core\.)?(?:PrefKey\s*\(|\w*PreferencesKey\s*\()"
)
MOD_WORDS = (
    "public|private|internal|protected|open|abstract|final|override|operator|infix|inline|"
    "suspend|tailrec|external|actual|expect|const|lateinit"
)


def header_before(text, pos):
    """`fun`/`val`-dan əvvəlki modifikator və annotasiyalar (yuxarıdakı annotasiya sətirləri daxil)."""
    ls = text.rfind("\n", 0, pos) + 1
    head = text[ls:pos]
    j = ls - 1
    while j > 0:
        pls = text.rfind("\n", 0, j) + 1
        line = text[pls:j].strip()
        if not line.startswith("@"):
            break
        head = line + " " + head
        j = pls - 1
    mods = set(re.findall(r"\b(" + MOD_WORDS + r")\b", head))
    anns = {a.split(".")[-1] for a in re.findall(r"@(?:\w+:)?([\w.]+)", head)}
    return mods, anns


def matching(text, i, open_c, close_c):
    depth = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == open_c:
            depth += 1
        elif c == close_c:
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return n - 1


def indent_of(text, pos):
    ls = text.rfind("\n", 0, pos) + 1
    j = ls
    while j < len(text) and text[j] in " \t":
        j += 1
    return j - ls


def body_span(text, decl_start, paren_open):
    """Funksiyanın gövdəsinin sonu (gövdəsizdirsə imzanın sonu)."""
    close = matching(text, paren_open, "(", ")")
    eol = text.find("\n", close)
    eol = len(text) if eol < 0 else eol
    seg = text[close + 1:eol]
    body_at = None
    m = re.search(r"[{=]", seg)
    if m and not (m.group() == "=" and seg[m.start():m.start() + 2] == "=="):
        body_at = close + 1 + m.start()
    else:
        nxt = re.match(r"\s*\n\s*([{=])", text[eol:eol + 200])
        if nxt:
            body_at = eol + nxt.start(1)
    if body_at is None:
        return close + 1
    if text[body_at] == "{":
        return matching(text, body_at, "{", "}") + 1
    # ifadə gövdəsi: fun sətrindən az/eyni girintili ilk sətrə qədər
    base = indent_of(text, decl_start)
    depth = 0
    i = body_at + 1
    n = len(text)
    while i < n:
        c = text[i]
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
            if depth < 0:
                return i
        elif c == "\n" and depth == 0:
            j = i + 1
            while j < n and text[j] in " \t\n":
                if text[j] == "\n":
                    i = j
                j += 1
            if j >= n or indent_of(text, j) <= base:
                return i
        i += 1
    return n


class Decl:
    def __init__(self, **kw):
        self.__dict__.update(kw)

    @property
    def label(self):
        return ".".join(self.owners + [self.name]) if self.owners else self.name

    @property
    def ident(self):
        return f"{self.kind}:{self.path}:{self.label}"


def scan_decls(path, text):
    decls = []
    scope = []  # hər `{` üçün ("type", ad, növ) və ya ("block",)
    pending = None
    paren = 0
    for m in TOKEN_RE.finditer(text):
        tok = m.group(1) or m.group(0)
        if tok == "(":
            paren += 1
            continue
        if tok == ")":
            paren = max(0, paren - 1)
            continue
        if tok == "{":
            scope.append(("type",) + pending if pending and paren == 0 else ("block",))
            pending = None
            continue
        if tok == "}":
            if scope:
                scope.pop()
            pending = None
            continue
        if tok == ";" or (tok == "=" and paren == 0):
            pending = None
            continue
        if tok == "=" or paren:
            continue
        if tok.startswith("companion"):
            nm = re.match(r"\s*([A-Za-z_]\w*)", text[m.end():m.end() + 60])
            name = nm.group(1) if nm and nm.group(1) not in ("private", "internal") else "Companion"
            pending = (name, "companion")
            continue
        if tok in ("object", "class", "interface"):
            nm = TYPE_NAME_RE.match(text, m.end())
            if nm:
                pending = (nm.group(1).strip("`"), tok)
            continue
        types = [s for s in scope if s[0] == "type"]
        local = bool(scope) and scope[-1][0] == "block"
        owner = types[-1] if types else None
        owners = [s[1] for s in types if s[2] != "companion"]
        if tok == "fun":
            fm = FUN_RE.match(text, m.start())
            if not fm:
                continue
            mods, anns = header_before(text, m.start())
            decls.append(Decl(
                kind="fun", name=fm.group(2).strip("`"), path=path, pos=fm.start(2),
                end=body_span(text, m.start(), fm.end() - 1), owner=owner, owners=owners,
                local=local, receiver=fm.group(1), mods=mods, anns=anns,
            ))
        elif tok in ("val", "var") and not local:
            vm = re.compile(r"va[lr]\s+(\w+)\s*(?::[^=\n]*)?=").match(text, m.start())
            if not vm or not KEY_INIT_RE.match(text, vm.end()):
                continue
            mods, anns = header_before(text, m.start())
            decls.append(Decl(
                kind="key", name=vm.group(1), path=path, pos=vm.start(1), end=vm.end(),
                owner=owner, owners=owners, local=False, receiver=None, mods=mods, anns=anns,
            ))
    return decls


all_decls = []
for p in kt_files:
    all_decls.extend(scan_decls(p, corpus[p]))
funs = [d for d in all_decls if d.kind == "fun"]
keys = [d for d in all_decls if d.kind == "key"]

decl_sites = defaultdict(set)
for d in funs:
    decl_sites[d.name].add((d.path, d.pos))


# ---------------------------------------------------------------------------
# İstifadə yerləri
# ---------------------------------------------------------------------------
def raw_hits(d):
    sites = decl_sites.get(d.name, ()) if d.kind == "fun" else {(d.path, d.pos)}
    hits = [h for h in index.get(d.name, ()) if h not in sites]
    if d.local or "private" in d.mods:
        return [h for h in hits if h[0] == d.path]
    if d.owner is not None and d.owner[2] == "object" and d.receiver is None:
        return qualified(d, hits)
    return hits


def qualified(d, hits):
    """Obyekt üzvü: başqa faylda `Obj.ad` / `Obj::ad`, Swift `Obj.shared.ad`, Java `Obj.INSTANCE.ad`;
    öz faylında, `Obj.ad`-ı import edən və ya `with(Obj)` işlədən faylda adın özü kifayətdir."""
    obj = d.owner[1]
    q = re.compile(r"(?<![\w$])" + re.escape(obj) + r"\s*(?:\.\s*(?:shared|INSTANCE)\s*)?(?:\.|::)\s*$")
    open_scope = re.compile(
        r"^[ \t]*import[ \t][\w.]*\b" + re.escape(obj) + r"\.(?:" + re.escape(d.name) + r"|\*)\b"
        r"|with\s*\(\s*" + re.escape(obj) + r"\s*\)"
        r"|(?<![\w$])" + re.escape(obj) + r"\s*\.\s*(?:run|apply|also)\s*\{", re.M)
    out, cache = [], {}
    for p, h in hits:
        if p == d.path:
            out.append((p, h))
            continue
        if p not in cache:
            cache[p] = bool(open_scope.search(corpus[p]))
        if cache[p] or q.search(corpus[p][max(0, h - len(obj) - 30):h]):
            out.append((p, h))
    return out


hits_of = {id(d): raw_hits(d) for d in all_decls}

spans_by_file = defaultdict(list)  # yol → [(başlanğıc, son, decl)]
for d in funs:
    spans_by_file[d.path].append((d.pos, d.end, d))


def skipped(d):
    return (d.mods & SKIP_MODIFIERS) or (d.anns & SKIP_ANNOTATIONS) or d.name in SYNTAX_NAMES \
        or d.name in selector_names


def enclosing_dead(path, pos, dead_ids, own=None):
    for s, e, f in spans_by_file.get(path, ()):
        if s <= pos < e and (id(f) in dead_ids or f is own):
            return f
    return None


# Sabit nöqtə: məhsul kodunda canlı çağırışı olmayan funksiyalar «ölü»dür; onların gövdəsindəki
# çağırışlar da sayılmır.
candidates = [d for d in funs if not is_test(d.path) and not skipped(d)]
dead = set()
while True:
    new = set()
    for d in candidates:
        live = any(not is_test(p) and not enclosing_dead(p, h, dead, own=d)
                   for p, h in hits_of[id(d)])
        if not live:
            new.add(id(d))
    if new == dead:
        break
    dead = new


def callers_note(d):
    """Ölü funksiyanın test-xarici çağırışları hansı ölü funksiyalardadır."""
    names = set()
    for p, h in hits_of[id(d)]:
        if is_test(p):
            continue
        f = enclosing_dead(p, h, dead)
        if f is not None and f is not d:
            names.add(f.label)
    return sorted(names)


# ---------------------------------------------------------------------------
# DataStore açarları
# ---------------------------------------------------------------------------
def enclosing_call(text, pos):
    dp = db = 0
    i = pos - 1
    lim = max(0, pos - 1500)
    while i >= lim:
        c = text[i]
        if c == ")":
            dp += 1
        elif c == "(":
            if dp == 0:
                m = re.search(r"(\w+)\s*(?:<[^()]*>)?\s*$", text[max(0, i - 80):i])
                return ("(", m.group(1) if m else "")
            dp -= 1
        elif c == "]":
            db += 1
        elif c == "[":
            if db == 0 and dp == 0:
                close = matching(text, i, "[", "]")
                return ("[", bool(re.match(r"\s*=(?!=)", text[close + 1:close + 5])))
            db -= 1
        elif c in "{};" and dp == 0 and db == 0:
            return None
        i -= 1
    return None


def classify_key_use(path, pos):
    if not path.endswith(".kt"):
        return "other"
    ctx = enclosing_call(corpus[path], pos)
    if ctx is None:
        return "other"
    if ctx[0] == "[":
        return "write" if ctx[1] else "read"
    if ctx[1] in WRITE_CALLS:
        return "write"
    if ctx[1] in READ_CALLS:
        return "read"
    if ctx[1] == "PrefKey":
        return "alias"
    return "other"


# DataStore-da açarın kimliyi Kotlin `val`-ı deyil, **sətir adıdır**: miqrasiya bir `val` ilə yazır,
# ViewModel başqası ilə oxuyur (`ReaderIndexFavouritesMigration.KEY` ↔ `ReaderIndexViewModel.KEY`).
# Ona görə oxu/yazı eyni ada işarə edən bütün `val`-lar üzrə toplanır.
CONST_RE = re.compile(r'const\s+val\s+(\w+)\s*(?::\s*String\s*)?=\s*"([^"]*)"')
consts = defaultdict(set)
file_consts = defaultdict(dict)  # eyni ad iki faylda fərqli dəyərlə ola bilər (dua./hadith.)
for path, t in raw_kt.items():
    for m in CONST_RE.finditer(t):
        consts[m.group(1)].add(m.group(2))
        file_consts[path][m.group(1)] = m.group(2)
keys_by_file = defaultdict(dict)
for d in keys:
    keys_by_file[d.path][d.name] = d


def store_name(d, seen=()):
    text, raw = corpus[d.path], raw_kt[d.path]
    op = text.find("(", d.end)
    seg = raw[d.end:matching(text, op, "(", ")") + 1] if op >= 0 else ""
    m = re.search(r'PreferencesKey\s*\(\s*(?:"([^"]*)"|([\w.]+))', seg)
    if m and m.group(1) is not None:
        return m.group(1)
    if m:
        ref = m.group(2).split(".")[-1]
        if "." not in m.group(2) and ref in file_consts[d.path]:
            return file_consts[d.path][ref]
        if "." in m.group(2):  # `QuranScriptUtils.KEY_SCRIPT` — sahib obyektin faylından
            owner = m.group(2).split(".")[-2]
            for path, cs in file_consts.items():
                if ref in cs and re.search(r"\bobject\s+" + re.escape(owner) + r"\b", raw_kt[path]):
                    return cs[ref]
        vals = consts.get(ref, set())
        return next(iter(vals)) if len(vals) == 1 else d.ident
    # `PrefKey(KEY_X, …)` — eyni fayldakı başqa açarın üzərinə bükülüb
    a = re.match(r"\s*PrefKey\s*\(\s*([\w.]+)", seg)
    inner = keys_by_file[d.path].get(a.group(1).split(".")[-1]) if a else None
    if inner is not None and inner is not d and inner.name not in seen:
        return store_name(inner, seen + (d.name,))
    return d.ident


groups = defaultdict(list)
for d in keys:
    if not is_test(d.path):
        groups[store_name(d)].append(d)

key_findings = {"write_only": [], "unused": [], "read_only": []}
for name, members in groups.items():
    members.sort(key=lambda x: (x.path, x.pos))
    kinds = defaultdict(int)
    dead_readers = set()
    for d in members:
        for p, h in hits_of[id(d)]:
            if is_test(p):
                continue
            k = classify_key_use(p, h)
            if k == "alias":
                continue  # başqa `val`-ın tərifidir, istifadə deyil
            f = enclosing_dead(p, h, dead)
            if f is not None:
                if k == "read":
                    dead_readers.add(f.label)
                continue
            kinds[k] += 1
    rep = members[0]
    rep.dead_readers = sorted(dead_readers)
    rep.siblings = members[1:]
    rep.store = name
    if not any(kinds.values()):
        key_findings["unused"].append(rep)
    elif kinds["write"] and not kinds["read"] and not kinds["other"]:
        key_findings["write_only"].append(rep)
    elif kinds["read"] and not kinds["write"] and not kinds["other"]:
        key_findings["read_only"].append(rep)


# ---------------------------------------------------------------------------
# Hesabat
# ---------------------------------------------------------------------------
def changed_files():
    files = set()
    cmds = [["git", "diff", "--name-only"], ["git", "diff", "--name-only", "--cached"],
            ["git", "ls-files", "--others", "--exclude-standard"]]
    for ref in ("origin/main", "main"):
        r = subprocess.run(["git", "merge-base", "HEAD", ref], capture_output=True, text=True)
        if r.returncode == 0:
            cmds.append(["git", "diff", "--name-only", r.stdout.strip(), "HEAD"])
            break
    for cmd in cmds:
        r = subprocess.run(cmd, capture_output=True, text=True)
        files.update(x for x in r.stdout.split() if x.endswith(".kt"))
    return files


args = sys.argv[1:]
show_all = "--all" in args
update_baseline = "--update-baseline" in args
changed = changed_files() if "--changed" in args else None
filters = [a.rstrip("/") for a in args if not a.startswith("--")]

if update_baseline and (filters or changed is not None):
    sys.exit("--update-baseline yalnız bütün layihə üzərində işləyir (süzgəcsiz).")


def selected(d):
    if changed is not None and d.path not in changed:
        return False
    return not filters or any(d.path == f or d.path.startswith(f + "/") for f in filters)


cat_a, cat_b, cat_t = [], [], []
for d in candidates:
    if id(d) not in dead:
        continue
    test_hits = sorted({p for p, _ in hits_of[id(d)] if is_test(p)})
    if test_hits:
        cat_t.append((d, test_hits))
    elif d.owner is not None and d.owner[2] == "object" and not d.local and d.receiver is None:
        cat_a.append(d)
    else:
        cat_b.append(d)

sections = [
    ("A", "obyekt üzvü, heç yerdən çağırılmır", [(d, None) for d in cat_a]),
    ("B", "funksiya, adı başqa heç yerdə keçmir", [(d, None) for d in cat_b]),
    ("C1", "DataStore açarı yazılır, heç vaxt oxunmur", [(d, None) for d in key_findings["write_only"]]),
    ("C2", "DataStore açarı heç istifadə olunmur", [(d, None) for d in key_findings["unused"]]),
    ("T", "yalnız testlərdən çağırılır (məhsulda ölüdür)", cat_t),
]

if update_baseline:
    ids = sorted(f"{code} {d.ident}" for code, _, items in sections for d, _ in items)
    with open(BASELINE, "w", encoding="utf-8") as f:
        f.write("# dead-scan baseline — bilinən tapıntılar. Yeniləmə: --update-baseline\n")
        f.write("# Sətir silmək = həmin tapıntını yenidən göstər. Əl ilə əlavə etmə.\n")
        f.write("\n".join(ids) + "\n")
    print(f"baseline yazıldı: {len(ids)} tapıntı → {os.path.relpath(BASELINE)}")
    sys.exit(0)

baseline = set()
if os.path.exists(BASELINE) and not show_all:
    with open(BASELINE, encoding="utf-8") as f:
        baseline = {ln.strip() for ln in f if ln.strip() and not ln.startswith("#")}

scope = []
if changed is not None:
    scope.append("yalnız main-dən dəyişən fayllar")
if filters:
    scope.append("süzgəc: " + ", ".join(filters))
if baseline:
    scope.append("baseline-dakılar gizlədilib, --all hamısını göstərir")
print(f"{len(kt_files)} Kotlin faylı, {len(funs)} funksiya, {len(keys)} DataStore açarı"
      + (f" ({'; '.join(scope)})" if scope else ""))
print()

new_count = hidden = 0
for code, title, items in sections:
    shown = []
    for d, extra in items:
        if not selected(d):
            continue
        if f"{code} {d.ident}" in baseline:
            hidden += 1
            continue
        shown.append((d, extra))
    mark = "⚠️ " if code == "T" else "❌"
    if not shown:
        print(f"✅ {code}) {title} — yoxdur")
        print()
        continue
    if code != "T":
        new_count += len(shown)
    print(f"{mark} {code}) {title} — {len(shown)}")
    for d, extra in sorted(shown, key=lambda x: (x[0].path, x[0].pos)):
        print(f"   {d.path}:{line_of(d.path, d.pos)}  {d.label}")
        if d.kind == "fun":
            via = callers_note(d)
            if via:
                print(f"         ↳ yalnız ölü koddan: {', '.join(via[:3])}")
        else:
            if d.dead_readers:
                print(f"         ↳ oxuyan funksiyalar ölüdür: {', '.join(d.dead_readers[:3])}")
            for sib in d.siblings[:3]:
                print(f"         ↳ eyni açar «{d.store}»: {sib.path}:{line_of(sib.path, sib.pos)}")
        for t in (extra or [])[:2]:
            print(f"         ↳ test: {t}")
    print()

ro = [d for d in key_findings["read_only"] if selected(d)]
if ro and show_all:
    print(f"ℹ️  C3) açar oxunur, kodda yazılmır — {len(ro)} (çox vaxt normaldır: köhnə quraşdırma,")
    print("    miqrasiya, ehtiyat nüsxədən bərpa)")
    for d in sorted(ro, key=lambda x: x.path):
        print(f"   {d.path}:{line_of(d.path, d.pos)}  {d.label}")
    print()

if hidden:
    print(f"({hidden} bilinən tapıntı baseline-dadır — --all ilə bax)")
if new_count:
    print("Hər sətir üçün sual: «bu işə düşməli idi?» Bəli → çağırış yeri əskikdir (dua açılış")
    print("rejimi kimi), onu bağla. Yox → sil. Yanlış-müsbətlər SKILL.md-dədir.")
    sys.exit(1)
print("Təzə ölü funksiya və ya açar yoxdur.")
