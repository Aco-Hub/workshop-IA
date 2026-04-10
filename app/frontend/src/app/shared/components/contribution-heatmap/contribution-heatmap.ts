import { Component, computed, input, signal } from '@angular/core';
import { SlicePipe } from '@angular/common';

import { TimeEntry } from '../../../core/models/time-entry.model';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { getDevColor } from '../../../core/utils/dev-colors';

type HeatmapView = 'day' | 'week' | 'month';

interface HeatmapCell {
  key: string;
  row: number;
  col: number;
  hours: number;
  level: number;
  tooltip: string;
}

interface DevRow {
  devId: number;
  devName: string;
  color: string;
  cells: HeatmapCell[];
}

interface MonthLabel {
  month: number;
  column: number;
}

@Component({
  selector: 'app-contribution-heatmap',
  standalone: true,
  imports: [SlicePipe, TranslatePipe],
  templateUrl: './contribution-heatmap.html',
  styleUrl: './contribution-heatmap.scss',
})
export class ContributionHeatmapComponent {

  // Inputs

  readonly entries = input.required<TimeEntry[]>();

  // Attributes

  readonly view = signal<HeatmapView>('day');
  readonly year = signal(new Date().getFullYear());

  // Computed

  readonly developers = computed(() => {
    const map = new Map<number, string>();
    for (const entry of this.entries()) {
      if (!map.has(entry.developerId)) {
        map.set(entry.developerId, entry.developerUsername);
      }
    }
    return Array.from(map.entries()).map(([id, name]) => ({
      id,
      name,
      color: getDevColor(id),
    }));
  });

  readonly perDevDailyTotals = computed(() => {
    const map = new Map<number, Map<string, number>>();
    const y = this.year();

    for (const entry of this.entries()) {
      const start = new Date(entry.startTime);
      if (start.getFullYear() !== y) {
        continue;
      }
      const dateKey = this._toDateKey(start);
      const hours = (new Date(entry.endTime).getTime() - start.getTime()) / 3600000;

      let devMap = map.get(entry.developerId);
      if (!devMap) {
        devMap = new Map<string, number>();
        map.set(entry.developerId, devMap);
      }
      devMap.set(dateKey, (devMap.get(dateKey) ?? 0) + hours);
    }

    return map;
  });

  readonly devRows = computed<DevRow[]>(() => {
    const devs = this.developers();
    const perDev = this.perDevDailyTotals();
    const v = this.view();

    return devs.map(dev => {
      const totals = perDev.get(dev.id) ?? new Map<string, number>();
      let cells: HeatmapCell[];

      if (v === 'day') {
        cells = this._buildDayCells(totals);
      } else if (v === 'week') {
        cells = this._buildWeekCells(totals);
      } else {
        cells = this._buildMonthCells(totals);
      }

      return { devId: dev.id, devName: dev.name, color: dev.color, cells };
    });
  });

  readonly monthLabels = computed<MonthLabel[]>(() => {
    const y = this.year();
    const labels: MonthLabel[] = [];
    let lastMonth = -1;

    const jan1 = new Date(y, 0, 1);
    const jan1Dow = this._mondayBasedDow(jan1);
    const daysInYear = this._isLeapYear(y) ? 366 : 365;

    for (let dayIndex = 0; dayIndex < daysInYear; dayIndex++) {
      const d = new Date(y, 0, 1 + dayIndex);
      const m = d.getMonth();
      if (m !== lastMonth) {
        const totalDayOffset = jan1Dow + dayIndex;
        const col = Math.floor(totalDayOffset / 7) + 1;
        labels.push({ month: m, column: col });
        lastMonth = m;
      }
    }

    return labels;
  });

  readonly totalColumns = computed(() => {
    const y = this.year();
    const jan1 = new Date(y, 0, 1);
    const dec31 = new Date(y, 11, 31);
    const jan1Dow = this._mondayBasedDow(jan1);
    const dayOfYear = Math.floor((dec31.getTime() - jan1.getTime()) / 86400000);
    return Math.floor((jan1Dow + dayOfYear) / 7) + 1;
  });

  // Methods

  setView(v: HeatmapView): void {
    this.view.set(v);
  }

  yearBack(): void {
    this.year.update(y => y - 1);
  }

  yearForward(): void {
    this.year.update(y => y + 1);
  }

  getCellBackground(color: string, level: number): string {
    if (level === 0) {
      return 'var(--heatmap-level-0)';
    }
    const opacity = level === 1 ? 0.3 : level === 2 ? 0.6 : 1;
    return this._hexToRgba(color, opacity);
  }

