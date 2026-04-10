import { beforeAll, vi } from 'vitest';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { getTestBed } from '@angular/core/testing';
import { BrowserDynamicTestingModule, platformBrowserDynamicTesting } from '@angular/platform-browser-dynamic/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { WhiteboardComponent } from './whiteboard';
import { WhiteboardService } from '../../core/services/whiteboard.service';
import { AuthService } from '../../core/services/auth.service';
import { I18nService } from '../../core/services/i18n.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting());
});

const mockRooms = [
  {
    id: 1,
    name: 'Design Board',
    createdBy: { id: 1, username: 'Alice', discordAvatarUrl: null },
    createdAt: '2026-04-01T10:00:00',
    updatedAt: '2026-04-01T12:00:00',
  },
  {
    id: 2,
    name: 'Sprint Planning',
    createdBy: { id: 2, username: 'Bob', discordAvatarUrl: null },
    createdAt: '2026-04-02T10:00:00',
    updatedAt: '2026-04-02T12:00:00',
  },
];

const mockWhiteboardService = {
  getRooms: vi.fn(() => of(mockRooms)),
  createRoom: vi.fn((name: string) => of({ id: 3, name, createdBy: { id: 1, username: 'Alice', discordAvatarUrl: null }, createdAt: '2026-04-03T10:00:00', updatedAt: '2026-04-03T10:00:00' })),
};

const mockAuthService = {
  currentUser: vi.fn(() => ({ id: 1, username: 'Alice', role: 'ADMIN' })),
  isAuthenticated: vi.fn(() => true),
  getToken: vi.fn(() => 'mock-token'),
};

describe('WhiteboardComponent', () => {
  const user = userEvent.setup();

  async function renderWhiteboard() {
    return render(WhiteboardComponent, {
      providers: [
        provideRouter([]),
        { provide: WhiteboardService, useValue: mockWhiteboardService },
        { provide: AuthService, useValue: mockAuthService },
        I18nService,
      ],
    });
  }

  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('renders room list with room names', async () => {
    await renderWhiteboard();
    expect(screen.getByText('Design Board')).toBeTruthy();
    expect(screen.getByText('Sprint Planning')).toBeTruthy();
  });

  it('calls getRooms on init', async () => {
    await renderWhiteboard();
    expect(mockWhiteboardService.getRooms).toHaveBeenCalled();
  });

  it('shows create button', async () => {
    await renderWhiteboard();
    expect(screen.getByTestId('create-whiteboard-btn')).toBeTruthy();
  });
});
