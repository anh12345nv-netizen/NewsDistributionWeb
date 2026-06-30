# 📰 News Distribution Web — Hệ thống Phân phối Báo Chí

Hệ thống quản lý phân phối báo chí tích hợp Web, WinForm, đồng bộ dữ liệu và Trợ lý Ảo AI.

---

## 🚀 Cài đặt & Chạy thử

### Yêu cầu
- Java 17+
- SQL Server (đã tạo sẵn 3 database: `Thanhnien`, `NewsDistributionWeb`, `NewsDistributionMaster`)
- Maven

### Các bước

**1. Clone project về máy**
```bash
git clone https://github.com/anh12345nv-netizen/NewsDistributionWeb.git
cd NewsDistributionWeb
```

**2. Tạo file cấu hình bí mật local**

Copy file mẫu và đặt tên thành `application-local.yml`:
```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

Mở file `application-local.yml` vừa tạo và điền API Key Gemini của bạn:
```yaml
ai:
  gemini:
    api-key: "YOUR_GEMINI_API_KEY_HERE"
```

> 💡 Lấy API Key Gemini **miễn phí** tại: https://aistudio.google.com/apikey

**3. Cấu hình Database**

Mở `src/main/resources/application.yml`, tìm phần datasource và cập nhật thông tin SQL Server:
```yaml
datasource-b:
  url: jdbc:sqlserver://localhost:1234;databaseName=NewsDistributionWeb;...
  username: sa
  password: YourPassword123
```

**4. Khởi động**
```bash
./mvnw spring-boot:run
```

Mở trình duyệt và truy cập: `http://localhost:8080`

---

## 🔑 Tài khoản mặc định

| Vai trò | Tên đăng nhập | Mật khẩu |
|---------|---------------|----------|
| Admin | admin | admin@123 |
| Kế toán | ketoan01 | 123 |
| Khách hàng | (tạo từ hệ thống) | — |

---

## 🤖 Tính năng nổi bật

- ✅ Đặt báo & Thanh toán QR SePay trực tuyến
- ✅ Trợ lý Ảo AI (Google Gemini) hỏi đáp số liệu
- ✅ Đồng bộ dữ liệu WinForm ↔ Web real-time
- ✅ Báo cáo doanh thu Kế toán đa chiều
- ✅ Quản lý đơn hàng & theo dõi giao hàng
