import { profileApi } from '../../api/endpoints.js';
import { TextArea, TextInput } from '../../components/Form.jsx';
import { formatRange } from '../../lib/format.js';
import { useProfile } from '../../lib/useProfile.js';
import ListEditor from './ListEditor.jsx';

export default function ExperienceEditor() {
  const { profile } = useProfile();
  if (!profile) return <p className="muted">Đang tải…</p>;

  return (
    <ListEditor
      noun="kinh nghiệm"
      items={profile.experiences}
      empty="Chưa có kinh nghiệm nào."
      describe={(e) => ({ title: `${e.role} tại ${e.organization}`, subtitle: formatRange(e.startDate, e.endDate) })}
      toDraft={(e) => ({
        role: e?.role || '',
        organization: e?.organization || '',
        location: e?.location || '',
        startDate: e?.startDate || '',
        endDate: e?.endDate || '',
        description: e?.description || '',
        sortOrder: e?.sortOrder ?? 0,
      })}
      toRequest={(d) => ({ ...d, endDate: d.endDate || null, startDate: d.startDate || null, sortOrder: Number(d.sortOrder) || 0 })}
      api={{
        add: profileApi.addExperience,
        update: profileApi.updateExperience,
        remove: profileApi.deleteExperience,
      }}
      renderForm={(d, setD, errors) => {
        const bind = (k) => ({ value: d[k], onChange: (e) => setD({ ...d, [k]: e.target.value }), error: errors[k] });
        return (
          <>
            <div className="grid-2">
              <TextInput label="Vị trí" required maxLength={120} {...bind('role')} />
              <TextInput label="Công ty / tổ chức" required maxLength={120} {...bind('organization')} />
              <TextInput label="Bắt đầu" type="date" required {...bind('startDate')} />
              <TextInput label="Kết thúc" type="date" hint="Để trống nếu vẫn đang làm" {...bind('endDate')} />
              <TextInput label="Địa điểm" maxLength={120} {...bind('location')} />
              <TextInput label="Thứ tự hiển thị" type="number" {...bind('sortOrder')} />
            </div>
            <TextArea label="Mô tả (Markdown)" rows={5} maxLength={4000} {...bind('description')} />
          </>
        );
      }}
    />
  );
}
