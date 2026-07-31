const { getContainer } = require("../docker/dockerService");

module.exports = (io) => {

    io.on("connection", (socket) => {

        let stream = null;

        socket.on("start-terminal", async (containerId) => {

            try {

                const container = getContainer(containerId);

                const exec = await container.exec({

                    Cmd: ["/bin/bash"],

                    AttachStdin: true,
                    AttachStdout: true,
                    AttachStderr: true,

                    Tty: true

                });

                stream = await exec.start({
                    hijack: true,
                    stdin: true
                });

                stream.on("data", (chunk) => {

                    socket.emit("output", chunk.toString("utf8"));

                });

            } catch (err) {

                console.error(err);

                socket.emit(
                    "output",
                    "\r\nUnable to connect to Ubuntu container.\r\n"
                );

            }

        });

        socket.on("input", (data) => {

            if (stream) {
                stream.write(data);
            }

        });

        socket.on("disconnect", () => {

            if (stream) {

                try {
                    stream.end();
                } catch (e) {}

            }

        });

    });

};