export interface Developer {
  id: number;
  email: string;
  username: string;
  title: string;
  discordLink: string;
  discordAvatarUrl: string;
  role: 'STANDARD' | 'ADMIN';
  createdAt: string;
  updatedAt: string;
}
