# Qazan — NFC kart sistemi

Durum: **1. mərhələ hazır və test serverində işləyir** (2026-10-02).
Köhnə QR axını silinməyib — `companies.loyalty_mode` (`QR | NFC | BOTH`) ilə
məkan-məkan seçilir, standart `BOTH`.

## Model

Hər məkana kart verilir, hər kart BİR işi görür:

| Kart | Nə edir | Haraya bağlıdır |
|---|---|---|
| `STAMP` | möhür basır | bir kampaniyaya (`program_id`) — hər kampaniyanın öz kartı |
| `POINTS` | sabit xal (default 100) yazır | məkana (coin balansı) |

Kassir hansı kartı toxundurursa o əməliyyat olur. Müştərinin telefonu kartı
**oxuyur** (kart passivdir), token serverə gedir, qərarı server verir.

## Təhlükəsizlik (NTAG 213 = statik token)

Statik etiketin tokenini kart oxunan hər telefon görür, yəni **surət çıxarmaq
nəzəri olaraq mümkündür**. Ona görə qəbul qaydaları serverdədir:

1. **UID bağlanması** — ilk oxunuşda çipin seriyası yadda saxlanılır, sonrakı
   oxunuşlarda eyni olmalıdır. Tokeni başqa çipə köçürmək işləmir
   (yalnız "magic" UID-dəyişən çip lazımdır).
2. **Cooldown** — eyni müştəri eyni kartı `cooldown_sec` (default 300 san.)
   tezliyindən çox oxuda bilməz.
3. **Günlük limit** — `daily_cap_per_user` (default 3) qəbul edilmiş oxunuş.
4. **Geofence** — telefon məkandan `companies.nfc_geofence_m` (default 300 m)
   uzaqdırsa rədd. Koordinat yoxdursa qəbul edilir, amma **şübhəli** işarələnir.
5. **Blok** — itmiş/kopyalanmış kart `LOST`/`REVOKED` edilir, dərhal dayanır.
6. **Yeni kod** — `rotate` tokeni dəyişir, kart yenidən yazılır (UID da sıfırlanır).
7. **Tam audit** — `nfc_taps` hər cəhdi yazır (rədd edilənlər də, ayrı
   transaksiyada ki, rollback onları silməsin). Admin "şübhəli" filtri ilə baxır.
8. **İdempotentlik** — `clientTapId` eyni oxunuşun iki dəfə pul yazmasının
   qarşısını alır (oflayn növbə üçün vacibdir).

NTAG 424 DNA-ya keçid üçün sxem hazırdır: `key_version`, `last_counter` və
`counter/cmac` sahələri var; sayğac irəli getmirsə `REPLAY` qaytarılır.

## Verilənlər

- `nfc_tags` — kart (şirkət, kampaniya, növ, token, UID, limitlər, status).
- `nfc_taps` — hər oxunuş (nəticə, xal/möhür dəyişikliyi, məsafə, cihaz, şübhəli).
- `companies.loyalty_mode`, `companies.nfc_geofence_m`.
- Möhür və xal üçün **yeni cədvəl yoxdur** — mövcud `loyalty_cards`,
  `loyalty_events`, `coin_transactions` istifadə olunur, yəni QR ilə NFC eyni
  balansı doldurur, tarixçə qalır.

## API

| Metod | Yol | Kim |
|---|---|---|
| POST | `/api/v1/nfc/tap` | müştəri (oxunuşu göndərir) |
| GET | `/api/v1/business/nfc/tags` | sahib (öz kartları) |
| PATCH | `/api/v1/business/nfc/tags/{id}` | sahib (xal/cooldown/limit) |
| PATCH | `/api/v1/business/nfc/tags/{id}/status?value=` | sahib (blok) |
| GET | `/api/v1/business/nfc/taps?days=&limit=` | sahib (oxunuş jurnalı) |
| GET/POST | `/api/v1/admin/nfc/tags` | admin (inventar, kart ver) |
| POST | `/api/v1/admin/nfc/tags/{id}/rotate` | admin (yeni kod) |
| POST | `/api/v1/admin/nfc/tags/{id}/provisioned` | admin (karta yazıldı) |
| PATCH | `/api/v1/admin/nfc/tags/{id}` / `/status` | admin |
| GET | `/api/v1/admin/nfc/taps?suspicious=&limit=` | admin (sui-istifadə) |

