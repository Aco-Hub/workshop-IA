import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Project } from '../models/project.model';

export interface ProjectRequest {
  name: string;
  type: 'INTERNAL' | 'EXTERNAL';
  repoUrl: string;
  clientId: number | null;
}

@Injectable({ providedIn: 'root' })
export class ProjectService {

  // Constructor

  constructor(private readonly _http: HttpClient) {}

  // Methods

  getAll(): Observable<Project[]> {
    return this._http.get<Project[]>('/api/projects');
  }

  getById(id: number): Observable<Project> {
    return this._http.get<Project>(`/api/projects/${id}`);
  }

  create(request: ProjectRequest, creatorId: number): Observable<Project> {
    return this._http.post<Project>(`/api/projects?creatorId=${creatorId}`, request);
  }

  update(id: number, request: ProjectRequest): Observable<Project> {
    return this._http.put<Project>(`/api/projects/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this._http.delete<void>(`/api/projects/${id}`);
  }

  assign(projectId: number, developerId: number): Observable<Project> {
    return this._http.post<Project>(`/api/projects/${projectId}/assign`, { developerId });
  }

  unassign(projectId: number, developerId: number): Observable<Project> {
    return this._http.post<Project>(`/api/projects/${projectId}/unassign`, { developerId });
  }
}
