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
        if (response.ok) {
            return response.json();
        }
        throw new Error('Erreur serveur');
    }).then(data => {
        let article = document.createElement("article");
        article.classList.add("me")
        let em = document.createElement("em");
        em.textContent = data.sender.prenom + " " + data.sender.nom;
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