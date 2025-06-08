document.addEventListener('DOMContentLoaded', () => {
    const removeUserForm = document.getElementById('removeUserForm');
    if (removeUserForm) {
        removeUserForm.addEventListener('submit', async (event) => {
            event.preventDefault(); // Prevent default form submission

            const usernameInput = document.getElementById('removeUserName');
            const username = usernameInput.value.trim();

            if (!username) {
                alert('Please enter a username to remove.');
                return;
            }

            try {
                // URL to match the single server endpoint
                const response = await fetch(`/api/admin/remove-user/${encodeURIComponent(username)}`, {
                    method: 'DELETE',
                });

                const responseText = await response.text(); // Get text response from server

                if (response.ok) { // Status 200-299
                    alert(`Server: ${responseText}`);
                    usernameInput.value = ''; // Clear the input field
                    if (typeof refreshStatus === 'function') {
                        refreshStatus(); // Refresh the user list on the page
                    }
                } else {
                    alert(`Error ${response.status}: ${responseText}`);
                }
            } catch (error) {
                console.error('Failed to send remove user request:', error);
                alert('Failed to send remove user request. See console for details.');
            }
        });
    }
});