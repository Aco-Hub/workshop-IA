import { beforeAll, vi } from 'vitest';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { getTestBed } from '@angular/core/testing';
import { BrowserDynamicTestingModule, platformBrowserDynamicTesting } from '@angular/platform-browser-dynamic/testing';
import { WhiteboardZoomComponent } from './whiteboard-zoom';
import { I18nService } from '../../../core/services/i18n.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting());
});

describe('WhiteboardZoomComponent', () => {
  const user = userEvent.setup();

  async function renderZoom(zoomLevel = 1) {
    return render(WhiteboardZoomComponent, {
      inputs: { zoomLevel },
      providers: [I18nService],
    });
  }

  afterEach(() => {
    localStorage.clear();
  });

  it('displays zoom percentage from input when zoomLevel is 1', async () => {
    await renderZoom(1);
    expect(screen.getByTestId('zoom-level').textContent?.trim()).toBe('100%');
  });

  it('displays zoom percentage from input when zoomLevel is 0.5', async () => {
    await renderZoom(0.5);
    expect(screen.getByTestId('zoom-level').textContent?.trim()).toBe('50%');
  });

  it('displays zoom percentage from input when zoomLevel is 1.5', async () => {
    await renderZoom(1.5);
    expect(screen.getByTestId('zoom-level').textContent?.trim()).toBe('150%');
  });

  it('clicking + button emits zoomIn', async () => {
    const onZoomIn = vi.fn();
    await render(WhiteboardZoomComponent, {
      inputs: { zoomLevel: 1 },
      on: { zoomIn: onZoomIn },
      providers: [I18nService],
    });

    await user.click(screen.getByTestId('zoom-in'));

    expect(onZoomIn).toHaveBeenCalled();
  });

  it('clicking - button emits zoomOut', async () => {
    const onZoomOut = vi.fn();
    await render(WhiteboardZoomComponent, {
      inputs: { zoomLevel: 1 },
      on: { zoomOut: onZoomOut },
      providers: [I18nService],
    });

    await user.click(screen.getByTestId('zoom-out'));

    expect(onZoomOut).toHaveBeenCalled();
  });

  it('clicking percentage button emits zoomReset', async () => {
    const onZoomReset = vi.fn();
    await render(WhiteboardZoomComponent, {
      inputs: { zoomLevel: 1 },
      on: { zoomReset: onZoomReset },
      providers: [I18nService],
    });

    await user.click(screen.getByTestId('zoom-level'));

    expect(onZoomReset).toHaveBeenCalled();
  });

  it('rounds fractional zoom levels to nearest integer percent', async () => {
    await renderZoom(1.337);
    expect(screen.getByTestId('zoom-level').textContent?.trim()).toBe('134%');
  });
});
