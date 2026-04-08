# Timesheet Web Application — Coding Agent Specification

## Context & Goal

Build a **full-stack internal timesheet web application** for a SaaS company.  Your workflow should be as such:

Developers log daily work in 30-minute increments, organized by project and client. Admins manage users and data. The UI must mirror Notion's aesthetic and support English/French.

### Tech Stack

| Layer              | Technology      |
| ------------------ | --------------- |
| Runtime            | OpenJDK 25.02   |
| Frontend Runtime   | Node.js 22 LTS  |
| Frontend Framework | Angular 21      |
| Build Tool         | Gradle KTS 9.4  |
| Database           | PostgreSQL 18.1 |
| Backend Framework  | Spring Boot 4   |
| Security           | Spring Security |

---

## Database Rules

* **Language**: All schema identifiers, column names, seed data, and comments must be in **English**.
* **Storage**: Local PostgreSQL instance (no cloud for now).
* **Backup strategy**: Maintain **3 versions** of the database:
  * `db_current` — live data
  * `db_save1` — automated snapshot, 1 week behind current
  * `db_save2` — automated snapshot, 1 week behind `db_save1`
* **Security**: Do NOT hand-roll security logic. Use Spring Security for auth and BCrypt for password hashing. Follow OWASP best practices for this stack. Rely on battle-tested libraries; do not implement cryptography or auth flows yourself.

---

## Domain Model

### Developer (User)

Each developer has:

* `email` (login identifier)
* `password` (BCrypt hashed)
* `username` / display name
* `title` (e.g., "Senior Backend Engineer") — editable by the dev themselves
* `discord_link` — stores the developer's Discord username (e.g. `albinvernhes`); displayed as a badge with the Discord icon on the profile. This is a plain text field — no URL, no link, no Discord ID. The user enters their Discord username and it is shown as-is.
* `discord_avatar_url` — cached Discord CDN URL (resolved server-side via Discord OAuth2 or Bot API)
* `role`: `STANDARD` | `ADMIN`

A developer's calendar operates on **30-minute intervals**.

Calendar event types a developer can log (self-service only, no one else can log for them):

1. **Work session** — linked to a project; includes a free-text description of what was done
2. **Leave** — vacation, sick day, etc.

Non-editable calendar overlays (computed, not manually entered):

* **Weekends**
* **Geneva canton public holidays** — hard-code the official list and apply it automatically

### Project

Fields:

* `name`
* `type`: `INTERNAL` | `EXTERNAL`
* `repo_url` — link to source repository
* `client` — required if `EXTERNAL`, null if `INTERNAL`
* `assigned_devs` — list; a dev auto-assigns themselves when creating a project; others can self-assign

Project must expose:

* Full work log: who worked, when, for how long, and what they wrote
* Aggregated hours: total and per-developer, filterable by date range

### Client

Fields:

* `name`
* `assigned_devs` — computed as the union of devs across all linked projects (do not store separately; derive at query time)

Client must expose:

* All work logged across its projects
* Aggregated hours: total and per-developer, filterable by date range

---

## Discord Integration — OAuth2

Developers connect their Discord profile via OAuth2. A **"Connect Discord"** button on the profile page initiates the flow — no manual ID entry required.

**Flow:**

1. User clicks **"Connect Discord"** on their profile page.
2. Frontend redirects to `https://discord.com/oauth2/authorize` with `scope=identify` and a `redirect_uri` pointing to the backend callback.
3. Backend exchanges the authorization code for a token (`POST /oauth2/token`) and fetches the user object (including `avatar` hash).
4. Backend builds the CDN URL: `https://cdn.discordapp.com/avatars/{user_id}/{avatar_hash}.png` and caches it in `discord_avatar_url`.
5. The OAuth token is discarded — only the resolved avatar URL is persisted.

**Required backend endpoints:**

| Method | Endpoint                       | Description                                          |
| ------ | ------------------------------ | ---------------------------------------------------- |
| GET    | `/api/auth/discord/connect`  | Redirect to Discord OAuth2 authorize URL             |
| GET    | `/api/auth/discord/callback` | Handle OAuth code exchange, update developer profile |

**Environment variables:**

```
DISCORD_CLIENT_ID=your_app_client_id
DISCORD_CLIENT_SECRET=your_app_client_secret
DISCORD_REDIRECT_URI=http://localhost:8080/api/auth/discord/callback
```

