# Backend Startup Error - Troubleshooting Guide

## Issue
```
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

This is a Lombok annotation processing compatibility issue.

---

## Solution 1: Update IDE Settings (IntelliJ IDEA)

### Step 1: Enable Annotation Processing
1. Open IntelliJ IDEA
2. Go to: **File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors**
3. Check: **Enable annotation processing**
4. Check: **Obtain processors from project classpath**

### Step 2: Rebuild Project
1. Go to: **Build → Clean Project**
2. Go to: **Build → Rebuild Project**

### Step 3: Invalidate IDE Cache
1. Go to: **File → Invalidate Caches and Restart**
2. Click: **Invalidate and Restart**

---

## Solution 2: Maven Clean Build

Open terminal and run:

```bash
cd e:\Workspace\Karmika\backend

# Clean everything
mvn clean

# Rebuild with dependencies
mvn install -DskipTests

# Package the application
mvn package -DskipTests

# Start the application
mvn spring-boot:run
```

---

## Solution 3: Start with Spring Boot Maven Plugin

```bash
cd e:\Workspace\Karmika\backend
mvn spring-boot:run
```

---

## Solution 4: Run Pre-built JAR

```bash
cd e:\Workspace\Karmika\backend
java -jar target/hrms-1.0.0.jar
```

---

## What We Fixed

✅ Updated Lombok to version 1.18.30 (stable)
✅ Changed Lombok scope to 'provided' (correct)
✅ Added Maven compiler parameters
✅ Created lombok.config file for proper initialization
✅ Added annotation processor paths configuration

---

## Expected Output After Fix

When the backend starts successfully, you should see:

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

Tomcat started on port(s): 8080 (http)
Started KarmikaHrmsApplication in 5.234 seconds
```

---

## Verify Installation

Once the backend is running, test with:

```bash
# In a new terminal
curl http://localhost:8080/api/auth/test

# Or open in browser
http://localhost:8080/swagger-ui.html
```

You should see Swagger UI documentation.

---

## Still Having Issues?

### Check MySQL Connection
```bash
# Verify MySQL is running
# Database: karmika_hrms
# Username: root
# Password: moin
```

### Check Java Version
```bash
java -version
# Should be Java 17 or higher
```

### Check Port 8080
```bash
# Make sure port 8080 is not in use
netstat -ano | findstr :8080
```

### View Full Logs
```bash
cd e:\Workspace\Karmika\backend
mvn spring-boot:run -X
```

---

## Files We Updated

1. **pom.xml** - Updated Lombok configuration
2. **lombok.config** - Added Lombok initialization config (NEW)
3. **.idea/compiler.xml** - IDE annotation processing config (EXISTS)

---

## Next Steps

1. Try Maven clean build (Solution 2)
2. If that fails, try IDE settings (Solution 1)
3. If that fails, try spring-boot plugin (Solution 3)
4. If that fails, try pre-built JAR (Solution 4)

---

**Created**: February 25, 2026
**Version**: 1.0.0
**Status**: Ready to Use

