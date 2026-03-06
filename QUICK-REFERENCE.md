# ⚡ Backend Startup - Quick Reference

## 🚀 Start Backend (Pick One)

### PowerShell ✅ (RECOMMENDED)
```powershell
cd e:\Workspace\Karmika\backend
.\START-BACKEND.ps1
```

### Batch
```cmd
cd e:\Workspace\Karmika\backend
START-BACKEND.bat
```

### Maven
```bash
cd e:\Workspace\Karmika\backend
mvn spring-boot:run
```

### Maven Package & Run
```bash
cd e:\Workspace\Karmika\backend
mvn clean package -DskipTests
java -jar target/hrms-1.0.0.jar
```

---

## 🔑 Default Logins

```
Admin:    admin / admin123
HR:       hr / hr123
Employee: employee / employee123
```

---

## 🌐 Access Points

```
API:        http://localhost:8080/api
Swagger:    http://localhost:8080/swagger-ui.html
Health:     http://localhost:8080/api/auth/test
```

---

## 📋 Prerequisites

- Java 17+ ✓
- Maven 3.6+ ✓
- MySQL 8.0+ (running) ✓
- Port 8080 (free) ✓

---

## 🆘 Quick Fixes

| Problem | Fix |
|---------|-----|
| **Lombok Error** | `mvn clean install -DskipTests` |
| **MySQL Fail** | Start MySQL service |
| **Port 8080 used** | Kill process: `taskkill /PID <id> /F` |
| **Build slow** | Add `-DskipTests` flag |

---

## 📂 Important Files

| File | Purpose |
|------|---------|
| `pom.xml` | Maven config (UPDATED) |
| `lombok.config` | Lombok settings (NEW) |
| `START-BACKEND.ps1` | Startup script (NEW) |
| `application.yml` | Spring config |

---

## 📖 Documentation

- `BACKEND-SETUP-GUIDE.md` - Complete guide
- `STARTUP-TROUBLESHOOTING.md` - Detailed help
- `STARTUP-FIX-SUMMARY.md` - What was fixed

---

## ✅ Verification

Backend is running when you see:
```
✅ Default admin user created
Tomcat started on port(s): 8080
Started KarmikaHrmsApplication
```

---

## 🎯 Next

1. Start backend: `.\START-BACKEND.ps1`
2. Wait for startup message
3. Open: http://localhost:8080/swagger-ui.html
4. Login with: admin / admin123

---

**Last Updated**: Feb 25, 2026
**Status**: ✅ Ready

