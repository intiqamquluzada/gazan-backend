package az.qazan.backend.legal;

/**
 * Texts of the legal pages. Az (Azerbaijan, Law No. 998-IIIQ "On personal
 * data") and tr (Türkiye, KVKK No. 6698, incl. the Aydınlatma Metni) are the
 * complete versions; en and ru summarise them and point to the full texts.
 *
 * <p>The descriptions follow what the app and the backend actually do; keep
 * them in step when data handling changes.
 */
final class LegalTexts {

    private LegalTexts() {
    }

    record Ctx(String entity, String email, String address, String date) {
    }

    record Page(String title, String meta, String body) {
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String sec(String h, String html) {
        return "<section><h2>" + h + "</h2>" + html + "</section>";
    }

    private static String ul(String... items) {
        StringBuilder b = new StringBuilder("<ul>");
        for (String i : items) b.append("<li>").append(i).append("</li>");
        return b.append("</ul>").toString();
    }

    private static String p(String s) {
        return "<p>" + s + "</p>";
    }

    // ═════════════════════════════ PRIVACY ═════════════════════════════

    static Page privacy(String lang, Ctx c) {
        return switch (lang) {
            case "tr" -> privacyTr(c);
            case "en" -> privacyEn(c);
            case "ru" -> privacyRu(c);
            default -> privacyAz(c);
        };
    }