  // Private methods

  private _buildDayCells(totals: Map<string, number>): HeatmapCell[] {
    const y = this.year();
    const cells: HeatmapCell[] = [];

    const jan1 = new Date(y, 0, 1);
    const jan1Dow = this._mondayBasedDow(jan1);
    const daysInYear = this._isLeapYear(y) ? 366 : 365;

    const allHours = Array.from(totals.values());
    const maxHours = allHours.length > 0 ? Math.max(...allHours) : 0;

    for (let dayIndex = 0; dayIndex < daysInYear; dayIndex++) {
      const d = new Date(y, 0, 1 + dayIndex);
      const dateKey = this._toDateKey(d);
      const hours = totals.get(dateKey) ?? 0;
      const totalDayOffset = jan1Dow + dayIndex;
      const col = Math.floor(totalDayOffset / 7) + 1;
      const row = (totalDayOffset % 7) + 1;

      cells.push({
        key: dateKey,
        row,
        col,
        hours,
        level: this._getLevel(hours, maxHours),
        tooltip: hours > 0
          ? `${hours.toFixed(1)}h — ${this._formatDate(d)}`
          : this._formatDate(d),
      });
    }

    return cells;
  }

  private _buildWeekCells(totals: Map<string, number>): HeatmapCell[] {
    const y = this.year();
    const weekMap = new Map<number, number>();

    const jan1 = new Date(y, 0, 1);
    const jan1Dow = this._mondayBasedDow(jan1);
    const daysInYear = this._isLeapYear(y) ? 366 : 365;

    for (let dayIndex = 0; dayIndex < daysInYear; dayIndex++) {
      const d = new Date(y, 0, 1 + dayIndex);
      const dateKey = this._toDateKey(d);
      const hours = totals.get(dateKey) ?? 0;
      const totalDayOffset = jan1Dow + dayIndex;
      const weekIndex = Math.floor(totalDayOffset / 7);
      weekMap.set(weekIndex, (weekMap.get(weekIndex) ?? 0) + hours);
    }

    const allHours = Array.from(weekMap.values());
    const maxHours = allHours.length > 0 ? Math.max(...allHours) : 0;
    const cells: HeatmapCell[] = [];

    for (const [weekIndex, hours] of weekMap) {
      cells.push({
        key: `w${weekIndex}`,
        row: 1,
        col: weekIndex + 1,
        hours,
        level: this._getLevel(hours, maxHours),
        tooltip: `${hours.toFixed(1)}h`,
      });
    }

    return cells.sort((a, b) => a.col - b.col);
  }

  private _buildMonthCells(totals: Map<string, number>): HeatmapCell[] {
    const monthMap = new Map<number, number>();

    for (const [dateKey, hours] of totals) {
      const month = parseInt(dateKey.substring(5, 7), 10) - 1;
      monthMap.set(month, (monthMap.get(month) ?? 0) + hours);
    }

    const allHours = Array.from(monthMap.values());
    const maxHours = allHours.length > 0 ? Math.max(...allHours) : 0;
    const cells: HeatmapCell[] = [];

    for (let m = 0; m < 12; m++) {
      const hours = monthMap.get(m) ?? 0;
      cells.push({
        key: `m${m}`,
        row: 1,
        col: m + 1,
        hours,
        level: this._getLevel(hours, maxHours),
        tooltip: `${hours.toFixed(1)}h`,
      });
    }

    return cells;
  }

  private _getLevel(hours: number, max: number): number {
    if (hours === 0 || max === 0) {
      return 0;
    }
    const ratio = hours / max;
    if (ratio <= 0.33) {
      return 1;
    }
    if (ratio <= 0.66) {
      return 2;
    }
    return 3;
  }

  private _hexToRgba(hex: string, alpha: number): string {
    const r = parseInt(hex.slice(1, 3), 16);
    const g = parseInt(hex.slice(3, 5), 16);
    const b = parseInt(hex.slice(5, 7), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }

  private _mondayBasedDow(date: Date): number {
    const dow = date.getDay();
    return dow === 0 ? 6 : dow - 1;
  }

  private _isLeapYear(y: number): boolean {
    return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0;
  }

  private _toDateKey(d: Date): string {
    return `${d.getFullYear()}-${this._pad(d.getMonth() + 1)}-${this._pad(d.getDate())}`;
  }

  private _formatDate(d: Date): string {
    return `${this._pad(d.getDate())}/${this._pad(d.getMonth() + 1)}/${d.getFullYear()}`;
  }

  private _pad(n: number): string {
    return n.toString().padStart(2, '0');
  }
}
