# `qibla-tiles`

Qiblə xəritəsinin tayl proxy-si. Tətbiq taylları **yalnız** buradan çəkir.

## Nə üçün

| Səbəb | İzah |
|---|---|
| Məxfilik | İstifadəçinin IP-si xəritə provayderinə çatmır — sorğu Supabase-dən gedir |
| Açar | HD peyk açarı serverdə qalır, açıq qaynaq repoda görünmür |
| Keçid | Provayder tətbiq yeniləməsi olmadan dəyişdirilə bilir |

## Yerləşdirmə

```
supabase functions deploy qibla-tiles
```

## Env dəyişənləri

| Dəyişən | Məcburi | İzah |
|---|---|---|
| `QIBLA_SAT_HD_KEY` | HD qatı üçün | Yoxdursa HD qatı 503 verir və tətbiq açıq peyk qatına qayıdır |
| `QIBLA_HD_ENABLED` | xeyr | `"false"` → HD qatı söndürülür (kvota/xərc açarı) |
| `QIBLA_STREET_URL` | xeyr | Küçə qatının şablonu |
| `QIBLA_SAT_URL` | xeyr | Açıq peyk qatının şablonu |
| `QIBLA_SAT_HD_URL` | xeyr | HD qatının şablonu (`{key}` yer tutucusu ilə) |

## Loglama

«Heç bir veri toplanmasın» tələbinə görə funksiya **heç nə loglamır**. Sürət limiti IP-nin
hash-ini yaddaşda pəncərə müddətincə saxlayır, sonra atır.

⚠️ Supabase-in öz platforma logları ayrı məsələdir — layihə ayarlarından log saxlama müddətini
minimuma endirmək tövsiyə olunur.

## Qat hədləri

| Qat | Mənbə | Maks zoom |
|---|---|---|
| `street` | OpenStreetMap | 18 |
| `sat` | Sentinel-2 cloudless (EOX, CC BY 4.0) | 14 — 10 m/piksel |
| `sat_hd` | Açarlı provayder | 18 |

Hədlər tətbiqdəki `QiblaMapLayer` ilə **eyni olmalıdır** — fərqlənsə tətbiq mövcud olmayan tayl
istəyir və xəritə boş qalır.
