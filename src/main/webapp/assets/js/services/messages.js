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
        const date = new Date(data.createdAt);

        const formatted = date.getFullYear() + '-' +
            String(date.getMonth() + 1).padStart(2, '0') + '-' +
            String(date.getDate()).padStart(2, '0') + ' ' +
            String(date.getHours()).padStart(2, '0') + ':' +
            String(date.getMinutes()).padStart(2, '0') + ':' +
            String(date.getSeconds()).padStart(2, '0') + '.' +
            String(date.getMilliseconds()).padStart(3, '0');
        em.textContent = data.sender.prenom + " " + data.sender.nom + " " + formatted;
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