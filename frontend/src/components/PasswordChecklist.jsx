/**
 * Live checklist mirroring the server rules (GET /api/auth/providers). The server re-validates
 * everything; this only helps the user get it right the first time.
 */
export const LOCAL_RULES = [
  { id: 'length', label: 'Ít nhất 8 ký tự', test: (p) => [...p].length >= 8 },
  { id: 'upper', label: 'Có ít nhất 1 chữ hoa', test: (p) => /\p{Lu}/u.test(p) },
  { id: 'lower', label: 'Có ít nhất 1 chữ thường', test: (p) => /\p{Ll}/u.test(p) },
  { id: 'digit', label: 'Có ít nhất 1 chữ số', test: (p) => /\p{Nd}/u.test(p) },
  { id: 'special', label: 'Có ít nhất 1 ký tự đặc biệt (!@#$%...)', test: (p) => /[^\p{L}\p{Nd}\s]/u.test(p) },
  { id: 'nospace', label: 'Không chứa khoảng trắng', test: (p) => !/\s/.test(p) },
  { id: 'maxBytes', label: 'Không quá 72 byte', test: (p) => new TextEncoder().encode(p).length <= 72 },
];

export const passwordOk = (p) => LOCAL_RULES.every((r) => r.test(p));

export default function PasswordChecklist({ password, labels }) {
  const labelFor = (rule) => labels?.find((l) => l.id === rule.id)?.label || rule.label;
  const visible = LOCAL_RULES.filter((r) => r.id !== 'maxBytes' || !r.test(password));
  return (
    <ul className="checklist" aria-label="Yêu cầu mật khẩu">
      {visible.map((rule) => {
        const ok = password.length > 0 && rule.test(password);
        return (
          <li key={rule.id} className={ok ? 'ok' : ''}>
            <span className="check" aria-hidden="true" />
            <span>{labelFor(rule)}</span>
            <span className="sr-only">{ok ? ' (đạt)' : ' (chưa đạt)'}</span>
          </li>
        );
      })}
    </ul>
  );
}
