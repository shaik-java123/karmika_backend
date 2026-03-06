# ✅ Backend Startup - Pre-Launch Checklist

## 🔍 Verification Checklist

### Prerequisites
- [ ] Java 17+ installed: `java -version`
- [ ] Maven 3.6+ installed: `mvn -version`
- [ ] MySQL 8.0+ running: `mysql -u root -p` (password: moin)
- [ ] Port 8080 is free: `netstat -ano | findstr :8080`
- [ ] Internet connection available

### Project Setup
- [ ] Navigate to: `e:\Workspace\Karmika\backend`
- [ ] `pom.xml` exists and has Lombok 1.18.30
- [ ] `lombok.config` exists in backend folder
- [ ] `START-BACKEND.ps1` exists
- [ ] `src/main/resources/application.yml` exists

### Before Running
- [ ] MySQL is running and accessible
- [ ] Database `karmika_hrms` can be created
- [ ] No process is using port 8080
- [ ] No Java processes left from previous runs

---

## 🚀 Startup Steps

### Step 1: Prepare
```powershell
# Open PowerShell or Command Prompt
cd e:\Workspace\Karmika\backend
```
- [ ] Command executed successfully
- [ ] You're in the backend folder

### Step 2: Clean (First Time Only)
```bash
mvn clean
```
- [ ] Clean completed successfully
- [ ] `target` folder is empty

### Step 3: Start Backend
```powershell
.\START-BACKEND.ps1
```
- [ ] Script started execution
- [ ] Building in progress...

### Step 4: Wait for Startup
Look for these messages:
```
✅ Default admin user created
✅ Default HR user created
✅ Tomcat started on port(s): 8080
✅ Started KarmikaHrmsApplication
```
- [ ] Building completed
- [ ] Database initialized
- [ ] Users created
- [ ] Tomcat started on port 8080
- [ ] No errors in console

### Step 5: Verify Running
```powershell
# In a NEW terminal/PowerShell window
curl http://localhost:8080/api/auth/test
```
- [ ] Returns success response
- [ ] No connection errors
- [ ] HTTP 200 response

### Step 6: Check Swagger UI
Open in browser: `http://localhost:8080/swagger-ui.html`
- [ ] Page loads successfully
- [ ] Swagger API documentation visible
- [ ] Can see all endpoints

### Step 7: Test Login
Use credentials:
```
Username: admin
Password: admin123
```
- [ ] Login successful
- [ ] JWT token received
- [ ] Can access protected endpoints

---

## 📊 Expected Startup Output

Look for these exact messages:

```
Building Karmika HRMS...
[INFO] Building jar: target/hrms-1.0.0.jar

✅ Default admin user created:
   Username: admin
   Password: admin123
   Email: admin@karmika.com

✅ Default HR user created:
   Username: hr
   Password: hr123

✅ Default employee user created:
   Username: employee
   Password: employee123

Tomcat started on port(s): 8080 (http)
Started KarmikaHrmsApplication in X.XXX seconds
```

Mark these:
- [ ] Build successful message seen
- [ ] Admin user created message seen
- [ ] HR user created message seen
- [ ] Employee user created message seen
- [ ] Tomcat started on port 8080 message seen
- [ ] No ERROR messages in console

---

## 🆘 Troubleshooting Checklist

### If Build Fails

**Lombok Error**:
- [ ] Delete `target` folder
- [ ] Run: `mvn clean install -DskipTests`
- [ ] Check: `pom.xml` has Lombok 1.18.30
- [ ] Check: `lombok.config` exists

**Java Version Error**:
- [ ] Verify Java 17+: `java -version`
- [ ] Update Java if needed
- [ ] Set JAVA_HOME environment variable
- [ ] Restart terminal

**Maven Not Found**:
- [ ] Verify Maven installed: `mvn -version`
- [ ] Add Maven to PATH
- [ ] Restart terminal

### If MySQL Connection Fails

- [ ] Start MySQL service
- [ ] Test connection: `mysql -u root -p` (password: moin)
- [ ] Database is created: `SHOW DATABASES;`
- [ ] Check application.yml credentials
- [ ] Verify port 3306 is accessible

### If Port 8080 in Use

- [ ] Check what's using port: `netstat -ano | findstr :8080`
- [ ] Kill process: `taskkill /PID <id> /F`
- [ ] Verify port is free
- [ ] Retry startup

### If Still Failing

- [ ] Check STARTUP-TROUBLESHOOTING.md
- [ ] Read BUILD-SETUP-GUIDE.md
- [ ] Try Maven direct: `mvn spring-boot:run`
- [ ] Check logs for specific error message

---

## ✅ Success Criteria

Backend is successfully running when ALL are true:

- [ ] No errors in startup console
- [ ] "Tomcat started on port 8080" message visible
- [ ] http://localhost:8080/swagger-ui.html loads
- [ ] Can call http://localhost:8080/api/auth/test
- [ ] Login works with admin/admin123
- [ ] Can see API endpoints in Swagger

---

## 🎯 Production Readiness

Before deploying to production:

- [ ] Change JWT secret in application.yml
- [ ] Change all default passwords
- [ ] Set `hibernate.ddl-auto: validate`
- [ ] Configure HTTPS/SSL
- [ ] Set proper database credentials
- [ ] Increase JVM heap size
- [ ] Configure logging properly
- [ ] Setup database backups
- [ ] Test all API endpoints
- [ ] Load test the application

---

## 📝 Important Notes

**First Startup**:
- Takes 2-5 minutes (Lombok processing)
- Compile messages are normal
- Database auto-created
- Sample data auto-loaded

**Subsequent Startups**:
- Takes 5-10 seconds
- Much faster after first build
- Processes cached

**Common Warnings** (These are OK):
```
HHH90000022: Hibernate is generating extra join statements
(Non-critical ORM optimization message)
```

---

## 🔧 If You Need to Stop Backend

Press: **Ctrl + C** in the terminal running the backend

```
Shutdown key entered; shutting down all active threads, please wait...
Shutting down ProtocolHandler ["http-nio-8080"]
```

---

## 🎉 Final Checklist

Once you see "Tomcat started":

- [ ] Backend is running
- [ ] Swagger UI accessible
- [ ] APIs documented
- [ ] Ready for frontend
- [ ] Ready for testing

Next: Start frontend in separate terminal:
```bash
cd e:\Workspace\Karmika\frontend
npm run dev
```

---

## 📞 Support Resources

| Issue | File |
|-------|------|
| Quick start | QUICK-REFERENCE.md |
| Full setup | BACKEND-SETUP-GUIDE.md |
| Problems | STARTUP-TROUBLESHOOTING.md |
| What was fixed | STARTUP-FIX-SUMMARY.md |

---

**Checklist Created**: February 25, 2026
**Status**: Ready to Use
**All Items**: 50+

✅ Use this checklist for every startup!

