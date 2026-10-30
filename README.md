# Calculator REST API: CI/CD mini-project

A small Spring Boot calculator service, wrapped in an automated DevOps pipeline:
**GitHub → Jenkins (build, test, image, push) → Docker Hub → Docker Compose deployment**.

CSE 816 Software Production Engineering, mini-project (Term I 2026-27).

- Repository: <https://github.com/Dd1235/SPE_Miniproject>
- Docker image: <https://hub.docker.com/r/deepya123/calculator-api>

## API

| Operation | Request | Result |
|---|---|---|
| Add | `GET /api/v1/add?a=2&b=3` | `{"operation":"add","a":2,"b":3,"result":5}` |
| Subtract | `GET /api/v1/subtract?a=2&b=3` | `{"operation":"subtract","a":2,"b":3,"result":-1}` |
| Multiply | `GET /api/v1/multiply?a=2.5&b=4` | `{"operation":"multiply","a":2.5,"b":4,"result":10}` |
| Divide | `GET /api/v1/divide?a=10&b=4` | `{"operation":"divide","a":10,"b":4,"result":2.5}` |
| Health | `GET /actuator/health` | `{"status":"UP", ...}` |

- Numbers may be integers or decimals, and decimal arithmetic is exact
  (`0.1 + 0.2 = 0.3`).
- Non-terminating divisions are rounded to 34 significant digits.
- Invalid input returns **HTTP 400** with an
  [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem-details body
  (`application/problem+json`). For example, `GET /api/v1/divide?a=5&b=0`
  returns:

```json
{"title":"Division by zero","status":400,"detail":"Parameter 'b' must not be zero for division","instance":"/api/v1/divide"}
```

The same happens for a missing, empty or non-numeric parameter.

## Stack

Java 21 · Spring Boot 4.1 (Spring MVC, Actuator) · Maven 3.9 · JUnit Jupiter 6 +
MockMvc + JaCoCo · Docker (multi-stage, multi-arch) · Docker Compose · Nginx ·
Jenkins (declarative pipeline).

## Build, test, run

You need JDK 21 and Maven 3.9+, or just Docker. `./mvnw` works in place of `mvn`
if Maven isn't installed.

```bash
mvn test                        # 31 unit + integration tests; reports in target/surefire-reports
                                # coverage report: target/site/jacoco/index.html
mvn clean package               # builds target/calculator-api.jar
java -jar target/calculator-api.jar            # http://localhost:8080
```

**Docker (single container):**

```bash
docker build -t calculator-api .
docker run --rm -p 8080:8080 calculator-api
curl "http://localhost:8080/api/v1/add?a=2&b=3"
```

**Docker Compose (full stack: Nginx proxy + 2 API replicas):**

```bash
docker compose up -d --build --wait
curl -i "http://localhost:8085/api/v1/divide?a=1&b=3"    # X-Upstream header shows which replica answered
docker compose ps
docker compose logs -f app
docker compose down
```

## CI/CD pipeline (`Jenkinsfile`)

| Stage | What it does |
|---|---|
| Checkout | Pulls the triggering commit from GitHub (webhook, or Poll SCM every ~2 min) |
| Build | `mvn clean package -DskipTests`; archives the JAR |
| Test | `mvn test`; publishes JUnit results and the JaCoCo coverage report; a failing test fails the build |
| Docker Build | Builds the image for `linux/amd64` and `linux/arm64`, tagged `:<build number>` and `:latest` |
| Push | Logs in with the Jenkins credential `dockerhub-creds` and pushes both tags to Docker Hub |
| Deploy | `docker compose up -d --wait` with the new tag, then smoke-tests through the proxy |

Afterwards Jenkins e-mails SUCCESS or FAILURE with the job and build details, and
cleans the workspace.

## One-time Jenkins setup

1. **Plugins:** Pipeline, Pipeline: Stage View, Git, GitHub, Credentials Binding,
   JUnit, Email Extension, Workspace Cleanup.
2. **Docker Hub credential:** under *Manage Jenkins → Credentials*, add a *Username
   with password* credential with the ID `dockerhub-creds`. Use a Docker Hub access
   token with Read & Write access as the password.
3. **E-mail:** under *Manage Jenkins → System → Extended E-mail Notification*, set
   the SMTP server to `smtp.gmail.com`, port `465`, SSL on, with a credential made
   of a Gmail address and an app password. Then add the global environment variable
   `NOTIFY_EMAIL` with the recipient address, so no address is committed to Git.
4. **Job:** create a *Pipeline* job using *Pipeline script from SCM*, with this
   repository, branch `*/main` and script path `Jenkinsfile`. Run it once by hand
   to register the triggers.
5. **Triggers:** Poll SCM works with no further setup. For a webhook, expose Jenkins
   with `ngrok http 8080 --url=<your-ngrok-domain>`, set the *Jenkins URL* to that
   address, and add a GitHub webhook pointing to
   `https://<your-ngrok-domain>/github-webhook/` with content type
   `application/json` and push events only.

## Repository layout

```
SPE_Miniproject/
├── src/main/java/.../calculator/   # Spring Boot application
├── src/main/resources/             # application.properties
├── src/test/java/.../calculator/   # JUnit unit + MockMvc integration tests
├── pom.xml                         # Maven build (Spring Boot 4.1, JaCoCo)
├── Dockerfile                      # multi-stage: Maven/JDK build -> JRE runtime
├── .dockerignore                   # allow-list: only pom.xml and src/ enter the build
├── docker-compose.yml              # proxy + app replicas, networks, health checks
├── proxy/                          # Nginx reverse-proxy image (Dockerfile + nginx.conf)
├── Jenkinsfile                     # declarative CI/CD pipeline
├── mvnw, mvnw.cmd, .mvn/           # Maven wrapper
└── .gitignore, .gitattributes
```
