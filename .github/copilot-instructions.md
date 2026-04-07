All changes and new features must be tested including scripts: run them yourself and iterate until they produce the expected output.

## Architecture Decisions

- DTO mappers use **MapStruct** with decorator classes when needed
- Follow **SOLID principles** across the backend
- Backend returns **DTOs**, never entities — mappers live in the controller layer
- Use **semantic HTML** (`<nav>`, `<main>`, `<button>`) over generic `<div>` tags
- **Mobile-first CSS**: design at 375px, then scale to desktop
- Group TypeScript class members with category comments (`// Methods`, `// Private methods`) — first letter capitalized only
- SCSS: parent selectors (`&.modifier`) placed before child selectors (`.child`)

## Testing (UI) — Testing Trophy

**Principle**: test from the user's perspective, never test implementation details (Kent C. Dodds).

- **Stack**: Vitest + `@testing-library/angular` (`render`/`screen`/`userEvent`) + Playwright for E2E
- **Components**: use `render()` + `screen.getByRole/getByLabelText/getByTestId` + `userEvent` — never `fixture.nativeElement.querySelector()` or direct signal access
- **Services**: TestBed + `HttpTestingController` for API contract testing
- **E2E**: Playwright for critical flows (login → calendar → CRUD) in `frontend/e2e/`
- **Anti-patterns**: no `component.signal.set()`, no `vi.spyOn()` in component tests, no `expect(component.loading())` — assert visible DOM, not internal state
