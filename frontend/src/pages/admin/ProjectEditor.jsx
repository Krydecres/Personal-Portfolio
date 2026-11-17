import { profileApi } from '../../api/endpoints.js';
import { TextArea, TextInput } from '../../components/Form.jsx';
import { splitList } from '../../lib/format.js';
import { useProfile } from '../../lib/useProfile.js';
import ListEditor from './ListEditor.jsx';

export default function ProjectEditor() {
  const { profile } = useProfile();
  if (!profile) return <p className="muted">Đang tải…</p>;

  return (
    <ListEditor
      noun="dự án"
      items={profile.projects}
      empty="Chưa có dự án nào."
      describe={(p) => ({ title: p.name, subtitle: `${p.featured ? 'Nổi bật — ' : ''}${p.techStack.join(', ')}` })}
      toDraft={(p) => ({
        name: p?.name || '',
        summary: p?.summary || '',
        techStack: p?.techStack?.join(', ') || '',
        projectUrl: p?.projectUrl || '',
        repoUrl: p?.repoUrl || '',
        imageUrl: p?.imageUrl || '',
        featured: p?.featured || false,
        sortOrder: p?.sortOrder ?? 0,
      })}
      toRequest={(d) => ({ ...d, techStack: splitList(d.techStack), sortOrder: Number(d.sortOrder) || 0 })}
      api={{ add: profileApi.addProject, update: profileApi.updateProject, remove: profileApi.deleteProject }}
      renderForm={(d, setD, errors) => {
        const bind = (k) => ({ value: d[k], onChange: (e) => setD({ ...d, [k]: e.target.value }), error: errors[k] });
        return (
          <>
            <div className="grid-2">
              <TextInput label="Tên dự án" required maxLength={120} {...bind('name')} />
              <TextInput label="Công nghệ" hint="Phân tách bằng dấu phẩy" {...bind('techStack')} />
              <TextInput label="Link sản phẩm" type="url" {...bind('projectUrl')} />
              <TextInput label="Link mã nguồn" type="url" {...bind('repoUrl')} />
              <TextInput label="Ảnh minh hoạ (URL https)" type="url" {...bind('imageUrl')} />
              <TextInput label="Thứ tự hiển thị" type="number" {...bind('sortOrder')} />
            </div>
            <TextArea label="Mô tả ngắn" rows={4} maxLength={2000} {...bind('summary')} />
            <label className="checkbox">
              <input type="checkbox" checked={d.featured} onChange={(e) => setD({ ...d, featured: e.target.checked })} />
              Đánh dấu là dự án nổi bật
            </label>
          </>
        );
      }}
    />
  );
}