**Discord Application Setup:**

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Create a new application (no bot needed)
3. Under **OAuth2 → Redirects**, add `http://localhost:8080/api/auth/discord/callback`
4. Use only the `identify` scope

**Security notes:**

- Include a `state` parameter in the OAuth redirect to prevent CSRF (generate per-session, validate on callback)
- Never persist the Discord access token — only the resolved `discord_avatar_url` is stored
- The connect flow requires an active authenticated session (a user can only link their own profile)

---

## Permissions

### STANDARD role

* Can log their own hours only
* Can create projects and clients (auto-assigned on creation)
* Can self-assign to any existing project
* Can only log hours within projects they are assigned to
* Can remove themselves from the company (account deletion)
* Can view everything (all devs, projects, clients, calendars)

### ADMIN role

* All STANDARD permissions, plus:
* Delete any dev, project, or client
* Create accounts for new developers via a GUI form (input: email) → system generates an invite link → dev follows the link to set their own password, Discord link, and title

### Seed account

On first run, automatically create:

* **Username**: `Albin`
* **Password**: `Vernhes`
* **Role**: `ADMIN`

---

## Frontend Requirements

### Internationalization

* Default language: **French**
* Toggle in Settings between **French** and **English**
* All UI strings must go through an i18n layer (no hardcoded display text)

### Theming

* Inspired by **Notion**: clean white/light-grey backgrounds, subtle borders, sans-serif typography, minimal chrome
* Support at least a **light** and **dark** theme, toggleable in Settings Those theme must use notion cclor palette. from the calendar fig if possible.
  Take the color for the theme here https://www.figma.com/design/Tv4ak0BnNSvKmJB8b6bzmA/Notion-Calendar-with-Variables--Components-and-Tokens--Community---Copy-?node-id=3-7&p=f&t=3h1trrrWE7V8dsm4-0

### Authentication — Login Screen

* Fields: **email** + **password** only
* Must be compatible with browser **autofill / password managers**
* Session persists as long as the IP address does not change (implement via JWT with IP binding)
* After login, redirect to the authenticated developer's own calendar

---

## Views & Layouts

### 1. Developer Calendar (own profile + calendar)

The developer's calendar doubles as their **profile page**.

**Profile section** (editable inline by the dev themselves):

* Display name, title, Discord username (displayed as a badge with Discord icon — not a link, just text)

**Calendar section:**

The calendar follows a **Jira-style day-by-day layout**: each day is a vertical column with time slots, and entries are rendered as blocks within their time range — similar to Jira's "Board" calendar or Google Calendar's day view.

* Toggle between **Day**, **Week**, and **Month** views — adapt layout intelligently per view
* Time grid uses **30-minute intervals**
* Overlay filters: select a **client** → then a **project** to filter what's shown, or directly a project
* Hours worked for the selected project/client are displayed for the visible time period
* You can select working time by pressing and releasing your mouse

**Export:**

An **Export** button with a date-range picker and format selector (PDF or Excel) generates a personal timesheet report — no new route needed. Report includes:

* Headline KPIs: total hours, projects worked on, leave days taken
* Hours breakdown by project with relative share
* Full daily log: date, project, client, duration, description — leave entries visually distinct from work entries
* Weekly summary: work hours, leave hours, total per week

Layout is based on the Figma filehttps://www.figma.com/design/Tv4ak0BnNSvKmJB8b6bzmA/Notion-Calendar-with-Variables--Components-and-Tokens--Community---Copy-?node-id=3-7&p=f&t=3h1trrrWE7V8dsm4-0. Adapt it intelligently to the requirements of the app; do not just copy-paste.

---

### 2. Project / Client View (profile + two-mode timeline)

This view acts as both a **profile page** and a **data dashboard** for a project or client.

**Profile section:**

* Name, repo link (for projects), list of assigned devs with their titles and Discord usernames
* Devs can add themselves to the project from this view
* Total hours worked in the selected time period
* Per-developer hours breakdown in the selected time period

**Export:**

An **Export** button (date-range picker + format selector) is available on both project and client views — no new route needed.

* **Project report**: headline KPIs (total hours, number of developers, number of sessions); per-developer breakdown with % share; week-by-week activity heatmap (developers × weeks); full log sorted by date, grouped by developer.
* **Client report**: headline KPIs (total hours, number of projects, number of developers); hours by project with relative share; developer × project cross-tab matrix with totals; monthly trend; full log (date, project, developer, duration, description).

