# GitHub Organization Access Report Service

A Spring Boot REST service that connects to GitHub and generates
a structured report showing which users have access to which
repositories within a given GitHub organization.

---

## Table of Contents

- [How to Run the Project](#how-to-run-the-project)
- [How Authentication is Configured](#how-authentication-is-configured)
- [How to Call the API Endpoint](#how-to-call-the-api-endpoint)
- [Assumptions and Design Decisions](#assumptions-and-design-decisions)

---

## Tech Stack

- Java 17
- Spring Boot 3.2.3
- Spring Web (REST API)
- Spring Cache + Caffeine (in-memory caching)
- Spring Boot Actuator (health check)
- Lombok

---

## Project Structure
```
src/main/java/com/github/accessreport/
├── AccessreportApplication.java       # Main entry point
├── config/
│   ├── AppConfig.java                 # RestTemplate + thread pool
│   
├── controller/
│   └── ReportController.java          # REST endpoints
├── service/
│   └── GitHubService.java             # Business logic + parallel fetch
├── client/
│   └── GitHubClient.java              # GitHub API HTTP calls + pagination
├── model/
│   ├── Repository.java                # GitHub repo model
│   ├── Collaborator.java              # GitHub collaborator model
│   ├── UserAccess.java                # User + repo access model
│   └── AccessReport.java             # Final report model
└── exception/
    └── GlobalExceptionHandler.java    # Centralized error handling
```

---

## How to Run the Project

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- A GitHub account
- A GitHub Organization

### Step 1 — Clone the repository
```bash
git clone https://github.com/your-username/github-access-report.git
cd github-access-report
```

### Step 2 — Set environment variables

On Mac/Linux:
```bash
export GITHUB_TOKEN=ghp_your_token_here
export GITHUB_ORG=your-org-name
```

On Windows (Command Prompt):
```cmd
set GITHUB_TOKEN=ghp_your_token_here
set GITHUB_ORG=your-org-name
```

On Windows (PowerShell):
```powershell
$env:GITHUB_TOKEN="ghp_your_token_here"
$env:GITHUB_ORG="your-org-name"
```

### Step 3 — Run the application
```bash
./mvnw spring-boot:run
```

The service starts on:
```
http://localhost:8082
```

### Step 4 — Verify it is running
```
GET http://localhost:8082/api/ping
```

Expected response:
```json
{ "status": "ok" }
```

---

## How Authentication is Configured

This service uses a **GitHub Fine-grained Personal Access Token**
for authentication. The token is passed as a Bearer token in every
request header to the GitHub API.

### How to generate the token

1. Go to GitHub → click your profile photo → **Settings**
2. Scroll down → click **Developer settings**
3. Click **Personal access tokens → Fine-grained tokens**
4. Click **Generate new token**
5. Fill in:
   - Token name: `github-access-report-token`
   - Expiration: 30 days
   - Resource owner: select your **Organization** (not personal account)
   - Repository access: **All repositories**

6. Set these permissions:

**Repository permissions:**

| Permission | Level |
|---|---|
| Administration | Read-only |
| Contents | Read-only |
| Metadata | Read-only |

**Organization permissions:**

| Permission | Level |
|---|---|
| Members | Read-only |

7. Click **Generate token** and copy it immediately

### Where the token is used

The token is read from the `GITHUB_TOKEN` environment variable
defined in `application.yml`:
```yaml
github:
  token: ${GITHUB_TOKEN}
  org: ${GITHUB_ORG}
```

It is injected into every GitHub API request as a Bearer token:
```
Authorization: Bearer ghp_your_token_here
Accept: application/vnd.github+json
X-GitHub-Api-Version: 2022-11-28
```

The token is never hardcoded in source code or committed to GitHub.

---

## How to Call the API Endpoint

### 1. Get Access Report

Returns a full JSON report of all users and their repository
access within the organization.
```
GET http://localhost:8082/api/access-report
```

**Example response:**
```json
{
  "organization": "raghava-org",
  "generatedAt": "2026-03-14T13:07:53.993116100Z",
  "totalRepositories": 5,
  "totalUsers": 4,
  "users": [
    {
      "login": "alice",
      "totalRepos": 4,
      "repositories": [
        {
          "name": "backend-api",
          "fullName": "raghava-org/backend-api",
          "privateRepo": true,
          "role": "admin"
        },
        {
          "name": "frontend-app",
          "fullName": "raghava-org/frontend-app",
          "privateRepo": false,
          "role": "write"
        }
      ]
    },
    {
      "login": "bob",
      "totalRepos": 2,
      "repositories": [
        {
          "name": "backend-api",
          "fullName": "raghava-org/backend-api",
          "privateRepo": true,
          "role": "write"
        }
      ]
    }
  ]
}
```





### 3. Ping

Simple health check to confirm the service is running.
```
GET http://localhost:8082/api/ping
```

**Response:**
```json
{ "status": "ok" }
```

### 4. Actuator Health

Spring Boot built-in health endpoint.
```
GET http://localhost:8081/actuator/health
```

**Response:**
```json
{ "status": "UP" }
```

## Error Responses

| HTTP Status | Meaning | Solution |
|---|---|---|
| 401 | Invalid GitHub token | Check GITHUB_TOKEN value |
| 403 | Token lacks permissions | Regenerate token with correct permissions |
| 404 | Organization not found | Check GITHUB_ORG value |
| 500 | Unexpected server error | Check application logs |
