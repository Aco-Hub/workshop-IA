import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class ExportService {

  // Attributes

  private readonly _http = inject(HttpClient);

  // Methods

  exportDeveloper(id: number, format: 'pdf' | 'excel', startDate?: string, endDate?: string, lang = 'fr'): void {
    this._download(`/api/export/developer/${id}`, format, startDate, endDate, lang);
  }

  exportProject(id: number, format: 'pdf' | 'excel', startDate?: string, endDate?: string, lang = 'fr'): void {
    this._download(`/api/export/project/${id}`, format, startDate, endDate, lang);
  }

  exportClient(id: number, format: 'pdf' | 'excel', startDate?: string, endDate?: string, lang = 'fr'): void {
    this._download(`/api/export/client/${id}`, format, startDate, endDate, lang);
  }

  // Private methods

  private _download(url: string, format: string, startDate?: string, endDate?: string, lang = 'fr'): void {
    let params = `?format=${format}&lang=${lang}`;
    if (startDate) {
      params += `&startDate=${startDate}`;
    }
    if (endDate) {
      params += `&endDate=${endDate}`;
    }

    this._http.get(url + params, { responseType: 'blob' }).subscribe(blob => {
      const ext = format === 'pdf' ? 'pdf' : 'xlsx';
      const filename = `timesheet_export_${new Date().toISOString().slice(0, 10)}.${ext}`;
      const objectUrl = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = objectUrl;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(objectUrl);
    });
  }
}
