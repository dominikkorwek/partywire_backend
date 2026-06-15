# `partywire_backend`

Backend Spring Boot jest gotowy do deployu z Neonem.

## Wymagane zmienne

Skopiuj `.env.example` albo ustaw te same wartości w hostingu:

- `PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `ALLOWED_ORIGINS`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`
- `SPRING_SQL_INIT_MODE`

## Neon

Używaj JDBC URL w formacie:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>/<db>?sslmode=require&channelBinding=require
```

## Render

Repo zawiera `Dockerfile` i `render.yaml`, więc backend można podpiąć bez dodatkowej konfiguracji buildu.

Po deployu ustaw:

- `ALLOWED_ORIGINS=https://<twoj-frontend>.vercel.app`

Jeśli chcesz zachować lokalny frontend i produkcyjny frontend jednocześnie:

```env
ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000,https://<twoj-frontend>.vercel.app
```
