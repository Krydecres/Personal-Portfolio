import { Link } from 'react-router-dom';
import { useProfile } from '../lib/useProfile.js';

const UPDATED = '17/11/2026';

/** Owner name and contact e-mail come from the public profile (edited in /admin). */
function useOwner() {
  const { profile } = useProfile();
  return {
    name: profile?.fullName || 'chủ trang web',
    email: profile?.contactEmail || null,
  };
}

function Contact({ email }) {
  return email ? (
    <a href={`mailto:${email}`}>{email}</a>
  ) : (
    <>email liên hệ trên <Link to="/">trang chủ</Link></>
  );
}

export function PrivacyPolicy() {
  const { name, email } = useOwner();
  const site = window.location.origin;
  return (
    <section className="page-narrow legal">
      <h1 className="page-title">Chính sách quyền riêng tư</h1>
      <p className="muted">Cập nhật lần cuối: {UPDATED}</p>
      <p>
        Trang web {site} do {name} vận hành cho mục đích giới thiệu cá nhân và học tập.
      </p>

      <h2>1. Thông tin được thu thập</h2>
      <ul>
        <li>
          Đăng ký bằng email: địa chỉ email, tên hiển thị và mật khẩu. Mật khẩu chỉ được lưu dưới dạng hash
          BCrypt, không ai đọc được mật khẩu gốc.
        </li>
        <li>
          Đăng nhập bằng Google: địa chỉ email, tên, ảnh đại diện, mã định danh tài khoản Google và trạng thái
          xác minh email do Google cung cấp. Trang web không nhận và không lưu mật khẩu Google.
        </li>
        <li>Nội dung bạn tạo: bình luận và thông tin tài khoản bạn tự nhập.</li>
        <li>
          Dữ liệu kỹ thuật: thời điểm đăng nhập gần nhất, số lần đăng nhập sai (để khoá tạm thời khi có dấu
          hiệu dò mật khẩu), mã phiên đăng nhập dưới dạng hash. Địa chỉ IP chỉ được giữ tạm trong bộ nhớ máy
          chủ để giới hạn số request, không được lưu vào cơ sở dữ liệu.
        </li>
      </ul>

      <h2>2. Mục đích sử dụng</h2>
      <p>
        Thông tin chỉ dùng để tạo và duy trì tài khoản, cho phép đăng nhập, hiển thị tên bên cạnh nội dung bạn
        đăng, và bảo vệ trang web khỏi truy cập trái phép.
      </p>

      <h2>3. Cookie</h2>
      <p>
        Trang web chỉ dùng cookie bắt buộc: <code>pf_refresh</code> để duy trì phiên đăng nhập (tối đa 1 ngày)
        và <code>PF_OAUTH</code> để hoàn tất đăng nhập Google (tối đa 5 phút). Không dùng cookie quảng cáo hay
        theo dõi.
      </p>

      <h2>4. Chia sẻ thông tin</h2>
      <p>
        Thông tin cá nhân không được bán, cho thuê hay chia sẻ với bên thứ ba, trừ khi pháp luật yêu cầu. Dữ
        liệu được lưu trên hạ tầng của nhà cung cấp hosting (Railway) chỉ để vận hành trang web.
      </p>

      <h2>5. Lưu trữ và xoá dữ liệu</h2>
      <p>
        Dữ liệu được lưu trong thời gian trang web hoạt động. Bạn có thể yêu cầu xoá tài khoản và toàn bộ dữ
        liệu liên quan qua <Contact email={email} />.
      </p>

      <h2>6. Bảo mật</h2>
      <p>
        Kết nối dùng HTTPS. Mật khẩu được lưu dưới dạng hash BCrypt. Phiên đăng nhập dùng token có thời hạn
        ngắn và cookie HttpOnly.
      </p>

      <h2>7. Liên hệ</h2>
      <p>
        Câu hỏi về chính sách này: <Contact email={email} />.
      </p>
    </section>
  );
}

export function TermsOfService() {
  const { name, email } = useOwner();
  const site = window.location.origin;
  return (
    <section className="page-narrow legal">
      <h1 className="page-title">Điều khoản sử dụng</h1>
      <p className="muted">Cập nhật lần cuối: {UPDATED}</p>
      <p>Khi sử dụng trang web {site}, bạn đồng ý với các điều khoản sau.</p>

      <h2>1. Mục đích</h2>
      <p>
        Đây là trang portfolio cá nhân của {name}, phục vụ mục đích giới thiệu và học tập. Trang web được cung
        cấp miễn phí, theo hiện trạng, không cam kết hoạt động liên tục.
      </p>

      <h2>2. Tài khoản</h2>
      <ul>
        <li>Bạn chịu trách nhiệm giữ bí mật mật khẩu của mình.</li>
        <li>Thông tin đăng ký phải là thông tin thật, không mạo danh người khác.</li>
        <li>Tài khoản bị khoá đăng nhập 15 phút sau 5 lần nhập sai mật khẩu liên tiếp.</li>
      </ul>

      <h2>3. Nội dung người dùng</h2>
      <ul>
        <li>Bạn giữ quyền với nội dung bạn đăng.</li>
        <li>Không đăng nội dung vi phạm pháp luật, xúc phạm, spam, mã độc hoặc vi phạm bản quyền.</li>
        <li>Quản trị viên có quyền xoá nội dung vi phạm và khoá tài khoản vi phạm.</li>
      </ul>

      <h2>4. Hành vi bị cấm</h2>
      <p>
        Không được truy cập trái phép, dò mật khẩu, gửi request tự động với số lượng lớn hoặc làm gián đoạn
        hoạt động của trang web.
      </p>

      <h2>5. Giới hạn trách nhiệm</h2>
      <p>
        Chủ trang web không chịu trách nhiệm cho thiệt hại phát sinh từ việc sử dụng hoặc không thể sử dụng
        trang web, bao gồm mất dữ liệu.
      </p>

      <h2>6. Thay đổi</h2>
      <p>Điều khoản có thể được cập nhật; phiên bản mới có hiệu lực khi được đăng trên trang này.</p>

      <h2>7. Liên hệ</h2>
      <p>
        <Contact email={email} />
      </p>
    </section>
  );
}
