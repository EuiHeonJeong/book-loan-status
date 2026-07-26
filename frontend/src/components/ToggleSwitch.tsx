export function ToggleSwitch({ on, onToggle, busy }: { on: boolean; onToggle: () => void; busy?: boolean }) {
  return (
    <span
      role="switch"
      aria-checked={on}
      aria-busy={busy}
      onClick={busy ? undefined : onToggle}
      style={{
        width: 40,
        height: 22,
        borderRadius: 999,
        display: 'flex',
        alignItems: 'center',
        padding: 2,
        cursor: busy ? 'default' : 'pointer',
        opacity: busy ? 0.6 : 1,
        background: on ? 'var(--color-primary-700)' : 'var(--color-neutral-300)',
      }}
    >
      <span
        style={{
          width: 16,
          height: 16,
          borderRadius: '50%',
          background: '#fff',
          transition: 'margin .15s',
          marginLeft: on ? 18 : 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        {busy && (
          <span
            style={{
              width: 10,
              height: 10,
              borderRadius: '50%',
              border: '1.5px solid var(--color-neutral-300)',
              borderTopColor: 'var(--color-primary-700)',
              animation: 'spin .6s linear infinite',
            }}
          />
        )}
      </span>
    </span>
  );
}
