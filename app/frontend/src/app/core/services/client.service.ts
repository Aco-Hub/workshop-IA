import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Client } from '../models/client.model';

export interface ClientRequest {
  name: string;
}

@Injectable({ providedIn: 'root' })
export class ClientService {

  // Constructor

  constructor(private readonly _http: HttpClient) {}

  // Methods

  getAll(): Observable<Client[]> {
    return this._http.get<Client[]>('/api/clients');
  }

  getById(id: number): Observable<Client> {
    return this._http.get<Client>(`/api/clients/${id}`);
  }

  create(request: ClientRequest): Observable<Client> {
    return this._http.post<Client>('/api/clients', request);
  }

  update(id: number, request: ClientRequest): Observable<Client> {
    return this._http.put<Client>(`/api/clients/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this._http.delete<void>(`/api/clients/${id}`);
  }
}
