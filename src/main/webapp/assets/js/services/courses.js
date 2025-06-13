function updateCourse(id) {
    const formData = new URLSearchParams();

    formData.append("nom", document.getElementById('nom').value);
    formData.append("adresse", document.getElementById('adresse').value);
    formData.append("ville", document.getElementById('ville').value);
    formData.append("codePostal", document.getElementById('codePostal').value);
    formData.append("description", document.getElementById('description').value);
    formData.append("distance", document.getElementById('distance').value);

    const causeValue = document.getElementById('cause').value;
    if (causeValue !== "null") {
        formData.append("idCause", causeValue);
    }

    formData.append("maxParticipants", document.getElementById('maxParticipants').value);
    formData.append("dateDepart", document.getElementById('date').value);
    formData.append("prixParticipation", document.getElementById('prixParticipation').value);
    formData.append("obstacles", document.getElementById('obstacles').checked); // true ou false

    fetch(`/color_run_war_exploded/courses/${id}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: formData.toString()
    }).then(response => {
        if (response.ok) {
            window.location.href = `/color_run_war_exploded/courses`;
        } else {
            alert("Erreur lors de la mise à jour");
        }
    }).catch(error => {
        console.error("Erreur réseau :", error);
        alert("Erreur réseau");
    });
}