import { beforeAll } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WhiteboardService } from './whiteboard.service';
import { WhiteboardRoom, WhiteboardElement, WhiteboardComment } from '../models/whiteboard.model';

beforeAll(() => {
  getTestBed().initTestEnvironment(
    BrowserDynamicTestingModule,
    platformBrowserDynamicTesting()
  );
});

const mockRoom: WhiteboardRoom = {
  id: 1,
  name: 'Design Sprint',
  createdBy: { id: 42, username: 'devuser', discordAvatarUrl: 'https://cdn.discordapp.com/avatars/42/abc.png' },
  createdAt: '2026-04-01T10:00:00Z',
  updatedAt: '2026-04-02T08:30:00Z',
};

const mockRoom2: WhiteboardRoom = {
  id: 2,
  name: 'Architecture Review',
  createdBy: { id: 43, username: 'otherdev' },
  createdAt: '2026-04-03T12:00:00Z',
};

const mockElement: WhiteboardElement = {
  id: 10,
  type: 'RECTANGLE',
  data: '{"x":0,"y":0,"width":100,"height":50}',
  color: '#ff0000',
  strokeWidth: 2,
  createdById: 42,
  zIndex: 1,
  createdAt: '2026-04-01T10:05:00Z',
};

const mockElement2: WhiteboardElement = {
  id: 11,
  type: 'PEN',
  data: '{"points":[[0,0],[10,10],[20,5]]}',
  color: '#0000ff',
  strokeWidth: 1,
  createdById: 43,
  zIndex: 2,
  createdAt: '2026-04-01T10:10:00Z',
};

const mockComment: WhiteboardComment = {
  id: 100,
  developer: { id: 42, username: 'devuser', discordAvatarUrl: 'https://cdn.discordapp.com/avatars/42/abc.png' },
  text: 'Looks good to me!',
  parentId: null,
  createdAt: '2026-04-01T11:00:00Z',
};

const mockReply: WhiteboardComment = {
  id: 101,
  developer: { id: 43, username: 'otherdev' },
  text: 'Agreed, merging.',
  parentId: 100,
  createdAt: '2026-04-01T11:05:00Z',
};

