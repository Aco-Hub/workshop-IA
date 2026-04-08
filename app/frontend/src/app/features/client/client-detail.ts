import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { ClientService } from '../../core/services/client.service';
import { ProjectService } from '../../core/services/project.service';
import { TimeEntryService } from '../../core/services/time-entry.service';
import { Client } from '../../core/models/client.model';
import { Project } from '../../core/models/project.model';
import { TimeEntry } from '../../core/models/time-entry.model';
import { Developer } from '../../core/models/developer.model';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { ModalComponent } from '../../shared/components/modal/modal';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state';
import { ExportService } from '../../core/services/export.service';
import { I18nService } from '../../core/services/i18n.service';
import { buildDevColorMap, getDevColor, getDevHeatColor } from '../../core/utils/dev-colors';

export type ViewMode = 'swimlane' | 'heatmap';
export type HeatmapPeriod = 'day' | 'week' | 'month' | 'trimester';

@Component({
  selector: 'app-client-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslatePipe, ModalComponent, LoadingSpinnerComponent, EmptyStateComponent],
  templateUrl: './client-detail.html',
  styleUrl: './client-detail.scss',
})
export class ClientDetailComponent implements OnInit {

  // Attributes

  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);
  private readonly _auth = inject(AuthService);
  private readonly _clientService = inject(ClientService);
  private readonly _projectService = inject(ProjectService);
  private readonly _timeEntryService = inject(TimeEntryService);
  private readonly _exportService = inject(ExportService);
  private readonly _i18n = inject(I18nService);

  client = signal<Client | null>(null);
  projects = signal<Project[]>([]);
  entries = signal<TimeEntry[]>([]);
  loading = signal(true);
  currentUser = this._auth.currentUser;
  isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  showExportPanel = signal(false);
  exportStartDate = signal('');
  exportEndDate = signal('');

  viewMode = signal<ViewMode>('heatmap');
  heatmapPeriod = signal<HeatmapPeriod>('week');
  showEditModal = signal(false);
  showDeleteConfirm = signal(false);
  editLoading = signal(false);
  editError = signal('');
  editName = signal('');

  readonly clientProjects = computed(() =>
    this.projects().filter(p => p.client?.id === this.client()?.id)
  );

  readonly uniqueDevs = computed<Developer[]>(() => {
    const map = new Map<number, Developer>();
    this.clientProjects().forEach(p => {
      p.assignedDevelopers.forEach(d => map.set(d.id, d));
    });
    this.entries().forEach(e => {
      if (!map.has(e.developerId)) {
        map.set(e.developerId, {
          id: e.developerId,
          email: '',
          username: e.developerUsername,
          title: '',
          discordLink: '',
          discordAvatarUrl: '',
          role: 'STANDARD',
          createdAt: '',
          updatedAt: '',
        });
      }
    });
    return Array.from(map.values());
  });

  readonly totalHours = computed(() =>
    this.entries().reduce((sum, e) => {
      return sum + (new Date(e.endTime).getTime() - new Date(e.startTime).getTime()) / 3600000;
    }, 0)
  );

  readonly devHoursMap = computed(() => {
    const devs = this.uniqueDevs();
    const entries = this.entries();
    const map = new Map<number, { developer: Developer; hours: number }>();

    devs.forEach(d => {
      map.set(d.id, { developer: d, hours: 0 });
    });

    entries.forEach(e => {
      const existing = map.get(e.developerId);
      const dur = (new Date(e.endTime).getTime() - new Date(e.startTime).getTime()) / 3600000;
      if (existing) {
        existing.hours += dur;
      }
    });

    return Array.from(map.values()).sort((a, b) => b.hours - a.hours);
  });

  readonly heatmapBuckets = computed(() => {
    const period = this.heatmapPeriod();
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const buckets: Date[] = [];

    if (period === 'day') {
      for (let i = 29; i >= 0; i--) {
        const d = new Date(today);
        d.setDate(d.getDate() - i);
        buckets.push(d);
      }
    } else if (period === 'week') {
      const start = new Date(today);
      start.setDate(start.getDate() - 13 * 7);
      const monday = this._getMonday(start);
      for (let i = 0; i < 14; i++) {
        const d = new Date(monday);
        d.setDate(d.getDate() + i * 7);
        buckets.push(d);
      }
    } else if (period === 'month') {
      for (let i = 11; i >= 0; i--) {
        const d = new Date(today.getFullYear(), today.getMonth() - i, 1);
        buckets.push(d);
      }
    } else {
      const currentQ = Math.floor(today.getMonth() / 3);
      const currentYear = today.getFullYear();
      for (let i = 7; i >= 0; i--) {
        const totalQ = currentYear * 4 + currentQ - i;
        const year = Math.floor(totalQ / 4);
        const q = totalQ % 4;
        buckets.push(new Date(year, q * 3, 1));
      }
    }
    return buckets;
  });

  readonly heatmapMax = computed(() => {
    const buckets = this.heatmapBuckets();
    const entries = this.entries();
    const devs = this.uniqueDevs();
    let max = 0;
    for (const dev of devs) {
      for (const b of buckets) {
        const end = this.getBucketEnd(b);
        const h = entries
          .filter(e => e.developerId === dev.id && new Date(e.startTime) >= b && new Date(e.startTime) < end)
          .reduce((sum, e) => sum + (new Date(e.endTime).getTime() - new Date(e.startTime).getTime()) / 3600000, 0);
        if (h > max) {
          max = h;
        }
      }
    }
    return max || 1;
  });

  readonly PIXELS_PER_DAY = 160;
  readonly MIN_BLOCK_WIDTH = 140;

  readonly swimlaneData = computed(() => {
    const clientProjects = this.clientProjects();
    const entries = this.entries();

    return clientProjects.map(project => {
      const projectEntries = entries.filter(e => e.projectId === project.id);
      const devs = project.assignedDevelopers;

      const devMap = new Map<number, Developer>();
      devs.forEach(d => devMap.set(d.id, d));
      projectEntries.forEach(e => {
        if (!devMap.has(e.developerId)) {
          devMap.set(e.developerId, {
            id: e.developerId,
            email: '',
            username: e.developerUsername,
            title: '',
            discordLink: '',
            discordAvatarUrl: '',
            role: 'STANDARD',
            createdAt: '',
            updatedAt: '',
          });
        }
      });

      const allDevs = Array.from(devMap.values());

      if (projectEntries.length === 0) {
        return {
          project,
          rows: allDevs.map(d => ({ developer: d, blocks: [] as { entry: TimeEntry; left: number; width: number; dateLabel: string; hours: number }[] })),
          totalWidth: 200,
          dayMarkers: [] as { px: number; label: string; isWeekend: boolean }[],
        };
      }

      const sorted = [...projectEntries].sort((a, b) =>
        new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
      );

      const firstDate = new Date(sorted[0].startTime);
      firstDate.setHours(0, 0, 0, 0);
      const lastDate = sorted.reduce((max, e) => {
        const d = new Date(e.startTime);
        d.setHours(0, 0, 0, 0);
        return d > max ? d : max;
      }, new Date(firstDate));

      const totalDays = Math.max(Math.round((lastDate.getTime() - firstDate.getTime()) / 86400000) + 1, 1);
      const totalWidth = totalDays * this.PIXELS_PER_DAY;

      const rows = allDevs.map(dev => {
        const devEntries = projectEntries.filter(e => e.developerId === dev.id);
        const blocks = devEntries.map(e => {
          const eDate = new Date(e.startTime);
          eDate.setHours(0, 0, 0, 0);
          const dayOffset = Math.round((eDate.getTime() - firstDate.getTime()) / 86400000);
          const hours = (new Date(e.endTime).getTime() - new Date(e.startTime).getTime()) / 3600000;
          return {
            entry: e,
            left: dayOffset * this.PIXELS_PER_DAY,
            width: Math.max(this.PIXELS_PER_DAY - 4, this.MIN_BLOCK_WIDTH),
            dateLabel: `${this._pad(eDate.getDate())}/${this._pad(eDate.getMonth() + 1)}`,
            hours,
          };
        });
        return { developer: dev, blocks };
      });

      const dayMarkers: { px: number; label: string; isWeekend: boolean }[] = [];
      for (let i = 0; i < totalDays; i++) {
        const d = new Date(firstDate);
        d.setDate(d.getDate() + i);
        dayMarkers.push({
          px: i * this.PIXELS_PER_DAY + this.PIXELS_PER_DAY / 2,
          label: `${this._pad(d.getDate())}/${this._pad(d.getMonth() + 1)}`,
          isWeekend: d.getDay() === 0 || d.getDay() === 6,
        });
      }

      return { project, rows, totalWidth, dayMarkers };
    });
  });

  // Lifecycle

  ngOnInit(): void {
    this._route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      this.loadClient(id);
    });
  }

  // Methods

  loadClient(id: number): void {
    this.loading.set(true);
    forkJoin({
      client: this._clientService.getById(id),
      projects: this._projectService.getAll(),
    }).subscribe({
      next: ({ client, projects }) => {
        this.client.set(client);
        this.projects.set(projects);
        this.editName.set(client.name);
        this.loading.set(false);
        this.loadEntries();
      },
      error: () => this.loading.set(false),
    });
  }

  loadEntries(): void {
    const client = this.client();
    if (!client) {
      return;
    }
    const projectIds = this.clientProjects().map(p => p.id);
    if (projectIds.length === 0) {
      this.entries.set([]);
      return;
    }
    forkJoin(
      projectIds.map(pid => this._timeEntryService.getAll({
        projectId: pid,
      }))
    ).subscribe({
      next: results => {
        const all = results.flat();
        this.entries.set(all);
        buildDevColorMap(this.uniqueDevs().map(d => d.id));
      }
    });
  }

  openEditModal(): void {
    const c = this.client();
    if (!c) {
      return;
    }
    this.editName.set(c.name);
    this.editError.set('');
    this.showEditModal.set(true);
  }

  saveEdit(): void {
    const c = this.client();
    if (!c) {
      return;
    }
    this.editLoading.set(true);
    this._clientService.update(c.id, { name: this.editName() }).subscribe({
      next: client => {
        this.client.set(client);
        this.showEditModal.set(false);
        this.editLoading.set(false);
      },
      error: () => {
        this.editError.set('client.error');
        this.editLoading.set(false);
      }
    });
  }

  deleteClient(): void {
    const c = this.client();
    if (!c) {
      return;
    }
    this._clientService.delete(c.id).subscribe({
      next: () => this._router.navigate(['/']),
    });
  }

  getBucketEnd(bucketStart: Date): Date {
    const period = this.heatmapPeriod();
    const end = new Date(bucketStart);
    if (period === 'day') {
      end.setDate(end.getDate() + 1);
    } else if (period === 'week') {
      end.setDate(end.getDate() + 7);
    } else if (period === 'month') {
      end.setMonth(end.getMonth() + 1);
    } else {
      end.setMonth(end.getMonth() + 3);
    }
    return end;
  }

  getHoursForDevInBucket(devId: number, bucketStart: Date): number {
    const end = this.getBucketEnd(bucketStart);
    return this.entries()
      .filter(e => e.developerId === devId && new Date(e.startTime) >= bucketStart && new Date(e.startTime) < end)
      .reduce((sum, e) => sum + (new Date(e.endTime).getTime() - new Date(e.startTime).getTime()) / 3600000, 0);
  }

  getTotalHoursForBucket(bucketStart: Date): number {
    const end = this.getBucketEnd(bucketStart);
    return this.entries()
      .filter(e => new Date(e.startTime) >= bucketStart && new Date(e.startTime) < end)
      .reduce((sum, e) => sum + (new Date(e.endTime).getTime() - new Date(e.startTime).getTime()) / 3600000, 0);
  }

  getDevColor(devId: number): string {
    return getDevColor(devId);
  }

  getDevHeatColorForDev(devId: number, hours: number): string {
    const intensity = hours / this.heatmapMax();
    return getDevHeatColor(devId, intensity);
  }

  formatBucketLabel(date: Date): string {
    const period = this.heatmapPeriod();
    if (period === 'day') {
      return `${this._pad(date.getDate())}/${this._pad(date.getMonth() + 1)}`;
    }
    if (period === 'week') {
      return `${this._pad(date.getDate())}/${this._pad(date.getMonth() + 1)}`;
    }
    if (period === 'month') {
      const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      return monthNames[date.getMonth()];
    }
    const q = Math.floor(date.getMonth() / 3) + 1;
    return `Q${q} ${date.getFullYear()}`;
  }

  formatDate(d: Date): string {
    return `${d.getFullYear()}-${this._pad(d.getMonth() + 1)}-${this._pad(d.getDate())}`;
  }

  exportReport(format: 'pdf' | 'excel'): void {
    const c = this.client();
    if (!c) {
      return;
    }
    const lang = this._i18n.currentLanguage();
    this._exportService.exportClient(
      c.id,
      format,
      this.exportStartDate() || undefined,
      this.exportEndDate() || undefined,
      lang
    );
    this.showExportPanel.set(false);
  }

  // Private methods

  private _getMonday(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    d.setHours(0, 0, 0, 0);
    return d;
  }

  private _pad(n: number): string {
    return n.toString().padStart(2, '0');
  }
}
