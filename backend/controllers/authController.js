const db = require("../db");
const bcrypt = require("bcrypt");

// ================= REGISTER =================

exports.register = async (req, res) => {
    try {
        const { full_name, email, password } = req.body;

        if (!full_name || !email || !password) {
            return res.status(400).json({
                success: false,
                message: "All fields are required."
            });
        }

        db.query(
            "SELECT * FROM users WHERE email = ?",
            [email],
            async (err, results) => {
                if (err) {
                    return res.status(500).json({
                        success: false,
                        message: "Database error."
                    });
                }

                if (results.length > 0) {
                    return res.status(409).json({
                        success: false,
                        message: "Email already exists."
                    });
                }

                const hashedPassword = await bcrypt.hash(password, 10);

                db.query(
                    "INSERT INTO users (full_name, email, password) VALUES (?, ?, ?)",
                    [full_name, email, hashedPassword],
                    (err) => {
                        if (err) {
                            return res.status(500).json({
                                success: false,
                                message: "Registration failed."
                            });
                        }

                        return res.status(201).json({
                            success: true,
                            message: "User registered successfully."
                        });
                    }
                );
            }
        );
    } catch (error) {
        console.error(error);
        return res.status(500).json({
            success: false,
            message: "Internal server error."
        });
    }
};

// ================= LOGIN =================

exports.login = (req, res) => {

    const { email, password } = req.body;

    if (!email || !password) {
        return res.status(400).json({
            success: false,
            message: "Email and password are required."
        });
    }

    db.query(
        "SELECT * FROM users WHERE email = ?",
        [email],
        async (err, results) => {

            if (err) {
                return res.status(500).json({
                    success: false,
                    message: "Database error."
                });
            }

            if (results.length === 0) {
                return res.status(401).json({
                    success: false,
                    message: "Invalid email or password."
                });
            }

            const user = results[0];

            const match = await bcrypt.compare(password, user.password);

            if (!match) {
                return res.status(401).json({
                    success: false,
                    message: "Invalid email or password."
                });
            }

            return res.status(200).json({
                success: true,
                message: "Login successful.",
                user: {
                    id: user.id,
                    full_name: user.full_name,
                    email: user.email
                }
            });

        }
    );
};

// ================= UPDATE NAME =================

exports.updateName = (req, res) => {

    const { id, full_name } = req.body;

    if (!id || !full_name) {
        return res.status(400).json({
            success: false,
            message: "User ID and name are required."
        });
    }

    db.query(
        "UPDATE users SET full_name = ? WHERE id = ?",
        [full_name, id],
        (err) => {

            if (err) {
                return res.status(500).json({
                    success: false,
                    message: "Failed to update name."
                });
            }

            return res.json({
                success: true,
                message: "Name updated successfully.",
                user: {
                    id,
                    full_name
                }
            });

        }
    );

};

// ================= UPDATE EMAIL =================

exports.updateEmail = (req, res) => {

    const { id, email } = req.body;

    if (!id || !email) {
        return res.status(400).json({
            success: false,
            message: "User ID and email are required."
        });
    }

    db.query(
        "SELECT * FROM users WHERE email = ? AND id <> ?",
        [email, id],
        (err, results) => {

            if (err) {
                return res.status(500).json({
                    success: false,
                    message: "Database error."
                });
            }

            if (results.length > 0) {
                return res.status(409).json({
                    success: false,
                    message: "Email already exists."
                });
            }

            db.query(
                "UPDATE users SET email = ? WHERE id = ?",
                [email, id],
                (err) => {

                    if (err) {
                        return res.status(500).json({
                            success: false,
                            message: "Failed to update email."
                        });
                    }

                    return res.json({
                        success: true,
                        message: "Email updated successfully.",
                        user: {
                            id,
                            email
                        }
                    });

                }
            );

        }
    );

};

// ================= UPDATE PASSWORD =================

exports.updatePassword = async (req, res) => {

    const { id, currentPassword, newPassword } = req.body;

    if (!id || !currentPassword || !newPassword) {
        return res.status(400).json({
            success: false,
            message: "All fields are required."
        });
    }

    db.query(
        "SELECT * FROM users WHERE id = ?",
        [id],
        async (err, results) => {

            if (err) {
                return res.status(500).json({
                    success: false,
                    message: "Database error."
                });
            }

            if (results.length === 0) {
                return res.status(404).json({
                    success: false,
                    message: "User not found."
                });
            }

            const user = results[0];

            const match = await bcrypt.compare(
                currentPassword,
                user.password
            );

            if (!match) {
                return res.status(401).json({
                    success: false,
                    message: "Current password is incorrect."
                });
            }

            const hashedPassword = await bcrypt.hash(newPassword, 10);

            db.query(
                "UPDATE users SET password = ? WHERE id = ?",
                [hashedPassword, id],
                (err) => {

                    if (err) {
                        return res.status(500).json({
                            success: false,
                            message: "Password update failed."
                        });
                    }

                    return res.json({
                        success: true,
                        message: "Password updated successfully."
                    });

                }
            );

        }
    );

};