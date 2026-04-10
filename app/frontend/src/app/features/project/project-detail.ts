import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { ProjectService } from '../../core/services/project.service';
import { TimeEntryService } from '../../core/services/time-entry.service';
import { ClientService } from '../../core/services/client.service';
import { Project } from '../../core/models/project.model';
import { TimeEntry } from '../../core/models/time-entry.model';
import { Client } from '../../core/models/client.model';
import { Developer } from '../../core/models/developer.model';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { ModalComponent } from '../../shared/components/modal/modal';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state';
import { I18nService } from '../../core/services/i18n.service';
import { ExportService } from '../../core/services/export.service';
import { buildDevColorMap, getDevColor } from '../../core/utils/dev-colors';


interface DevHours {
  developer: Developer;
  hours: number;
  entries: TimeEntry[];
}

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TranslatePipe, ModalComponent, LoadingSpinnerComponent, EmptyStateComponent],
  templateUrl: './project-detail.html',
  styleUrl: './project-detail.scss',
})
export class ProjectDetailComponent implements OnInit {

  // Attributes

  private readonly _route = inject(ActivatedRoute);
  private readonly _router = inject(Router);
  private readonly _auth = inject(AuthService);
  private readonly _projectService = inject(ProjectService);
  private readonly _timeEntryService = inject(TimeEntryService);
  private readonly _clientService = inject(ClientService);
  private readonly _i18n = inject(I18nService);
  private readonly _exportService = inject(ExportService);

  project = signal<Project | null>(null);
  entries = signal<TimeEntry[]>([]);
  clients = signal<Client[]>([]);
  loading = signal(true);
  currentUser = this._auth.currentUser;
  isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  showExportPanel = signal(false);
  exportStartDate = signal('');
  exportEndDate = signal('');

  showEditModal = signal(false);
  showDeleteConfirm = signal(false);
  editLoading = signal(false);
  editError = signal('');
  editForm = signal({ name: '', type: 'INTERNAL' as 'INTERNAL' | 'EXTERNAL', repoUrl: '', clientId: null as number | null });

  readonly totalHours = computed(() =>
    this.entries().reduce((sum, e) => {
      const start = new Date(e.startTime).getTime();
      const end = new Date(e.endTime).getTime();
      return sum + (end - start) / 3600000;
    }, 0)
  );

  readonly devHours = computed<DevHours[]>(() => {
    const project = this.project();
    if (!project) {
      return [];
    }
    const map = new Map<number, DevHours>();
    project.assignedDevelopers.forEach(dev => {
      map.set(dev.id, { developer: dev, hours: 0, entries: [] });
    });
    this.entries().forEach(e => {
      if (map.has(e.developerId)) {
        const dh = map.get(e.developerId)!;
        const dur = (new Date(e.endTime).getTime() - new Date(e.startTime).getTime()) / 3600000;
        dh.hours += dur;
        dh.entries.push(e);
      }
    });
    return Array.from(map.values()).sort((a, b) => b.hours - a.hours);
  });

  readonly PIXELS_PER_DAY = 160;
  readonly MIN_BLOCK_WIDTH = 140;

