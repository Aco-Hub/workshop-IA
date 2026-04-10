import { Injectable, inject } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';

import { AuthService } from './auth.service';
import { CursorPosition, WhiteboardEventMessage } from '../models/whiteboard.model';

@Injectable({ providedIn: 'root' })
export class WebSocketService {

  // Attributes

  private _client: Client | null = null;
  private _subscriptions: StompSubscription[] = [];
  private readonly _auth = inject(AuthService);

  // Methods

  connect(roomId: number): Observable<void> {
    this.disconnect();

    const connected = new Subject<void>();
    const token = this._auth.getToken();
    this._client = new Client({
      brokerURL: `ws://${window.location.hostname}:${window.location.port}/ws/websocket`,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    this._client.onConnect = () => {
      connected.next();
      connected.complete();
    };

    this._client.activate();
    return connected.asObservable();
  }

  disconnect(): void {
    this._subscriptions.forEach(sub => sub.unsubscribe());
    this._subscriptions = [];
    if (this._client?.connected) {
      this._client.deactivate();
    }
    this._client = null;
  }

  subscribeToCursors(roomId: number): Observable<CursorPosition> {
    return new Observable<CursorPosition>(observer => {
      if (!this._client) return;
      const sub = this._client.subscribe(
        `/topic/whiteboard/${roomId}/cursors`,
        (message: IMessage) => {
          observer.next(JSON.parse(message.body) as CursorPosition);
        }
      );
      this._subscriptions.push(sub);
      return () => sub.unsubscribe();
    });
  }

  subscribeToElements(roomId: number): Observable<WhiteboardEventMessage> {
    return new Observable<WhiteboardEventMessage>(observer => {
      if (!this._client) return;
      const sub = this._client.subscribe(
        `/topic/whiteboard/${roomId}/elements`,
        (message: IMessage) => {
          observer.next(JSON.parse(message.body) as WhiteboardEventMessage);
        }
      );
      this._subscriptions.push(sub);
      return () => sub.unsubscribe();
    });
  }

  sendCursorPosition(roomId: number, position: CursorPosition): void {
    if (!this._client?.connected) return;
    this._client.publish({
      destination: `/app/whiteboard/${roomId}/cursor`,
      body: JSON.stringify(position),
    });
  }

  sendElementCreate(roomId: number, element: { type: string; data: string; color: string; strokeWidth: number; zIndex: number }): void {
    if (!this._client?.connected) return;
    this._client.publish({
      destination: `/app/whiteboard/${roomId}/element.create`,
      body: JSON.stringify(element),
    });
  }

  sendElementDelete(roomId: number, elementId: number): void {
    if (!this._client?.connected) return;
    this._client.publish({
      destination: `/app/whiteboard/${roomId}/element.delete`,
      body: JSON.stringify(elementId),
    });
  }
}
