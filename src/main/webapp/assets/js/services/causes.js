function updateCause(id) {
    const formData = new URLSearchParams();

    formData.append("intitule", document.getElementById('intitule').value);

    fetch(`/color_run_war_exploded/causes/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: formData.toString()
    }).then(response => {
        if (response.ok) {
            window.location.href = `/color_run_war_exploded/causes`;
        } else {
            alert("Erreur lors de la mise à jour");
        }
    }).catch(error => {
        console.error("Erreur réseau :", error);
        alert("Erreur réseau");
    });
}