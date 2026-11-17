# Personal Portfolio — Spring Boot + React + MySQL

Website portfolio cá nhân: trang giới thiệu (hồ sơ, kỹ năng, kinh nghiệm, dự án), blog (viết / sửa / nháp / đăng bằng Markdown), bình luận, đăng nhập bằng **Google** hoặc **tài khoản đăng ký** (mật khẩu bắt buộc có chữ hoa, chữ thường, số, ký tự đặc biệt, tối thiểu 8 ký tự).

| Phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 4.1 (Spring MVC, Spring Security 7, Spring Data JPA, OAuth2 Client + Resource Server), Maven multi-module |
| Frontend | React 19, React Router 7, Vite 8, react-markdown |
| Database | MySQL 8.4 |

Chi tiết các biện pháp bảo mật: [SECURITY.md](SECURITY.md).

---

## 1. Kiến trúc (SOA)

Backend chia thành các **service độc lập**, mỗi service có *hợp đồng công khai* (`api`: interface + DTO), *phần cài đặt nội bộ* (`internal`: entity, repository, logic) và *endpoint* (`web`: REST controller). Service này chỉ gọi service khác qua interface trong `api`, không bao giờ đụng vào entity/bảng của nhau (bài viết chỉ lưu `authorId`, lấy tên tác giả qua `UserDirectory`).

```
backend/
├─ portfolio-common      BaseEntity, ServiceException, ApiError, PageResponse, @HttpUrl, Caller
├─ identity-service      Tài khoản, đăng ký / đăng nhập, Google OAuth2, JWT, refresh token
│   ├─ api/              AuthService, AccountService, UserDirectory, DTO, @StrongPassword
│   ├─ internal/         UserAccount, RefreshToken, repository, AuthServiceImpl, ...
│   └─ web/              AuthController, AccountController, Google handlers, cookie, origin guard
├─ profile-service       Hồ sơ, kỹ năng, kinh nghiệm, dự án
├─ content-service       Bài viết (nháp / đăng, thẻ, slug), bình luận  ──► dùng identity.api.UserDirectory
└─ portfolio-app         Composition root: SecurityFilterChain, CORS, rate limit, xử lý lỗi, cấu hình
```

Bên trong mỗi service là mô hình **Spring MVC** nhiều lớp: `Controller → Service (interface) → ServiceImpl → Repository (Spring Data JPA) → MySQL`.

```
frontend/src/
├─ api/          client.js (fetch + access token trong bộ nhớ + tự refresh), endpoints.js
├─ auth/         AuthContext, RequireAuth
├─ components/   Layout, Markdown (an toàn), PasswordChecklist, GoogleButton, Form
└─ pages/        Home, Posts, PostDetail, Login, Register, Account, admin/*
```

Giao diện được làm theo skill **frontend-design** trong repo GitHub `anthropics/skills`: bảng màu riêng (Paper / Ink / Slate / Cobalt / Marigold), 2 font tương phản (Bricolage Grotesque cho tiêu đề, Newsreader cho phần đọc), một điểm nhấn duy nhất (tên cỡ lớn + khối vàng sau ảnh chân dung), một chuyển động khi tải trang, hỗ trợ dark mode, bàn phím, `prefers-reduced-motion`, và màn hình điện thoại.

---

## 2. Chạy dự án

Yêu cầu: **JDK 21+**, **Node 20+**, **MySQL 8.x** (đã có Maven Wrapper, không cần cài Maven).

### 2.1 Tạo user MySQL riêng (khuyên dùng, thay vì root)

```sql
CREATE DATABASE portfolio CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'portfolio_app'@'localhost' IDENTIFIED BY 'mat-khau-manh-cua-ban';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON portfolio.* TO 'portfolio_app'@'localhost';
```

Hoặc dùng MySQL trong Docker: `DB_PASSWORD=... docker compose up -d` (cổng 3307, đặt `DB_PORT=3307`).

### 2.2 Cấu hình

```bash
cp backend/.env.example backend/.env
```

Mở `backend/.env`, điền `DB_USERNAME`, `DB_PASSWORD`. Bảng sẽ được tạo tự động lần chạy đầu.

### 2.3 Chạy backend (cổng 8080)

```bash
cd backend
./mvnw install -DskipTests
./mvnw -pl portfolio-app spring-boot:run
```

Hoặc chạy file jar: `java -jar portfolio-app/target/portfolio.jar` (từ thư mục `backend`).

Trên Windows PowerShell dùng `.\mvnw.cmd` thay cho `./mvnw`.

### 2.4 Chạy frontend (cổng 5173)

```bash
cd frontend
npm install
npm run dev
```