    private static Page privacyAz(Ctx c) {
        String b = sec("1. Məlumat operatoru", p("Qazan tətbiqi və xidməti (bundan sonra \"Qazan\") üzrə fərdi məlumatların operatoru: <b>"
                        + c.entity() + "</b>, ünvan: " + c.address() + ", e-poçt: " + c.email() + ".")
                        + p("Bu siyasət \"Fərdi məlumatlar haqqında\" Azərbaycan Respublikasının 11 may 2010-cu il tarixli 998-IIIQ nömrəli Qanununa uyğun hazırlanıb. Türkiyədəki istifadəçilər üçün KVKK (6698 saylı Qanun) üzrə məlumat <a href=\"?lang=tr\">türk dilində</a> verilir."))
                + sec("2. Hansı məlumatları toplayırıq", ul(
                        "<b>Hesab:</b> ad, soyad, e-poçt, telefon nömrəsi, doğum tarixi, şifrə (yalnız şifrələnmiş həş formasında), istəyə bağlı profil şəkli; Google və ya Apple ilə daxil olsanız həmin hesabın identifikatoru, adı və e-poçtu.",
                        "<b>Sadiqlik fəaliyyəti:</b> kartlarınız, möhürlər, coin hərəkətləri, alınan və istifadə olunan hədiyyələr, kassada açılan pəncərələr.",
                        "<b>NFC oxutmaları:</b> kartın kodu və çip nömrəsi (UID), cihaz identifikatoru, vaxt və nəticə. Kartı oxudanda telefonunuzun yeri (dəqiq və ya təxmini) yalnız məkana olan məsafəni yoxlamaq üçün göndərilir; koordinatlar saxlanmır, yalnız hesablanmış məsafə saxlanılır.",
                        "<b>Yer məlumatı (tətbiqdə):</b> icazə versəniz, yaxınlıqdakı məkanları və məsafəni göstərmək üçün cihazınızda istifadə olunur.",
                        "<b>Bildirişlər:</b> push bildirişləri üçün cihaz tokeni və tətbiqdaxili bildiriş qutunuz.",
                        "<b>Rəylər:</b> yazdığınız qiymətləndirmə və şərhlər, göndərdiyiniz şikayətlər.",
                        "<b>Biznes hesabları:</b> məkanın adı, ünvanı, əlaqə və iş saatları, yüklənən foto və loqolar, menyu, kampaniyalar, kassir hesabları.",
                        "<b>Texniki məlumatlar:</b> giriş vaxtı, IP ünvanı və brauzer/cihaz məlumatı (sessiya təhlükəsizliyi üçün), xəta qeydləri."))
                + sec("3. Məqsədlər və hüquqi əsaslar", ul(
                        "Hesabın yaradılması və idarəsi, möhür, coin və hədiyyələrin hesablanması, kassada hədiyyənin təsdiqi — <b>xidmət müqaviləsinin icrası</b>.",
                        "NFC oxutmasının həqiqətən məkanda edildiyini yoxlamaq, saxtakarlığın qarşısını almaq, hesab təhlükəsizliyi — <b>qanuni maraq</b>.",
                        "Yer icazəsi, push bildirişləri — cihazınızda verdiyiniz <b>razılıq</b>; istənilən vaxt telefon ayarlarından geri götürə bilərsiniz.",
                        "Qanunvericiliyin tələb etdiyi qeydlərin saxlanması — <b>hüquqi öhdəlik</b>."))
                + sec("4. Kimlərlə paylaşırıq", ul(
                        "<b>Məkanlar:</b> sadiqlik kartınız olan məkan sizin adınızı, telefonunuzu, e-poçtunuzu, o məkandakı möhür, coin və hədiyyələrinizi görür.",
                        "<b>Hostinq provayderi:</b> serverlərimiz Azərbaycandan kənarda yerləşə bilər.",
                        "<b>Google Firebase (Firebase Cloud Messaging):</b> push bildirişlərinin çatdırılması üçün cihaz tokeni.",
                        "<b>Google və ya Apple:</b> yalnız onlarla daxil olmağı seçsəniz.",
                        "Qanunla tələb olunduqda səlahiyyətli dövlət orqanları.")
                        + p("Məlumatlarınızı satmırıq və reklam izləməsi üçün istifadə etmirik."))
                + sec("5. Sərhədlərarası ötürmə", p("Server və Firebase infrastrukturu Azərbaycandan kənarda ola bildiyi üçün məlumatlarınız xaricə ötürülə bilər. Ötürmə 998-IIIQ saylı Qanunun tələblərinə uyğun olaraq, adekvat qorunma təmin edən ölkələrə və ya razılığınız əsasında həyata keçirilir."))
                + sec("6. Saxlama müddəti", p("Məlumatlar hesabınız aktiv olduğu müddətdə saxlanılır. Hesabı sildikdə ad, e-poçt, telefon, doğum tarixi, profil şəkli və giriş identifikatorları dərhal silinir və ya anonimləşdirilir, bütün sessiyalar və push tokenləri silinir. Məkanların uçotu üçün möhür, coin və hədiyyə qeydləri sizə bağlanmayan anonim formada qalır."))
                + sec("7. Hüquqlarınız", ul(
                        "Məlumatlarınızın emal edilib-edilmədiyini bilmək və onlara çıxış əldə etmək;",
                        "Yanlış məlumatların düzəldilməsini tələb etmək (Profil &gt; Profili düzənlə);",
                        "Məlumatların silinməsini tələb etmək (Profil &gt; Hesabı sil və ya <a href=\"delete-account?lang=az\">bu səhifə</a>);",
                        "Razılığı geri götürmək, emala etiraz etmək, məhkəməyə və ya səlahiyyətli orqana müraciət etmək.")
                        + p("Müraciətlər üçün: " + c.email() + ". Müraciətlərə ən gec 30 gün ərzində cavab veririk."))
                + sec("8. Uşaqlar", p("Qazan 13 yaşdan kiçik uşaqlar üçün nəzərdə tutulmayıb və onlardan bilərəkdən məlumat toplamırıq."))
                + sec("9. Təhlükəsizlik və dəyişikliklər", p("Şifrələr həş formasında saxlanılır, giriş cəhdləri məhdudlaşdırılır, hər əməliyyat yalnız səlahiyyətli hesablar üçün açıqdır. Bu siyasət dəyişərsə, yeni versiya bu səhifədə dərc olunur."));
        return new Page("Məxfilik siyasəti", "Qüvvəyə minmə tarixi: " + c.date(), b);
    }

