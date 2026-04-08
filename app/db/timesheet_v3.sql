--
-- PostgreSQL database dump
--

-- Dumped from database version 18.3 (Ubuntu 18.3-1.pgdg25.10+1)
-- Dumped by pg_dump version 18.3 (Ubuntu 18.3-1.pgdg25.10+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.time_entries DROP CONSTRAINT IF EXISTS time_entries_project_id_fkey;
ALTER TABLE IF EXISTS ONLY public.time_entries DROP CONSTRAINT IF EXISTS time_entries_developer_id_fkey;
ALTER TABLE IF EXISTS ONLY public.projects DROP CONSTRAINT IF EXISTS projects_client_id_fkey;
ALTER TABLE IF EXISTS ONLY public.project_developers DROP CONSTRAINT IF EXISTS project_developers_project_id_fkey;
ALTER TABLE IF EXISTS ONLY public.project_developers DROP CONSTRAINT IF EXISTS project_developers_developer_id_fkey;
DROP INDEX IF EXISTS public.idx_time_entries_type;
DROP INDEX IF EXISTS public.idx_time_entries_start_time;
DROP INDEX IF EXISTS public.idx_time_entries_recurrence_group;
DROP INDEX IF EXISTS public.idx_time_entries_project_id;
DROP INDEX IF EXISTS public.idx_time_entries_developer_id;
DROP INDEX IF EXISTS public.idx_projects_type;
DROP INDEX IF EXISTS public.idx_projects_client_id;
DROP INDEX IF EXISTS public.idx_developers_role;
DROP INDEX IF EXISTS public.idx_developers_email;
DROP INDEX IF EXISTS public.flyway_schema_history_s_idx;
ALTER TABLE IF EXISTS ONLY public.time_entries DROP CONSTRAINT IF EXISTS time_entries_pkey;
ALTER TABLE IF EXISTS ONLY public.projects DROP CONSTRAINT IF EXISTS projects_pkey;
ALTER TABLE IF EXISTS ONLY public.project_developers DROP CONSTRAINT IF EXISTS project_developers_pkey;
ALTER TABLE IF EXISTS ONLY public.flyway_schema_history DROP CONSTRAINT IF EXISTS flyway_schema_history_pk;
ALTER TABLE IF EXISTS ONLY public.developers DROP CONSTRAINT IF EXISTS developers_pkey;
ALTER TABLE IF EXISTS ONLY public.developers DROP CONSTRAINT IF EXISTS developers_email_key;
ALTER TABLE IF EXISTS ONLY public.clients DROP CONSTRAINT IF EXISTS clients_pkey;
ALTER TABLE IF EXISTS public.time_entries ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.projects ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.developers ALTER COLUMN id DROP DEFAULT;
ALTER TABLE IF EXISTS public.clients ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS public.time_entries_id_seq;
DROP TABLE IF EXISTS public.time_entries;
DROP SEQUENCE IF EXISTS public.projects_id_seq;
DROP TABLE IF EXISTS public.projects;
DROP TABLE IF EXISTS public.project_developers;
DROP TABLE IF EXISTS public.flyway_schema_history;
DROP SEQUENCE IF EXISTS public.developers_id_seq;
DROP TABLE IF EXISTS public.developers;
DROP SEQUENCE IF EXISTS public.clients_id_seq;
DROP TABLE IF EXISTS public.clients;
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: clients; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.clients (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: clients_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.clients_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: clients_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.clients_id_seq OWNED BY public.clients.id;


--
-- Name: developers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.developers (
    id bigint NOT NULL,
    email character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    username character varying(255),
    title character varying(255),
    discord_link character varying(500),
    discord_avatar_url character varying(500),
    role character varying(20) DEFAULT 'STANDARD'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: developers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.developers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: developers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.developers_id_seq OWNED BY public.developers.id;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: project_developers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.project_developers (
    project_id bigint NOT NULL,
    developer_id bigint NOT NULL
);


--
-- Name: projects; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.projects (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(20) NOT NULL,
    repo_url character varying(500),
    client_id bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: projects_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.projects_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: projects_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.projects_id_seq OWNED BY public.projects.id;


--
-- Name: time_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.time_entries (
    id bigint NOT NULL,
    developer_id bigint NOT NULL,
    project_id bigint,
    type character varying(20) NOT NULL,
    description text,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    recurrence_group_id uuid,
    recurrence_rule character varying(255)
);


--
-- Name: time_entries_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.time_entries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: time_entries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.time_entries_id_seq OWNED BY public.time_entries.id;


--
-- Name: clients id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients ALTER COLUMN id SET DEFAULT nextval('public.clients_id_seq'::regclass);


--
-- Name: developers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.developers ALTER COLUMN id SET DEFAULT nextval('public.developers_id_seq'::regclass);


--
-- Name: projects id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.projects ALTER COLUMN id SET DEFAULT nextval('public.projects_id_seq'::regclass);


--
-- Name: time_entries id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.time_entries ALTER COLUMN id SET DEFAULT nextval('public.time_entries_id_seq'::regclass);


--
-- Data for Name: clients; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.clients (id, name, created_at, updated_at) FROM stdin;
1	hrc	2026-04-01 17:12:25.015292	2026-04-01 17:12:25.015292
2	Firmenish	2026-04-01 17:12:31.707593	2026-04-01 17:12:31.707593
\.


--
-- Data for Name: developers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.developers (id, email, password, username, title, discord_link, discord_avatar_url, role, created_at, updated_at) FROM stdin;
1	albin@test.com	$2a$10$yNvXzvpMoO1F4/h2mVBq/.vRPO/ypAvqMkSYtORSMQ67BQ8uAXX96	Albin	Administrator	discord	\N	ADMIN	2026-03-31 17:14:04.917441	2026-04-01 11:34:08.788512
2	christoph@aiso.ch	$2a$10$BMojLRuIC3k5imZscYNUWeTMMzEUBpBO0KBKy05p/OEhDUdyHTdxy	Christoph	\N	totof	\N	STANDARD	2026-04-01 16:43:42.145933	2026-04-01 16:43:42.145933
3	jil@gmail.com	$2a$10$LQrR/Duck/N8SNHE1bMj9.3guLG68tYUrfCM4GyP2UmKLWCfF.ZBy	Jean-Luc	\N	jil	\N	STANDARD	2026-04-01 17:11:59.561292	2026-04-01 17:11:59.561292
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init schema	SQL	V1__init_schema.sql	-165131880	postgres	2026-03-31 17:14:04.88358	15	t
2	2	seed admin	SQL	V2__seed_admin.sql	280294511	postgres	2026-03-31 17:14:04.913411	2	t
3	3	add recurrence fields	SQL	V3__add_recurrence_fields.sql	1999130206	postgres	2026-04-01 17:47:15.320843	11	t
\.


--
-- Data for Name: project_developers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.project_developers (project_id, developer_id) FROM stdin;
1	3
2	3
1	1
\.


--
-- Data for Name: projects; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.projects (id, name, type, repo_url, client_id, created_at, updated_at) FROM stdin;
1	hrc neo4j transfer	EXTERNAL		1	2026-04-01 17:13:08.653364	2026-04-01 17:13:08.653364
2	vibe coding lab	INTERNAL		\N	2026-04-01 17:45:12.963085	2026-04-01 17:45:12.963085
\.


--
-- Data for Name: time_entries; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.time_entries (id, developer_id, project_id, type, description, start_time, end_time, created_at, updated_at, recurrence_group_id, recurrence_rule) FROM stdin;
1	3	1	WORK	changing the oracle db to neo4j	2026-03-30 07:00:00	2026-03-30 15:00:00	2026-04-01 17:13:23.948271	2026-04-01 17:17:02.44483	\N	\N
5	3	1	WORK	starting the project	2026-03-25 08:00:00	2026-03-25 09:00:00	2026-04-01 17:43:10.863688	2026-04-01 17:43:10.863688	\N	\N
6	3	2	WORK	supervising Albin	2025-12-22 08:00:00	2025-12-22 08:30:00	2026-04-01 17:47:55.109852	2026-04-01 17:47:55.109852	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
7	3	2	WORK	supervising Albin	2025-12-29 08:00:00	2025-12-29 08:30:00	2026-04-01 17:47:55.119328	2026-04-01 17:47:55.119328	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
8	3	2	WORK	supervising Albin	2026-01-05 08:00:00	2026-01-05 08:30:00	2026-04-01 17:47:55.120201	2026-04-01 17:47:55.120201	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
9	3	2	WORK	supervising Albin	2026-01-12 08:00:00	2026-01-12 08:30:00	2026-04-01 17:47:55.120695	2026-04-01 17:47:55.120695	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
10	3	2	WORK	supervising Albin	2026-01-19 08:00:00	2026-01-19 08:30:00	2026-04-01 17:47:55.121085	2026-04-01 17:47:55.121085	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
11	3	2	WORK	supervising Albin	2026-01-26 08:00:00	2026-01-26 08:30:00	2026-04-01 17:47:55.121457	2026-04-01 17:47:55.121457	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
12	3	2	WORK	supervising Albin	2026-02-02 08:00:00	2026-02-02 08:30:00	2026-04-01 17:47:55.121792	2026-04-01 17:47:55.121792	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
13	3	2	WORK	supervising Albin	2026-02-09 08:00:00	2026-02-09 08:30:00	2026-04-01 17:47:55.12211	2026-04-01 17:47:55.12211	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
14	3	2	WORK	supervising Albin	2026-02-16 08:00:00	2026-02-16 08:30:00	2026-04-01 17:47:55.122409	2026-04-01 17:47:55.122409	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
15	3	2	WORK	supervising Albin	2026-02-23 08:00:00	2026-02-23 08:30:00	2026-04-01 17:47:55.122692	2026-04-01 17:47:55.122692	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
16	3	2	WORK	supervising Albin	2026-03-02 08:00:00	2026-03-02 08:30:00	2026-04-01 17:47:55.123	2026-04-01 17:47:55.123	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
17	3	2	WORK	supervising Albin	2026-03-09 08:00:00	2026-03-09 08:30:00	2026-04-01 17:47:55.123289	2026-04-01 17:47:55.123289	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
18	3	2	WORK	supervising Albin	2026-03-16 08:00:00	2026-03-16 08:30:00	2026-04-01 17:47:55.123567	2026-04-01 17:47:55.123567	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
19	3	2	WORK	supervising Albin	2026-03-23 08:00:00	2026-03-23 08:30:00	2026-04-01 17:47:55.123857	2026-04-01 17:47:55.123857	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
20	3	2	WORK	supervising Albin	2026-03-30 08:00:00	2026-03-30 08:30:00	2026-04-01 17:47:55.124224	2026-04-01 17:47:55.124224	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
21	3	2	WORK	supervising Albin	2026-04-06 08:00:00	2026-04-06 08:30:00	2026-04-01 17:47:55.124537	2026-04-01 17:47:55.124537	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
22	3	2	WORK	supervising Albin	2026-04-13 08:00:00	2026-04-13 08:30:00	2026-04-01 17:47:55.124893	2026-04-01 17:47:55.124893	31a00f73-62c2-4dca-b5cd-5ca59d8f5179	WEEKLY;UNTIL=2026-04-16
23	1	1	WORK	testing and speed requirement	2026-04-01 07:00:00	2026-04-01 15:00:00	2026-04-01 17:49:41.01981	2026-04-01 17:53:29.709731	\N	\N
\.


--
-- Name: clients_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.clients_id_seq', 2, true);


--
-- Name: developers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.developers_id_seq', 3, true);


--
-- Name: projects_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.projects_id_seq', 2, true);


--
-- Name: time_entries_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.time_entries_id_seq', 25, true);


--
-- Name: clients clients_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_pkey PRIMARY KEY (id);


--
-- Name: developers developers_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.developers
    ADD CONSTRAINT developers_email_key UNIQUE (email);


--
-- Name: developers developers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.developers
    ADD CONSTRAINT developers_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: project_developers project_developers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_developers
    ADD CONSTRAINT project_developers_pkey PRIMARY KEY (project_id, developer_id);


--
-- Name: projects projects_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.projects
    ADD CONSTRAINT projects_pkey PRIMARY KEY (id);


--
-- Name: time_entries time_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.time_entries
    ADD CONSTRAINT time_entries_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_developers_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_developers_email ON public.developers USING btree (email);


--
-- Name: idx_developers_role; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_developers_role ON public.developers USING btree (role);


--
-- Name: idx_projects_client_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_projects_client_id ON public.projects USING btree (client_id);


--
-- Name: idx_projects_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_projects_type ON public.projects USING btree (type);


--
-- Name: idx_time_entries_developer_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_time_entries_developer_id ON public.time_entries USING btree (developer_id);


--
-- Name: idx_time_entries_project_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_time_entries_project_id ON public.time_entries USING btree (project_id);


--
-- Name: idx_time_entries_recurrence_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_time_entries_recurrence_group ON public.time_entries USING btree (recurrence_group_id);


--
-- Name: idx_time_entries_start_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_time_entries_start_time ON public.time_entries USING btree (start_time);


--
-- Name: idx_time_entries_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_time_entries_type ON public.time_entries USING btree (type);


--
-- Name: project_developers project_developers_developer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_developers
    ADD CONSTRAINT project_developers_developer_id_fkey FOREIGN KEY (developer_id) REFERENCES public.developers(id) ON DELETE CASCADE;


--
-- Name: project_developers project_developers_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.project_developers
    ADD CONSTRAINT project_developers_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.projects(id) ON DELETE CASCADE;


--
-- Name: projects projects_client_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.projects
    ADD CONSTRAINT projects_client_id_fkey FOREIGN KEY (client_id) REFERENCES public.clients(id) ON DELETE SET NULL;


--
-- Name: time_entries time_entries_developer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.time_entries
    ADD CONSTRAINT time_entries_developer_id_fkey FOREIGN KEY (developer_id) REFERENCES public.developers(id) ON DELETE CASCADE;


--
-- Name: time_entries time_entries_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.time_entries
    ADD CONSTRAINT time_entries_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.projects(id) ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--

