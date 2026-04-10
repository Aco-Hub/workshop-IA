import { beforeAll, vi } from 'vitest';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { getTestBed } from '@angular/core/testing';
import { BrowserDynamicTestingModule, platformBrowserDynamicTesting } from '@angular/platform-browser-dynamic/testing';
import { WhiteboardToolbarComponent } from './whiteboard-toolbar';
import { I18nService } from '../../../core/services/i18n.service';

beforeAll(() => {
  getTestBed().initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting());
});

describe('WhiteboardToolbarComponent', () => {
  const user = userEvent.setup();

  async function renderToolbar(activeTool = 'pen', activeColor = '#000000') {
    return render(WhiteboardToolbarComponent, {
      inputs: { activeTool, activeColor },
      providers: [I18nService],
    });
  }

  afterEach(() => {
    localStorage.clear();
  });

  it('renders all 8 tool buttons', async () => {
    await renderToolbar();
    expect(screen.getByTestId('tool-select')).toBeTruthy();
    expect(screen.getByTestId('tool-shape')).toBeTruthy();
    expect(screen.getByTestId('tool-text')).toBeTruthy();
    expect(screen.getByTestId('tool-pen')).toBeTruthy();
    expect(screen.getByTestId('tool-image')).toBeTruthy();
    expect(screen.getByTestId('tool-link')).toBeTruthy();
    expect(screen.getByTestId('tool-comment')).toBeTruthy();
    expect(screen.getByTestId('tool-more')).toBeTruthy();
  });

  it('clicking select button emits toolChanged with "select"', async () => {
    const onToolChanged = vi.fn();
    await render(WhiteboardToolbarComponent, {
      inputs: { activeTool: 'pen', activeColor: '#000000' },
      on: { toolChanged: onToolChanged },
      providers: [I18nService],
    });

    await user.click(screen.getByTestId('tool-select'));

    expect(onToolChanged).toHaveBeenCalledWith('select');
  });

  it('clicking shape button shows shape submenu', async () => {
    const { fixture } = await renderToolbar();

    expect(screen.queryByTestId('shape-rect')).toBeNull();
    expect(screen.queryByTestId('shape-ellipse')).toBeNull();
    expect(screen.queryByTestId('shape-line')).toBeNull();

    await user.click(screen.getByTestId('tool-shape'));
    fixture.detectChanges();

    expect(screen.getByTestId('shape-rect')).toBeTruthy();
    expect(screen.getByTestId('shape-ellipse')).toBeTruthy();
    expect(screen.getByTestId('shape-line')).toBeTruthy();
  });

  it('clicking comment button emits commentsToggled', async () => {
    const onCommentsToggled = vi.fn();
    await render(WhiteboardToolbarComponent, {
      inputs: { activeTool: 'pen', activeColor: '#000000' },
      on: { commentsToggled: onCommentsToggled },
      providers: [I18nService],
    });

    await user.click(screen.getByTestId('tool-comment'));

    expect(onCommentsToggled).toHaveBeenCalled();
  });

  it('clicking more button shows dropdown with eraser and color options', async () => {
    const { fixture } = await renderToolbar();

    expect(screen.queryByTestId('more-eraser')).toBeNull();

    await user.click(screen.getByTestId('tool-more'));
    fixture.detectChanges();

    expect(screen.getByTestId('more-eraser')).toBeTruthy();
  });

  it('clicking image button emits imageRequested', async () => {
    const onImageRequested = vi.fn();
    await render(WhiteboardToolbarComponent, {
      inputs: { activeTool: 'pen', activeColor: '#000000' },
      on: { imageRequested: onImageRequested },
      providers: [I18nService],
    });

    await user.click(screen.getByTestId('tool-image'));

    expect(onImageRequested).toHaveBeenCalled();
  });

  it('clicking a shape option in submenu emits toolChanged with the shape type', async () => {
    const onToolChanged = vi.fn();
    const { fixture } = await render(WhiteboardToolbarComponent, {
      inputs: { activeTool: 'pen', activeColor: '#000000' },
      on: { toolChanged: onToolChanged },
      providers: [I18nService],
    });

    await user.click(screen.getByTestId('tool-shape'));
    fixture.detectChanges();

    await user.click(screen.getByTestId('shape-rect'));

    expect(onToolChanged).toHaveBeenCalledWith('rectangle');
  });

  it('clicking eraser in more menu emits toolChanged with "eraser"', async () => {
    const onToolChanged = vi.fn();
    const { fixture } = await render(WhiteboardToolbarComponent, {
      inputs: { activeTool: 'pen', activeColor: '#000000' },
      on: { toolChanged: onToolChanged },
      providers: [I18nService],
    });

    await user.click(screen.getByTestId('tool-more'));
    fixture.detectChanges();

    await user.click(screen.getByTestId('more-eraser'));

    expect(onToolChanged).toHaveBeenCalledWith('eraser');
  });

  it('clicking link button emits linkRequested', async () => {
    const onLinkRequested = vi.fn();
    await render(WhiteboardToolbarComponent, {
      inputs: { activeTool: 'pen', activeColor: '#000000' },
      on: { linkRequested: onLinkRequested },
      providers: [I18nService],
    });

    await user.click(screen.getByTestId('tool-link'));

    expect(onLinkRequested).toHaveBeenCalled();
  });
});