    private static Page privacyTr(Ctx c) {
        String b = sec("1. Veri sorumlusu", p("Qazan uygulaması ve hizmeti (\"Qazan\") kapsamında kişisel verileriniz, 6698 sayılı Kişisel Verilerin Korunması Kanunu (\"KVKK\") uyarınca veri sorumlusu sıfatıyla <b>"
                        + c.entity() + "</b> (adres: " + c.address() + ", e-posta: " + c.email() + ") tarafından işlenir."))
                + sec("2. Aydınlatma Metni (KVKK m. 10)", p("İşbu aydınlatma metni, KVKK'nın 10. maddesi ve Aydınlatma Yükümlülüğünün Yerine Getirilmesinde Uyulacak Usul ve Esaslar Hakkında Tebliğ kapsamında; hangi kişisel verilerinizin, hangi amaçla, hangi hukuki sebebe dayanılarak işlendiğini, kimlere ve hangi amaçla aktarılabileceğini, toplama yöntemini ve haklarınızı açıklamak için hazırlanmıştır.")
                        + "<table><tr><th>Veri kategorisi</th><th>Veriler</th><th>Amaç</th><th>Hukuki sebep (KVKK)</th></tr>"
                        + "<tr><td>Kimlik ve iletişim</td><td>Ad, soyad, e-posta, telefon, doğum tarihi, isteğe bağlı profil fotoğrafı</td><td>Üyelik, hesabın yönetimi, işletmenin sizi tanıması</td><td>m. 5/2-c sözleşmenin ifası</td></tr>"
                        + "<tr><td>Hesap güvenliği</td><td>Şifre (yalnızca şifrelenmiş özet), oturum kayıtları, IP adresi, cihaz/tarayıcı bilgisi</td><td>Hesap güvenliği, kötüye kullanımın önlenmesi</td><td>m. 5/2-f meşru menfaat; m. 5/2-ç hukuki yükümlülük</td></tr>"
                        + "<tr><td>Sadakat işlemleri</td><td>Kartlar, damgalar, coin hareketleri, alınan/kullanılan hediyeler, kasa pencereleri</td><td>Damga, puan ve hediyelerin hesaplanması ve kasada teslimi</td><td>m. 5/2-c sözleşmenin ifası</td></tr>"
                        + "<tr><td>NFC okutma kayıtları</td><td>Kart kodu, çip numarası (UID), cihaz kimliği, zaman, sonuç, işletmeye uzaklık</td><td>Okutmanın işletmede yapıldığının doğrulanması, sahteciliğin önlenmesi</td><td>m. 5/2-f meşru menfaat</td></tr>"
                        + "<tr><td>Konum</td><td>Kart okutulurken anlık konum (yalnızca uzaklık hesabı için; koordinat saklanmaz), uygulamada yakındaki yerler</td><td>Doğrulama ve yakındaki işletmelerin gösterilmesi</td><td>Cihaz izni ile açık rıza (m. 5/1); doğrulama için m. 5/2-c</td></tr>"
                        + "<tr><td>Bildirim</td><td>Cihaz bildirim anahtarı (token), uygulama içi bildirimler</td><td>İşlem ve kampanya bildirimleri</td><td>Bildirim izni ile açık rıza (m. 5/1)</td></tr>"
                        + "<tr><td>Kullanıcı içeriği</td><td>Yorum ve puanlar, şikayetler</td><td>Yorumların yayınlanması ve denetimi</td><td>m. 5/2-c, m. 5/2-f</td></tr>"
                        + "<tr><td>İşletme hesapları</td><td>İşletme adı, adres, iletişim, çalışma saatleri, yüklenen görseller, menü, kampanyalar, kasiyer hesapları</td><td>İşletmenin uygulamada listelenmesi</td><td>m. 5/2-c sözleşmenin ifası</td></tr>"
                        + "</table>"
                        + p("<b>Toplama yöntemi:</b> Verileriniz uygulamaya girdiğiniz bilgiler, kart okutma ve uygulama kullanımı sırasında elektronik ortamda, otomatik veya kısmen otomatik yollarla toplanır."))
                + sec("3. Verilerin aktarılması (KVKK m. 8 ve 9)", ul(
                        "<b>İşletmeler:</b> Sadakat kartınızın bulunduğu işletme; adınızı, telefonunuzu, e-postanızı ve o işletmedeki damga, coin ve hediyelerinizi görür (sözleşmenin ifası).",
                        "<b>Barındırma hizmeti sağlayıcısı:</b> Sunucularımız Türkiye dışında bulunabilir.",
                        "<b>Google Firebase (Firebase Cloud Messaging):</b> Bildirimlerin iletilmesi için cihaz anahtarı; sunucuları yurt dışındadır.",
                        "<b>Google veya Apple:</b> Yalnızca bu hesaplarla giriş yapmayı seçerseniz.",
                        "Kanunen yetkili kamu kurum ve kuruluşları.")
                        + p("Yurt dışına aktarım, KVKK'nın 9. maddesinde öngörülen şartlara (ör. Kurul'un ilan ettiği standart sözleşmeler) ve bu şartların bulunmadığı hallerde açık rızanıza dayanılarak yapılır. Verilerinizi satmayız ve reklam takibi için kullanmayız."))
                + sec("4. Saklama süresi", p("Verileriniz hesabınız açık olduğu sürece saklanır. Hesabınızı sildiğinizde ad, e-posta, telefon, doğum tarihi, profil fotoğrafı ve giriş kimlikleriniz derhal silinir veya anonim hale getirilir; tüm oturumlarınız ve bildirim anahtarlarınız silinir. İşletmelerin muhasebesi için damga, coin ve hediye kayıtları size bağlanamayacak şekilde anonim olarak kalır. Kanunen saklanması zorunlu kayıtlar ilgili süre boyunca tutulur."))
                + sec("5. Haklarınız (KVKK m. 11)", ul(
                        "Kişisel verilerinizin işlenip işlenmediğini öğrenme ve işlenmişse buna ilişkin bilgi talep etme,",
                        "İşlenme amacını ve amacına uygun kullanılıp kullanılmadığını öğrenme,",
                        "Yurt içinde veya yurt dışında aktarıldığı üçüncü kişileri bilme,",
                        "Eksik veya yanlış işlenmişse düzeltilmesini isteme (Profil &gt; Profili düzenle),",
                        "KVKK m. 7 şartları çerçevesinde silinmesini veya yok edilmesini isteme (Profil &gt; Hesabı sil ya da <a href=\"delete-account?lang=tr\">bu sayfa</a>),",
                        "Düzeltme ve silme işlemlerinin aktarıldığı üçüncü kişilere bildirilmesini isteme,",
                        "Münhasıran otomatik sistemlerle analiz sonucu aleyhinize bir sonuç çıkmasına itiraz etme,",
                        "Kanuna aykırı işleme nedeniyle zarara uğramanız halinde zararın giderilmesini talep etme."))
                + sec("6. Başvuru", p("Haklarınıza ilişkin taleplerinizi, Veri Sorumlusuna Başvuru Usul ve Esasları Hakkında Tebliğ'e uygun olarak, kimliğinizi teyit eden bilgilerle birlikte "
                        + c.email() + " adresine (sistemimizde kayıtlı e-posta adresinizden) veya yazılı olarak " + c.address()
                        + " adresine iletebilirsiniz. Talepler en geç 30 gün içinde ücretsiz sonuçlandırılır. Başvurunuzun reddedilmesi veya cevabı yetersiz bulmanız halinde Kişisel Verileri Koruma Kurulu'na şikayette bulunabilirsiniz."))
                + sec("7. Çocuklar", p("Qazan 13 yaşından küçükler için tasarlanmamıştır; bu yaştaki çocuklardan bilerek veri toplamayız."))
                + sec("8. Güvenlik ve değişiklikler", p("Şifreler özet (hash) olarak saklanır, giriş denemeleri sınırlandırılır, verilere yalnızca yetkili hesaplar erişir. Bu metin değişirse güncel sürüm bu sayfada yayımlanır."))
                + sec("Azerbaycan kullanıcıları", p("Azerbaycan'daki kullanıcılar için \"Fərdi məlumatlar haqqında\" 998-IIIQ sayılı Kanun kapsamındaki metin <a href=\"?lang=az\">Azerbaycan dilindedir</a>."));
        return new Page("Gizlilik politikası ve KVKK Aydınlatma Metni", "Yürürlük tarihi: " + c.date(), b);
    }

