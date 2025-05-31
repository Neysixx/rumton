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