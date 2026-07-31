Virtual Lab Repository
A web-based virtual laboratory platform that provides users with an interactive environment to perform cybersecurity-related experiments and practical exercises. The application consists of a frontend, backend, and MySQL database.

---

Prerequisites
Before running this project, ensure the following software is installed on your system:

- Visual Studio Code (VS Code)
- Node.js
- Docker Desktop
- MySQL Workbench 8.0 CE

---

Project Setup

1. Configure MySQL

Open MySQL Workbench 8.0 CE and create a new MySQL connection using the following details:

| Connection Name: cyberlab
| Hostname:        localhost
| Port:            3306
| Password:        1234

Create the Database Schema

1. Create and Connect to the "cyberlab" MySQL connection.
2. Create a new schema named "cyberlab"
3. Apply the changes.

Import the Database

1. In MySQL Workbench, click Server → Data Import
2. Select Import from Self-Contained File
3. Browse to the following folder: Virtual-Lab-Repository/sql/dumps/

4. Select the required .sql dump file
5. Under Default Target Schema, select cyberlab
6. Click Start Import
7. Wait until the database import completes successfully.

> Note: Ensure the cyberlab schema is created and selected before importing the database.

---

2. Run the Application

Start the Backend

cd backend
npm start

---

3. Docker

Ensure Docker Desktop is running before starting any Docker containers required by the project.

---

Technologies Used

- HTML5
- CSS3
- JavaScript
- Node.js
- Express.js
- MySQL
- Docker

---

# Notes

- Ensure MySQL Server is connected before starting the backend.
- Verify that the cyberlab MySQL connection is configured correctly.
- Import the SQL dump before running the application.
- Install all Node.js dependencies using npm install.
- Keep Docker Desktop running if your project uses Docker services.

---

# Authors

NIZAMPATNAM YASHWANTH
Developed as part of the Virtual Lab Repository project.