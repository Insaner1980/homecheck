# homecheckin Android-tarkistusskriptien toteutussuunnitelma

## Yhteenveto

homecheckiin lisätään 16 projektikohtaista tarkistusskriptiä yksi kerrallaan. Jokaisen valmistuttua työ pysäytetään, käyttäjälle annetaan kyseisen skriptin testikomento ja seuraava skripti aloitetaan vasta testituloksen jälkeen.

Kanoninen yhteinen tarkistusmoottori on `C:\Dev\Android-check`. Kansio `C:\Dev\android-project-maintenance` sisältää auditointi- ja worktree-aineistoa, eikä sinne tehdä homecheck-kohtaisia muutoksia.

Sonar jätetään pois, koska homecheckille ei ole määritetty SonarCloud- tai SonarQube-projektia. KnitToolsin `rs`- ja `rst`-skriptit eivät kuulu homecheckiin.

## Yhteinen rakenne

Ensimmäisen skriptin yhteydessä luodaan tarvittava projektikohtainen perusta:

- `tools/Invoke-homecheckProjectCheck.ps1`
- PowerShell-funktio `Resolve-homecheckProjectCheck`
- `config/android-check.json`
- `config/check-exceptions.json`
- `config/semgrep/homecheck-security.yml`
- tarvittavat raportti- ja työkalupoikkeukset `.gitignoreen`

Resolveri etsii yhteisen moottorin tässä järjestyksessä:

1. Ympäristömuuttujan `ANDROID_CHECK_ROOT` osoittama kansio.
2. `C:\Dev\Android-check`.
3. homecheckin sisarhakemisto `Android-check`.

Jos `InvokeProjectCheck.ps1` ei löydy tai löydetty moottori on virheellinen, wrapper palauttaa teknisen virheen eikä vihreää ohitusta.

Jokainen `tools/<komento>.ps1`:

- määrittää vain oman yhteisen komentonsa;
- käyttää `Resolve-homecheckProjectCheck`-funktiota;
- välittää kaikki argumentit muuttamatta;
- palauttaa yhteisen moottorin poistumiskoodin;
- toimii myös suoraan ilman PowerShell-profiilin aliaksia.

## homecheckin manifesti

`config/android-check.json` tehdään väliaikaisesti yhteensopivaksi Android-checkin molempien nykyisten manifestilukijoiden kanssa. Yhteistä moottoria ei muuteta.

Manifestin identiteetti ja rajaus:

- `projectId`: `homecheck`
- Android-namespace: `com.finnvek.homecheck`
- application ID: `com.finnvek.homecheck`
- moduuli: `:app`
- suhteellinen moduulipolku: `app`
- moduulityyppi: `android-application`
- variantit: `debug`, `release`
- olemassa olevat lähdesetit: `main`, `debug`, `test`, `androidTest`
- included buildeja ei ole
- build-tehtävä: `:app:assembleDebug`
- testitehtävä: `:app:testDebugUnitTest`
- tavallinen lint-tehtävä: `:app:lintDebug`
- full-lint-tehtävät: `:app:lintDebug`, `:app:lintRelease`
- riippuvuuskonfiguraatiot: `debugRuntimeClasspath`, `releaseRuntimeClasspath`
- OWASP-tehtävä: `:app:dependencyCheckAnalyze`

Manifestissa ovat rinnakkain:

- legacy-kentät, kuten `path`, `relativePath`, `kind`, `buildTasks`, `testTasks` ja `checks`;
- uuden lukijan kentät, kuten `id`, `gradlePath`, `type`, `tasks`, `phases`, `tools`, `reports` ja `exceptions`.

Build ja unit-testit ovat pakollisia vaiheita. Lint-, ktlint-, Detekt- ja Compose stability -vaiheet merkitään soveltuviksi sitä mukaa, kun niiden Gradle-tehtävät lisätään.

`config/check-exceptions.json` aloitetaan tyhjänä. Baselineja, suppressioita tai muita poikkeuksia ei lisätä ilman todettua ja erikseen arvioitua löydöstä.

