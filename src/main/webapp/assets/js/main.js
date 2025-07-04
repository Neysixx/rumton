const navBarButton = document.querySelector('#menu-toggle');
const navBarItems = document.querySelector('#nav-links');
const iconMenuBurger = "&#9776;";
const iconClose = "&times;";
const btn = document.getElementById("profileBtn");
const popover = document.getElementById("profilePopover");

function toggleNavBar() {
    navBarItems.classList.toggle('open');
    if(navBarItems.classList.contains('open')){
        navBarButton.innerHTML = iconClose;
    }
    else{
        navBarButton.innerHTML = iconMenuBurger;
    }
}

if(btn){
    btn.addEventListener("click", () => {
        popover.style.display = popover.style.display === "block" ? "none" : "block";
    });
}
document.addEventListener("click", (event) => {
    if (!btn.contains(event.target) && !popover.contains(event.target)) {
        popover.style.display = "none";
    }
});
navBarButton.addEventListener('click', toggleNavBar)