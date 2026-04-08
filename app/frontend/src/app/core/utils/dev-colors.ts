// Notion calendar-style color palette for developers
const DEV_COLORS = [
  '#2383E2', // Blue
  '#D9730D', // Orange
  '#0F7B6C', // Green
  '#9065B0', // Purple
  '#C94A4A', // Red
  '#C14C8A', // Pink
  '#CB912F', // Yellow
  '#337EA9', // Teal
  '#448361', // Forest
  '#937264', // Brown
  '#3B5F8C', // Navy
  '#787774', // Gray
];

// Deterministic color based on dev ID - same dev always gets the same color
// across all views and pages regardless of load order
export function getDevColor(devId: number): string {
  const index = ((devId * 2654435761) >>> 0) % DEV_COLORS.length;
  return DEV_COLORS[index];
}

// No-op kept for backwards compatibility - color is now purely ID-based
export function buildDevColorMap(_devIds: number[]): void {
  // Intentionally empty: colors are now deterministic from devId
}

export function getDevHeatColor(devId: number, intensity: number): string {
  if (intensity === 0) {
    return 'var(--bg-tertiary)';
  }
  const hex = getDevColor(devId);
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r}, ${g}, ${b}, ${0.15 + intensity * 0.85})`;
}
