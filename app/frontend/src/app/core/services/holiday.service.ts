import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Holiday } from '../models/holiday.model';

@Injectable({ providedIn: 'root' })
export class HolidayService {

  // Constructor

  constructor(private readonly _http: HttpClient) {}

  // Methods

  getHolidays(year: number): Observable<Holiday[]> {
    const params = new HttpParams().set('year', year.toString());
    return this._http.get<Holiday[]>('/api/holidays', { params });
  }
}
