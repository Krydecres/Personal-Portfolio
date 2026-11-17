import { useState } from 'react';
import { safeUrl } from '../lib/format.js';

const initials = (name = '') =>
  name
    .split(/\s+/)
    .filter(Boolean)
    .slice(-2)
    .map((w) => w[0])
    .join('')
    .toUpperCase() || '?';

export default function Avatar({ name, url, size = 36 }) {
  const [broken, setBroken] = useState(false);
  const src = safeUrl(url);
  const dim = { width: size, height: size, fontSize: Math.round(size * 0.4) };
  if (src && !broken) {
    return (
      <img
        className="avatar"
        src={src}
        alt=""
        width={size}
        height={size}
        referrerPolicy="no-referrer"
        onError={() => setBroken(true)}
      />
    );
  }
  return (
    <span className="avatar avatar-fallback" style={dim} aria-hidden="true">
      {initials(name)}
    </span>
  );
}
