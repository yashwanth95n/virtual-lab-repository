const express = require("express");
const router = express.Router();

const authController = require("../controllers/authController");

// Authentication
router.post("/register", authController.register);
router.post("/login", authController.login);

// Profile
router.put("/profile/name", authController.updateName);
router.put("/profile/email", authController.updateEmail);
router.put("/profile/password", authController.updatePassword);

module.exports = router;