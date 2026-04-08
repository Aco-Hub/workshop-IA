export interface TimeEntry {
  id: number;
  developerId: number;
  developerUsername: string;
  projectId: number | null;
  projectName: string | null;
  type: 'WORK' | 'LEAVE';
  description: string;
  startTime: string;
  endTime: string;
  recurrenceGroupId: string | null;
  recurrenceRule: string | null;
  createdAt: string;
  updatedAt: string;
}
