import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { WhiteboardRoom, WhiteboardElement, WhiteboardComment } from '../models/whiteboard.model';

@Injectable({ providedIn: 'root' })
export class WhiteboardService {

  // Constructor

  constructor(private readonly _http: HttpClient) {}

  // Methods

  getRooms(): Observable<WhiteboardRoom[]> {
    return this._http.get<WhiteboardRoom[]>('/api/whiteboards');
  }

  getRoom(id: number): Observable<WhiteboardRoom> {
    return this._http.get<WhiteboardRoom>(`/api/whiteboards/${id}`);
  }

  createRoom(name: string): Observable<WhiteboardRoom> {
    return this._http.post<WhiteboardRoom>('/api/whiteboards', { name });
  }

  deleteRoom(id: number): Observable<void> {
    return this._http.delete<void>(`/api/whiteboards/${id}`);
  }

  getElements(roomId: number): Observable<WhiteboardElement[]> {
    return this._http.get<WhiteboardElement[]>(`/api/whiteboards/${roomId}/elements`);
  }

  getComments(roomId: number): Observable<WhiteboardComment[]> {
    return this._http.get<WhiteboardComment[]>(`/api/whiteboards/${roomId}/comments`);
  }

  addComment(roomId: number, text: string, parentId?: number): Observable<WhiteboardComment> {
    return this._http.post<WhiteboardComment>(`/api/whiteboards/${roomId}/comments`, { text, parentId: parentId ?? null });
  }

  deleteComment(roomId: number, commentId: number): Observable<void> {
    return this._http.delete<void>(`/api/whiteboards/${roomId}/comments/${commentId}`);
  }
}
