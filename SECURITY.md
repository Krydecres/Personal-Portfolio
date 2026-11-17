# Các biện pháp bảo mật đã triển khai

Mỗi mục ghi rõ nằm ở đâu trong mã nguồn. Các mục có ✔ đã có integration test (`backend/portfolio-app/src/test`).

## 1. Xác thực & mật khẩu
- **Chính sách mật khẩu mạnh** ✔ — tối thiểu 8 ký tự, có chữ hoa, chữ thường, chữ số, ký tự đặc biệt, không khoảng trắng, tối đa 72 byte (giới hạn của BCrypt, tránh bị cắt ngầm). Kiểm tra ở **server** (`@StrongPassword`, `PasswordPolicy`) và hiển thị checklist trực tiếp ở client. Quy tắc được publish qua `/api/auth/providers` để hai phía không lệch nhau.
- **Băm mật khẩu bằng BCrypt cost 12**, tự nâng cấp hash khi đăng nhập nếu cost thay đổi (`upgradeEncoding`).
- **Khoá tài khoản tạm thời** ✔ — sai 5 lần → khoá 15 phút, kể cả nhập đúng mật khẩu trong lúc khoá.
- **Chống dò tài khoản (user enumeration)** ✔ — đăng nhập sai email hay sai mật khẩu đều trả cùng một thông báo; khi email không tồn tại vẫn chạy BCrypt với hash giả để thời gian phản hồi tương đương.
- **Đổi mật khẩu đăng xuất mọi thiết bị khác** ✔ — thu hồi toàn bộ refresh token, cấp phiên mới cho thiết bị hiện tại; yêu cầu mật khẩu cũ và mật khẩu mới phải khác.
- Không bao giờ log hay trả mật khẩu/token: các DTO nhạy cảm override `toString()` thành `***`.

## 2. Đăng nhập Google (OAuth2 / OpenID Connect)
- Dùng Spring Security OAuth2 Client: tự kiểm tra **state** (chống CSRF trong luồng OAuth), **nonce**, chữ ký / issuer / audience / hạn của **ID token**.
- Bật thêm **PKCE** — mã authorization bị chặn giữa đường cũng vô dụng.
- Chỉ chấp nhận tài khoản Google có **email đã xác minh**; liên kết theo `sub` (ID cố định) chứ không chỉ theo email.
- **Chống chiếm tài khoản trước (pre-account takeover)** — nếu ai đó đã đăng ký trước bằng email của bạn (chưa xác minh), khi bạn đăng nhập Google lần đầu, mật khẩu của tài khoản đó bị xoá và mọi phiên của nó bị thu hồi.
- **Không có token trong URL**: sau khi Google trả về, backend đặt refresh cookie rồi redirect sang frontend; frontend tự gọi `/api/auth/refresh`. Không rò token qua lịch sử trình duyệt, log hay header Referer.
- URL redirect lấy từ cấu hình, không lấy từ input người dùng (không có open redirect). Phía frontend, tham số "quay lại trang trước" chỉ nhận đường dẫn nội bộ (`/...`, không nhận `//...`).
- Session server chỉ tồn tại trong lúc bắt tay OAuth (5 phút) và bị huỷ ngay sau khi xong.

## 3. Token & phiên đăng nhập
- **Access token JWT ngắn hạn (15 phút)**, ký HS256, **cố định thuật toán** khi giải mã ✔ (token `alg: none` bị từ chối), kiểm tra issuer và thời hạn. Payload tối giản (id, role) — không chứa email hay dữ liệu cá nhân.
- Secret JWT lấy từ biến môi trường, bắt buộc ≥ 256 bit (file `.env` đã có secret ngẫu nhiên 384 bit, nên khởi động lại server không làm mất đăng nhập); nếu không đặt thì sinh khoá ngẫu nhiên mỗi lần chạy — **không bao giờ có secret cứng trong mã**.
- **Access token chỉ nằm trong bộ nhớ JavaScript**, không lưu localStorage/sessionStorage (đã kiểm tra trực tiếp trên trình duyệt).
- **Refresh token** ✔:
  - Chuỗi ngẫu nhiên 256 bit (SecureRandom), DB **chỉ lưu SHA-256 hash** — lộ DB cũng không dùng được token.
  - Nằm trong cookie **HttpOnly** (JS không đọc được), **Secure**, **SameSite=Strict**, **Path=/api/auth**.
  - **Phiên đăng nhập tuyệt đối 1 ngày** ✔ (`SESSION_TTL`, mặc định `1d`): tắt trình duyệt hay khởi động lại server vẫn còn đăng nhập, nhưng refresh không bao giờ kéo dài quá 24 giờ kể từ lúc đăng nhập — token bị đánh cắp cũng chỉ dùng được tối đa 1 ngày.
  - **Xoay vòng** mỗi lần refresh; **phát hiện tái sử dụng**: dùng lại token đã xoay → thu hồi cả "họ" token (đăng xuất cả kẻ trộm lẫn nạn nhân). Có khoảng ân hạn ngắn để hai tab refresh cùng lúc không bị coi là tấn công.
  - Khoá dòng `PESSIMISTIC_WRITE` khi refresh để hai request đồng thời không cùng thành công.
  - Đăng xuất thu hồi token phía server; job đêm dọn token hết hạn.
