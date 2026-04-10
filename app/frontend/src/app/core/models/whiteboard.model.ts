export type ElementType = 'PEN' | 'RECTANGLE' | 'ELLIPSE' | 'LINE' | 'TEXT' | 'IMAGE' | 'LINK';

export interface WhiteboardRoom {
  id: number;
  name: string;
  createdBy: {
    id: number;
    username: string;
    discordAvatarUrl?: string;
  };
  createdAt: string;
  updatedAt?: string;
}

export interface WhiteboardElement {
  id: number;
  type: ElementType;
  data: string;
  color: string;
  strokeWidth: number;
  createdById: number;
  zIndex: number;
  createdAt: string;
}

export interface WhiteboardComment {
  id: number;
  developer: {
    id: number;
    username: string;
    discordAvatarUrl?: string;
  };
  text: string;
  parentId: number | null;
  createdAt: string;
}

export interface CursorPosition {
  userId: number;
  username: string;
  color: string;
  x: number;
  y: number;
}

export interface WhiteboardEventMessage {
  action: 'CREATE' | 'DELETE';
  element: WhiteboardElement | null;
}
