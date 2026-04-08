import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { TimeEntry } from '../models/time-entry.model';

export interface TimeEntryRequest {
  developerId: number;
  projectId: number | null;
  type: 'WORK' | 'LEAVE';
  description: string;
  startTime: string;
  endTime: string;
}

export interface TimeEntryFilter {
  developerId?: number;
  projectId?: number;
  startDate?: string;
  endDate?: string;
}

export type RecurrenceFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY';

export interface RecurringTimeEntryRequest {
  developerId: number;
  projectId: number | null;
  type: 'WORK' | 'LEAVE';
  description: string;
  startTime: string;
  endTime: string;
  frequency: RecurrenceFrequency;
  endDate: string | null;
  count: number | null;
}

@Injectable({ providedIn: 'root' })
export class TimeEntryService {

  // Constructor

  constructor(private readonly _http: HttpClient) {}

  // Methods

  getAll(filter?: TimeEntryFilter): Observable<TimeEntry[]> {
    let params = new HttpParams();
    if (filter?.developerId) {
      params = params.set('developerId', filter.developerId.toString());
    }
    if (filter?.projectId) {
      params = params.set('projectId', filter.projectId.toString());
    }
    if (filter?.startDate) {
      params = params.set('startDate', filter.startDate);
    }
    if (filter?.endDate) {
      params = params.set('endDate', filter.endDate);
    }
    return this._http.get<TimeEntry[]>('/api/time-entries', { params });
  }

  create(request: TimeEntryRequest): Observable<TimeEntry> {
    return this._http.post<TimeEntry>('/api/time-entries', request);
  }

  update(id: number, request: TimeEntryRequest): Observable<TimeEntry> {
    return this._http.put<TimeEntry>(`/api/time-entries/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this._http.delete<void>(`/api/time-entries/${id}`);
  }

  createRecurring(request: RecurringTimeEntryRequest): Observable<TimeEntry[]> {
    return this._http.post<TimeEntry[]>('/api/time-entries/recurring', request);
  }

  deleteRecurrenceGroup(groupId: string): Observable<void> {
    return this._http.delete<void>(`/api/time-entries/recurring/${groupId}`);
  }

  updateRecurrenceGroup(groupId: string, request: TimeEntryRequest): Observable<TimeEntry[]> {
    return this._http.put<TimeEntry[]>(`/api/time-entries/recurring/${groupId}`, request);
  }
}
