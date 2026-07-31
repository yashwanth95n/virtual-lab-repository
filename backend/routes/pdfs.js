const express = require("express");
const router = express.Router();
const db = require("../config/db");

// Get PDFs by Course
router.get("/:courseId", (req, res) => {
    const { courseId } = req.params;

    db.query(
        "SELECT * FROM pdfs WHERE course_id = ? ORDER BY id",
        [courseId],
        (err, results) => {
            if (err) {
                return res.status(500).json({
                    success: false,
                    message: err.message
                });
            }

            res.json({
                success: true,
                pdfs: results
            });
        }
    );
});

// Get PDF Progress
router.get("/progress/:userId/:courseId", (req, res) => {
    const { userId, courseId } = req.params;

    const sql = `
        SELECT pp.pdf_id
        FROM pdf_progress pp
        JOIN pdfs p ON pp.pdf_id = p.id
        WHERE pp.user_id = ? AND p.course_id = ?
    `;

    db.query(sql, [userId, courseId], (err, results) => {

        if (err) {
            return res.status(500).json({
                success: false,
                message: err.message
            });
        }

        res.json({
            success: true,
            completed: results
        });

    });
});

// Mark PDF Completed
router.post("/complete", (req, res) => {

    const { user_id, pdf_id } = req.body;

    const sql = `
        INSERT IGNORE INTO pdf_progress(user_id,pdf_id)
        VALUES(?,?)
    `;

    db.query(sql, [user_id, pdf_id], (err) => {

        if (err) {
            return res.status(500).json({
                success: false,
                message: err.message
            });
        }

        res.json({
            success: true,
            message: "PDF marked as completed."
        });

    });

});

module.exports = router;