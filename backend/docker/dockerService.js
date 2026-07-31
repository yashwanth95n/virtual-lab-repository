const Docker = require("dockerode");

const docker = new Docker();

async function launchUbuntuContainer() {

    try {

        const container = await docker.createContainer({

            Image: "ubuntu:22.04",

            Tty: true,

            OpenStdin: true,
            StdinOnce: false,

            AttachStdin: true,
            AttachStdout: true,
            AttachStderr: true,

            Cmd: ["/bin/bash"],

            HostConfig: {
                AutoRemove: false
            }

        });

        await container.start();

        return {
            success: true,
            id: container.id
        };

    } catch (err) {

        console.error(err);

        return {
            success: false,
            error: err.message
        };

    }

}

function getContainer(containerId) {
    return docker.getContainer(containerId);
}

module.exports = {
    launchUbuntuContainer,
    getContainer
};