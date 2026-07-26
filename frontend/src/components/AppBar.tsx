import type { ReactNode } from 'react';

interface AppBarProps {
  title: string;
  leading?: ReactNode;
  trailing?: ReactNode;
}

export function AppBar({ title, leading, trailing }: AppBarProps) {
  return (
    <div className="appbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        {leading}
        <span className="appbar-title">{title}</span>
      </div>
      <div style={{ display: 'flex', gap: 8 }}>{trailing}</div>
    </div>
  );
}
