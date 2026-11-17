# Contact Book Frontend

A React + Vite single-page application that connects to the Spring Boot API in
the `backend/` directory.

## Requirements

- Node.js 20.19 or later, or Node.js 22.12 or later
- A running backend at `http://localhost:8080` by default

## Run Locally

```powershell
cd frontend
npm install
npm run dev
```

Open the local URL displayed in the terminal. The development server proxies
`/api` requests to `http://localhost:8080`.

For production builds, set `VITE_API_BASE_URL` to the backend API URL. The URL
must not end with a slash.

```powershell
npm test
npm run build
```

## Features

- Paginated contact list and keyword search
- Favorite and unfavorite actions with favorite-only filtering
- Contact creation, editing, and deletion
- Dynamic support for multiple contact methods
- Excel template download, import, and export

Imports accept only `.xlsx` files with a maximum size of 5 MB. Exported files
respect the active search keyword and favorite filter.
