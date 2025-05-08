const inputs = document.querySelectorAll('.otp-input');

inputs.forEach((input, index) => {
input.addEventListener('input', (e) => {
    const value = e.target.value;
    if (value.length === 1 && index < inputs.length - 1) {
    inputs[index + 1].focus();
    }
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
    inputs[i].value = char;
    });
    if (pasteData.length === inputs.length) {
    inputs[inputs.length - 1].focus();
    } else {
    inputs[pasteData.length].focus();
    }
});
  });