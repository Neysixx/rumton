const inputs = document.querySelectorAll('.otp-input');
const verificationCode = document.getElementById('verification_code');
const verificationForm = document.getElementById('verification-form');

// Fonction pour mettre à jour le code de vérification
function updateVerificationCode() {
    if (verificationCode) {
        const code = Array.from(inputs).map(input => input.value).join('');
        verificationCode.value = code;
        console.log('Code mis à jour:', code);
    }
}

inputs.forEach((input, index) => {
    input.addEventListener('input', (e) => {
        // Autoriser seulement les chiffres
        const value = e.target.value.replace(/[^0-9]/g, '');
        e.target.value = value;
        
        // Passer au champ suivant automatiquement
        if (value.length === 1 && index < inputs.length - 1) {
            inputs[index + 1].focus();
        }
        
        // Mettre à jour le code complet
        updateVerificationCode();
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Backspace' && !input.value && index > 0) {
            inputs[index - 1].focus();
        }
    });

    input.addEventListener('paste', (e) => {
        e.preventDefault();
        const pasteData = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, inputs.length);
        pasteData.split('').forEach((char, i) => {
            if (i < inputs.length) {
                inputs[i].value = char;
            }
        });
        
        // Mettre à jour le code complet après collage
        updateVerificationCode();
        
        if (pasteData.length === inputs.length) {
            inputs[inputs.length - 1].focus();
        } else if (pasteData.length < inputs.length) {
            inputs[pasteData.length].focus();
        }
    });
});

// Validation avant soumission
if (verificationForm) {
    verificationForm.addEventListener('submit', (e) => {
        updateVerificationCode();
        
        const code = verificationCode.value;
        console.log('Code soumis:', code);
        
        if (code.length !== 6) {
            e.preventDefault();
            alert('Veuillez saisir le code de vérification complet (6 chiffres)');
            return false;
        }
        
        // Laisser la soumission naturelle du formulaire se faire
    });
}

// Gestion du renvoi d'email
const resendEmailLink = document.getElementById('resend-email');
if (resendEmailLink) {
    resendEmailLink.addEventListener('click', function(e) {
        e.preventDefault();
        const email = this.getAttribute('data-email');
        
        if (email) {
            // Désactiver le lien temporairement
            this.style.pointerEvents = 'none';
            this.style.color = '#ccc';
            this.textContent = 'Envoi en cours...';

            console.log('Email:', email);
            console.log('Window location pathname:', window.location.pathname + '/resend');
            
            // Envoyer la requête pour renvoyer l'email
            fetch(window.location.pathname + '/resend', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: 'email=' + encodeURIComponent(email)
            })
            .then(response => {
                console.log('Response:', response);
                if (response.ok) {
                    this.textContent = 'Email renvoyé !';
                    // Réactiver le lien après 30 secondes
                    setTimeout(() => {
                        this.style.pointerEvents = 'auto';
                        this.style.color = '';
                        this.textContent = 'Renvoyer un mail';
                    }, 30000);
                } else {
                    this.textContent = 'Erreur lors de l\'envoi';
                    this.style.pointerEvents = 'auto';
                    this.style.color = '';
                }
            })
            .catch(error => {
                console.error('Erreur:', error);
                this.textContent = 'Erreur lors de l\'envoi';
                this.style.pointerEvents = 'auto';
                this.style.color = '';
            });
        }
    });
}