    private static Page privacyEn(Ctx c) {
        String b = sec("Summary", p("Controller: <b>" + c.entity() + "</b>, " + c.address() + ", " + c.email()
                        + ". This is a summary; the binding texts are the <a href=\"?lang=az\">Azerbaijani</a> (Law No. 998-IIIQ) and <a href=\"?lang=tr\">Turkish</a> (KVKK No. 6698) versions."))
                + sec("What we collect", ul("Account: name, surname, e-mail, phone, birth date, password hash, optional photo; Google/Apple identifier if you use social sign-in.",
                        "Loyalty activity: cards, stamps, coin movements, gifts bought and used.",
                        "NFC taps: card code, chip UID, device ID, time, result and distance to the venue (your location is sent at tap time to compute it; coordinates are not stored).",
                        "Push device token, in-app notifications, reviews and reports, business profile data and photos, security logs (IP, device)."))
                + sec("Why and with whom", p("To run the loyalty service (contract), prevent fraud and secure accounts (legitimate interest), and with your consent for location and notifications. Venues see your name, contact details and activity at that venue. Data is hosted by our hosting provider and Google Firebase (push), possibly outside Azerbaijan and Türkiye. We do not sell data or use it for ad tracking."))
                + sec("Retention and your rights", p("Deleting your account (Profile &gt; Delete account, or <a href=\"delete-account?lang=en\">this page</a>) removes or anonymises your personal data immediately; anonymised loyalty records remain for venues' bookkeeping. You may access, correct, delete and object; contact " + c.email() + " (answer within 30 days). Not intended for children under 13."));
        return new Page("Privacy policy", "Effective date: " + c.date(), b);
    }

