const API_URL = "http://localhost:5000/api/auth";

// ---------------- Popup Functions ----------------

function showPopup(message, success = true) {
    const popup = document.getElementById("popup");
    const popupTitle = popup.querySelector("h3");
    const popupMessage = document.getElementById("popupMessage");

    popupTitle.innerText = success ? "Success" : "Error";
    popupTitle.style.color = success ? "#16a34a" : "#dc2626";

    popupMessage.innerText = message;
    popup.style.display = "flex";
}

function closePopup() {
    document.getElementById("popup").style.display = "none";
}

// ---------------- Register ----------------

const registerForm = document.getElementById("registerForm");

if (registerForm) {
    registerForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const full_name = document.getElementById("full_name").value;
        const email = document.getElementById("email").value;
        const password = document.getElementById("password").value;

        try {
            const response = await fetch(`${API_URL}/register`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    full_name,
                    email,
                    password
                })
            });

            const data = await response.json();

            showPopup(data.message, data.success);

            if (data.success) {
                setTimeout(() => {
                    window.location.href = "login.html";
                }, 1500);
            }

        } catch (error) {
            showPopup("Unable to connect to server.", false);
        }
    });
}

// ---------------- Login ----------------

const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const email = document.getElementById("loginEmail").value;
        const password = document.getElementById("loginPassword").value;

        try {
            const response = await fetch(`${API_URL}/login`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    email,
                    password
                })
            });

            const data = await response.json();

            showPopup(data.message, data.success);

            if (data.success) {
                localStorage.setItem("user", JSON.stringify(data.user));

                setTimeout(() => {
                    window.location.href = "home.html";
                }, 1500);
            }

        } catch (error) {
            showPopup("Unable to connect to server.", false);
        }
    });
}