# Reči u pokretu

Android aplikacija za učenje vokabulara kroz slike. Korisnik prvo bira tematski folder, zatim srpski, engleski ili ruski, dodiruje predmet na slici i vidi odgovarajuću reč. Kartice i folderi se čuvaju lokalno na uređaju.

## Pokretanje

1. Otvori ovaj direktorijum kao projekat u Android Studiju.
2. Sačekaj da Android Studio preuzme Gradle 8.9 i Android SDK 35 i završi sinhronizaciju.
3. Pokreni modul `app` na emulatoru ili telefonu sa Androidom 9 (API 28) ili novijim.

Na Windowsu možeš da napraviš APK komandom `gradlew.bat assembleDebug`. APK će biti u `app/build/outputs/apk/debug/`.

Gotov APK za instalaciju nalazi se u odeljku **Releases** ovog repozitorijuma.

## Korišćenje

- Na početnoj strani izaberi **Priroda**, **Škola**, **Kuhinja**, **Dnevna soba** ili **Ostalo**. Možeš i da dodaš novi folder.
- Svaki početni folder ima pet realističnih scena. Otvori scenu i dodirni jedan od tri označena predmeta; pojaviće se reč na izabranom jeziku. Scene imaju blago animirano pomeranje kamere. To nisu video snimci niti GIF animacije pojedinačnih predmeta.
- U folderu pritisni **Dodaj sliku u ovaj folder**, izaberi GIF ili animirani WebP i upiši reč na sva tri jezika. Pri izmeni kartice možeš da je premestiš u drugi folder.
- Izaberi jezik za učenje i dodirni karticu da otkriješ prevod.
- U otvorenoj kartici možeš da promeniš jezik, izmeniš reči ili obrišeš karticu.
- Zadrži prst na folderu da ga preimenuješ ili obrišeš. Početni folderi sa ugrađenim scenama ne mogu da se obrišu; dodatni folder sa karticama mora prvo da se isprazni.

Kartice napravljene u verziji 1.0 ostaju sačuvane i pojavljuju se u folderu **Ostalo**.

Slike se ne kopiraju u aplikaciju: čuva se trajna dozvola za izabranu datoteku. Ako obrišeš ili premestiš izvornu datoteku tako da njen URI više nije dostupan, slika neće moći da se prikaže. Podaci su lokalni na uređaju i trenutno se ne sinhronizuju između telefona.
