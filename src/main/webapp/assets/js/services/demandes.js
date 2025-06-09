const modal = document.querySelector('#modal');
const motivationParagraph = document.querySelector('#motivationParagraph');
const modalClose = document.querySelector('.close');
let demandeId;

function showModal(motivations, id) {
    modal.style.display = "block";
    motivationParagraph.innerHTML = motivations;
    demandeId = id;
}

modalClose.addEventListener("click", () => {
    modal.style.display = "none";
});

window.addEventListener("click", (event) => {
    if (event.target === modal) {
        modal.style.display = "none";
    }
});

function updateDemandeStatus(id, decision) {
    fetch(`/color_run_war_exploded/demandes/${id}?decision=${encodeURIComponent(decision)}`, {
        method: 'PUT'
    }).then(response => {
        if (response.ok) {
            window.location.reload();
        } else {
            alert("Erreur lors de la mise à jour");
        }
    });
}

document.getElementById('btnAccepterModal').addEventListener('click', () => {
    if (demandeId !== null) {
        updateDemandeStatus(demandeId, true);
    }
});

document.getElementById('btnRefuserModal').addEventListener('click', () => {
    if (demandeId !== null) {
        updateDemandeStatus(demandeId, false);
    }
});
