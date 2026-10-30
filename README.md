# Calculator DevOps (SPE Mini-Project)

A simple Java calculator exposed over HTTP (using the JDK's built-in `HttpServer`, no frameworks),
built with Maven and shipped through a Jenkins → Docker Hub → Docker Compose pipeline.

## API

| Operation | Endpoint                      | Example response |
|-----------|-------------------------------|------------------|
| Add       | `GET /api/v1/add?a=2&b=3`      | `{"operation":"add","a":2,"b":3,"result":5}` |
| Subtract  | `GET /api/v1/subtract?a=5&b=3` | `{"operation":"subtract","a":5,"b":3,"result":2}` |
| Multiply  | `GET /api/v1/multiply?a=2&b=3` | `{"operation":"multiply","a":2,"b":3,"result":6}` |
| Divide    | `GET /api/v1/divide?a=5&b=2`   | `{"operation":"divide","a":5,"b":2,"result":2.5}` |
| Health    | `GET /actuator/health`         | `{"status":"UP"}` |

Division by zero, missing or non-numeric parameters return **HTTP 400**:
```json
{"status":400,"error":"Division by zero is not allowed"}
```

## Build & test

Requires Java 17+ and Maven.

```bash
mvn clean package      # builds target/calculator.jar (runs tests)
mvn test               # JUnit reports in target/surefire-reports/
```

## Run

```bash
java -jar target/calculator.jar            # http://localhost:8080
```

With Docker:
```bash
docker build -t calculator .
docker run -p 8080:8080 calculator
```

With Docker Compose (calculator + Nginx reverse proxy):
```bash
docker compose up -d --build
curl "http://localhost:9090/api/v1/add?a=2&b=3"
docker compose down
```

## Project layout

```
├── src/main/java/com/calculator/   Calculator.java, CalculatorServer.java
├── src/test/java/com/calculator/   CalculatorTest (unit), CalculatorServerTest (integration)
├── pom.xml
├── Dockerfile            multi-stage: Maven build → JRE alpine runtime, non-root user
├── .dockerignore
├── docker-compose.yml    calculator + nginx, network, health check, restart policy
├── nginx/nginx.conf      reverse proxy → calculator:8080
└── Jenkinsfile           Checkout → Build → Test → Docker Build → Push → Deploy
```

## Jenkins setup

1. Plugins: Pipeline, Git, GitHub, JUnit, Mailer, Credentials Binding.
2. Credentials → add *Username with password* with ID `dockerhub-creds` (Docker Hub username + access token).
3. Set `DOCKERHUB_USER` and `EMAIL` at the top of the `Jenkinsfile`.
4. Manage Jenkins → System → *E-mail Notification*: SMTP server (e.g. `smtp.gmail.com`, port 465, SSL, Gmail app password).
5. New Item → Pipeline → *Pipeline script from SCM* → Git → this repo URL, branch `*/main`.
6. Trigger: tick *GitHub hook trigger for GITScm polling*, and add a webhook in GitHub
   (Settings → Webhooks → `http://<jenkins-url>/github-webhook/`). For a local Jenkins expose it with
   `ngrok http 8080`, or rely on the Poll SCM fallback in the Jenkinsfile.
7. Jenkins must be able to run `mvn`, `docker` and `curl`. On macOS (Homebrew Jenkins), add
   `PATH+EXTRA=/opt/homebrew/bin:/usr/local/bin` under Manage Jenkins → System → Global properties → Environment variables.

