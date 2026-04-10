import { beforeAll, vi } from 'vitest';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';
import { getTestBed } from '@angular/core/testing';
import { BrowserDynamicTestingModule, platformBrowserDynamicTesting } from '@angular/platform-browser-dynamic/testing';
import { WhiteboardBottomBarComponent } from './whiteboard-bottom-bar';

beforeAll(() => {
  getTestBed().initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting());
});

describe('WhiteboardBottomBarComponent', () => {
  const user = userEvent.setup();

  afterEach(() => {
    localStorage.clear();
  });

  it('shows fill mode buttons when shape tool is active', async () => {
    await render(WhiteboardBottomBarComponent, {
      inputs: { activeTool: 'rectangle', activeColor: '#000000' },
    });
    expect(screen.getByTestId('fill-solid')).toBeTruthy();
    expect(screen.getByTestId('fill-stroke')).toBeTruthy();
    expect(screen.getByTestId('fill-pattern')).toBeTruthy();
    expect(screen.getByTestId('fill-gradient')).toBeTruthy();
  });

  it('shows stroke width slider for shape tools', async () => {
    await render(WhiteboardBottomBarComponent, {
      inputs: { activeTool: 'rectangle', activeColor: '#000000' },
    });
    expect(screen.getByTestId('stroke-width-slider')).toBeTruthy();
  });

  it('shows stroke width slider for pen tool', async () => {
    await render(WhiteboardBottomBarComponent, {
      inputs: { activeTool: 'pen', activeColor: '#000000' },
    });
    expect(screen.getByTestId('pen-stroke-width-slider')).toBeTruthy();
  });

  it('shows font controls when text tool is active', async () => {
    await render(WhiteboardBottomBarComponent, {
      inputs: { activeTool: 'text', activeColor: '#000000' },
    });
    expect(screen.getByTestId('font-family-select')).toBeTruthy();
    expect(screen.getByTestId('font-size-select')).toBeTruthy();
    expect(screen.getByTestId('text-bold')).toBeTruthy();
    expect(screen.getByTestId('text-italic')).toBeTruthy();
  });

  it('does not show bottom bar for select tool', async () => {
    await render(WhiteboardBottomBarComponent, {
      inputs: { activeTool: 'select', activeColor: '#000000' },
    });
    expect(screen.queryByTestId('fill-solid')).toBeNull();
    expect(screen.queryByTestId('font-family-select')).toBeNull();
    expect(screen.queryByTestId('pen-stroke-width-slider')).toBeNull();
  });

  it('clicking fill mode button emits fillModeChanged', async () => {
    const onFillModeChanged = vi.fn();
    await render(WhiteboardBottomBarComponent, {
      inputs: { activeTool: 'rectangle', activeColor: '#000000' },
      on: { fillModeChanged: onFillModeChanged },
    });

    await user.click(screen.getByTestId('fill-solid'));
    expect(onFillModeChanged).toHaveBeenCalledWith('solid');
  });

  it('changing font size emits fontSizeChanged', async () => {
    const onFontSizeChanged = vi.fn();
    await render(WhiteboardBottomBarComponent, {
      inputs: { activeTool: 'text', activeColor: '#000000' },
      on: { fontSizeChanged: onFontSizeChanged },
    });

    await user.selectOptions(screen.getByTestId('font-size-select'), '24');
    expect(onFontSizeChanged).toHaveBeenCalledWith(24);
  });

  it('changing font family emits fontFamilyChanged', async () => {
    const onFontFamilyChanged = vi.fn();
    await render(WhiteboardBottomBarComponent, {
      inputs: { activeTool: 'text', activeColor: '#000000' },
      on: { fontFamilyChanged: onFontFamilyChanged },
    });

    await user.selectOptions(screen.getByTestId('font-family-select'), 'Arial');
    expect(onFontFamilyChanged).toHaveBeenCalledWith('Arial');
  });
});