**Developer filter:** multi-select control to show only specific developers. This filter applies to both visualization modes below and persists when switching between them.

**Mode A — Swimlane Timeline:**

A swimlane is a **horizontal lane-per-developer timeline chart** — each developer gets their own row (lane), and time flows left to right. It provides a bird's-eye view of when each developer was active on the project, how much they worked, and where gaps occurred. Think of it as a Gantt chart focused on people rather than tasks.

* One horizontal row per developer
* Time axis runs left to right; supports multiple zoom levels (day, week, month, quarter)
* Each block represents a logged work period, width proportional to hours
* **Dead zones**: gaps between blocks are compressed visually into a thin dashed separator labeled (e.g., "6 weeks inactive") — do not leave empty whitespace
* Colors are assigned per developer and remain consistent between both modes
* Hovering a block opens a detail panel with the individual log entries for that period
* Rows sorted by activity (most active developer at the top)

No Figma available for this mode — copy the Notion app style.

**Mode B — Heatmap:**

* GitHub-style contribution grid with **three granularity levels**: **Day**, **Week**, and **Month** — toggled via a control in the view header
  * **Day view**: columns = individual days, rows = developers — shows fine-grained daily activity
  * **Week view**: columns = weeks, rows = developers — the default; good for spotting sprint patterns
  * **Month view**: columns = months, rows = developers — compact overview of long-running projects
* Plus a "Total" summary row at the bottom in all granularity levels
* Cell saturation = hours logged in that period (light = low activity, saturated = heavy sprint, grey = no activity)
* Entire project history fits on one screen — no horizontal scrolling regardless of project age
* Pauses and bursts must be immediately obvious

Layout is from the Figma file https://www.figma.com/design/Ap9QmGk6N52WdBdZaQ1mx0/Heatmap-2026--Community-?node-id=0-1&p=f&t=3h1trrrWE7V8dsm4-0

Use the 2026 layout according to the notion color theme, Adapt it to day, week, and month granularities.

**Switching between modes:**

* A toggle button in the top-right of the view header switches between Swimlane and Heatmap
* Transition: simple **fade animation**
* Active developer filter and date range are **preserved** when switching modes


---

### 3. Global Layout (sidebar / nav)

The persistent navigation panel shows:

* **Developers** button that allows seeing a layout with a list: display name, title, Discord username (text badge). Clicking opens their calendar.
* **Projects** list: with a "Create Project" button visible to all roles. Only the projects you are assigned to are displayed. Clicking it opens a separate layout showing all projects.
* **Clients** list: with a "Create Client" button visible to all roles. Only the clients linked to your projects are displayed. Clicking it opens a separate layout showing all clients.

Admin-only controls in the Developers section:

* "Add Developer" button → opens a form to enter an email → generates an invite link to share with the new hire

All three sections may display useful summary metadata (e.g., total hours this week, number of active projects) — use your judgment for what adds value without cluttering.

---

## CRUD Requirements

All entities must support full **Create, Read, Update, Delete** operations through the API and the UI.

### Developers

- **Create**: Admin invites a developer via email → invite token → registration form (username, password, Discord username, title)
- **Read**: List all developers in sidebar; view any developer's calendar/profile
- **Update**: Developer edits own profile inline (username, title, Discord username); profile updates persist immediately
- **Delete**: Admin can delete any developer

### Projects

- **Create**: Any developer can create a project (auto-assigned as member); specify name, type, repo URL, client
- **Read**: List all projects; view project detail with swimlane/heatmap, assigned devs, hours breakdown
- **Update**: Edit project name, type, repo URL, client assignment
- **Delete**: Admin-only
- **Join/Leave**: Any developer can join or leave a project via the project detail view

### Clients

- **Create**: Any developer can create a client (name only)
- **Read**: List all clients; view client detail with aggregated hours across linked projects
- **Update**: Edit client name
- **Delete**: Admin-only

### Time Entries

- **Create**: Developer selects time range on calendar (drag or modal), picks project (for WORK type), adds description; entries must be in 30-minute intervals
- **Read**: Entries displayed on calendar grid; filterable by project and client; API supports filtering by developerId, projectId, date range (ISO datetime format required: `2026-03-13T00:00:00`)
- **Update**: Click existing entry to edit type, project, description, time range
- **Delete**: Delete individual entries with confirmation modal

### Data Persistence

