const btns = document.querySelectorAll('.orgaBtns');
const modal = document.querySelector('#modal');
const modalClose = document.querySelector('.close');

btns.forEach(btn => {
    btn.addEventListener("click", () => {
        modal.style.display = "block";
    });
})

modalClose.addEventListener("click", () => {
    modal.style.display = "none";
});

window.addEventListener("click", (event) => {
    if (event.target === modal) {
      modal.style.display = "none";
    }
});
