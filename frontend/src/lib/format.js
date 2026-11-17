const dateFmt = new Intl.DateTimeFormat('vi-VN', { day: 'numeric', month: 'long', year: 'numeric' });
const monthFmt = new Intl.DateTimeFormat('vi-VN', { month: '2-digit', year: 'numeric' });

export const formatDate = (iso) => (iso ? dateFmt.format(new Date(iso)) : '');

export const formatMonth = (isoDate) => (isoDate ? monthFmt.format(new Date(`${isoDate}T00:00:00`)) : '');

export const formatRange = (start, end) => `${formatMonth(start)} – ${end ? formatMonth(end) : 'nay'}`;

/** Only allow http(s) links into href/src. The server validates too; this is defence in depth. */
export const safeUrl = (url) => (typeof url === 'string' && /^https?:\/\//i.test(url.trim()) ? url.trim() : undefined);

export const splitList = (text) =>
  text
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
