const adresseinput = document.getElementById('adresse');
const villeinput = document.getElementById('ville');
const codePostalinput = document.getElementById('codePostal');
const resultsList = document.getElementById('autocomplete-results');
const map = document.getElementById('map');
const mapIframe = document.getElementById('mapIframe');
const lat = document.getElementById('lat');
const long = document.getElementById('long');

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
        return data.features; // on retourne juste le nom d'affichage
    } catch (error) {
        console.error("Erreur lors de la requête :", error);
        return [];
    }
}

async function getCityImage(cityName) {
    const response = await fetch(
        `https://fr.wikipedia.org/w/api.php?action=query&format=json&origin=*&titles=${encodeURIComponent(cityName)}&prop=pageimages&piprop=original`
    );
    const data = await response.json();
    const pages = data.query.pages;
    const firstPage = Object.values(pages)[0];
    return firstPage.original?.source || null;
}

function setMapLocation(lat, lng, zoom = 15) {
    const src = `https://www.google.com/maps?q=${lat},${lng}&hl=fr&z=${zoom}&output=embed`;
    mapIframe.src = src;
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
        li.textContent = item.properties.label;
        li.addEventListener('click', () => {
            adresseinput.value = item.properties.name;
            villeinput.value = item.properties.city;
            codePostalinput.value = item.properties.postcode;
            resultsList.innerHTML = '';
            resultsList.style.display = 'none';
            setMapLocation(item.geometry.coordinates[1], item.geometry.coordinates[0]);
            lat.value = item.geometry.coordinates[1];
            long.value = item.geometry.coordinates[0];
            map.classList.remove("hide")
        });
        resultsList.appendChild(li);
    });

    resultsList.style.display = 'block';
}

// Ajout d'un listener sur l'input
if(adresseinput){
    adresseinput.addEventListener('input', async (e) => {
        const value = e.target.value.trim();
        if (value.length < 3) {
            resultsList.innerHTML = '';
            resultsList.style.display = 'none';
            map.classList.add("hide")
            return;
        }

        const suggestions = await fetchWithParams(value);
        updateAutocompleteList(suggestions);
    });

}
if(resultsList){
    document.addEventListener('click', (e) => {
        if (!adresseinput.contains(e.target) && !resultsList.contains(e.target)) {
            resultsList.innerHTML = '';
            resultsList.style.display = 'none';
        }
    });
}
// Cacher la liste si on clique ailleurs

document.addEventListener("DOMContentLoaded", async () => {
    const section = document.querySelector("#course-desc");
    if(section){
        const img = document.querySelector("#ville-image");
        const loader = document.querySelector("#ville-loader");
        const ville = section.dataset.ville;

        if (!ville) return;

        try {
            loader.style.display = "block";

            const storageKey = `cityImage:${ville}`;
            let imageUrl = localStorage.getItem(storageKey);

            if (!imageUrl) {
                imageUrl = await getCityImage(ville);
                if (imageUrl) {
                    localStorage.setItem(storageKey, imageUrl);
                }
            }

            if (imageUrl) {
                img.src = imageUrl;
                img.alt = `Image de la ville de ${ville}`;
            }
        } catch (error) {
            console.error("Erreur lors de la récupération de l'image de la ville :", error);
        } finally {
            loader.style.display = "none";
        }
    } else {
        const images = document.querySelectorAll(".image[data-ville]");
        for (const element of images) {
            const ville = element.dataset.ville;
            if (!ville) continue;

            const storageKey = `cityImage:${ville}`;
            let imageUrl = localStorage.getItem(storageKey);

            if (!imageUrl) {
                try {
                    imageUrl = await getCityImage(ville);
                    if (imageUrl) localStorage.setItem(storageKey, imageUrl);
                } catch (e) {
                    console.error("Erreur de chargement pour la ville :", ville, e);
                }
            }

            if (imageUrl) {
                element.style.backgroundImage = `url(${imageUrl})`;
            }
        }
    }

});

