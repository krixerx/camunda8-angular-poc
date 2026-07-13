# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current State

**Spec phase — no implementation code yet.** This repo is a learning POC: Camunda 8.9 + Spring Boot 4 middleware + Angular 22 frontend, AI-friendly spec-first monorepo, dockerized at the end. It mirrors the architecture of the sibling POC `C:\Users\kriks\git\cib7-react-poc` (CIB seven + React).

Read in this order:

1. `openspec/changes/bootstrap-poc/` — proposal, design, capability specs, and the implementation task checklist (`tasks.md`). This is the active change; implement tasks in order and tick them off.
2. `docs/architecture.md` — topology, ports, conventions (kebab-case ids, camelCase variables, API v2 only, Camunda user tasks + linked forms, `bindingType="deployment"` for DMN).
3. `docs/business/services/<service>/` — source of truth for process content (flows, fields, decision tables). Never let BPMN/DMN/form files drift from this markdown.

## Workflow

- Spec-first via OpenSpec: propose (`/opsx:propose`) → specs → apply (`/opsx:apply`) → archive. Don't write implementation code for behavior that has no spec.
- Key constraint: local default JDK is 11 — backend work needs JDK 21+ (Spring Boot 4). Use `backend/mvnw`, not the global Maven.

Regenerate this file with real build/run/test commands once implementation lands (tracked in `openspec/changes/bootstrap-poc/tasks.md` 6.4).
