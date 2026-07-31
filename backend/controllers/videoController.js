const db = require("../db");

// Get all videos for a course
exports.getVideos = (req, res) => {
    const { courseId } = req.params;

    const sql = `
        SELECT *
        FROM videos
        WHERE course_id = ?
        ORDER BY order_no ASC
    `;

    db.query(sql, [courseId], (err, results) => {
        if (err) {
            console.error(err);
            return res.status(500).json({
                success: false,
                message: "Database error"
            });
        }

        res.json({
            success: true,
            videos: results
        });
    });
};

// Get user's progress for a course
exports.getProgress = (req, res) => {
    const { userId, courseId } = req.params;

    const sql = `
        SELECT
            vp.video_id,
            vp.completed
        FROM video_progress vp
        INNER JOIN videos v
        ON vp.video_id = v.id
        WHERE
            vp.user_id = ?
            AND v.course_id = ?
    `;

    db.query(sql, [userId, courseId], (err, results) => {

        if (err) {
            console.error(err);
            return res.status(500).json({
                success: false,
                message: "Database error"
            });
        }

        res.json({
            success: true,
            progress: results
        });

    });
};

// Mark video as completed
exports.completeVideo = (req, res) => {

    const { user_id, video_id } = req.body;

    const sql = `
        INSERT INTO video_progress
        (user_id, video_id, completed, completed_at)
        VALUES (?, ?, TRUE, NOW())
        ON DUPLICATE KEY UPDATE
        completed = TRUE,
        completed_at = NOW()
    `;

    db.query(sql, [user_id, video_id], (err) => {

        if (err) {
            console.error(err);
            return res.status(500).json({
                success: false,
                message: "Database error"
            });
        }

        res.json({
            success: true,
            message: "Video marked as completed."
        });

    });

};

// Dashboard Progress
exports.dashboardProgress = (req, res) => {

    const { userId } = req.params;

    const sql = `
        SELECT
            c.id,
            c.title,

            COUNT(v.id) AS totalVideos,

            SUM(
                CASE
                    WHEN vp.completed = TRUE THEN 1
                    ELSE 0
                END
            ) AS completedVideos

        FROM courses c

        LEFT JOIN videos v
            ON c.id = v.course_id

        LEFT JOIN video_progress vp
            ON vp.video_id = v.id
            AND vp.user_id = ?

        GROUP BY c.id
    `;

    db.query(sql, [userId], (err, results) => {

        if (err) {
            console.error(err);

            return res.status(500).json({
                success: false,
                message: "Database error"
            });
        }

        const progress = results.map(course => {

            const percent =
                course.totalVideos == 0
                    ? 0
                    : Math.round(
                          (course.completedVideos / course.totalVideos) * 100
                      );

            return {
                courseId: course.id,
                courseName: course.title,
                completedVideos: course.completedVideos,
                totalVideos: course.totalVideos,
                progress: percent
            };

        });

        res.json({
            success: true,
            progress
        });

    });

};