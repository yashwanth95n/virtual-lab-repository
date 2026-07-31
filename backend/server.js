const express = require("express");
const cors = require("cors");
const dotenv = require("dotenv");
const http = require("http");
const { Server } = require("socket.io");

dotenv.config();

const db = require("./db");

const authRoutes = require("./routes/auth");
const videoRoutes = require("./routes/videos");
const pdfRoutes = require("./routes/pdfs");
const noteRoutes = require("./routes/notes");
const labRoutes = require("./routes/labs");

const terminalSocket = require("./sockets/terminal");

const app = express();

// Create HTTP server
const server = http.createServer(app);

// Initialize Socket.IO
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

app.use(cors());
app.use(express.json());

// API Routes
app.use("/api/auth", authRoutes);
app.use("/api/videos", videoRoutes);
app.use("/api/pdfs", pdfRoutes);
app.use("/api/notes", noteRoutes);
app.use("/api/labs", labRoutes);

// Static Files
app.use("/pdfs", express.static("public/pdfs"));
app.use("/notes", express.static("public/notes"));

app.get("/", (req, res) => {
    res.send("Virtual Lab Repository Backend Running 🚀");
});

// Initialize Terminal Socket
terminalSocket(io);

const PORT = process.env.PORT || 5000;

// Start Server
server.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});