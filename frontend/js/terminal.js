const params = new URLSearchParams(window.location.search);
const containerId = params.get("container");

const socket = io("http://localhost:5000");

const terminal = new Terminal({
    cursorBlink: true,
    theme: {
        background: "#111827"
    }
});

terminal.open(document.getElementById("terminal"));

terminal.writeln("Connecting to Ubuntu container...");
terminal.writeln("");

socket.on("connect", () => {

    socket.emit("start-terminal", containerId);

});

socket.on("output", (data) => {

    terminal.write(data);

});

terminal.onData((data) => {

    socket.emit("input", data);

});

function closeLab() {

    fetch("http://localhost:5000/api/labs/stop", {

        method: "POST",

        headers: {
            "Content-Type": "application/json"
        },

        body: JSON.stringify({
            containerId
        })

    }).finally(() => {

        window.location.href = "labs.html";

    });

}