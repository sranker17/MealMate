# ShoppingListManager UI Portolási Útmutató

Ez a dokumentum tartalmazza a ShoppingListManager alkalmazásból átemelendő UI elemeket és stílusokat, hogy az új projekt vizuálisan konzisztens legyen.

## 1. Színpaletta (Color.kt)
Az alkalmazás sötét alapú, minimalista stílust használ, több választható akcentus színnel.

```kotlin
// Alap sötét háttér
val MinimalistBackground = Color(0xFF151515)

// Téma akcentusok (Primary színek)
val OceanTeal = Color(0xFF14B8A6)
val ForestGreen = Color(0xFF4ADE80)
val SunsetPink = Color(0xFFD34273)
val SnowSlate = Color(0xFF94A3B8)

// Világos témák (Opcionális)
val SkyPrimary = Color(0xFF7DD3FC)
val SkyBackground = Color(0xFFF8FAFC)
val RosePrimary = Color(0xFFFDA4AF)
val RoseBackground = Color(0xFFFFF1F2)
val SandPrimary = Color(0xFFD4D4D8)
val SandBackground = Color(0xFFFAFAFA)
```

## 2. Témarendszer (AppTheme.kt)
A téma kezelése `MaterialTheme` alapú, sötét (`darkColorScheme`) és világos (`lightColorScheme`) sémákkal.

### Koncepció:
- Minden sötét téma a `MinimalistBackground`-ot használja háttérnek és felületnek (`surface`).
- Az `onBackground` és `onSurface` színek az akcentus szín halványabb verziói.
- Dinamikus betűméret szorzó (`multiplier`) támogatása a kis/közepes/nagy szövegmérethez.

## 3. Tipográfia (Type.kt)
Az alkalmazás a **Google Fonts: Outfit** betűtípust használja.

```kotlin
val fontName = GoogleFont("Outfit")
val outfitFontFamily = FontFamily(
    Font(googleFont = fontName, fontProvider = provider),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Bold)
)
```
A `getTypographyWithMultiplier(multiplier: Float)` függvény segítségével minden betűméret (display, headline, title, body, label) dinamikusan skálázódik.

## 4. Központi UI Komponensek

### CustomHeader
Egy `Box` alapú fejléc, ami középre igazított címet, bal oldali vissza gombot és jobb oldali opcionális akciógombot tartalmaz.
- Stílus: `MaterialTheme.typography.displaySmall`
- Padding: `horizontal = 16.dp, vertical = 24.dp`

### EmptyState
Ikon és szöveg megjelenítése üres listák esetén.
- Ikon méret: `80.dp`
- Ikon áttetszőség: `alpha = 0.2f`
- Szöveg stílus: `titleMedium`, `alpha = 0.5f`

### Shimmer (Skeleton) Loading
Lineáris gradiens animáció (`rememberInfiniteTransition`) a betöltési állapotokhoz.
- `ShimmerItem`: 72.dp magas, lekerekített sarkú (12.dp) téglalap.
- `LoadingShimmerList`: 5 db ShimmerItem egymás alatt, 8.dp közzel.

## 5. Elrendezés és Navigáció (AppScaffold)
- **BottomBar:** `NavigationBar` transzparens háttérrel (`Color.Transparent`).
- **Indikátor:** A kijelölt elem indikátora szintén transzparens, csak az ikon színe változik `primary`-ra.
- **Vizuális elkülönítés:** Nincsenek erős elválasztó vonalak, a térközök és a színek definiálják a struktúrát.

## 6. Projekt beállítások (Hint)
- **Google Fonts Cert:** Szükséges a `res/values/preloaded_fonts.xml` és a hozzá tartozó tanúsítványok beállítása a `res/values/arrays.xml`-ben.
- **Dependencies:** Compose Material3, Navigation Compose, Google Fonts.
