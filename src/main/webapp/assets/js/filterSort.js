const filterButton = document.querySelector("#filter-button");
const sortButton = document.querySelector("#sort-button");
const filterContent = document.querySelector("#filter-content");
const sortContent = document.querySelector("#sort-content");
let filterIsExpanded = false;
let sortIsExpanded = false;

function hide(element){
    element.classList.add('hide')
}
function show(element){
    element.classList.remove('hide')
}
function activate(button){
    button.classList.add('active');
}
function desactivate(button){
    button.classList.remove('active');
}

function displayFilterContent(){
    if(filterIsExpanded){
        hide(filterContent);
        desactivate(filterButton);
    }else{
        show(filterContent);
        activate(filterButton);
        desactivate(sortButton);
        hide(sortContent);
        sortIsExpanded = false;
    }
    filterIsExpanded = !filterIsExpanded;
}

function displaySortContent(){
    if(sortIsExpanded){
        hide(sortContent);
        desactivate(sortButton);
    }else{
        show(sortContent);
        hide(filterContent);
        activate(sortButton);
        desactivate(filterButton);
        filterIsExpanded = false;
    }
    sortIsExpanded = !sortIsExpanded;
}

filterButton.addEventListener('click', displayFilterContent);
sortButton.addEventListener('click', displaySortContent);