  readonly swimlaneData = computed(() => {
    const entries = this.entries();
    const devs = this.project()?.assignedDevelopers || [];

    const emptyResult = {
      rows: [] as { developer: any; blocks: { entry: any; left: number; width: number; dateLabel: string; hours: number }[] }[],
      totalWidth: 0,
      dayMarkers: [] as { px: number; label: string; isWeekend: boolean }[]
    };

    if (entries.length === 0 || devs.length === 0) {
      return emptyResult;
    }

    const sorted = [...entries].sort((a, b) =>
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

    const rows = devs.map(dev => {
      const devEntries = entries.filter(e => e.developerId === dev.id);
      const blocks = devEntries.map(e => {
        const eDate = new Date(e.startTime);
        eDate.setHours(0, 0, 0, 0);
        const dayOffset = Math.round((eDate.getTime() - firstDate.getTime()) / 86400000);
        const hours = (new Date(e.endTime).getTime() - new Date(e.startTime).getTime()) / 3600000;

        const fmt = (d: Date): string => `${this._pad(d.getDate())}/${this._pad(d.getMonth() + 1)}`;

        return {
          entry: e,
          left: dayOffset * this.PIXELS_PER_DAY,
          width: Math.max(this.PIXELS_PER_DAY - 4, this.MIN_BLOCK_WIDTH),
          dateLabel: fmt(eDate),
          hours,
        };
      });

      return { developer: dev, blocks };
    }).sort((a, b) => {
      const aH = a.blocks.reduce((s, bl) => s + bl.hours, 0);
      const bH = b.blocks.reduce((s, bl) => s + bl.hours, 0);
      return bH - aH;
    });

    const dayMarkers: { px: number; label: string; isWeekend: boolean }[] = [];
    for (let i = 0; i < totalDays; i++) {
      const d = new Date(firstDate);
      d.setDate(d.getDate() + i);
      const dayOfWeek = d.getDay();
      dayMarkers.push({
        px: i * this.PIXELS_PER_DAY + this.PIXELS_PER_DAY / 2,
        label: `${this._pad(d.getDate())}/${this._pad(d.getMonth() + 1)}`,
        isWeekend: dayOfWeek === 0 || dayOfWeek === 6,
      });
    }

    return { rows, totalWidth, dayMarkers };
  });

  // Lifecycle

  ngOnInit(): void {
    this._route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      this.loadProject(id);
    });
  }

  // Methods

  loadProject(id: number): void {
    this.loading.set(true);
    forkJoin({
      project: this._projectService.getById(id),
      clients: this._clientService.getAll(),
    }).subscribe({
      next: ({ project, clients }) => {
        this.project.set(project);
        this.clients.set(clients);
        this.editForm.set({
          name: project.name,
          type: project.type,
          repoUrl: project.repoUrl,
          clientId: project.client?.id ?? null,
        });
        buildDevColorMap(project.assignedDevelopers.map(d => d.id));
        this.loading.set(false);
        this.loadEntries();
      },
      error: () => this.loading.set(false),
    });
  }

  loadEntries(): void {
    const project = this.project();
    if (!project) {
      return;
    }
    this._timeEntryService.getAll({
      projectId: project.id,
    }).subscribe({ next: entries => this.entries.set(entries) });
  }

  joinProject(): void {
    const project = this.project();
    const user = this.currentUser();
    if (!project || !user) {
      return;
    }
    this._projectService.assign(project.id, user.id).subscribe({
      next: p => this.project.set(p),
    });
  }

  leaveProject(): void {
    const project = this.project();
    const user = this.currentUser();
    if (!project || !user) {
      return;
    }
    this._projectService.unassign(project.id, user.id).subscribe({
      next: p => this.project.set(p),
    });
  }

  isAssigned(): boolean {
    const user = this.currentUser();
    return this.project()?.assignedDevelopers.some(d => d.id === user?.id) ?? false;
  }

  openEditModal(): void {
    const p = this.project();
    if (!p) {
      return;
    }
    this.editForm.set({ name: p.name, type: p.type, repoUrl: p.repoUrl, clientId: p.client?.id ?? null });
    this.editError.set('');
    this.showEditModal.set(true);
  }

  saveEdit(): void {
    const p = this.project();
    if (!p) {
      return;
    }
    this.editLoading.set(true);
    this._projectService.update(p.id, this.editForm()).subscribe({
      next: project => {
        this.project.set(project);
        this.showEditModal.set(false);
        this.editLoading.set(false);
      },
      error: () => {
        this.editError.set('project.error');
        this.editLoading.set(false);
      }
    });
  }

  deleteProject(): void {
    const p = this.project();
    if (!p) {
      return;
    }
    this._projectService.delete(p.id).subscribe({
      next: () => this._router.navigate(['/']),
    });
  }

  getDevColor(devId: number): string {
    return getDevColor(devId);
  }

  formatDate(d: Date): string {
    return `${d.getFullYear()}-${this._pad(d.getMonth() + 1)}-${this._pad(d.getDate())}`;
  }

  formatGapLabel(hours: number): string {
    if (hours < 48) {
      return `${Math.round(hours)}h`;
    }
    const days = Math.round(hours / 24);
    if (days < 14) {
      return `${days}d`;
    }
    const weeks = Math.round(days / 7);
    if (weeks < 9) {
      return `${weeks}w`;
    }
    const months = Math.round(days / 30);
    return `${months}mo`;
  }

  formatClusterLabel(start: Date, end: Date): string {
    const fmt = (d: Date): string => `${this._pad(d.getDate())}/${this._pad(d.getMonth() + 1)}`;
    if (start.toDateString() === end.toDateString()) {
      return fmt(start);
    }
    if (start.getFullYear() !== end.getFullYear()) {
      return `${fmt(start)}/${start.getFullYear()} - ${fmt(end)}/${end.getFullYear()}`;
    }
    return `${fmt(start)} - ${fmt(end)}`;
  }

  updateEditField(field: string, value: string | number | null): void {
    this.editForm.update(f => ({ ...f, [field]: value }));
  }

  exportReport(format: 'pdf' | 'excel'): void {
    const p = this.project();
    if (!p) {
      return;
    }
    const lang = this._i18n.currentLanguage();
    this._exportService.exportProject(
      p.id,
      format,
      this.exportStartDate() || undefined,
      this.exportEndDate() || undefined,
      lang
    );
    this.showExportPanel.set(false);
  }

  // Private methods

  private _pad(n: number): string {
    return n.toString().padStart(2, '0');
  }
}
