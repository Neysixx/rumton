const navBarButton = document.querySelector('#menu-toggle');
const navBarItems = document.querySelector('#nav-links');
const iconMenuBurger = "&#9776;";
const iconClose = "&times;";

function toggleNavBar() {
    navBarItems.classList.toggle('open');
    if(navBarItems.classList.contains('open')){
        navBarButton.innerHTML = iconClose;
    }
    else{
        navBarButton.innerHTML = iconMenuBurger;
    }
}

navBarButton.addEventListener('click', toggleNavBar)