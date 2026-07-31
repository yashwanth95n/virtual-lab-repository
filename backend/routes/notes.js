const express = require("express");
const router = express.Router();

router.get("/:courseId", (req, res) => {
    res.json([
        {
            id: 1,
            title: "Linux Basic Notes",
            note_file: "linux-basic-notes.txt"
        },
        {
            id: 2,
            title: "Linux Commands Notes",
            note_file: "linux-commands-notes.txt"
        },
        {
            id: 3,
            title: "Linux Security Notes",
            note_file: "linux-security-notes.txt"
        }
    ]);
});

module.exports = router;