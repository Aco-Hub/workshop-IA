import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import { BrowserDynamicTestingModule, platformBrowserDynamicTesting } from '@angular/platform-browser-dynamic/testing';
import { WebSocketService } from './websocket.service';
import { AuthService } from './auth.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting());
});

describe('WebSocketService', () => {
  let service: WebSocketService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        {
          provide: AuthService,
          useValue: {
            getToken: () => 'mock-jwt-token',
            currentUser: vi.fn(() => ({ id: 1, username: 'TestUser' })),
            isAuthenticated: vi.fn(() => true),
          },
        },
      ],
    });
    service = TestBed.inject(WebSocketService);
  });

  it('is injectable', () => {
    expect(service).toBeTruthy();
  });

  it('disconnect does not throw when no connection exists', () => {
    expect(() => service.disconnect()).not.toThrow();
  });

  it('double disconnect does not throw', () => {
    expect(() => {
      service.disconnect();
      service.disconnect();
    }).not.toThrow();
  });

  it('sendCursorPosition does not throw when not connected', () => {
    expect(() => {
      service.sendCursorPosition(1, { userId: 1, username: 'Test', color: '#000', x: 0, y: 0 });
    }).not.toThrow();
  });

  it('sendElementCreate does not throw when not connected', () => {
    expect(() => {
      service.sendElementCreate(1, { type: 'PEN', data: '{}', color: '#000', strokeWidth: 2, zIndex: 0 });
    }).not.toThrow();
  });

  it('sendElementDelete does not throw when not connected', () => {
    expect(() => {
      service.sendElementDelete(1, 42);
    }).not.toThrow();
  });

  it('subscribeToCursors returns an Observable when no client exists', () => {
    const obs = service.subscribeToCursors(1);
    expect(obs).toBeTruthy();
    expect(typeof obs.subscribe).toBe('function');
  });

  it('subscribeToElements returns an Observable when no client exists', () => {
    const obs = service.subscribeToElements(1);
    expect(obs).toBeTruthy();
    expect(typeof obs.subscribe).toBe('function');
  });
});
