const express = require("express");
const router = express.Router();

const videoController = require("../controllers/videoController");

router.get("/progress/:userId/:courseId", videoController.getProgress);

router.get("/dashboard/:userId", videoController.dashboardProgress);

router.post("/complete", videoController.completeVideo);

router.get("/:courseId", videoController.getVideos);

module.exports = router;