    private static Page privacyRu(Ctx c) {
        String b = sec("Кратко", p("Оператор данных: <b>" + c.entity() + "</b>, " + c.address() + ", " + c.email()
                        + ". Это краткая версия; полные тексты — на <a href=\"?lang=az\">азербайджанском</a> (Закон № 998-IIIQ) и <a href=\"?lang=tr\">турецком</a> (KVKK № 6698)."))
                + sec("Что мы собираем", ul("Аккаунт: имя, фамилия, e-mail, телефон, дата рождения, хэш пароля, фото по желанию; идентификатор Google/Apple при входе через них.",
                        "Программы лояльности: карты, штампы, движение коинов, подарки.",
                        "NFC-считывания: код карты, UID чипа, ID устройства, время, результат и расстояние до заведения (координаты не сохраняются).",
                        "Токен push-уведомлений, отзывы и жалобы, данные профиля заведения и фото, журналы безопасности (IP, устройство)."))
                + sec("Цели, передача, права", p("Данные нужны для работы сервиса, защиты от мошенничества и безопасности аккаунта; геолокация и уведомления — с вашего согласия. Заведение видит ваше имя, контакты и активность у себя. Данные хранятся у хостинг-провайдера и в Google Firebase, возможно за пределами Азербайджана и Турции. Мы не продаём данные. Удаление аккаунта (Профиль &gt; Удалить аккаунт или <a href=\"delete-account?lang=ru\">эта страница</a>) сразу удаляет или обезличивает персональные данные. Запросы: " + c.email() + ". Сервис не предназначен для детей до 13 лет."));
        return new Page("Политика конфиденциальности", "Дата вступления в силу: " + c.date(), b);
    }

    // ═════════════════════════════ TERMS ═════════════════════════════