describe('WhiteboardService', () => {
  let service: WhiteboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(WhiteboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  // getRooms

  it('should call GET /api/whiteboards', () => {
    service.getRooms().subscribe();
    const req = httpMock.expectOne('/api/whiteboards');
    expect(req.request.method).toBe('GET');
    req.flush([mockRoom, mockRoom2]);
  });

  it('should return an array of rooms from getRooms', () => {
    let result: WhiteboardRoom[] | undefined;
    service.getRooms().subscribe(rooms => { result = rooms; });
    const req = httpMock.expectOne('/api/whiteboards');
    req.flush([mockRoom, mockRoom2]);
    expect(result).toHaveLength(2);
    expect(result![0].name).toBe('Design Sprint');
    expect(result![1].id).toBe(2);
  });

  // getRoom

  it('should call GET /api/whiteboards/:id', () => {
    service.getRoom(1).subscribe();
    const req = httpMock.expectOne('/api/whiteboards/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockRoom);
  });

  it('should return the matching room from getRoom', () => {
    let result: WhiteboardRoom | undefined;
    service.getRoom(1).subscribe(room => { result = room; });
    const req = httpMock.expectOne('/api/whiteboards/1');
    req.flush(mockRoom);
    expect(result?.id).toBe(1);
    expect(result?.name).toBe('Design Sprint');
    expect(result?.createdBy.username).toBe('devuser');
  });

  // createRoom

  it('should call POST /api/whiteboards', () => {
    service.createRoom('New Board').subscribe();
    const req = httpMock.expectOne('/api/whiteboards');
    expect(req.request.method).toBe('POST');
    req.flush(mockRoom);
  });

  it('should send the room name in the request body for createRoom', () => {
    service.createRoom('New Board').subscribe();
    const req = httpMock.expectOne('/api/whiteboards');
    expect(req.request.body).toEqual({ name: 'New Board' });
    req.flush(mockRoom);
  });

  it('should return the created room from createRoom', () => {
    let result: WhiteboardRoom | undefined;
    service.createRoom('Design Sprint').subscribe(room => { result = room; });
    const req = httpMock.expectOne('/api/whiteboards');
    req.flush(mockRoom);
    expect(result?.id).toBe(1);
    expect(result?.name).toBe('Design Sprint');
  });

  // deleteRoom

  it('should call DELETE /api/whiteboards/:id', () => {
    service.deleteRoom(1).subscribe();
    const req = httpMock.expectOne('/api/whiteboards/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should complete after deleteRoom with no response body', () => {
    let completed = false;
    service.deleteRoom(2).subscribe({ complete: () => { completed = true; } });
    const req = httpMock.expectOne('/api/whiteboards/2');
    req.flush(null);
    expect(completed).toBe(true);
  });

  // getElements

  it('should call GET /api/whiteboards/:roomId/elements', () => {
    service.getElements(1).subscribe();
    const req = httpMock.expectOne('/api/whiteboards/1/elements');
    expect(req.request.method).toBe('GET');
    req.flush([mockElement, mockElement2]);
  });

  it('should return an array of elements from getElements', () => {
    let result: WhiteboardElement[] | undefined;
    service.getElements(1).subscribe(elements => { result = elements; });
    const req = httpMock.expectOne('/api/whiteboards/1/elements');
    req.flush([mockElement, mockElement2]);
    expect(result).toHaveLength(2);
    expect(result![0].type).toBe('RECTANGLE');
    expect(result![1].type).toBe('PEN');
  });

  it('should return elements with correct shape from getElements', () => {
    let result: WhiteboardElement[] | undefined;
    service.getElements(1).subscribe(elements => { result = elements; });
    const req = httpMock.expectOne('/api/whiteboards/1/elements');
    req.flush([mockElement]);
    expect(result![0].color).toBe('#ff0000');
    expect(result![0].strokeWidth).toBe(2);
    expect(result![0].zIndex).toBe(1);
    expect(result![0].createdById).toBe(42);
  });

  // getComments

  it('should call GET /api/whiteboards/:roomId/comments', () => {
    service.getComments(1).subscribe();
    const req = httpMock.expectOne('/api/whiteboards/1/comments');
    expect(req.request.method).toBe('GET');
    req.flush([mockComment, mockReply]);
  });

  it('should return an array of comments from getComments', () => {
    let result: WhiteboardComment[] | undefined;
    service.getComments(1).subscribe(comments => { result = comments; });
    const req = httpMock.expectOne('/api/whiteboards/1/comments');
    req.flush([mockComment, mockReply]);
    expect(result).toHaveLength(2);
    expect(result![0].text).toBe('Looks good to me!');
    expect(result![0].parentId).toBeNull();
    expect(result![1].parentId).toBe(100);
  });

  // addComment

  it('should call POST /api/whiteboards/:roomId/comments', () => {
    service.addComment(1, 'Nice work').subscribe();
    const req = httpMock.expectOne('/api/whiteboards/1/comments');
    expect(req.request.method).toBe('POST');
    req.flush(mockComment);
  });

  it('should send text and null parentId when parentId is omitted in addComment', () => {
    service.addComment(1, 'Looks good to me!').subscribe();
    const req = httpMock.expectOne('/api/whiteboards/1/comments');
    expect(req.request.body).toEqual({ text: 'Looks good to me!', parentId: null });
    req.flush(mockComment);
  });

  it('should send text and parentId when addComment is called as a reply', () => {
    service.addComment(1, 'Agreed, merging.', 100).subscribe();
    const req = httpMock.expectOne('/api/whiteboards/1/comments');
    expect(req.request.body).toEqual({ text: 'Agreed, merging.', parentId: 100 });
    req.flush(mockReply);
  });

  it('should return the created comment from addComment', () => {
    let result: WhiteboardComment | undefined;
    service.addComment(1, 'Looks good to me!').subscribe(comment => { result = comment; });
    const req = httpMock.expectOne('/api/whiteboards/1/comments');
    req.flush(mockComment);
    expect(result?.id).toBe(100);
    expect(result?.text).toBe('Looks good to me!');
    expect(result?.developer.username).toBe('devuser');
  });

  // deleteComment

  it('should call DELETE /api/whiteboards/:roomId/comments/:commentId', () => {
    service.deleteComment(1, 100).subscribe();
    const req = httpMock.expectOne('/api/whiteboards/1/comments/100');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('should complete after deleteComment with no response body', () => {
    let completed = false;
    service.deleteComment(1, 101).subscribe({ complete: () => { completed = true; } });
    const req = httpMock.expectOne('/api/whiteboards/1/comments/101');
    req.flush(null);
    expect(completed).toBe(true);
  });
});
