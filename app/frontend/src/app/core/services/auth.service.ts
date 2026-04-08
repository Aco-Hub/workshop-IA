import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { Developer } from '../models/developer.model';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  token: string;
  username: string;
  password: string;
  title?: string;
  discordLink?: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  email: string;
  username: string;
  title: string;
  discordLink: string;
  discordAvatarUrl: string;
  role: 'STANDARD' | 'ADMIN';
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  // Attributes

  private readonly _TOKEN_KEY = 'ts_jwt';
  private readonly _USER_KEY = 'ts_user';

  readonly currentUser = signal<Developer | null>(this._loadUser());
  readonly isAuthenticated = signal<boolean>(!!this.getToken());

  // Constructor

  constructor(private readonly _http: HttpClient) {}

  // Methods

  login(request: LoginRequest): Observable<AuthResponse> {
    return this._http.post<AuthResponse>('/api/auth/login', request).pipe(
      tap(response => this._storeAuth(response))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this._http.post<AuthResponse>('/api/auth/register', request).pipe(
      tap(response => this._storeAuth(response))
    );
  }

  getMe(): Observable<Developer> {
    return this._http.get<Developer>('/api/auth/me').pipe(
      tap(user => {
        this.currentUser.set(user);
        localStorage.setItem(this._USER_KEY, JSON.stringify(user));
      })
    );
  }

  updateProfile(data: { username: string; title: string; discordLink: string }): Observable<Developer> {
    return this._http.put<Developer>('/api/auth/profile', data).pipe(
      tap(user => {
        this.currentUser.set(user);
        localStorage.setItem(this._USER_KEY, JSON.stringify(user));
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this._TOKEN_KEY);
    localStorage.removeItem(this._USER_KEY);
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
  }

  getToken(): string | null {
    return localStorage.getItem(this._TOKEN_KEY);
  }

  // Private methods

  private _storeAuth(response: AuthResponse): void {
    localStorage.setItem(this._TOKEN_KEY, response.token);
    const user: Developer = {
      id: response.id,
      email: response.email,
      username: response.username,
      title: response.title,
      discordLink: response.discordLink,
      discordAvatarUrl: response.discordAvatarUrl,
      role: response.role,
      createdAt: '',
      updatedAt: '',
    };
    localStorage.setItem(this._USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
    this.isAuthenticated.set(true);
  }

  private _loadUser(): Developer | null {
    const stored = localStorage.getItem(this._USER_KEY);
    if (!stored) {
      return null;
    }
    try {
      return JSON.parse(stored) as Developer;
    } catch {
      return null;
    }
  }
}