- All CRUD operations must persist to the PostgreSQL database via the REST API
- Calendar state (current view, date, filters) must persist across navigation within the same session
- Page refreshes must reload all data from the server — no data loss

---

## Implementation Constraints

1. **Geneva public holidays** must be applied automatically; hard-code the official list.
2. **Session IP binding** must be implemented server-side.
3. **Invite flow** for new devs must be token-based (one-time-use, expiring link).
4. **Self-assignment** to projects must be enforced at the API level, not just the UI.
5. **All database schema and API naming** in English.
6. **i18n layer** must be in place from day one — no hardcoded French or English strings in components.
7. Build must run with a single command (`./start.sh`) and shutdown cleanly.
8. **Export libraries** — add to `build.gradle.kts`: `org.apache.poi:poi-ooxml:5.3.0` (Excel) and `com.github.librepdf:openpdf:2.0.3` (PDF). All report labels must go through the existing i18n layer.
9. **PDF quality**: A4 portrait; headline KPIs prominent at top; repeated header (subject + date range) and footer (page X of Y, generation date) on every page; numbers right-aligned, text left-aligned, alternating row shading; page breaks between summary and detail sections; leave entries visually distinct from work entries.
10. **Excel quality**: frozen header row and auto-filters on every sheet; `Summary` sheet as a KPI dashboard; conditional formatting on hour columns (heavier = darker); consistent developer/project colors across all sheets; duration stored as decimal hours (`4.5`) formatted as `4h 30m`; auto-fitted column widths.

---

## Default Admin Account

- **Email**: albin@test.com
- **Password**: Vernhes
- **Discord username**: albinvernhes

---

## API Endpoints

### Authentication

| Method | Endpoint               | Auth | Description                                            |
| ------ | ---------------------- | ---- | ------------------------------------------------------ |
| POST   | `/api/auth/login`    | No   | Authenticate with email + password; returns JWT token  |
| POST   | `/api/auth/register` | No   | Register a new developer using an invite token         |
| POST   | `/api/auth/logout`   | Yes  | Invalidate session (clears security context)           |
| GET    | `/api/auth/me`       | Yes  | Get the currently authenticated developer's profile    |
| PUT    | `/api/auth/profile`  | Yes  | Update own profile (username, title, discord username) |

### Developers

| Method | Endpoint                   | Auth  | Description                                          |
| ------ | -------------------------- | ----- | ---------------------------------------------------- |
| GET    | `/api/developers`        | Yes   | List all developers                                  |
| GET    | `/api/developers/{id}`   | Yes   | Get developer by ID                                  |
| POST   | `/api/developers`        | Admin | Create a developer directly (with password)          |
| PUT    | `/api/developers/{id}`   | Yes   | Update a developer's details                         |
| DELETE | `/api/developers/{id}`   | Admin | Delete a developer                                   |
| POST   | `/api/developers/invite` | Admin | Generate invite link and send email to new developer |

### Projects

| Method | Endpoint                         | Auth  | Description                                                |
| ------ | -------------------------------- | ----- | ---------------------------------------------------------- |
| GET    | `/api/projects`                | Yes   | List all projects                                          |
| GET    | `/api/projects/{id}`           | Yes   | Get project by ID                                          |
| POST   | `/api/projects?creatorId={id}` | Yes   | Create a project; auto-assigns the creator                 |
| PUT    | `/api/projects/{id}`           | Yes   | Update a project                                           |
| DELETE | `/api/projects/{id}`           | Admin | Delete a project                                           |
| POST   | `/api/projects/{id}/assign`    | Yes   | Assign a developer to a project (body:`{developerId}`)   |
| POST   | `/api/projects/{id}/unassign`  | Yes   | Remove a developer from a project (body:`{developerId}`) |

### Clients

| Method | Endpoint              | Auth  | Description      |
| ------ | --------------------- | ----- | ---------------- |
| GET    | `/api/clients`      | Yes   | List all clients |
| GET    | `/api/clients/{id}` | Yes   | Get client by ID |
| POST   | `/api/clients`      | Yes   | Create a client  |
| PUT    | `/api/clients/{id}` | Yes   | Update a client  |
| DELETE | `/api/clients/{id}` | Admin | Delete a client  |

### Time Entries

