// AnaMuslim: backup.cafarovceyxun.com → ehtiyat Supabase (Frankfurt, vyacxuwhtqqbythsovzt).
//
// Supabase-in öz Custom Domain-i pulludur, ona görə ehtiyat ünvanı bu Worker verir: tətbiq domenə baxır,
// ehtiyat server dəyişsə yalnız ORIGIN dəyişir (tətbiq yeniləməsi lazım olmur).
//
// Ehtiyatda yalnız cədvəllər (gecəlik köçürmə) və Edge Function-lar (qibla-tiles, db-backup) var —
// storage fayları çox yer tutduğu üçün köçürülmür, ona görə /storage/ bağlıdır. Ötürülənlər: REST
// (cədvəllər + admin keçid düyməsi), auth (admin girişi), funksiyalar.
const ORIGIN = "https://vyacxuwhtqqbythsovzt.supabase.co";
const ALLOWED = ["/rest/v1/", "/auth/v1/", "/functions/v1/"];

export default {
  async fetch(request) {
    const url = new URL(request.url);
    if (!ALLOWED.some((p) => url.pathname.startsWith(p))) {
      return new Response("not found", { status: 404 });
    }
    const target = ORIGIN + url.pathname + url.search;
    return fetch(new Request(target, request));
  },
};
