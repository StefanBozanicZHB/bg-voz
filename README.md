# BG:VOZ — Batajnica ↔ Ovča

Android aplikacija koja pokazuje kada je voz na izabranoj stanici, sa težištem na
trenutnom vremenu. Pokriva samo relaciju **Batajnica – Ovča** u oba smera.

## Odakle podaci

Red vožnje se **pri svakom pokretanju preuzima i parsira** sa zvaničnog izvora:

    https://www.srbvoz.rs/wp-content/redvoznje/rv_bg_voza_za_2025.pdf

PDF nema nikakav mašinski format, pa se tabela rekonstruiše iz pozicija teksta
(`data/pdf/PdfTimetableParser.kt`):

* red sa ≥ 8 četvorocifrenih brojeva = zaglavlje tabele, brojevi vozova su kolone;
* svaki sledeći red je jedna stanica, a vreme se dodeljuje koloni po x koordinati;
* prazna ćelija = voz tu ne staje ili ne ide do te stanice;
* `│` u svim kolonama = voz prolazi bez zaustavljanja (trenutno Novi Beograd).

Vozovi koji **ne saobraćaju vikendom i praznikom** u PDF-u su označeni isključivo
zelenom bojom kolone, bez ikakvog teksta. Zato se strana i renderuje, pa se uzima
najčešća boja pozadine svake kolone (`data/pdf/AndroidShadingProbe.kt`). Period
važenja i spisak državnih praznika se čitaju iz legende ispod tabele.

Redosled izvora: svež PDF → poslednji uspešno preuzet PDF iz keša → ugrađena kopija
(`data/BundledTimetable.kt`). Traka na dnu ekrana uvek piše odakle su prikazani podaci.

## Šta ekran pokazuje

* smer, stanicu (padajuća lista + prečice) i dan (danas / radni dan / vikend-praznik);
* prvi voz posle trenutnog vremena — uokviren, sa odbrojavanjem; lista se sama skroluje
  na njega, prošli polasci su izbledeli;
* crvenu oznaku na vozovima koji **ne idu do krajnje stanice** (okreću se u Zemunu);
* dolaske umesto polazaka kada je izabrana krajnja stanica;
* celu trasu voza sa vremenima na klik.

## Razvoj

```bash
./gradlew :app:testDebugUnitTest   # parsira PDF iz src/test/resources i poredi sa sadržajem
./gradlew :app:assembleDebug
```

Testovi parsiraju pravi PDF preko desktop Apache PDFBox-a (`app/src/test/.../DesktopPdf.kt`),
dok aplikacija koristi PdfBox-Android port — parser je zato čist Kotlin, bez zavisnosti
od PDF biblioteke.

Kada izađe novi red vožnje, zameni PDF u `app/src/test/resources/` i pokreni:

```bash
./gradlew :app:regenerateBundledTimetable
```

Za rad je potreban Android SDK naveden u `local.properties` (nije u repou).
