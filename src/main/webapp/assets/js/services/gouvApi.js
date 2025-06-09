const adresseinput = document.getElementById('adresse');
const villeinput = document.getElementById('ville');
const codePostalinput = document.getElementById('codePostal');
const resultsList = document.getElementById('autocomplete-results');

/**
 * Fait une requête GET vers l'API Adresse
 * @param {string} query
 * @returns {Promise<Array>} - Liste des résultats
 */
async function fetchWithParams(query) {
    const encodedQuery = encodeURIComponent(query);
    const fullUrl = `https://api-adresse.data.gouv.fr/search/?q=${encodedQuery}`;

    try {
        const response = await fetch(fullUrl);
        if (!response.ok) {
            throw new Error(`Erreur HTTP : ${response.status}`);
        }
        const data = await response.json();
        return data.features.map(f => f.properties); // on retourne juste le nom d'affichage
    } catch (error) {
        console.error("Erreur lors de la requête :", error);
        return [];
    }
}

/**
 * Met à jour la liste d'autocomplétion
 * @param {Array} suggestions
 */
function updateAutocompleteList(suggestions) {
    resultsList.innerHTML = '';
    if (suggestions.length === 0) {
        resultsList.style.display = 'none';
        return;
    }

    suggestions.forEach(item => {
        const li = document.createElement('li');
        li.textContent = item.label;
        li.addEventListener('click', () => {
            adresseinput.value = item.name;
            villeinput.value = item.city;
            codePostalinput.value = item.postcode;
            resultsList.innerHTML = '';
            resultsList.style.display = 'none';
        });
        resultsList.appendChild(li);
    });

    resultsList.style.display = 'block';
}

// Ajout d'un listener sur l'input
adresseinput.addEventListener('input', async (e) => {
    const value = e.target.value.trim();
    if (value.length < 3) {
        resultsList.innerHTML = '';
        resultsList.style.display = 'none';
        return;
    }

    const suggestions = await fetchWithParams(value);
    updateAutocompleteList(suggestions);
});

// Cacher la liste si on clique ailleurs
document.addEventListener('click', (e) => {
    if (!adresseinput.contains(e.target) && !resultsList.contains(e.target)) {
        resultsList.innerHTML = '';
        resultsList.style.display = 'none';
    }
});
