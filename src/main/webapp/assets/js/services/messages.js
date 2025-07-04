const contentInput = document.querySelector("#messageContent");
const messageContainer = document.querySelector("#messageContainer");

async function postMessage (courseId) {
    const formData = new URLSearchParams();

    formData.append("contenu", contentInput.value);
    formData.append("courseId", courseId);

    fetch(`/color_run_war_exploded/messages`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: formData.toString()
    }).then(response => {
        console.log(response)
        if (response.ok) {
            return response.json();
        }
        throw new Error('Erreur serveur');
    }).then(data => {
        let article = document.createElement("article");
        article.classList.add("me")
        let em = document.createElement("em");
        em.textContent = data.sender.prenom + " " + data.sender.nom + " " + formatTimestampToDate(data.createdAt);
        let p = document.createElement("p");
        p.textContent = data.message;
        article.appendChild(em);
        article.appendChild(p);
        messageContainer.appendChild(article);
    }).catch(error => {
        console.error("Erreur réseau :", error);
        alert("Erreur réseau");
    });
    contentInput.value = "";
}

function formatTimestampToDate(timestamp) {
    const date = new Date(timestamp);

    return date.getFullYear() + '-' +
        String(date.getMonth() + 1).padStart(2, '0') + '-' +
        String(date.getDate()).padStart(2, '0') + ' ' +
        String(date.getHours()).padStart(2, '0') + ':' +
        String(date.getMinutes()).padStart(2, '0');
}

function formatToMinute(dateStr) {
    const date = new Date(dateStr);
    const formatted = date.getFullYear() + '-' +
        String(date.getMonth() + 1).padStart(2, '0') + '-' +
        String(date.getDate()).padStart(2, '0') + ' ' +
        String(date.getHours()).padStart(2, '0') + ':' +
        String(date.getMinutes()).padStart(2, '0');
    return formatted;
}

// Gestion de la suppression des messages pour l'organisateur
document.addEventListener('DOMContentLoaded', function() {
    const deleteButtons = document.querySelectorAll('.delete-message-btn');
    
    deleteButtons.forEach(button => {
        button.addEventListener('click', function() {
            const messageId = this.getAttribute('data-message-id');
            
            if (messageId) {
                confirmDeleteMessage(messageId);
            }
        });
    });
});

function confirmDeleteMessage(messageId) {
    // Créer une modal de confirmation personnalisée
    const modal = document.createElement('div');
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1000;
    `;
    
    const modalContent = document.createElement('div');
    modalContent.style.cssText = `
        background: white;
        padding: 30px;
        border-radius: 8px;
        max-width: 500px;
        text-align: center;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    `;
    
    modalContent.innerHTML = `
        <h3 style="color: #dc3545; margin-bottom: 20px;">⚠️ Confirmation de suppression</h3>
        <p style="margin-bottom: 15px; color: #333;">
            <strong>Attention !</strong> Vous êtes sur le point de supprimer ce message.
        </p>
        <p style="margin-bottom: 20px; color: #666; font-size: 14px;">
            <strong>Cette action aura les conséquences suivantes :</strong><br>
            • Le message sera définitivement supprimé<br>
            • Toutes les réponses à ce message seront également supprimées<br>
            • Cette action est irréversible
        </p>
        <div style="display: flex; gap: 10px; justify-content: center;">
            <button id="cancelDelete" style="
                background: #6c757d; 
                color: white; 
                border: none; 
                padding: 10px 20px; 
                border-radius: 4px; 
                cursor: pointer;
            ">Annuler</button>
            <button id="confirmDelete" style="
                background: #dc3545; 
                color: white; 
                border: none; 
                padding: 10px 20px; 
                border-radius: 4px; 
                cursor: pointer;
            ">Supprimer définitivement</button>
        </div>
    `;
    
    modal.appendChild(modalContent);
    document.body.appendChild(modal);
    
    // Gestion des événements de la modal
    document.getElementById('cancelDelete').onclick = function() {
        document.body.removeChild(modal);
    };
    
    document.getElementById('confirmDelete').onclick = function() {
        document.body.removeChild(modal);
        deleteMessage(messageId);
    };
    
    // Fermer en cliquant à l'extérieur
    modal.onclick = function(e) {
        if (e.target === modal) {
            document.body.removeChild(modal);
        }
    };
}

function deleteMessage(messageId) {
    // Afficher un indicateur de chargement
    const loadingIndicator = document.createElement('div');
    loadingIndicator.style.cssText = `
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background: white;
        padding: 20px;
        border-radius: 8px;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        z-index: 1001;
    `;
    loadingIndicator.innerHTML = '<p>Suppression en cours...</p>';
    document.body.appendChild(loadingIndicator);
    
    fetch(`/color_run_war_exploded/messages/${messageId}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
        }
    })
    .then(response => {
        document.body.removeChild(loadingIndicator);
        
        if (response.ok) {
            return response.json();
        } else {
            throw new Error('Erreur lors de la suppression');
        }
    })
    .then(data => {
        if (data.success) {
            // Afficher un message de succès
            showSuccessMessage(data.repliesDeleted);
            
            // Recharger la page pour actualiser la liste des messages
            setTimeout(() => {
                window.location.reload();
            }, 2000);
        } else {
            throw new Error('Échec de la suppression');
        }
    })
    .catch(error => {
        console.error('Erreur:', error);
        alert('Erreur lors de la suppression du message. Veuillez réessayer.');
    });
}

function showSuccessMessage(repliesDeleted) {
    const successModal = document.createElement('div');
    successModal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1000;
    `;
    
    const successContent = document.createElement('div');
    successContent.style.cssText = `
        background: white;
        padding: 30px;
        border-radius: 8px;
        max-width: 400px;
        text-align: center;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    `;
    
    let message = '✅ Message supprimé avec succès';
    if (repliesDeleted > 0) {
        message += `<br><small style="color: #666;">${repliesDeleted} réponse(s) également supprimée(s)</small>`;
    }
    
    successContent.innerHTML = `
        <h3 style="color: #28a745; margin-bottom: 15px;">${message}</h3>
        <p style="color: #666;">La page va se recharger automatiquement...</p>
    `;
    
    successModal.appendChild(successContent);
    document.body.appendChild(successModal);
    
    // Supprimer la modal après 2 secondes
    setTimeout(() => {
        if (document.body.contains(successModal)) {
            document.body.removeChild(successModal);
        }
    }, 2000);
}