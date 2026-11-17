# Contact Book

Contact Book is an Extreme Programming course project built with a separate
frontend and backend architecture.

```text
contact-book/
├── backend/   Spring Boot backend
├── frontend/  React + Vite frontend
├── docs/      API and database documentation
└── README.md
```

## Backend

### Requirements

- JDK 17
- Maven 3.9 or later

### Run Locally

```powershell
cd backend
mvn spring-boot:run
```

The application uses an embedded H2 file database by default, so no external
database installation is required.

```text
backend/data/contact-book.mv.db
```

Available endpoints:

- Health check: http://localhost:8080/api/health
- Swagger UI: http://localhost:8080/swagger-ui.html

### Use MySQL

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="contact_book"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="YOUR_PASSWORD"
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

The reference database schema is available at
`backend/src/main/resources/db/schema.sql`.

### Test and Build

```powershell
cd backend
mvn test
mvn clean package
```

See [API documentation](docs/api.md) and [database design](docs/database.md)
for more details.

## Frontend

Start the backend at `http://localhost:8080`, then open another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open the local URL displayed by the development server. The default address is
usually `http://127.0.0.1:5173/`. The development server proxies `/api`
requests to the backend.

Run frontend tests and build the production bundle with:

```powershell
npm test
npm run build
```

See [frontend/README.md](frontend/README.md) for frontend-specific setup
instructions.
