import "jsr:@supabase/functions-js/edge-runtime.d.ts"

// Qiblə xəritəsinin tayl proxy-si.
//
// Niyə var:
//   1. MƏXFİLİK — istifadəçinin IP-si xəritə provayderinə **heç vaxt çatmır**. Tətbiq yalnız bu
//      funksiya ilə danışır, yuxarı axına isə Supabase-in özü gedir.
//   2. AÇAR — HD peyk qatının API açarı serverdə qalır, yəni GPLv3 repoda görünmür.
//   3. KEÇİD — provayder tətbiq yeniləməsi olmadan dəyişdirilə bilir (aşağıdakı env dəyişənləri).
//
// Yol: GET /qibla-tiles/{layer}/{z}/{x}/{y}
//
// ⚠️ Tayl xidmətlərinin istifadə siyasətləri həcmə həssasdır. Bu funksiya məhz ona görə lazımdır
// ki, yuxarı axın bir yerdən idarə olunsun; həcm artarsa şablonu dəyişmək kifayətdir.

const CACHE_SECONDS = 60 * 60 * 24 * 30

/** Sorğuda tətbiqi tanıdan ad — bir çox tayl xidməti bunu tələb edir. */
const USER_AGENT = "AnaMuslim/1.0 (+https://github.com/cafarovceyxun)"

type Layer = {
  /** {z}/{x}/{y} yer tutucuları ilə şablon. */
  template: string
  maxZoom: number
  /** Açar tələb edirsə, hansı env dəyişənindən gəlir. */
  keyEnv?: string
  contentType: string
}

const LAYERS: Record<string, Layer> = {
  street: {
    template: Deno.env.get("QIBLA_STREET_URL") ??
      "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
    maxZoom: 18,
    contentType: "image/png",
  },
  // Sentinel-2 cloudless (EOX, CC BY 4.0) — pulsuz və açarsız, amma 10 m/piksel.
  // ⚠️ WMTS yolu {z}/{y}/{x} sırasındadır (TileMatrix/TileRow/TileCol), {z}/{x}/{y} deyil.
  sat: {
    template: Deno.env.get("QIBLA_SAT_URL") ??
      "https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless-2024_3857/default/g/{z}/{y}/{x}.jpg",
    maxZoom: 14,
    contentType: "image/jpeg",
  },
  // Dam səviyyəsi. Açar YOXDURSA və ya QIBLA_HD_ENABLED="false" olarsa qat söndürülür və tətbiq
  // özü açıq peyk qatına qayıdır (QiblaTileStore.highResAvailable).
  sat_hd: {
    template: Deno.env.get("QIBLA_SAT_HD_URL") ??
      "https://api.maptiler.com/tiles/satellite-v2/{z}/{x}/{y}.jpg?key={key}",
    maxZoom: 18,
    keyEnv: "QIBLA_SAT_HD_KEY",
    contentType: "image/jpeg",
  },
}

/**
 * Sadə sürət limiti.
 *
 * IP **açıq saxlanılmır** — yalnız hash-i sayğac açarı kimi işlədilir və pəncərə bitəndə atılır.
 * Yaddaşdadır, yəni instansiyalar arasında paylaşılmır; məqsəd bir klientin funksiyanı pulsuz
 * xəritə proxy-sinə çevirməsini çətinləşdirməkdir, tam qoruma deyil.
 */
const WINDOW_MS = 60_000
const MAX_PER_WINDOW = 600
const hits = new Map<string, { count: number; resetAt: number }>()

async function rateLimited(request: Request): Promise<boolean> {
  const raw = request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ?? "unknown"
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(raw))
  const key = Array.from(new Uint8Array(digest).slice(0, 8))
    .map((b) => b.toString(16).padStart(2, "0")).join("")

  const now = Date.now()
  const entry = hits.get(key)

  if (!entry || now > entry.resetAt) {
    hits.set(key, { count: 1, resetAt: now + WINDOW_MS })
    if (hits.size > 10_000) {
      for (const [k, v] of hits) if (now > v.resetAt) hits.delete(k)
    }
    return false
  }

  entry.count++
  return entry.count > MAX_PER_WINDOW
}

Deno.serve(async (request: Request) => {
  if (request.method !== "GET") return new Response("method not allowed", { status: 405 })

  const parts = new URL(request.url).pathname.split("/").filter(Boolean)
  // .../qibla-tiles/{layer}/{z}/{x}/{y}
  const tail = parts.slice(-4)
  if (tail.length !== 4) return new Response("bad path", { status: 400 })

  const [layerId, zRaw, xRaw, yRaw] = tail
  const layer = LAYERS[layerId]
  if (!layer) return new Response("unknown layer", { status: 400 })

  const z = Number(zRaw), x = Number(xRaw), y = Number(yRaw)
  if (!Number.isInteger(z) || !Number.isInteger(x) || !Number.isInteger(y)) {
    return new Response("bad tile", { status: 400 })
  }
  // Diapazon yoxlaması açıq proxy-yə çevrilməyin qarşısını alır: zoom həddi qatın nativ həddidir,
  // x/y isə həmin zoom-da mövcud olan tayl sayından kənara çıxa bilməz.
  if (z < 0 || z > layer.maxZoom) return new Response("zoom out of range", { status: 400 })
  const limit = 2 ** z
  if (x < 0 || x >= limit || y < 0 || y >= limit) {
    return new Response("tile out of range", { status: 400 })
  }

  if (layer.keyEnv && Deno.env.get("QIBLA_HD_ENABLED") === "false") {
    // Söndürülüb: tətbiq bunu görüb qatı gizlədir.
    return new Response("layer disabled", { status: 503 })
  }

  let url = layer.template
    .replace("{z}", String(z))
    .replace("{x}", String(x))
    .replace("{y}", String(y))

  if (layer.keyEnv) {
    const key = Deno.env.get(layer.keyEnv)
    if (!key) return new Response("layer unavailable", { status: 503 })
    url = url.replace("{key}", key)
  }

  if (await rateLimited(request)) return new Response("slow down", { status: 429 })

  const upstream = await fetch(url, { headers: { "User-Agent": USER_AGENT } })
  if (!upstream.ok) {
    return new Response("upstream error", { status: upstream.status === 404 ? 404 : 502 })
  }

  return new Response(upstream.body, {
    status: 200,
    headers: {
      "Content-Type": upstream.headers.get("content-type") ?? layer.contentType,
      // Uzun keş: CDN eyni taylı təkrar çəkmir, yəni yuxarı axına yük və xərc düşür.
      "Cache-Control": `public, max-age=${CACHE_SECONDS}, immutable`,
    },
  })
})