| Method | Endpoint                   | Auth | Description                                                                             |
| ------ | -------------------------- | ---- | --------------------------------------------------------------------------------------- |
| GET    | `/api/time-entries`      | Yes  | List time entries; filter by `developerId`, `projectId`, `startDate`, `endDate` |
| GET    | `/api/time-entries/{id}` | Yes  | Get a time entry by ID                                                                  |
| POST   | `/api/time-entries`      | Yes  | Create a time entry                                                                     |
| PUT    | `/api/time-entries/{id}` | Yes  | Update a time entry                                                                     |
| DELETE | `/api/time-entries/{id}` | Yes  | Delete a time entry                                                                     |

### Export

| Method | Endpoint                       | Auth | Description                  |
| ------ | ------------------------------ | ---- | ---------------------------- |
| GET    | `/api/export/developer/{id}` | Yes  | Developer personal timesheet |
| GET    | `/api/export/project/{id}`   | Yes  | Project work log             |
| GET    | `/api/export/client/{id}`    | Yes  | Client aggregated report     |

Query parameters (all endpoints): `format=pdf|excel`, `startDate` (ISO date), `endDate` (ISO date), `lang=fr|en` (defaults to French).

Response: binary file download with `Content-Disposition: attachment` header.

### System

| Method | Endpoint             | Auth | Description          |
| ------ | -------------------- | ---- | -------------------- |
| GET    | `/actuator/health` | No   | Backend health check |

---

## Environment Variables

All configuration is driven by environment variables with safe defaults for local development.

### Database

| Variable        | Default                                        | Description         |
| --------------- | ---------------------------------------------- | ------------------- |
| `DB_URL`      | `jdbc:postgresql://localhost:5432/timesheet` | JDBC connection URL |
| `DB_USERNAME` | `postgres`                                   | Database user       |
| `DB_PASSWORD` | `postgres`                                   | Database password   |

### JWT Security

| Variable                  | Default       | Description                                      |
| ------------------------- | ------------- | ------------------------------------------------ |
| `JWT_SECRET_KEY`        | *(base64)*  | 256-bit HMAC secret key (override in production) |
| `JWT_EXPIRATION`        | `86400000`  | Access token TTL in ms (default: 24 h)           |
| `JWT_INVITE_EXPIRATION` | `604800000` | Invite token TTL in ms (default: 7 days)         |

### Email (SMTP)

| Variable           | Default                     | Description                         |
| ------------------ | --------------------------- | ----------------------------------- |
| `SMTP_HOST`      | `smtp.gmail.com`          | SMTP server hostname                |
| `SMTP_PORT`      | `587`                     | SMTP server port (STARTTLS)         |
| `SMTP_USERNAME`  | *(empty)*                 | SMTP authentication username        |
| `SMTP_PASSWORD`  | *(empty)*                 | SMTP authentication password        |
| `APP_EMAIL_FROM` | `noreply@timesheet.local` | Sender address for outgoing emails  |
| `APP_BASE_URL`   | `http://localhost:5173`   | Base URL used to build invite links |

### Discord OAuth2

| Variable                  | Description                                                    |
| ------------------------- | -------------------------------------------------------------- |
| `DISCORD_CLIENT_ID`     | Discord application client ID                                  |
| `DISCORD_CLIENT_SECRET` | Discord application client secret                              |
| `DISCORD_REDIRECT_URI`  | Must match `http://localhost:8080/api/auth/discord/callback` |

### Server

| Variable     | Default  | Description         |
| ------------ | -------- | ------------------- |
| `APP_PORT` | `8080` | Backend server port |

---

## Authentication & Security Details

### JWT with IP Binding

The application uses stateless JWT tokens (HS256). Each token embeds the client's IP address at login time. Every subsequent request validates that the token's IP matches the incoming request's IP — the token is rejected if they differ.

IP detection respects reverse-proxy headers in order: `X-Forwarded-For` → `X-Real-IP` → `request.remoteAddr`.

### Invite Flow

1. Admin submits an email via the "Add Developer" form.
2. Backend generates a signed JWT invite token (7-day TTL) bound to that email.
3. A registration link (`/register?token=<token>`) is returned in the response and sent by email automatically (requires SMTP configuration).
4. The new developer opens the link, sets their username, password, and optional Discord username.
5. On successful registration, they are immediately logged in.

> **Note:** If SMTP is not configured, the invite link is still returned in the API response so the admin can share it manually.

### Password Hashing

All passwords are hashed with BCrypt (cost factor 10) via Spring Security's `BCryptPasswordEncoder`.

---

## Database Schema

