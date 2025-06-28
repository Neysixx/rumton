function deleteParticipant(id) {
    fetch(`/color_run_war_exploded/participants/${id}`, {
        method: 'DELETE'
    }).then(response => {
        if (response.ok) {
            window.location.reload();
        } else {
            alert("Erreur lors de la suppression");
        }
    });
}

function updateParticipant(id) {
    const formData = new URLSearchParams();

    formData.append("nom", document.getElementById('nom').value);
    formData.append("prenom", document.getElementById('prenom').value);
    formData.append("role", document.getElementById('role').value);


    fetch(`/color_run_war_exploded/participants/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: formData.toString()
    }).then(response => {
        if (response.ok) {
            window.location.href = `/color_run_war_exploded/participants`;
        } else {
            alert("Erreur lors de la mise à jour");
        }
    }).catch(error => {
        console.error("Erreur réseau :", error);
        alert("Erreur réseau");
    });
}