## Toteutusjärjestys

### 1. `bc` / build-check

Luodaan:

- yhteinen homecheck-resolveri;
- kaksiyhteensopiva manifesti;
- tyhjä poikkeusrekisteri;
- Semgrep-konfiguraation perusta;
- `tools/bc.ps1`.

Validointi:

- `Resolve-homecheckProjectCheck` löytää `C:\Dev\Android-check`;
- `bc -ResolveOnly` palauttaa `C:\Dev\homecheck`;
- `bc -PlanOnly` näyttää projektin `homecheck`, moduulin `:app` ja tehtävän `:app:assembleDebug`;
- molemmat manifestilukijat hyväksyvät manifestin.

Käyttäjän testi:

```powershell
bc
```

### 2. `tc` / test-check

Lisätään vain `tools/tc.ps1`.

`tc` ajaa manifestissa määritetyn `:app:testDebugUnitTest`-tehtävän. Instrumentointitestejä tai laiteasennuksia ei käynnistetä automaattisesti.

Käyttäjän testi:

```powershell
tc
```

### 3. `ss` / secret-scan

Lisätään `tools/ss.ps1`.

Tarkistus kattaa:

- working treen;
- Git-ignoreen osuvat ja generoidut tiedostot;
- arkistot ja enkoodatun sisällön;
- kaikki tavoitettavat Git-refit;
- Gitleaksin, TruffleHogin ja Semgrep secrets -säännöt.

Raakalöydöksiä tai varsinaisia salaisuuksia ei kirjoiteta raportteihin.

Käyttäjän testi:

```powershell
ss
```

Ennen testiä kerrotaan, että ajo voi kestää pitkään, koska se tarkistaa myös Git-historian.

### 4. `lc` / lint-check

Lisätään:

- `tools/lc.ps1`;
- ktlint Gradle -plugin;
- Detekt Gradle -plugin;
- minimaalinen Detekt-konfiguraatio;
- koneellisesti luettavat ktlint- ja Detekt-raportit.

Manifestiin kytketään:

- `:app:ktlintCheck`
- `:app:detekt`
- `:app:lintDebug`
- `:app:lintRelease`

Baselinea ei luoda automaattisesti.

Käyttäjän testit:

```powershell
lc
lc -Full
```

### 5. `cr` / compose-rules

Lisätään `tools/cr.ps1` ja mrmans0n Compose-säännöt olemassa oleviin ktlint- ja Detekt-polkuhin.

Sääntöjä ei toteuteta kolmantena erillisenä analyysipolkuna. `cr` käyttää samoja lähteitä ja konfiguraatioita kuin `lc`.

Käyttäjän testi:

```powershell
cr
```

### 6. `cs` / compose-stability

Lisätään:

- `tools/cs.ps1`;
- Skydoves Compose Stability Analyzer;
- manifestiin `:app:stabilityCheck`.

`stabilityDump`-tehtävää ei käynnistetä.

Käyttäjän testi:

```powershell
cs
```

### 7. `ga` / google-android-security

Lisätään:

- `tools/ga.ps1`;
- Google Android Security Lints sovellusmoduulin `lintChecks`-riippuvuudeksi.

Tarkistus käyttää Android Lintiä ja tuottaa erillisen security-lint-raportin.

Käyttäjän testi:

```powershell
ga
```

### 8. `ms` / mobsf-scan

Lisätään `tools/ms.ps1`.

MobSF-poikkeuksia ei lisätä etukäteen. Poikkeus voidaan myöhemmin lisätä vain tietylle säännölle ja olemassa olevalle täsmälliselle tiedostopolulle.

Käyttäjän testi:

```powershell
ms
```

### 9. `ac` / android-check

Lisätään `tools/ac.ps1`.

homecheckin Semgrep-konfiguraatio kattaa vähintään:

