const {
    launchUbuntuContainer,
    stopContainer
} = require("../docker/dockerService");

// Launch a new Ubuntu lab
exports.launchLab = async (req, res) => {

    try {

        const result = await launchUbuntuContainer();

        if (!result.success) {
            return res.status(500).json(result);
        }

        res.status(200).json({
            success: true,
            message: "Ubuntu Lab Started",
            containerId: result.id
        });

    } catch (err) {

        console.error(err);

        res.status(500).json({
            success: false,
            error: err.message
        });

    }

};

// Stop the Ubuntu lab
exports.stopLab = async (req, res) => {

    try {

        const { containerId } = req.body;

        if (!containerId) {
            return res.status(400).json({
                success: false,
                message: "Container ID is required"
            });
        }

        const result = await stopContainer(containerId);

        res.json(result);

    } catch (err) {

        console.error(err);

        res.status(500).json({
            success: false,
            error: err.message
        });

    }

};