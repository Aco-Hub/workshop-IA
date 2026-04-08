import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../../../core/services/auth.service';
import { DeveloperService } from '../../../core/services/developer.service';
import { ProjectService } from '../../../core/services/project.service';
import { ClientService } from '../../../core/services/client.service';
import { Developer } from '../../../core/models/developer.model';
import { Project } from '../../../core/models/project.model';
import { Client } from '../../../core/models/client.model';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { getDevColor } from '../../../core/utils/dev-colors';
import { ModalComponent } from '../modal/modal';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, TranslatePipe, ModalComponent, FormsModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
})
export class SidebarComponent implements OnInit {

  // Attributes

  private readonly _auth = inject(AuthService);
  private readonly _developerService = inject(DeveloperService);
  private readonly _projectService = inject(ProjectService);
  private readonly _clientService = inject(ClientService);
  private readonly _router = inject(Router);

  readonly currentUser = this._auth.currentUser;
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  developers = signal<Developer[]>([]);
  projects = signal<Project[]>([]);
  clients = signal<Client[]>([]);
  mobileOpen = signal(false);
  devsExpanded = signal(true);
  projectsExpanded = signal(true);
  clientsExpanded = signal(true);

  showInviteModal = signal(false);
  inviteEmail = signal('');
  inviteLoading = signal(false);
  inviteResult = signal<{ inviteLink: string; message: string } | null>(null);
  inviteError = signal('');
  linkCopied = signal(false);

  showCreateProjectModal = signal(false);
  newProject = signal({ name: '', type: 'INTERNAL' as 'INTERNAL' | 'EXTERNAL', repoUrl: '', clientId: null as number | null });
  projectLoading = signal(false);
  projectError = signal('');

  showCreateClientModal = signal(false);
  newClientName = signal('');
  clientLoading = signal(false);
  clientError = signal('');

  // Lifecycle

  ngOnInit(): void {
    this.loadData();
  }

  // Methods

  loadData(): void {
    this._developerService.getAll().subscribe({ next: devs => this.developers.set(devs) });
    this._projectService.getAll().subscribe({ next: projects => this.projects.set(projects) });
    this._clientService.getAll().subscribe({ next: clients => this.clients.set(clients) });
  }

  toggleMobile(): void {
    this.mobileOpen.update(v => !v);
  }

  closeMobile(): void {
    this.mobileOpen.set(false);
  }

  logout(): void {
    this._auth.logout();
    this._router.navigate(['/login']);
  }

  getAvatarUrl(): string {
    return this.currentUser()?.discordAvatarUrl || '';
  }

  getInitials(): string {
    const user = this.currentUser();
    if (!user) {
      return '?';
    }
    return user.username.charAt(0).toUpperCase();
  }

  getDevColor(devId: number): string {
    return getDevColor(devId);
  }

  openInviteModal(): void {
    this.inviteEmail.set('');
    this.inviteResult.set(null);
    this.inviteError.set('');
    this.linkCopied.set(false);
    this.showInviteModal.set(true);
  }

  sendInvite(): void {
    const email = this.inviteEmail().trim();
    if (!email) {
      return;
    }
    this.inviteLoading.set(true);
    this.inviteError.set('');
    this._developerService.invite({ email }).subscribe({
      next: result => {
        this.inviteResult.set(result);
        this.inviteLoading.set(false);
      },
      error: () => {
        this.inviteError.set('developer.invite_error');
        this.inviteLoading.set(false);
      }
    });
  }

  copyInviteLink(): void {
    const link = this.inviteResult()?.inviteLink;
    if (link) {
      navigator.clipboard.writeText(link).then(() => {
        this.linkCopied.set(true);
        setTimeout(() => this.linkCopied.set(false), 2000);
      });
    }
  }

  openCreateProjectModal(): void {
    this.newProject.set({ name: '', type: 'INTERNAL', repoUrl: '', clientId: null });
    this.projectError.set('');
    this.showCreateProjectModal.set(true);
  }

  submitCreateProject(): void {
    const p = this.newProject();
    if (!p.name.trim()) {
      return;
    }
    const userId = this.currentUser()?.id;
    if (!userId) {
      return;
    }
    this.projectLoading.set(true);
    this._projectService.create(p, userId).subscribe({
      next: project => {
        this.projects.update(list => [...list, project]);
        this.showCreateProjectModal.set(false);
        this.projectLoading.set(false);
        this._router.navigate(['/project', project.id]);
      },
      error: () => {
        this.projectError.set('project.error');
        this.projectLoading.set(false);
      }
    });
  }

  openCreateClientModal(): void {
    this.newClientName.set('');
    this.clientError.set('');
    this.showCreateClientModal.set(true);
  }

  submitCreateClient(): void {
    const name = this.newClientName().trim();
    if (!name) {
      return;
    }
    this.clientLoading.set(true);
    this._clientService.create({ name }).subscribe({
      next: client => {
        this.clients.update(list => [...list, client]);
        this.showCreateClientModal.set(false);
        this.clientLoading.set(false);
        this._router.navigate(['/client', client.id]);
      },
      error: () => {
        this.clientError.set('client.error');
        this.clientLoading.set(false);
      }
    });
  }

  updateProjectName(value: string): void {
    this.newProject.update(p => ({ ...p, name: value }));
  }

  updateProjectType(value: string): void {
    this.newProject.update(p => ({ ...p, type: value as 'INTERNAL' | 'EXTERNAL' }));
  }

  updateProjectRepoUrl(value: string): void {
    this.newProject.update(p => ({ ...p, repoUrl: value }));
  }

  updateProjectClientId(value: string): void {
    this.newProject.update(p => ({ ...p, clientId: value ? Number(value) : null }));
  }
}