- Android-varmuuskopioinnin;
- cleartext-liikenteen;
- exported-komponentit;
- FileProvider-polkujen laajuuden;
- kovakoodatut allekirjoitus- ja salaisuustiedot;
- heikon kryptografian;
- WebViewin vaaralliset asetukset;
- arkaluonteisten URI-, tiedosto-, backup- ja tietokantatietojen lokituksen.

`ac` yhdistää Semgrepin ja mobsfscanin. DeepSec ei sisälly komentoon.

Käyttäjän testi:

```powershell
ac
```

### 10. `os` / osv-scan

Lisätään `tools/os.ps1`.

Wrapper johtaa tarkistettavan juuren omasta `$PSScriptRoot`-sijainnistaan ja välittää `C:\Dev\homecheck`-juuren eksplisiittisesti yhteiselle moottorille. Näin tarkistuskohde ei riipu kutsujan nykyhakemistosta.

OSV-poikkeuksia ei lisätä ilman todettua haavoittuvuutta ja määräaikaista perustelua.

Käyttäjän testi:

```powershell
os
```

### 11. `dc` / dependency-check

Lisätään:

- `tools/dc.ps1`;
- OWASP Dependency-Check Gradle -plugin;
- `:app:dependencyCheckAnalyze`;
- Gradlen `gradle/verification-metadata.xml`.

Tarkistus kattaa:

- Gradle dependency verificationin;
- OSV-Scannerin;
- debug- ja release-runtime-riippuvuudet;
- OWASP Dependency-Checkin.

Tyhjää suppressiotiedostoa ei luoda. Mahdolliset suppressiot lisätään vasta todetulle löydökselle tarkalla package URL -rajauksella, perustelulla ja päättymispäivällä.

Käyttäjän testi:

```powershell
dc
```

### 12. `pc` / pmd-check

Lisätään `tools/pc.ps1`.

PMD CPD tarkistaa Kotlin- ja Java-lähteiden duplikaatit. Oletusraja on 100 tokenia, ellei yhteisen moottorin nykyinen lukittu oletus ole tiukempi.

Käyttäjän testi:

```powershell
pc
```

### 13. `ql` / codeql-check

Lisätään:

- `tools/ql.ps1`;
- `.github/workflows/codeql.yml`.

Workflow:

- analysoi Java-, Kotlin- ja Gradle-lähteet;
- käyttää yhteisen `github-actions.lock.json`-tiedoston hyväksyttyjä 40-merkkisiä commit-SHA-kiinnityksiä;
- ei käytä kelluvia action-tageja.

Paikallinen validointi kattaa YAML-rakenteen, action-kiinnitykset ja `ql -PlanOnly` -tuloksen. Todellinen GitHub-workflow voidaan todentaa vasta commitin ja pushin jälkeen.

Käyttäjän testi:

```powershell
ql
```

### 14. `db` / dependabot-check

Lisätään:

- `tools/db.ps1`;
- `.github/dependabot.yml`.

Dependabot-konfiguraatio sisältää viikoittaiset tarkistukset:

- Gradle-ekosysteemille hakemistossa `/`;
- GitHub Actions -ekosysteemille hakemistossa `/`.

homecheckin GitHub-repossa Dependabot-alertit ovat tällä hetkellä pois käytöstä. Ulkoista asetusta ei muuteta ilman erillistä pyyntöä. Siihen asti oikea tulos on `DEPENDABOT_ALERTS_DISABLED/NOT_APPLICABLE`, ei virheellinen `CLEAN`.

Käyttäjän testi:

```powershell
db
```

### 15. `ds` / deep-sec

Lisätään:

- `tools/ds.ps1`;
- homecheck-kohtainen `.deepsec`-konfiguraatio;
- DeepSec-projektitunnus `homecheck`;
- lukittu DeepSec-riippuvuus ja tarvittavat paikalliset testit.

Custom-ajoon valitaan homecheckiin soveltuvat matcherit:

