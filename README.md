# ExAntiBotFilter

Cookie tanlash captcha asosidagi anti-bot plugin (Spigot / Paper / Purpur).

## Xususiyatlari

- Java **8 dan 25** gacha JVM versiyalarida ishlaydi (plugin Java 8 bytecode bilan kompilyatsiya qilinadi).
- Server APIsi: Spigot/Paper **1.13+** (`api-version: 1.13`).
- Player kirgan zahoti **54 slotli menyu** ochiladi: "Cookie tanlang (1/2)".
- Tasodifiy **5 ta cookie** (sozlanadigan), qolgan slotlar tasodifiy itemlar bilan to'ldiriladi.
- Foydalanuvchi har bir cookie ustiga **o'ng tugma** bilan bosishi kerak.
- Hammasini bossa: chatda `siz bot emasligingiz tasdiqlandi` xabari chiqadi.
- Test tugagunga qadar:
  - chatda hech narsa yoza olmaydi (silent),
  - faqat `/server`, `/lobby`, `/hub` (sozlanadigan) buyruqlariga ruxsat,
  - yura olmaydi, urolmaydi, item ololmaydi.
- Menyuni yopsa avtomatik qayta ochiladi.
- Noto'g'ri item bossa **(2/2) sahifa** ochiladi.
- 2-marta xato qilsa **kick** qilinadi.
- Tanlashga **60 soniya** vaqt (sozlanadigan) — vaqt tugasa kick.
- **GeyserMC + Floodgate** orqali kirgan **Bedrock** playerlar uchun maxsus **Floodgate Form** ishlatiladi (Java menyusi Bedrock'da ko'rinmasligi muammosi tuzatildi).

## Yuklab olish

Eng so'nggi build: [GitHub Releases](../../releases/tag/latest) sahifasidagi `ExAntiBotFilter-1.0.0.jar` faylini yuklab oling.

Yoki har bir push uchun GitHub Actions sahifasidan artifact sifatida ham yuklab olishingiz mumkin.

## O'rnatish

1. JAR faylni serverning `plugins/` papkasiga tashlang.
2. (Ixtiyoriy) Bedrock playerlar uchun `floodgate` plugini ham `plugins/` papkasida bo'lsin.
3. Serverni qayta ishga tushiring.
4. Sozlamalar `plugins/ExAntiBotFilter/config.yml` fayli orqali boshqariladi.

## Buyruqlar

- `/eabf reload` — konfiguratsiyani qayta yuklash (`exantibotfilter.admin` huquqi).

## Permissionlar

- `exantibotfilter.bypass` — captchani o'tkazib yuborish (default: op).
- `exantibotfilter.admin` — admin buyruqlari (default: op).
