import { useState } from 'react';
import { Notice } from '../../components/Form.jsx';
import { reloadProfile } from '../../lib/useProfile.js';

/**
 * Generic "list of items + one inline form" editor used for experience and projects.
 * `renderForm(draft, setDraft, errors)` draws the fields; `toRequest` maps the draft to the API body.
 */
export default function ListEditor({ items, empty, describe, toDraft, toRequest, api, renderForm, noun }) {
  const [editing, setEditing] = useState(null); // null | 'new' | id
  const [draft, setDraft] = useState(null);
  const [errors, setErrors] = useState({});
  const [msg, setMsg] = useState(null);
  const [busy, setBusy] = useState(false);

  const open = (item) => {
    setEditing(item ? item.id : 'new');
    setDraft(toDraft(item));
    setErrors({});
    setMsg(null);
  };
  const close = () => {
    setEditing(null);
    setDraft(null);
  };

  const save = async (e) => {
    e.preventDefault();
    setBusy(true);
    setErrors({});
    try {
      const body = toRequest(draft);
      if (editing === 'new') await api.add(body);
      else await api.update(editing, body);
      await reloadProfile();
      setMsg({ kind: 'success', text: `Đã lưu ${noun}.` });
      close();
    } catch (err) {
      setErrors(err.fieldErrors);
      setMsg({ kind: 'error', text: err.message });
    } finally {
      setBusy(false);
    }
  };

  const remove = async (item) => {
    if (!window.confirm(`Xoá ${noun} “${describe(item).title}”?`)) return;
    try {
      await api.remove(item.id);
      await reloadProfile();
      setMsg({ kind: 'success', text: `Đã xoá ${noun}.` });
    } catch (err) {
      setMsg({ kind: 'error', text: err.message });
    }
  };

  // Only build the form while something is being edited: renderForm reads fields from the draft.
  const form = draft && (
    <form className="editor inline-editor" onSubmit={save}>
      {renderForm(draft, setDraft, errors)}
      <div className="form-actions">
        <button className="btn" disabled={busy}>
          {busy ? 'Đang lưu…' : `Lưu ${noun}`}
        </button>
        <button type="button" className="btn btn-ghost" onClick={close}>
          Huỷ
        </button>
      </div>
    </form>
  );

  return (
    <div>
      <Notice kind={msg?.kind}>{msg?.text}</Notice>
      {items.length === 0 && editing !== 'new' && <p className="muted">{empty}</p>}
      <ul className="admin-list">
        {items.map((item) =>
          editing === item.id ? (
            <li key={item.id} className="editing">
              {form}
            </li>
          ) : (
            <li key={item.id}>
              <div>
                <strong>{describe(item).title}</strong>
                <span className="muted small">{describe(item).subtitle}</span>
              </div>
              <div className="row-actions">
                <button type="button" className="link-button" onClick={() => open(item)}>
                  Sửa
                </button>
                <button type="button" className="link-button danger" onClick={() => remove(item)}>
                  Xoá
                </button>
              </div>
            </li>
          ),
        )}
      </ul>
      {editing === 'new' ? (
        form
      ) : (
        <button type="button" className="btn" onClick={() => open(null)}>
          Thêm {noun}
        </button>
      )}
    </div>
  );
}