- exported Android -komponentit;
- liian laajat FileProvider-polut;
- URI-jako ilman asianmukaisia käyttöoikeuksia;
- backup- ja restore-tiedostojen käsittely;
- tietokantatiedostojen käsittely;
- arkaluonteinen Android-lokitus.

Automaattisesti ajetaan vain:

```powershell
ds -PlanOnly
```

Varsinainen lähdekoodia ulkoiselle AI-palvelulle lähettävä DeepSec-ajo tehdään vain käyttäjän erillisellä tietoisella hyväksynnällä.

### 16. `sc` / security-check

Lisätään viimeisenä `tools/sc.ps1`.

Oletusajo yhdistää:

- dependency-tarkistukset;
- working tree -rajatun salaisuustarkistuksen;
- kevyen Kotlin/Android Semgrep -tarkistuksen.

Valinnat:

- `-History` lisää Git-historian salaisuustarkistuksen;
- `-Full` lisää `ac`-tarkistuksen;
- DeepSec ei kuulu kumpaankaan muotoon.

Käyttäjän testit:

```powershell
sc
sc -Full
```

## Versioiden valinta

Jokaisen Gradle-pluginin tai analyysiriippuvuuden lisäämisen yhteydessä tarkistetaan sen virallinen julkaisu- ja yhteensopivuustieto.

Valintasääntö:

1. Uusin vakaa versio, joka tukee homecheckin nykyistä AGP 9.3.1-, Kotlin 2.2.10-, Gradle 9.7.0- ja Java 17 -kokoonpanoa.
2. Sovelluksen Kotlin-, AGP- tai Gradle-versioita ei muuteta vain tarkistustyökalun vuoksi.
3. Alpha-, beta- tai RC-versiota ei käytetä ilman erillistä hyväksyntää.
4. Tavallinen tarkistusajo ei asenna tai päivitä yhteisiä CLI-työkaluja.

## Jokaisen skriptin hyväksyntäportti

Kun yksi skripti on toteutettu:

- PowerShell-syntaksi tarkistetaan;
- suora projektikohtainen wrapperi tarkistetaan;
- `-ResolveOnly` ja `-PlanOnly` ajetaan;
- Gradle-muutoksissa tarkistetaan uuden tehtävän olemassaolo;
- poistumiskoodi ja raportin syntyminen tarkistetaan;
- `git diff --check` ajetaan;
- varmistetaan, etteivät käyttäjän aiemmat muutokset ole kadonneet tai muuttuneet vahingossa;
- käyttäjälle annetaan vain kyseisen skriptin testikomento;
- seuraavaa skriptiä ei aloiteta ennen käyttäjän testitulosta.

Kun `sc` on hyväksytty, suoritetaan lopuksi:

- kaikkien 16 wrapperin `-PlanOnly`-matriisi;
- `bc`;
- `tc`;
- kaikkien raporttien ja tunnisteiden tekstihaku sanalla `homecheck`;
- tarkistus, että projektin nimi on kaikkialla täsmälleen `homecheck` ja resolverien nimet ovat `Invoke-homecheckProjectCheck` sekä `Resolve-homecheckProjectCheck`.

## Rajaukset

- Sovelluksen nimi kirjoitetaan kaikkialla `homecheck`.
- Projektitunnus on `homecheck`.
- Projektijuuri on `C:\Dev\homecheck`.
- Projektikohtainen resolveri on `tools/Invoke-homecheckProjectCheck.ps1`.
- Resolverifunktio on `Resolve-homecheckProjectCheck`.
- Nykyinen likainen työpuu ja kaikki käyttäjän muutokset säilytetään.
- PowerShell-profiilia ei muuteta.
- `C:\Dev\Android-check`-repoon ei tehdä muutoksia.
- `C:\Dev\android-project-maintenance`-kansioon ei tehdä muutoksia.
- Committeja tai pushia ei tehdä ilman erillistä pyyntöä.
- Pull requestia ei luoda.
- Sonar ei kuulu tähän toteutukseen.
</proposed_plan>