Schema is managed by **Flyway** migrations. The application never uses `ddl-auto: create` in production.

### Tables

**`developers`** — application users

| Column           | Type           | Notes                                               |
| ---------------- | -------------- | --------------------------------------------------- |
| `id`           | BIGSERIAL PK   |                                                     |
| `email`        | VARCHAR UNIQUE | Login identifier                                    |
| `password`     | VARCHAR        | BCrypt hash                                         |
| `username`     | VARCHAR        | Display name                                        |
| `title`        | VARCHAR        | e.g. "Senior Backend Engineer"                      |
| `discord_link` | VARCHAR        | Discord username (plain text, e.g.`albinvernhes`) |
| `role`         | VARCHAR        | `STANDARD` \| `ADMIN`                           |
| `created_at`   | TIMESTAMP      |                                                     |
| `updated_at`   | TIMESTAMP      |                                                     |

**`clients`** — external clients

| Column         | Type         |
| -------------- | ------------ |
| `id`         | BIGSERIAL PK |
| `name`       | VARCHAR      |
| `created_at` | TIMESTAMP    |
| `updated_at` | TIMESTAMP    |

**`projects`** — work units

| Column         | Type         | Notes                              |
| -------------- | ------------ | ---------------------------------- |
| `id`         | BIGSERIAL PK |                                    |
| `name`       | VARCHAR      |                                    |
| `type`       | VARCHAR      | `INTERNAL` \| `EXTERNAL`       |
| `repo_url`   | VARCHAR(500) | Optional                           |
| `client_id`  | BIGINT FK    | Required when type is `EXTERNAL` |
| `created_at` | TIMESTAMP    |                                    |
| `updated_at` | TIMESTAMP    |                                    |

**`project_developers`** — many-to-many join

| Column           | Type      |
| ---------------- | --------- |
| `project_id`   | BIGINT FK |
| `developer_id` | BIGINT FK |

**`time_entries`** — work sessions and leave

| Column           | Type         | Notes                                    |
| ---------------- | ------------ | ---------------------------------------- |
| `id`           | BIGSERIAL PK |                                          |
| `developer_id` | BIGINT FK    |                                          |
| `project_id`   | BIGINT FK    | Nullable (required for `WORK` entries) |
| `type`         | VARCHAR      | `WORK` \| `LEAVE`                    |
| `description`  | TEXT         | Free-text notes for work sessions        |
| `start_time`   | TIMESTAMP    |                                          |
| `end_time`     | TIMESTAMP    |                                          |
| `created_at`   | TIMESTAMP    |                                          |
| `updated_at`   | TIMESTAMP    |                                          |

### Indexes

```sql
idx_developers_email, idx_developers_role
idx_projects_client_id, idx_projects_type
idx_time_entries_developer_id, idx_time_entries_project_id,
idx_time_entries_start_time, idx_time_entries_type
```

---

## Frontend Routes

| Route                       | Description                                     |
| --------------------------- | ----------------------------------------------- |
| `/login`                  | Login screen                                    |
| `/register?token=<token>` | Registration screen (invite-only)               |
| `/` or `/calendar`      | Own developer calendar + profile (default view) |
| `/calendar/:developerId`  | Another developer's calendar (read-only)        |
| `/project/:id`            | Project profile + swimlane/heatmap dashboard    |
| `/client/:id`             | Client profile + swimlane/heatmap dashboard     |
| `/settings`               | Language and theme settings                     |

---

## Running the Application

```bash
./start.sh
```

The script:

1. Checks that PostgreSQL is running on port 5432
2. Creates the `timesheet` database if it does not exist
3. Ensures the seed admin password hash is correct
4. Starts the backend on **http://localhost:8080**
5. Waits for the backend health check to pass
6. Starts the frontend on **http://localhost:5173**
7. Handles `Ctrl+C` to cleanly shut down both services

**Default credentials:** `albin@test.com` / `Vernhes`

Press `Ctrl+C` to stop all services.

---

## Testing

### Backend

```bash
cd backend
./gradlew test
```

Covers: `JwtTokenProviderTest`, `ClientServiceImplTest`, `DeveloperServiceImplTest`, `ProjectServiceImplTest`, `TimeEntryServiceImplTest`, `DeveloperControllerTest`.

### Frontend

```bash
cd frontend
npm test          # unit tests (Vitest)
npm run test:e2e  # end-to-end tests (Playwright)
```
