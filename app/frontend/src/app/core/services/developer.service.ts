import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Developer } from '../models/developer.model';

export interface InviteRequest {
  email: string;
}

export interface InviteResponse {
  inviteLink: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class DeveloperService {

  // Constructor

  constructor(private readonly _http: HttpClient) {}

  // Methods

  getAll(): Observable<Developer[]> {
    return this._http.get<Developer[]>('/api/developers');
  }

  getById(id: number): Observable<Developer> {
    return this._http.get<Developer>(`/api/developers/${id}`);
  }

  invite(request: InviteRequest): Observable<InviteResponse> {
    return this._http.post<InviteResponse>('/api/developers/invite', request);
  }

  delete(id: number): Observable<void> {
    return this._http.delete<void>(`/api/developers/${id}`);
  }
}
