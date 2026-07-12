# AISME Frontend

React, TypeScript, and Vite UI for the AISME backend API.

Run from this directory:

```bash
npm ci
npm run dev
```

Build clean production assets:

```bash
npm run clean
npm run build
```

Or run from the repository root:

```bash
./gradlew :frontend:run
```

The UI calls the backend through `VITE_BACKEND_API_BASE_URL`, defaulting to
`http://localhost:8080`.