    static Page terms(String lang, Ctx c) {
        return switch (lang) {
            case "tr" -> new Page("Kullanım koşulları", "Yürürlük tarihi: " + c.date(),
                    sec("1. Taraflar ve kapsam", p("Bu koşullar, " + c.entity() + " tarafından sunulan Qazan uygulamasının müşteri, işletme ve kasiyer hesaplarıyla kullanımını düzenler. Uygulamayı kullanarak bu koşulları ve <a href=\"privacy?lang=tr\">Gizlilik politikasını</a> kabul etmiş olursunuz."))
                    + sec("2. Hesap", ul("Kayıt bilgilerinin doğru olması ve şifrenizin gizli tutulması sizin sorumluluğunuzdadır.",
                            "Her kişi yalnızca kendi adına hesap açabilir; hesaplar devredilemez.",
                            "13 yaşından küçükler hesap açamaz."))
                    + sec("3. Damga, coin ve hediyeler", ul("Damga ve coin'ler yalnızca kazanıldıkları işletmede geçerlidir; nakit değeri yoktur, paraya çevrilemez, satılamaz ve başkasına devredilemez.",
                            "Kampanya ve hediyeleri işletmeler belirler; hediyenin sağlanmasından ve kampanya koşullarından ilgili işletme sorumludur.",
                            "Hediyeler kasada, işletmenin harcama kartı okutularak veya kasiyer onayıyla kullanılır.",
                            "Kart kopyalama, uzaktan okutma veya sistemi kötüye kullanma tespit edilirse ilgili damga, coin ve hediyeler iptal edilebilir, hesap askıya alınabilir."))
                    + sec("4. Yorumlar ve kullanıcı içeriği", ul("Hakaret, nefret söylemi, taciz, yanıltıcı veya yasa dışı içerik paylaşmak yasaktır.",
                            "Her yorum uygulama içinden şikayet edilebilir; şikayetler en geç 24 saat içinde incelenir ve kurallara aykırı içerik kaldırılır.",
                            "Kuralları ihlal eden kullanıcıların yorum yazma hakkı kısıtlanabilir veya hesapları engellenebilir."))
                    + sec("5. İşletme hesapları", p("İşletmeler, girdikleri bilgilerin doğruluğundan, kampanya koşullarından, hediyelerin teslim edilmesinden ve kasiyer hesaplarının kullanımından sorumludur. Yeni işletmeler yönetici onayından sonra yayımlanır."))
                    + sec("6. Fesih", p("Hesabınızı dilediğiniz zaman uygulamadan silebilirsiniz (<a href=\"delete-account?lang=tr\">nasıl yapılır</a>). Koşulların ihlali halinde hesabınızı askıya alabilir veya kapatabiliriz."))
                    + sec("7. Sorumluluk ve değişiklikler", p("Hizmet \"olduğu gibi\" sunulur; kesintisiz olacağı garanti edilmez. Koşullar değişirse güncel sürüm bu sayfada yayımlanır. İletişim: " + c.email() + ".")));
            case "en" -> new Page("Terms of use", "Effective date: " + c.date(),
                    sec("Summary", ul("Using Qazan (provided by " + c.entity() + ") means you accept these terms and the <a href=\"privacy?lang=en\">privacy policy</a>. Full text: <a href=\"?lang=az\">AZ</a>, <a href=\"?lang=tr\">TR</a>.",
                            "Keep your account details accurate and your password secret; no accounts for children under 13.",
                            "Stamps and coins are valid only at the venue where earned, have no cash value and cannot be sold or transferred. Venues set and are responsible for their campaigns and rewards.",
                            "No abusive, hateful, misleading or illegal reviews. Every review can be reported in the app; reports are reviewed within 24 hours, offending content is removed and abusive users can be blocked.",
                            "You can delete your account at any time; we may suspend accounts that abuse the service. Contact: " + c.email() + ".")));
            case "ru" -> new Page("Условия использования", "Дата вступления в силу: " + c.date(),
                    sec("Кратко", ul("Используя Qazan (" + c.entity() + "), вы принимаете эти условия и <a href=\"privacy?lang=ru\">политику конфиденциальности</a>. Полный текст: <a href=\"?lang=az\">AZ</a>, <a href=\"?lang=tr\">TR</a>.",
                            "Штампы и коины действуют только в заведении, где получены, не имеют денежной стоимости и не передаются. За акции и подарки отвечает заведение.",
                            "Оскорбительные, ложные и незаконные отзывы запрещены. На любой отзыв можно пожаловаться в приложении; жалобы рассматриваются в течение 24 часов, нарушителей блокируют.",
                            "Аккаунт можно удалить в любой момент. Контакт: " + c.email() + ".")));
            default -> new Page("İstifadə şərtləri", "Qüvvəyə minmə tarixi: " + c.date(),
                    sec("1. Tərəflər və əhatə", p("Bu şərtlər " + c.entity() + " tərəfindən təqdim olunan Qazan tətbiqinin müştəri, biznes və kassir hesabları ilə istifadəsini tənzimləyir. Tətbiqdən istifadə etməklə bu şərtləri və <a href=\"privacy?lang=az\">Məxfilik siyasətini</a> qəbul edirsiniz."))
                    + sec("2. Hesab", ul("Qeydiyyat məlumatlarının doğruluğuna və şifrənizin gizliliyinə siz cavabdehsiniz.",
                            "Hər kəs yalnız öz adına hesab aça bilər; hesablar başqasına verilə bilməz.",
                            "13 yaşdan kiçiklər hesab aça bilməz."))
                    + sec("3. Möhür, coin və hədiyyələr", ul("Möhür və coinlər yalnız qazanıldığı məkanda keçərlidir; nağd dəyəri yoxdur, pula çevrilə, satıla və başqasına ötürülə bilməz.",
                            "Kampaniya və hədiyyələri məkanlar müəyyən edir; hədiyyənin verilməsinə və kampaniya şərtlərinə həmin məkan cavabdehdir.",
                            "Hədiyyələr kassada, məkanın xərcləmə kartı oxudularaq və ya kassir təsdiqi ilə istifadə olunur.",
                            "Kart kopyalama, uzaqdan oxutma və ya sistemdən sui-istifadə aşkarlanarsa, müvafiq möhür, coin və hədiyyələr ləğv edilə, hesab dayandırıla bilər."))
                    + sec("4. Rəylər və istifadəçi məzmunu", ul("Təhqir, nifrət nitqi, təqib, yanıldıcı və ya qanunsuz məzmun qadağandır.",
                            "Hər rəy tətbiqdən şikayət edilə bilər; şikayətlər ən gec 24 saat ərzində yoxlanılır və qaydalara zidd məzmun silinir.",
                            "Qaydaları pozan istifadəçilərin rəy yazmaq hüququ məhdudlaşdırıla və ya hesabı bloklana bilər."))
                    + sec("5. Biznes hesabları", p("Məkanlar daxil etdikləri məlumatların doğruluğuna, kampaniya şərtlərinə, hədiyyələrin verilməsinə və kassir hesablarının istifadəsinə cavabdehdir. Yeni məkanlar admin təsdiqindən sonra dərc olunur."))
                    + sec("6. Xitam", p("Hesabınızı istənilən vaxt tətbiqdən silə bilərsiniz (<a href=\"delete-account?lang=az\">necə edilir</a>). Şərtlər pozularsa hesabınızı dayandıra və ya bağlaya bilərik."))
                    + sec("7. Məsuliyyət və dəyişikliklər", p("Xidmət \"olduğu kimi\" təqdim olunur; fasiləsiz işləyəcəyinə zəmanət verilmir. Şərtlər dəyişərsə yeni versiya bu səhifədə dərc olunur. Əlaqə: " + c.email() + ".")));
        };
    }

