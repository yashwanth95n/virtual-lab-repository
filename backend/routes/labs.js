const express = require("express");
const router = express.Router();

const {
    launchLab,
    stopLab
} = require("../controllers/labController");

// Launch Ubuntu Lab
router.post("/launch", launchLab);

// Stop Ubuntu Lab
router.post("/stop", stopLab);

module.exports = router;