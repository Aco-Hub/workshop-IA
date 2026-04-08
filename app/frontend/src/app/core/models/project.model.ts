import { Developer } from './developer.model';

export interface Project {
  id: number;
  name: string;
  type: 'INTERNAL' | 'EXTERNAL';
  repoUrl: string;
  client: { id: number; name: string } | null;
  assignedDevelopers: Developer[];
  createdAt: string;
  updatedAt: string;
}
