import { beforeAll, vi } from 'vitest';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { getTestBed } from '@angular/core/testing';
import { BrowserDynamicTestingModule, platformBrowserDynamicTesting } from '@angular/platform-browser-dynamic/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { WhiteboardCommentsComponent } from './whiteboard-comments';
import { WhiteboardComment } from '../../../core/models/whiteboard.model';
import { AuthService } from '../../../core/services/auth.service';
import { I18nService } from '../../../core/services/i18n.service';
import { Developer } from '../../../core/models/developer.model';

beforeAll(() => {
  getTestBed().initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting());
});

const mockDeveloper: Developer = {
  id: 1,
  email: 'alice@example.com',
  username: 'alice',
  title: 'Frontend Dev',
  discordLink: '',
  discordAvatarUrl: '',
  role: 'STANDARD',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
};

const mockComments: WhiteboardComment[] = [
  {
    id: 1,
    developer: { id: 1, username: 'alice' },
    text: 'First comment',
    parentId: null,
    createdAt: '2024-06-01T10:00:00Z',
  },
  {
    id: 2,
    developer: { id: 2, username: 'bob' },
    text: 'Second comment',
    parentId: null,
    createdAt: '2024-06-02T11:00:00Z',
  },
];

function mockAuthProvider(user: Developer | null = mockDeveloper) {
  return {
    provide: AuthService,
    useValue: {
      currentUser: signal(user),
      isAuthenticated: signal(user !== null),
    },
  };
}

function baseProviders(user: Developer | null = mockDeveloper) {
  return [
    provideHttpClient(),
    provideHttpClientTesting(),
    I18nService,
    mockAuthProvider(user),
  ];
}

describe('WhiteboardCommentsComponent', () => {
  const user = userEvent.setup();

  afterEach(() => {
    localStorage.clear();
  });

  it('renders comments list with author names', async () => {
    await render(WhiteboardCommentsComponent, {
      inputs: { comments: mockComments },
      providers: baseProviders(),
    });

    expect(screen.getByText('alice')).toBeTruthy();
    expect(screen.getByText('bob')).toBeTruthy();
    expect(screen.getByText('First comment')).toBeTruthy();
    expect(screen.getByText('Second comment')).toBeTruthy();
  });

  it('shows empty state when no comments are provided', async () => {
    await render(WhiteboardCommentsComponent, {
      inputs: { comments: [] },
      providers: baseProviders(),
    });

    const panel = screen.getByRole('complementary');
    expect(panel.textContent).not.toContain('alice');
    expect(panel.textContent).not.toContain('bob');
  });

  it('typing in input and pressing Enter emits commentAdded with the text', async () => {
    const onCommentAdded = vi.fn();
    await render(WhiteboardCommentsComponent, {
      inputs: { comments: [] },
      on: { commentAdded: onCommentAdded },
      providers: baseProviders(),
    });

    const input = screen.getByTestId('wb-comment-input');
    await user.type(input, 'Hello world');
    await user.keyboard('{Enter}');

    expect(onCommentAdded).toHaveBeenCalledWith('Hello world');
  });

  it('send button is disabled when input is empty', async () => {
    await render(WhiteboardCommentsComponent, {
      inputs: { comments: [] },
      providers: baseProviders(),
    });

    const sendBtn = screen.getByTestId('wb-comment-send') as HTMLButtonElement;
    expect(sendBtn.disabled).toBe(true);
  });

  it('send button becomes enabled after typing text', async () => {
    const { fixture } = await render(WhiteboardCommentsComponent, {
      inputs: { comments: [] },
      providers: baseProviders(),
    });

    const input = screen.getByTestId('wb-comment-input');
    await user.type(input, 'Some text');
    fixture.detectChanges();

    const sendBtn = screen.getByTestId('wb-comment-send') as HTMLButtonElement;
    expect(sendBtn.disabled).toBe(false);
  });

  it('clicking send button emits commentAdded with the input text', async () => {
    const onCommentAdded = vi.fn();
    const { fixture } = await render(WhiteboardCommentsComponent, {
      inputs: { comments: [] },
      on: { commentAdded: onCommentAdded },
      providers: baseProviders(),
    });

    const input = screen.getByTestId('wb-comment-input');
    await user.type(input, 'My new comment');
    fixture.detectChanges();

    await user.click(screen.getByTestId('wb-comment-send'));

    expect(onCommentAdded).toHaveBeenCalledWith('My new comment');
  });

  it('input is cleared after sending a comment', async () => {
    const { fixture } = await render(WhiteboardCommentsComponent, {
      inputs: { comments: [] },
      providers: baseProviders(),
    });

    const input = screen.getByTestId('wb-comment-input') as HTMLInputElement;
    await user.type(input, 'Temporary comment');
    fixture.detectChanges();

    await user.click(screen.getByTestId('wb-comment-send'));
    fixture.detectChanges();

    expect(input.value).toBe('');
  });

  it('filter dropdown exists with All and Mine options', async () => {
    await render(WhiteboardCommentsComponent, {
      inputs: { comments: [] },
      providers: baseProviders(),
    });

    const filterSelect = screen.getByTestId('wb-comments-filter') as HTMLSelectElement;
    expect(filterSelect).toBeTruthy();

    const options = Array.from(filterSelect.options).map(o => o.value);
    expect(options).toContain('all');
    expect(options).toContain('mine');
  });

  it('close button emits panelClosed', async () => {
    const onPanelClosed = vi.fn();
    await render(WhiteboardCommentsComponent, {
      inputs: { comments: [] },
      on: { panelClosed: onPanelClosed },
      providers: baseProviders(),
    });

    await user.click(screen.getByTestId('wb-comments-close'));

    expect(onPanelClosed).toHaveBeenCalled();
  });

  it('filtering by Mine shows only current user comments', async () => {
    const currentUser: Developer = { ...mockDeveloper, id: 1 };
    const { fixture } = await render(WhiteboardCommentsComponent, {
      inputs: { comments: mockComments },
      providers: baseProviders(currentUser),
    });

    const filterSelect = screen.getByTestId('wb-comments-filter') as HTMLSelectElement;
    await user.selectOptions(filterSelect, 'mine');
    fixture.detectChanges();

    expect(screen.getByText('First comment')).toBeTruthy();
    expect(screen.queryByText('Second comment')).toBeNull();
  });
});
