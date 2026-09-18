import "jsr:@supabase/functions-js/edge-runtime.d.ts"

// AnaMuslim məzmun yedəyinin **oxu qapısı**.
//
// Yedək Supabase-də SAXLANMIR: Mac-dəki launchd agenti (`tools/supabase/backup.sh`, saatda bir
// yoxlayır, 3 gündə bir işləyir) cədvəlləri buradan çəkib birbaşa iCloud Drive-a yazır. Ona görə
// bu funksiyanın nə Storage-a yazısı, nə cron işi, nə də jurnalı var — yalnız iki rejim:
//
//   { "mode": "tables" }                              → cədvəl siyahısı + sətir sayları
//   { "mode": "table", "table": "hadith", "offset":0 } → həmin səhifənin JSON massivi
//
// ⚠️ Kimlik `Authorization` başlığından GƏLMİR. Köhnə `service_role` JWT-ləri 2026-nın sonunda
// dayanır, yeni `sb_secret_…` açarları isə `Authorization: Bearer`-də ümumiyyətlə qəbul olunmur —
// ona görə funksiya öz paylaşılan sirrini (`x-backup-secret`) Vault-dakı dəyərlə tutuşdurur
// (`backup_secret_ok`). Funksiya `verify_jwt = false` ilə yerləşdirilib.
//
// ⚠️ Cavab **parse edilmir**: RPC-nin mətni olduğu kimi ötürülür. Edge Function limiti sorğu başına
// **2 s CPU**-dur, 14 MB-lıq JSON-u JS-də açıb yenidən yığmaq həmin büdcəni yeyir.

const PAGE_LIMIT = 5000
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!

/**
 * Yeni açar sxemi (`SUPABASE_SECRET_KEYS`) varsa ondan, yoxdursa köhnə `service_role`-dan.
 * Beləliklə açar miqrasiyasından əvvəl də, sonra da eyni kod işləyir.
 */
function serviceKey(): string {
  const raw = Deno.env.get("SUPABASE_SECRET_KEYS")
  if (raw) {
    try {
      const parsed = JSON.parse(raw)
      if (parsed?.default) return parsed.default as string
    } catch {
      // köhnə açara düşürük
    }
  }
  return Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
}

/** `sb_secret_…` açarları yalnız `apikey` başlığında gedir; köhnə JWT hər ikisində. */
function authHeaders(): Record<string, string> {
  const key = serviceKey()
  const headers: Record<string, string> = { apikey: key }
  if (key.startsWith("eyJ")) headers["Authorization"] = `Bearer ${key}`
  return headers
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  })
}

async function rpcText(fn: string, body: unknown = {}): Promise<string> {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/rpc/${fn}`, {
    method: "POST",
    headers: { ...authHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`rpc ${fn}: HTTP ${res.status} — ${await res.text()}`)
  return await res.text()
}

async function rpc<T>(fn: string, body: unknown = {}): Promise<T> {
  return JSON.parse(await rpcText(fn, body)) as T
}

/**
 * Dəqiq sətir sayı — `Content-Range` başlığından. Mac səhifə sayını bundan hesablayır və yüklənən
 * sətirlərin sayını tutuşdurur (natamam yedək səssizcə keçməsin).
 */
async function tableCount(table: string): Promise<number> {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${table}?select=*`, {
    method: "HEAD",
    headers: { ...authHeaders(), Range: "0-0", Prefer: "count=exact" },
  })
  if (!res.ok) throw new Error(`count ${table}: HTTP ${res.status}`)
  const total = Number((res.headers.get("content-range") ?? "").split("/")[1])
  return Number.isFinite(total) ? total : 0
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return jsonResponse({ error: "yalnız POST" }, 405)

  const body = await req.json().catch(() => ({})) as {
    mode?: string
    table?: string
    offset?: number
    limit?: number
  }

  const secret = req.headers.get("x-backup-secret") ?? ""
  if (secret.length === 0 || !(await rpc<boolean>("backup_secret_ok", { p_secret: secret }))) {
    return jsonResponse({ error: "forbidden" }, 401)
  }

  try {
    const mode = body.mode ?? "tables"

    if (mode === "tables") {
      // Siyahı `pg_class`-dan gəlir: yeni cədvəl əlavə edən miqrasiya avtomatik yedəyə düşür.
      const tables = await rpc<{ table_name: string }[]>("backup_table_list")
      const withCounts = await Promise.all(
        tables.map(async ({ table_name }) => ({
          table: table_name,
          rows: await tableCount(table_name),
        })),
      )
      return jsonResponse({ page_limit: PAGE_LIMIT, tables: withCounts })
    }

    if (mode === "table") {
      if (!body.table) return jsonResponse({ error: "table adı yoxdur" }, 400)
      // Ağ siyahı yoxlaması `backup_table_json`-un içindədir (injection qapısı).
      const page = await rpcText("backup_table_json", {
        p_table: body.table,
        p_offset: Math.max(body.offset ?? 0, 0),
        p_limit: Math.min(body.limit ?? PAGE_LIMIT, PAGE_LIMIT),
      })
      return new Response(page, { headers: { "Content-Type": "application/json" } })
    }

    return jsonResponse({ error: `naməlum mode: ${mode}` }, 400)
  } catch (error) {
    return jsonResponse({ error: error instanceof Error ? error.message : String(error) }, 500)
  }
})
