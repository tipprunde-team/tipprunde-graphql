# tipprunde-graphql

GraphQL-Service der Fußball-Tippseite. Er bietet denselben Funktionsumfang wie die REST-API des Core-Service
(`tipprunde-rest`), ruft den Core nicht blockierend auf (Spring WebClient) und ist der zweite Zugang für die SPA.
Der Service hat keine eigene Datenbank.

## Schema

Der Vertrag zwischen GraphQL-Service und SPA steht in `src/main/resources/graphql/schema.graphqls`. Änderungen daran zuerst mit Niklas
(SPA) abstimmen.

## Starten (Windows)

Voraussetzung: JDK 17 oder neuer. Der Core-Service (`tipprunde-rest`) läuft lokal auf Port 8080.

```powershell
cd <Pfad zum Repo>\tipprunde-graphql
.\gradlew.bat bootRun
```

Der Service läuft dann auf Port **8081**. Prüfen im Browser:

- http://localhost:8081/actuator/health liefert `{"status":"UP"}`
- http://localhost:8081/graphiql öffnet GraphiQL zum Ausprobieren, der Header `Authorization: Bearer <token>` lässt sich dort unten eintragen

Beispiel (Token aus `POST /api/auth` im Core):

```graphql
{ me { id username isSystemAdmin } }
```

## Umgebungsvariablen

| Variable | Bedeutung | Standard |
|---|---|---|
| `CORE_BASE_URL` | Adresse des Core-Service (auf Heroku die Live-URL, ohne `/api`) | `http://localhost:8080` |
| `CORS_ALLOWED_ORIGINS` | erlaubte Origins der SPA, kommagetrennt, ohne Pfad und ohne Schrägstrich am Ende | `http://localhost:4200,http://127.0.0.1:4200` |
| `CORE_TIMEOUT_SECONDS` | Zeitlimit für Antworten des Core (Eco-Dynos brauchen nach dem Schlafen einige Sekunden) | `20` |
| `GRAPHIQL_ENABLED` | GraphiQL-Oberfläche unter `/graphiql` | `true` |
| `LOG_LEVEL` | Log-Level der Anwendung | `INFO` |
| `PORT` | Port, auf Heroku vorgegeben | `8081` |

Es gibt keine Zugangsdaten. Das `JWT_SECRET` liegt nur im Core: Der GraphQL-Service reicht den Header `Authorization` unverändert weiter
und prüft das Token nie selbst.

## Abfragen und Mutationen

| Typ | Name | Core-Aufruf |
|---|---|---|
| Query | `me` | `GET /api/users/me` |
| Query | `myGroups` | `GET /api/groups` |
| Query | `group(id)` | `GET /api/groups/{id}` |
| Query | `invitePreview(code)` | `GET /api/invites/{code}` (ohne Token) |
| Mutation | `register`, `login` | `POST /api/users`, `POST /api/auth` (ohne Token) |
| Mutation | `createGroup`, `joinGroup` | `POST /api/groups`, `POST /api/groups/join` |

Alle Regeln (Validierung, Tippsperre, Mitgliedschaft) bleiben im Core. Der GraphQL-Service reicht nur weiter und gibt die Fehler des Core zurück.
IDs müssen Zahlen sein und werden geprüft, bevor sie in einen Pfad eingesetzt werden. Beispiele stehen in
`docs/rest-client/graphql.http`.

## Fehler

Fehler des Core kommen als GraphQL-Fehler mit demselben Code zurück (`extensions.code`, zum Beispiel `UNAUTHORIZED`, `NOT_A_MEMBER`, `TIP_LOCKED`).
Ist der Core nicht erreichbar, lautet der Code `CORE_UNAVAILABLE`. Bei einer Antwort des Core ohne das bekannte Fehlerformat kommt `CORE_ERROR`.
Konflikte des Core (z. B. `EMAIL_TAKEN`, `ALREADY_MEMBER`) kommen als `BAD_REQUEST`, 401 als `UNAUTHORIZED`, 403 als `FORBIDDEN`, 404 als `NOT_FOUND`.
Abgelehnte Zugriffe (401, 403) stehen mit Feldname und Code im Log, nie mit Token, Passwort oder Argumenten.

## Tests

```powershell
.\gradlew.bat test
```

Die Tests starten den Service gegen einen kleinen Platzhalter-Core (JDK-HttpServer). Sie brauchen weder den Core noch eine Datenbank
und laufen deshalb auch in der CI.

## Deployment (Heroku)

`Procfile`, `system.properties` und `build.gradle` (feste JAR `tipprunde-graphql.jar`) sind vorbereitet. Wie beim Core: App anlegen, GitHub verbinden,
Config Vars `CORE_BASE_URL` und `CORS_ALLOWED_ORIGINS` setzen, Deploy, danach `/actuator/health` prüfen. `gradlew` muss in Git ausführbar sein
(`git update-index --chmod=+x gradlew`).
