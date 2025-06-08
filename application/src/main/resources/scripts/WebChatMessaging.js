let username = "";
let isLoggedOut = false;

function sendMessage(message) {
    // Check if user is logged out before sending
    if (isLoggedOut) {
        showLoginScreen();
        return;
    }

    fetch('/api/webchat/messages', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ user: username, message: message })
    })
    .then(response => {
        if (response.status === 401 || response.status === 403) {
            // User is no longer authenticated
            handleLogout();
        }
    })
    .catch(error => {
        console.error('Error sending message:', error);
    });
}

function fetchMessages() {
    // Check if user is logged out before fetching
    if (isLoggedOut) {
        return;
    }

    fetch('/api/webchat/messages')
        .then(response => {
            if (response.status === 401 || response.status === 403) {
                // User is no longer authenticated
                handleLogout();
                return;
            }
            return response.json();
        })
        .then(data => {
            if (data) {
                const chatMessages = document.getElementById("chatMessages");
                chatMessages.innerHTML = "";
                data.forEach(msg => {
                    chatMessages.innerHTML += `<div>${msg}</div>`;
                });
                chatMessages.scrollTop = chatMessages.scrollHeight;
            }
        })
        .catch(error => {
            console.error('Error fetching messages:', error);
        });
}

// Add a function to check if user is still logged in
function checkUserStatus() {
    if (isLoggedOut || !username) {
        return;
    }

    fetch('/api/webchat/status', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username })
    })
    .then(response => response.json())
    .then(data => {
        if (!data.loggedIn) {
            handleLogout();
        }
    })
    .catch(error => {
        console.error('Error checking user status:', error);
    });
}

// Function to handle logout
function handleLogout() {
    isLoggedOut = true;
    alert("You have been logged out by an administrator or your session has expired. The page will refresh.");
    
    // Force a page refresh to clear all state
    window.location.reload();
}

function showLoginScreen() {
    document.getElementById("loginOverlay").style.display = "flex";
    document.getElementById("chatSection").style.display = "none";
    // Clear the username fields
    document.getElementById("username").value = "";
    document.getElementById("password").value = "";
    // Reset state
    username = "";
    isLoggedOut = false;
}

document.addEventListener("DOMContentLoaded", function() {
    document.getElementById("chatForm").addEventListener("submit", function(e) {
        e.preventDefault();
        const message = document.getElementById("message").value;
        if (message && !isLoggedOut) {
            sendMessage(message);
            document.getElementById("message").value = "";
        }
    });
});

// Updated login function with password validation
window.login = function() {
    username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    fetch('/api/webchat/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password })
    })
    .then(response => response.json())
    .then(data => {
        if (data.valid) {
            isLoggedOut = false;
            document.getElementById("loginOverlay").style.display = "none";
            document.getElementById("chatSection").style.display = "block";
            
            // Start polling for messages and user status
            setInterval(fetchMessages, 1000);
            setInterval(checkUserStatus, 3000); // Check status every 3 seconds
            fetchMessages();
        } else {
            alert("Invalid username or password. Please try again.");
        }
    })
    .catch(() => {
        alert("Error connecting to server.");
    });
};

window.addEventListener("beforeunload", function () {
    if (username && !isLoggedOut) {
        navigator.sendBeacon(
            "/api/webchat/logout",
            JSON.stringify({ username: username })
        );
    }
});