Mở http://localhost:5173 → **Đăng nhập → Tạo tài khoản**. Tài khoản **đầu tiên** sẽ là chủ portfolio (ADMIN) — hoặc đặt email của bạn vào `ADMIN_EMAILS`. Sau đó vào menu tài khoản → **Quản lý portfolio** để điền thông tin cá nhân, kinh nghiệm, dự án và viết bài. Khi đã có tài khoản chủ, đặt `FIRST_USER_ADMIN=false`.

### 2.5 Bật đăng nhập Google

1. https://console.cloud.google.com → *Google Auth Platform*: ở *Audience* chọn External (Gmail cá nhân mặc định là External) và thêm email vào *Test users*; ở *Branding* điền tên app và email.
2. *Clients* → *Create client* → *Web application*.
3. *Authorized redirect URIs*: `http://localhost:8080/login/oauth2/code/google`
4. Điền `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET` vào `backend/.env`, khởi động lại backend. Nút **Tiếp tục với Google** sẽ tự hiện.

**Đăng nhập Google không cần gõ lại mật khẩu:** Google hỏi mật khẩu hay chỉ cần bấm "Có" trên điện thoại là do *tài khoản Google* của bạn quyết định, không phải website. Vào https://myaccount.google.com/security → *Cách bạn đăng nhập vào Google* → bật *Xác minh 2 bước* với *Lời nhắc của Google* (Google prompt) hoặc tạo *Khoá truy cập* (passkey). Nếu trình duyệt đã đăng nhập sẵn Google thì chỉ cần chọn tài khoản.

**Ghi nhớ đăng nhập:** mỗi lần đăng nhập có hiệu lực **1 ngày** (`SESSION_TTL=1d`), kể cả khi tắt trình duyệt hay khởi động lại server. Hết 1 ngày phải đăng nhập lại.

### 2.6 Chế độ demo không cần MySQL

```bash
cd backend
./mvnw install -DskipTests
./mvnw -pl portfolio-app spring-boot:test-run
```

Chạy ứng dụng thật trên H2 trong bộ nhớ (mất dữ liệu khi tắt). Chỉ dùng để thử nhanh.

### 2.7 Kiểm thử

```bash
cd backend
./mvnw test
```

21 integration test (MockMvc + H2) kiểm tra: chính sách mật khẩu, cookie an toàn, khoá tài khoản, chống dò tài khoản, xoay vòng và phát hiện tái sử dụng refresh token, chặn Origin lạ, token giả `alg: none`, phân quyền ADMIN / chủ bình luận, chặn URL `javascript:`, header bảo mật, giới hạn phân trang.

---

## 3. API chính

| Method | Đường dẫn | Quyền |
|---|---|---|
| GET | `/api/profile` | công khai |
| PUT | `/api/admin/profile` | ADMIN |
| POST/PUT/DELETE | `/api/admin/profile/experiences[/{id}]`, `/api/admin/profile/projects[/{id}]` | ADMIN |
| GET | `/api/posts?page&size&tag`, `/api/posts/tags`, `/api/posts/{slug}` | công khai (chỉ bài đã đăng) |
| GET / POST | `/api/posts/{slug}/comments` | xem: công khai, gửi: đã đăng nhập |
| DELETE | `/api/comments/{id}` | người viết hoặc ADMIN |
| GET/POST/PUT/DELETE | `/api/admin/posts[/{id}]` | ADMIN |
| GET | `/api/auth/providers` | công khai |
| POST | `/api/auth/register`, `/login`, `/refresh`, `/logout` | công khai (có rate limit) |
| GET | `/oauth2/authorization/google` | bắt đầu đăng nhập Google |
| GET/PUT | `/api/account/me` · POST `/api/account/password` | đã đăng nhập |

---

## 4. Triển khai thật (production)

`Dockerfile` ở thư mục gốc build React và đóng gói vào Spring Boot: **một image, một domain** cho cả giao diện lẫn `/api` (cookie refresh vẫn first-party, không cần cấu hình CORS thêm).

```bash
docker build -t portfolio .
docker run -p 8080:8080 --env-file backend/.env portfolio
```

Deploy lên Railway / Render: chọn repo GitHub, runtime Docker, khai báo biến môi trường:

| Biến | Giá trị |
| --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Thông tin MySQL trên cloud |
| `JWT_SECRET` | Khoá mới, ≥ 32 byte (`base64:<giá trị>`) |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Client Google OAuth |
| `FRONTEND_URL`, `ALLOWED_ORIGINS` | Cùng một giá trị: `https://<domain-của-app>` |
| `ADMIN_EMAILS` | Email chủ portfolio |
| `FIRST_USER_ADMIN` | `false` |

- Google Cloud Console: thêm redirect URI `https://<domain-của-app>/login/oauth2/code/google`.
- `server.forward-headers-strategy=native` đã bật sẵn: redirect URI dùng `https` và rate limit thấy đúng IP người dùng khi chạy sau proxy của nền tảng.
- Đặt `JPA_DDL_AUTO=validate` và quản lý schema bằng migration (Flyway) khi dữ liệu đã quan trọng.