- Đồng bộ đăng xuất giữa các tab qua `BroadcastChannel`.
- Response chứa token có `Cache-Control: no-store` ✔.

## 4. CSRF, CORS, header
- API dùng Bearer token trong header `Authorization` (trình duyệt không tự gửi) → không bị CSRF. Hai endpoint dùng cookie (`/refresh`, `/logout`) được bảo vệ hai lớp: **SameSite=Strict** + **kiểm tra Origin** ✔.
- **CORS** chỉ cho phép danh sách origin cấu hình sẵn (không dùng `*` khi có credentials), giới hạn method/header ✔ (đã thử preflight từ origin lạ → 403).
- **Security headers** ✔: `Content-Security-Policy` (API: `default-src 'none'`; frontend build: chỉ `'self'` + Google Fonts), `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Strict-Transport-Security`, `Permissions-Policy`, `Cross-Origin-Opener-Policy`.

## 5. Phân quyền
- **Mặc định từ chối**: request không khớp quy tắc nào → bị chặn (`denyAll`).
- **Hai lớp**: quy tắc URL (`/api/admin/**` cần ADMIN) + `@PreAuthorize` ở cả controller lẫn service (gọi service từ chỗ khác vẫn được bảo vệ) ✔.
- **Kiểm tra quyền sở hữu đối tượng** ✔ — chỉ người viết hoặc ADMIN được xoá bình luận (chống IDOR).
- Bài nháp không bao giờ lộ qua API công khai ✔.
- Route guard ở React chỉ để hiển thị; mọi quyết định thật nằm ở server.
- Lỗi 401/403 trả JSON thống nhất, không redirect sang trang login HTML.

## 6. Dữ liệu đầu vào & XSS / Injection
- **Bean Validation** trên mọi DTO (độ dài, định dạng email, ngày bắt đầu/kết thúc...) ✔.
- **Chặn URL nguy hiểm** ✔ — mọi trường link/ảnh chỉ nhận `http(s)://` (`@HttpUrl`), chặn `javascript:`, `data:`... Frontend lọc lại lần nữa (`safeUrl`).
- **Markdown an toàn** — `react-markdown` không bật HTML thô (không `rehype-raw`), `urlTransform` loại bỏ link `javascript:`; link ngoài có `rel="noopener noreferrer"`. Bình luận hiển thị dạng text thuần (React tự escape). Đã thử `<script>`, `<img onerror>`, `[x](javascript:...)` trên trình duyệt: không thực thi.
- **SQL injection** — chỉ dùng Spring Data JPA / JPQL với tham số bind, không nối chuỗi SQL.
- **Giới hạn kích thước**: body > 1 MB → 413 ✔; nội dung bài ≤ 200.000 ký tự; phân trang tối đa 50 bản ghi/trang ✔; tắt hoàn toàn multipart upload.
- Optimistic locking (`@Version`) — hai người sửa cùng lúc không ghi đè âm thầm.

## 7. Chống lạm dụng
- **Rate limit theo IP** ✔ (đã thử: request thứ 11/phút vào `/login` → 429 kèm `Retry-After`): login 10/phút, đăng ký 5/phút, refresh 30/phút, bình luận 10/phút, còn lại 300/phút.

## 8. Lỗi, log, cấu hình
- **Không lộ thông tin nội bộ** ✔ — tắt stacktrace / message / whitelabel; lỗi không lường trước trả mã lỗi (UUID) để tra log, không trả tên class hay SQL.
- Log cảnh báo cho sự kiện bảo mật: đăng nhập sai, phát hiện tái sử dụng token, liên kết tài khoản Google.
- **Bí mật nằm ngoài mã nguồn**: biến môi trường / file `.env` (đã có trong `.gitignore`), có `.env.example` mẫu.
- `open-in-view=false`: tầng web chỉ nhận DTO, không truy cập lazy entity.
- Hướng dẫn dùng user MySQL quyền tối thiểu thay vì root; docker-compose chỉ bind cổng vào `127.0.0.1`.
- Dependency: Spring Boot 4.1.1 (bản đang được hỗ trợ), `npm audit` = 0 lỗ hổng.

## Có thể làm thêm khi đưa lên production
- Xác minh email khi đăng ký bằng mật khẩu, quên mật khẩu qua email.
- 2FA (TOTP) cho tài khoản ADMIN.
- Rate limit dùng Redis khi chạy nhiều instance; kiểm tra mật khẩu bị lộ (HaveIBeenPwned k-anonymity).
- Flyway migration + `ddl-auto=validate`; WAF / reverse proxy giới hạn thêm.