    // ═════════════════════════════ DELETION ═════════════════════════════

    static Page deletion(String lang, Ctx c) {
        return switch (lang) {
            case "tr" -> new Page("Hesabını sil", "Qazan · " + c.entity(),
                    sec("Uygulamadan", "<ol><li>Qazan uygulamasını açın ve giriş yapın.</li><li>Müşteri hesabı: <b>Profil</b> sekmesi → <b>Hesabı sil</b>. İşletme hesabı: <b>Panel</b> → sağ üstteki profil simgesi (<b>Hesap</b>) → <b>Hesabı sil</b>.</li><li>Onaylayın. Hesabınız hemen silinir ve oturumunuz kapanır.</li></ol>")
                    + sec("E-posta ile", p("Uygulamaya erişemiyorsanız, hesabınıza kayıtlı e-posta adresinden " + c.email() + " adresine \"Hesap silme talebi\" konulu bir e-posta gönderin; kayıtlı telefon numaranızı da yazın. Talebiniz en geç 30 gün içinde sonuçlandırılır."))
                    + sec("Ne silinir, ne kalır", ul("<b>Silinir:</b> ad, soyad, e-posta, telefon, doğum tarihi, profil fotoğrafı, Google/Apple bağlantısı, tüm oturumlar ve bildirim anahtarları.",
                            "<b>Anonim olarak kalır:</b> işletmelerin muhasebesi için damga, coin ve hediye hareketleri; bu kayıtlar artık size bağlanamaz.",
                            "Kullanılmamış damga, coin ve hediyeleriniz silme ile birlikte geçersiz olur.")));
            case "en" -> new Page("Delete your account", "Qazan · " + c.entity(),
                    sec("In the app", "<ol><li>Open Qazan and sign in.</li><li>Customer account: <b>Profile</b> tab → <b>Delete account</b>. Business account: <b>Dashboard</b> → profile icon (<b>Account</b>) → <b>Delete account</b>.</li><li>Confirm. The account is deleted immediately and you are signed out.</li></ol>")
                    + sec("By e-mail", p("If you cannot use the app, e-mail " + c.email() + " from your registered address with the subject \"Account deletion request\" and your phone number. We complete requests within 30 days."))
                    + sec("What is deleted", ul("<b>Deleted:</b> name, e-mail, phone, birth date, photo, Google/Apple link, sessions and push tokens.",
                            "<b>Kept anonymised:</b> stamp, coin and gift movements needed for venues' bookkeeping, no longer linked to you.",
                            "Unused stamps, coins and gifts are forfeited.")));
            case "ru" -> new Page("Удаление аккаунта", "Qazan · " + c.entity(),
                    sec("В приложении", "<ol><li>Откройте Qazan и войдите.</li><li>Клиент: вкладка <b>Профиль</b> → <b>Удалить аккаунт</b>. Бизнес: <b>Панель</b> → значок профиля (<b>Аккаунт</b>) → <b>Удалить аккаунт</b>.</li><li>Подтвердите — аккаунт удаляется сразу.</li></ol>")
                    + sec("По e-mail", p("Если нет доступа к приложению, напишите на " + c.email() + " с зарегистрированного адреса, тема «Удаление аккаунта», укажите телефон. Срок — до 30 дней."))
                    + sec("Что удаляется", ul("Удаляются: имя, e-mail, телефон, дата рождения, фото, привязка Google/Apple, сессии и push-токены.",
                            "Остаются в обезличенном виде: операции со штампами, коинами и подарками для учёта заведений.",
                            "Неиспользованные штампы, коины и подарки аннулируются.")));
            default -> new Page("Hesabı sil", "Qazan · " + c.entity(),
                    sec("Tətbiqdən", "<ol><li>Qazan tətbiqini açın və daxil olun.</li><li>Müştəri hesabı: <b>Profil</b> bölməsi → <b>Hesabı sil</b>. Biznes hesabı: <b>Panel</b> → yuxarı sağdakı profil işarəsi (<b>Hesab</b>) → <b>Hesabı sil</b>.</li><li>Təsdiqləyin. Hesabınız dərhal silinir və sessiyanız bağlanır.</li></ol>")
                    + sec("E-poçtla", p("Tətbiqə daxil ola bilmirsinizsə, hesabınıza bağlı e-poçtdan " + c.email() + " ünvanına \"Hesabın silinməsi\" mövzulu məktub göndərin və qeydiyyatdakı telefon nömrənizi yazın. Müraciət ən gec 30 gün ərzində icra olunur."))
                    + sec("Nə silinir, nə qalır", ul("<b>Silinir:</b> ad, soyad, e-poçt, telefon, doğum tarixi, profil şəkli, Google/Apple bağlantısı, bütün sessiyalar və bildiriş tokenləri.",
                            "<b>Anonim qalır:</b> məkanların uçotu üçün möhür, coin və hədiyyə hərəkətləri; bu qeydlər artıq sizə bağlanmır.",
                            "İstifadə olunmamış möhür, coin və hədiyyələr silinmə ilə birlikdə etibarsız olur.")));
        };
    }
}
