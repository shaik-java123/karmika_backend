# Karmika HRMS - Backend Setup & Startup Guide

## 🚀 Quick Start

### Option 1: Using PowerShell (Recommended)
```powershell
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope CurrentUser
cd e:\Workspace\Karmika\backend
.\START-BACKEND.ps1
```

### Option 2: Using Batch File
```bash
cd e:\Workspace\Karmika\backend
START-BACKEND.bat
```

### Option 3: Using Maven Directly
```bash
cd e:\Workspace\Karmika\backend
mvn spring-boot:run
```

---

## 📋 Prerequisites

Before starting, ensure you have:

1. **Java 17 or Higher**
   ```bash
   java -version
   # Should show: openjdk version "17.x.x"
   ```

2. **Maven 3.6 or Higher**
   ```bash
   mvn -version
   # Should show: Apache Maven 3.6.x or higher
   ```

3. **MySQL 8.0 or Higher**
   ```bash
   mysql --version
   # Should show: mysql Ver 8.x
   ```

4. **MySQL Running**
   - Database: `karmika_hrms`
   - Username: `root`
   - Password: `moin`
   - Port: `3306`

---

## 🔧 Troubleshooting

### Issue: Lombok Compilation Error
**Error**: `java.lang.ExceptionInInitializerError` or `com.sun.tools.javac.code.TypeTag :: UNKNOWN`

**Solution**:
1. See `STARTUP-TROUBLESHOOTING.md` for detailed solutions
2. Try: `mvn clean install -DskipTests`
3. Then: `mvn spring-boot:run`

### Issue: MySQL Connection Failed
**Error**: `Communications link failure`

**Solution**:
1. Verify MySQL is running: `mysql -u root -p` (password: `moin`)
2. Check database exists: `SHOW DATABASES;`
3. If database doesn't exist, create it:
   ```sql
   CREATE DATABASE karmika_hrms;
   ```

### Issue: Port 8080 Already in Use
**Error**: `Address already in use`

**Solution**:
```bash
# Kill the process using port 8080
netstat -ano | findstr :8080
taskkill /PID <process_id> /F

# Or use a different port (edit application.yml)
# Change: server.port: 8081
```

### Issue: Build Takes Too Long
**Solution**: Skip tests during build
```bash
mvn clean package -DskipTests
```

---

## 📊 Expected Output

When the backend starts successfully:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_|\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 
2026-02-25T17:30:00.000+05:30  INFO 12345 --- [main] c.k.h.KarmikaHrmsApplication : Starting KarmikaHrmsApplication

✅ Default admin user created:
   Username: admin
   Password: admin123

✅ Default HR user created:
   Username: hr
   Password: hr123

✅ Default employee user created:
   Username: employee
   Password: employee123

INFO o.h.e.t.j.p.i.TieredStatementWarning : HHH90000022: Hibernate is generating extra join statements. This is due to fetch join fetch not properly handled.

Tomcat started on port(s): 8080 (http) with context path ''
Started KarmikaHrmsApplication in 5.234 seconds
```

---

## ✅ Verify Installation

After the backend starts, verify in another terminal:

### Test 1: Health Check
```bash
curl http://localhost:8080/api/auth/test
# Response: OK or success message
```

### Test 2: Swagger UI
Open in browser:
```
http://localhost:8080/swagger-ui.html
```

You should see the Swagger API documentation.

### Test 3: Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

You should get a JWT token in response.

---

## 🔑 Default Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | admin123 |
| HR | hr | hr123 |
| Employee | employee | employee123 |

**⚠️ IMPORTANT**: Change these passwords in production!

---

## 📁 Important Files

- **pom.xml** - Maven configuration
- **application.yml** - Spring Boot configuration
- **lombok.config** - Lombok annotation processor config
- **START-BACKEND.ps1** - PowerShell startup script
- **START-BACKEND.bat** - Batch startup script
- **STARTUP-TROUBLESHOOTING.md** - Detailed troubleshooting guide

---

## 🛑 Stopping the Server

Press `Ctrl+C` in the terminal where the server is running.

---

## 🔄 Frontend Setup

Once the backend is running, start the frontend in another terminal:

```bash
cd e:\Workspace\Karmika\frontend
npm run dev
```

Frontend will be available at: `http://localhost:5173`

---

## 📝 Configuration

### Change Server Port
Edit `src/main/resources/application.yml`:
```yaml
server:
  port: 8081  # Change from 8080 to 8081
```

### Change Database Credentials
Edit `src/main/resources/application.yml`:
```yaml
datasource:
  url: jdbc:mysql://localhost:3306/karmika_hrms
  username: root          # Change if needed
  password: moin          # Change if needed
```

### Change JWT Secret
Edit `src/main/resources/application.yml`:
```yaml
jwt:
  secret: your-new-secret-key-here
  expiration: 86400000
```

---

## 🐛 Debug Mode

To see full logs and debug information:

```bash
mvn spring-boot:run -X
```

Or with specific log level:

```bash
java -jar target/hrms-1.0.0.jar --logging.level.root=DEBUG
```

---

## 📊 Common Maven Commands

```bash
# Clean build
mvn clean

# Compile only
mvn compile

# Package as JAR
mvn package -DskipTests

# Run tests
mvn test

# Run application
mvn spring-boot:run

# View dependencies
mvn dependency:tree

# Update dependencies
mvn versions:display-dependency-updates
```

---

## 🚀 Production Deployment

Before deploying to production:

1. **Security**:
   - Change JWT secret to a strong random key
   - Change all default passwords
   - Use HTTPS/SSL
   - Set `ddl-auto: validate` (not `update`)

2. **Configuration**:
   - Update database credentials
   - Update upload directory
   - Set proper timezone
   - Configure CORS

3. **Performance**:
   - Increase heap memory: `-Xmx2g`
   - Enable caching
   - Optimize database queries
   - Use external database

4. **Monitoring**:
   - Enable logging
   - Set up monitoring tools
   - Configure alerts
   - Backup database regularly

---

## 📞 Support

If you encounter any issues:

1. Check `STARTUP-TROUBLESHOOTING.md`
2. Review the error message carefully
3. Check that all prerequisites are installed
4. Verify MySQL is running
5. Check that port 8080 is available
6. Try building with: `mvn clean install -DskipTests`

---

## 📝 Notes

- First startup may take 2-5 minutes (compiling Lombok annotations)
- Default sample data is auto-initialized on startup
- Database is auto-created if it doesn't exist
- All endpoints are documented in Swagger UI

---

**Created**: February 25, 2026
**Version**: 1.0.0
**Status**: Ready for Use

