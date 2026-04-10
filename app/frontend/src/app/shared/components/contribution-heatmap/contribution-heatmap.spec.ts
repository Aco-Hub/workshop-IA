import { beforeAll, describe, it, expect, afterEach } from 'vitest';
import { TestBed, getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { render, screen } from '@testing-library/angular';
import userEvent from '@testing-library/user-event';

import { ContributionHeatmapComponent } from './contribution-heatmap';
import { TimeEntry } from '../../../core/models/time-entry.model';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

function makeEntry(
  date: string,
  startHour: number,
  endHour: number,
  id: number,
  devId = 1,
  devName = 'Alice',
): TimeEntry {
  const pad = (n: number) => n.toString().padStart(2, '0');
  return {
    id,
    developerId: devId,
    developerUsername: devName,
    projectId: 1,
    projectName: 'P',
    type: 'WORK',
    description: '',
    startTime: `${date}T${pad(startHour)}:00:00`,
    endTime: `${date}T${pad(endHour)}:00:00`,
    recurrenceGroupId: null,
    recurrenceRule: null,
    createdAt: '',
    updatedAt: '',
  };
}

describe('ContributionHeatmapComponent', () => {

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('should render the heatmap wrapper', async () => {
    await render(ContributionHeatmapComponent, {
      componentInputs: { entries: [] },
    });
    expect(screen.getByTestId('contribution-heatmap')).toBeTruthy();
  });

  it('should show empty state when no entries', async () => {
    await render(ContributionHeatmapComponent, {
      componentInputs: { entries: [] },
    });
    expect(screen.queryAllByTestId('heatmap-dev-row').length).toBe(0);
  });

  it('should render one row per developer', async () => {
    const entries = [
      makeEntry('2026-04-09', 9, 12, 1, 1, 'Alice'),
      makeEntry('2026-04-09', 9, 12, 2, 2, 'Bob'),
      makeEntry('2026-04-10', 9, 11, 3, 1, 'Alice'),
    ];
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries },
    });
    fixture.componentInstance.year.set(2026);
    fixture.detectChanges();

    const rows = screen.getAllByTestId('heatmap-dev-row');
    expect(rows.length).toBe(2);
  });

  it('should render 365 cells per developer for a non-leap year in day view', async () => {
    const entries = [makeEntry('2025-06-15', 9, 17, 1, 1, 'Alice')];
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries },
    });
    fixture.componentInstance.year.set(2025);
    fixture.detectChanges();

    const devRows = fixture.componentInstance.devRows();
    expect(devRows.length).toBe(1);
    expect(devRows[0].cells.length).toBe(365);
  });

  it('should render 366 cells per developer for a leap year in day view', async () => {
    const entries = [makeEntry('2024-06-15', 9, 17, 1, 1, 'Alice')];
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries },
    });
    fixture.componentInstance.year.set(2024);
    fixture.detectChanges();

    const devRows = fixture.componentInstance.devRows();
    expect(devRows[0].cells.length).toBe(366);
  });

  it('should place Jan 1 2025 (Wednesday) at row 3 in day view', async () => {
    const entries = [makeEntry('2025-01-01', 9, 17, 1)];
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries },
    });
    fixture.componentInstance.year.set(2025);
    fixture.detectChanges();

    const cells = fixture.componentInstance.devRows()[0].cells;
    const jan1 = cells.find(c => c.key === '2025-01-01');
    expect(jan1?.row).toBe(3);
    expect(jan1?.col).toBe(1);
  });

  it('should aggregate hours per developer per day', async () => {
    const entries = [
      makeEntry('2026-04-09', 9, 12, 1, 1, 'Alice'),
      makeEntry('2026-04-09', 14, 16, 2, 1, 'Alice'),
      makeEntry('2026-04-09', 9, 11, 3, 2, 'Bob'),
    ];
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries },
    });
    fixture.componentInstance.year.set(2026);
    fixture.detectChanges();

    const perDev = fixture.componentInstance.perDevDailyTotals();
    expect(perDev.get(1)?.get('2026-04-09')).toBeCloseTo(5); // Alice: 3h + 2h
    expect(perDev.get(2)?.get('2026-04-09')).toBeCloseTo(2); // Bob: 2h
  });

  it('should assign correct intensity levels per developer', async () => {
    const entries = [
      makeEntry('2026-01-05', 9, 11, 1, 1, 'Alice'),  // 2h
      makeEntry('2026-01-06', 9, 14, 2, 1, 'Alice'),  // 5h
      makeEntry('2026-01-07', 9, 17, 3, 1, 'Alice'),  // 8h
    ];
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries },
    });
    fixture.componentInstance.year.set(2026);
    fixture.detectChanges();

    const cells = fixture.componentInstance.devRows()[0].cells;
    const c2h = cells.find(c => c.key === '2026-01-05');
    const c5h = cells.find(c => c.key === '2026-01-06');
    const c8h = cells.find(c => c.key === '2026-01-07');

    expect(c2h?.level).toBe(1);
    expect(c5h?.level).toBe(2);
    expect(c8h?.level).toBe(3);
  });

  it('should use developer color for cell backgrounds', async () => {
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries: [makeEntry('2026-06-15', 9, 17, 1, 1, 'Alice')] },
    });
    fixture.componentInstance.year.set(2026);
    fixture.detectChanges();

    const devRow = fixture.componentInstance.devRows()[0];
    expect(devRow.color).toBeTruthy();
    expect(devRow.color.startsWith('#')).toBe(true);

    // Level 3 cell should return full opacity color
    const bg = fixture.componentInstance.getCellBackground(devRow.color, 3);
    expect(bg).toContain('rgba');
    expect(bg).toContain(', 1)');

    // Level 0 should return the theme empty color
    const bg0 = fixture.componentInstance.getCellBackground(devRow.color, 0);
    expect(bg0).toContain('var(--heatmap-level-0)');
  });

  it('should switch to week view and show ~53 cells per dev', async () => {
    const user = userEvent.setup();
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries: [makeEntry('2026-06-15', 9, 17, 1)] },
    });
    fixture.componentInstance.year.set(2026);
    fixture.detectChanges();

    await user.click(screen.getByTestId('heatmap-view-week'));

    const devRows = fixture.componentInstance.devRows();
    expect(devRows[0].cells.length).toBeGreaterThanOrEqual(52);
    expect(devRows[0].cells.length).toBeLessThanOrEqual(54);
  });

  it('should switch to month view and show 12 cells per dev', async () => {
    const user = userEvent.setup();
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries: [makeEntry('2026-06-15', 9, 17, 1)] },
    });
    fixture.componentInstance.year.set(2026);
    fixture.detectChanges();

    await user.click(screen.getByTestId('heatmap-view-month'));

    const devRows = fixture.componentInstance.devRows();
    expect(devRows[0].cells.length).toBe(12);
  });

  it('should navigate year back and forward', async () => {
    const user = userEvent.setup();
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries: [] },
    });

    const currentYear = fixture.componentInstance.year();

    await user.click(screen.getByTestId('heatmap-year-back'));
    expect(screen.getByTestId('heatmap-year').textContent?.trim()).toBe(String(currentYear - 1));

    await user.click(screen.getByTestId('heatmap-year-forward'));
    expect(screen.getByTestId('heatmap-year').textContent?.trim()).toBe(String(currentYear));
  });

  it('should filter entries by selected year', async () => {
    const entries = [
      makeEntry('2025-06-15', 9, 17, 1, 1, 'Alice'),
      makeEntry('2026-06-15', 9, 17, 2, 1, 'Alice'),
    ];
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries },
    });
    fixture.componentInstance.year.set(2026);
    fixture.detectChanges();

    const perDev = fixture.componentInstance.perDevDailyTotals();
    const aliceTotals = perDev.get(1)!;
    expect(aliceTotals.has('2025-06-15')).toBe(false);
    expect(aliceTotals.has('2026-06-15')).toBe(true);
  });

  it('should compute correct month labels', async () => {
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries: [] },
    });
    fixture.componentInstance.year.set(2026);
    fixture.detectChanges();

    const labels = fixture.componentInstance.monthLabels();
    expect(labels.length).toBe(12);
    expect(labels[0].month).toBe(0);
    expect(labels[11].month).toBe(11);
    expect(labels[0].column).toBe(1);
  });

  it('should keep developers separate even on the same date', async () => {
    const entries = [
      makeEntry('2026-03-10', 9, 17, 1, 10, 'Alice'),
      makeEntry('2026-03-10', 9, 12, 2, 20, 'Bob'),
    ];
    const { fixture } = await render(ContributionHeatmapComponent, {
      componentInputs: { entries },
    });
    fixture.componentInstance.year.set(2026);
    fixture.detectChanges();

    const devRows = fixture.componentInstance.devRows();
    expect(devRows.length).toBe(2);

    const aliceRow = devRows.find(r => r.devName === 'Alice')!;
    const bobRow = devRows.find(r => r.devName === 'Bob')!;

    const aliceCell = aliceRow.cells.find(c => c.key === '2026-03-10')!;
    const bobCell = bobRow.cells.find(c => c.key === '2026-03-10')!;

    expect(aliceCell.hours).toBeCloseTo(8);
    expect(bobCell.hours).toBeCloseTo(3);
    expect(aliceRow.color).not.toBe(bobRow.color);
  });

  it('should not render Less or More legend labels', async () => {
    await render(ContributionHeatmapComponent, {
      componentInputs: { entries: [makeEntry('2026-04-09', 9, 12, 1, 1, 'Alice')] },
    });

    expect(screen.queryByText(/^Less$/i)).toBeNull();
    expect(screen.queryByText(/^More$/i)).toBeNull();
  });
});