Rədd səbəbləri i18n ilə 4 dildə qaytarılır: `NFC_UNKNOWN_TAG`,
`NFC_TAG_REVOKED`, `NFC_COOLDOWN`, `NFC_DAILY_CAP`, `NFC_OUT_OF_RANGE`,
`NFC_MODE_OFF`, `NFC_REPLAY`.

## Kartın üzərində nə yazılır

NDEF mesajı iki qeyd:
1. `qazan.az:tag` xarici tipli qeyd — payload = token;
2. Android Application Record (`az.qazan.app`) — kart toxunanda tətbiq açılır,
   bunun üçün domen lazım deyil.

Kartı yazmaq: Admin → NFC → kart yarat → "Karta yaz" → boş etiketi telefonun
arxasına yaxınlaşdır. Yazıldıqdan sonra `provisioned_at` qeyd olunur.

## Tətbiq

- **Müştəri:** alt menyudakı mərkəzi düymə artıq `/nfc` — "Kartı oxut" ekranı
  (NFC yoxdur/sönülüdür halları, oflayn növbə, uğur animasiyası, kart
  tamamlananda "Hədiyyən hazırdır" sheet-i). QR ekranı yuxarıdakı düymədə qalır.
- **Biznes:** Hesab → NFC kartlar (kartlar + blok + xal/cooldown/limit + jurnal).
- **Admin:** Panel → NFC kartlar (inventar, kart ver, karta yaz, yeni kod, blok,
  oxunuş jurnalı + şübhəli filtri).

## Yoxlanılıb (test serverində)

Qəbul: xal kartı +100, möhür kartı 1 möhür, eyni `clientTapId` ikinci dəfə pul
yazmır. Rədd: kopyalanmış token başqa çipdə, məkandan uzaq oxunuş, naməlum
token, cooldown, bloklanmış kart. Hamısı `nfc_taps`-a düşür, şübhəlilər
işarələnir.

## QR-dan tam keçid (2026-10-02)

QR ekranları və paketləri **repo-da qalır**, amma UI-dan tamamilə gizlədilib
(`/qr`, `/qr/show` route-ları silinib, heç bir düymə onlara getmir):

- Müştəri: alt menyunun mərkəzi düyməsi `/nfc`; "QR göstər" düymələri və
  "kassada QR göstər" mətnləri NFC/təhvil mətnləri ilə əvəzlənib.
- Biznes: ortadakı tab artıq **Gözləyən hədiyyələr** — kassir müştərini
  skan etmir, siyahıdan adını tapıb "Təhvil ver" deyir.
  Yeni endpoint: `GET /api/v1/business/rewards/pending`
  (ACTIVE coin claim-ləri + tamamlanmış möhür kartları, müştəri adı ilə).
- Onboarding mətni və app_strings da NFC dilinə keçirildi.

Geri qaytarmaq lazım olsa: router-ə `/qr` və `/qr/show` route-larını əlavə et,
`QrDisplayScreen` / `QrScannerScreen` faylları olduğu kimi durur.

## Növbəti

- Real telefon + real NTAG 213 ilə saha testi (yazma, oxuma, antenna yeri).
- iPhone: Core NFC oxuyur, amma arxa planda açılış üçün **HTTPS domen** və
  Apple doğrulama faylı lazımdır → domen alınandan sonra.
- Kassir-səviyyəli hesabatlıq: hər kassaya ayrı kart (`label`) verilsin ki,
  jurnalda kimin verdiyi bilinsin.
- Avtomatik askıya alma: bir kartda dəqiqədə N-dən çox qəbul → `SUSPENDED` + admin bildirişi.
- NTAG 424 DNA alınanda: provizyon aracına AES açar yazma + SDM konfiqurasiyası.
