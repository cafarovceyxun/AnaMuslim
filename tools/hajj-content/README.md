# Həcc və Ümrə bələdçisi — məzmun generatorları

Mənbə: Muheymin 2-ci cild, «Həcc kitabı» (№ 776–929, `hadith.id` 986–1146) və iCloud-dakı `anamuslim-mezmun-*.json` yedəyi.

`evidence_2026_10_06.py` — kitabın tam yoxlamasından sonra `hajj_evidence` dolğusu (76 sətir) və iki mövcud sətrin
düzəlişi. Namaz bələdçisindəki üsuldur (`tools/salah-content/`), bir fərqlə: azərbaycanca çıxarış da əl ilə yazılmır,
başlanğıc və son ifadə ilə hədisin öz mətnindən kəsilir. `--print` hər cütü (AZ / AR) çap edir — yazmazdan əvvəl
bir-bir oxu: ərəbcə ilə azərbaycanca eyni rəvayətdən olmalıdır (№ 861 kimi uzun hədislərdə rəvayət işarəsi ver).

Qaydalar: `{{ }}` çıxarışa düşməsin (kart mətni xam göstərir); hər izah hədisin özünə (№) söykənsin, bab adı dəlil deyil.

⚠️ Seed artıq tətbiq olunub — təkrar işlətmə (`on conflict do nothing` dublikatı bloklayır, amma düzəlişlər təkrar yazılar).
